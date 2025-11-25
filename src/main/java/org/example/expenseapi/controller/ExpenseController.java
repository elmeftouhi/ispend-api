package org.example.expenseapi.controller;

import jakarta.validation.Valid;
import org.example.expenseapi.dto.ExpenseCategoryBudgetDto;
import org.example.expenseapi.dto.ExpenseCreateRequest;
import org.example.expenseapi.dto.ExpenseDto;
import org.example.expenseapi.dto.ExpenseUpdateRequest;
import org.example.expenseapi.model.Expense;
import org.example.expenseapi.model.ExpenseStatus;
import org.example.expenseapi.service.ExpenseService;
import org.example.expenseapi.service.ExpenseCategoryService;
import org.example.expenseapi.service.ExpenseStatusService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Sort;
import org.example.expenseapi.service.ExpenseBudgetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

@RestController
@RequestMapping("/v1/expenses")
public class ExpenseController {

    private final ExpenseService service;
    private final ExpenseCategoryService categoryService;
    private final ExpenseStatusService statusService;
    private final ExpenseBudgetService budgetService;
    private static final Logger log = LoggerFactory.getLogger(ExpenseController.class);

    public ExpenseController(ExpenseService service, ExpenseCategoryService categoryService, ExpenseStatusService statusService, org.example.expenseapi.service.ExpenseBudgetService budgetService) {
        this.service = service;
        this.categoryService = categoryService;
        this.statusService = statusService;
        this.budgetService = budgetService;
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody ExpenseCreateRequest req) {
        // validate referenced ids existence
        var catOpt = categoryService.findById(req.getExpenseCategoryId());
        if (catOpt.isEmpty()) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Category not found"));

        // Budget-aware check: if a budget exists for the category/month, compute spent+amount and if it would exceed
        // the budget and the budget does NOT allow overspend, return 400 with details; otherwise continue.
        try {
            YearMonth ym = YearMonth.from(req.getExpenseDate());
            var budgetOpt = budgetService.findBudgetForCategoryMonth(catOpt.get().getId(), ym.getYear(), ym.getMonthValue());
            if (budgetOpt.isPresent()) {
                var budget = budgetOpt.get();
                BigDecimal currentSpent = budgetService.getBudgetStatus(catOpt.get().getId(), ym.getYear(), ym.getMonthValue()).getSpent();
                BigDecimal budgetAmount = budget.getBudget();
                BigDecimal newTotal = currentSpent.add(req.getAmount() == null ? BigDecimal.ZERO : req.getAmount());
                if (newTotal.compareTo(budgetAmount) > 0) {
                    // Over budget - check allowOverspend flag on the budget entity
                    Boolean allow = budget.getAllowOverspend();
                    if (allow == null) allow = Boolean.TRUE; // default
                    if (!allow) {
                        Map<String, Object> details = new HashMap<>();
                        details.put("error", "Expense would exceed monthly budget for category");
                        details.put("categoryId", catOpt.get().getId());
                        details.put("year", ym.getYear());
                        details.put("month", ym.getMonthValue());
                        details.put("budget", budgetAmount);
                        details.put("spent", currentSpent);
                        details.put("attemptedTotal", newTotal);
                        details.put("allowOverspend", false);
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(details);
                    }
                }
            }
        } catch (Exception ex) {
            // don't fail the request just because budget check failed; log and proceed with the existing isWithinBudget check for safety
            log.warn("Budget check failed for category id={} date={} amount={} - {}", req.getExpenseCategoryId(), req.getExpenseDate(), req.getAmount(), ex.getMessage());
        }

        // fallback simple budget check: prevent creating an expense that would exceed the monthly budget for the category
        boolean within = budgetService.isWithinBudgetForNewExpense(catOpt.get().getId(), req.getExpenseDate(), req.getAmount());
        if (!within) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Expense would exceed monthly budget for category"));
        }

        // Determine status: use provided id or fallback to default
        ExpenseStatus statusToUse;
        if (req.getExpenseStatusId() != null) {
            var stOpt = statusService.findById(req.getExpenseStatusId());
            if (stOpt.isEmpty()) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Status not found"));
            statusToUse = stOpt.get();
        } else {
            var defaultOpt = statusService.findDefaultStatus();
            if (defaultOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "No default ExpenseStatus configured"));
            }
            statusToUse = defaultOpt.get();
        }

        Expense e = new Expense();
        e.setExpenseDate(req.getExpenseDate());
        e.setDesignation(req.getDesignation());
        e.setExpenseCategory(catOpt.get());
        e.setExpenseStatus(statusToUse);
        e.setAmount(req.getAmount());

        Expense created = service.createExpense(e);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        var opt = service.findById(id);
        if (opt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Expense not found"));
        return ResponseEntity.ok(toDto(opt.get()));
    }

    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(name = "page", required = false, defaultValue = "1") int page,
            @RequestParam(name = "size", required = false, defaultValue = "20") int size,
            @RequestParam(name = "sort", required = false, defaultValue = "expenseDate,desc") String sort,
            // search params
            @RequestParam(name = "keyword", required = false, defaultValue = "") String keyword,
            @RequestParam(name = "categoryId", required = false) Long categoryId,
            @RequestParam(name = "categoryIds", required = false) String categoryIdsCsv,
            @RequestParam(name = "startDate", required = false) String startDateStr,
            @RequestParam(name = "endDate", required = false) String endDateStr
    ) {
        // parse sort param (e.g. "expenseDate,desc" or "amount,asc")
        Sort sortObj = Sort.by(Sort.Direction.DESC, "expenseDate");
        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",");
            String prop = parts[0];
            Sort.Direction dir = Sort.Direction.DESC;
            if (parts.length > 1) {
                try { dir = Sort.Direction.fromString(parts[1]); } catch (IllegalArgumentException ignored) {}
            }
            sortObj = Sort.by(dir, prop);
        }

        // convert 1-based page (API) -> 0-based page (Spring Data)
        int zeroBasedPage = Math.max(0, page - 1);
        var pageable = PageRequest.of(zeroBasedPage, size, sortObj);

        // parse date range
        LocalDate startDate = null; LocalDate endDate = null;
        try {
            if (startDateStr != null && !startDateStr.isBlank()) startDate = LocalDate.parse(startDateStr);
            if (endDateStr != null && !endDateStr.isBlank()) endDate = LocalDate.parse(endDateStr);
        } catch (Exception ex) {
            log.warn("Invalid date parameter(s): startDate='{}', endDate='{}' - {}", startDateStr, endDateStr, ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Invalid date format for startDate/endDate, expected yyyy-MM-dd"));
        }

        // Defaults: if startDate not provided -> first day of current month; if endDate not provided -> today
        if (startDate == null) {
            YearMonth ym = YearMonth.now();
            startDate = ym.atDay(1);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        // Ensure startDate <= endDate; if not, swap them
        if (startDate.isAfter(endDate)) {
            LocalDate tmp = startDate;
            startDate = endDate;
            endDate = tmp;
        }

        // parse category ids: either single categoryId or CSV list
        List<Long> categoryIds = null;
        if (categoryIdsCsv != null && !categoryIdsCsv.isBlank()) {
            String[] parts = categoryIdsCsv.split(",");
            categoryIds = new ArrayList<>();
            for (String p : parts) {
                try { categoryIds.add(Long.parseLong(p.trim())); } catch (NumberFormatException ignored) {}
            }
        }
        if (categoryId != null) {
            if (categoryIds == null) categoryIds = new ArrayList<>();
            categoryIds.add(categoryId);
        }

        // validate category existence before calling service
        if (categoryIds != null && !categoryIds.isEmpty()) {
            for (Long cid : categoryIds) {
                if (categoryService.findById(cid).isEmpty()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Category not found: " + cid));
                }
            }
        }

        // Log effective search parameters for easier debugging
        log.info("Listing expenses - keyword='{}' categoryIds={} startDate={} endDate={} requestedPage={} size={} sort={}",
                keyword, categoryIds, startDate, endDate, page, size, sortObj);

        try {
            var pageResult = service.search(keyword, categoryIds, startDate, endDate, pageable);
            var response = org.example.expenseapi.util.PaginationUtils.toPaginatedResponse(pageResult, this::toDto);
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Failed to list expenses with params keyword='{}' categoryIds={} startDate={} endDate={} - {}",
                    keyword, categoryIds, startDate, endDate, ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to list expenses"));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody ExpenseUpdateRequest req) {
        var opt = service.findById(id);
        if (opt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Expense not found"));

        // validate amount only if provided in update request
        if (req.getAmount() != null) {
            ResponseEntity<?> amountValidation = validateAmountForUpdate(req.getAmount());
            if (amountValidation != null) return amountValidation;
        }

        Expense existingExpense = new Expense();
        existingExpense.setExpenseDate(req.getExpenseDate());
        existingExpense.setDesignation(req.getDesignation());
        existingExpense.setAmount(req.getAmount());
        if (req.getExpenseCategoryId() != null) {
            var catOpt = categoryService.findById(req.getExpenseCategoryId());
            if (catOpt.isEmpty()) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Category not found"));
            existingExpense.setExpenseCategory(catOpt.get());
        }

        if (req.getExpenseStatusId() != null) {
            var stOpt = statusService.findById(req.getExpenseStatusId());
            if (stOpt.isEmpty()) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Status not found"));
            existingExpense.setExpenseStatus(stOpt.get());
        }

        Expense saved = service.updateExpense(id, existingExpense);
        return ResponseEntity.ok(toDto(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        var opt = service.findById(id);
        if (opt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Expense not found"));
        service.deleteExpense(id);
        return ResponseEntity.noContent().build();
    }

    // New endpoint: GET reports grouped by category -> year -> month
    @GetMapping("/reports")
    public ResponseEntity<?> getExpensesReport(
            @RequestParam(name = "categoryId", required = false) Long categoryId,
            @RequestParam(name = "year", required = false) Integer year
    ) {
        // Determine date range from optional year parameter. If year is null we'll let the service use full range.
        LocalDate startDate = null;
        LocalDate endDate = null;
        try {
            if (year != null) {
                startDate = LocalDate.of(year, 1, 1);
                endDate = LocalDate.of(year, 12, 31);
            }
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Invalid year parameter"));
        }

        // Validate single categoryId if provided
        java.util.List<Long> categoryIds = null;
        if (categoryId != null) {
            if (categoryService.findById(categoryId).isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Category not found: " + categoryId));
            }
            categoryIds = java.util.List.of(categoryId);
        }

        try {
            var reports = service.getExpensesReportByCategory(startDate, endDate, categoryIds);
            return ResponseEntity.ok(reports);
        } catch (Exception ex) {
            log.error("Failed to build expense report: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to generate report"));
        }
    }

    private ExpenseDto toDto(Expense e) {
        if (e == null) return null;
        ExpenseDto dto = new ExpenseDto();
        dto.setId(e.getId());
        dto.setExpenseDate(e.getExpenseDate());
        dto.setDesignation(e.getDesignation());
        dto.setExpenseCategoryId(e.getExpenseCategory() != null ? e.getExpenseCategory().getId() : null);
        dto.setExpenseStatusId(e.getExpenseStatus() != null ? e.getExpenseStatus().getId() : null);
        dto.setAmount(e.getAmount());
        dto.setCreatedAt(e.getCreatedAt());
        dto.setCreatedBy(e.getCreatedBy());
        dto.setUpdatedAt(e.getUpdatedAt());
        dto.setUpdatedBy(e.getUpdatedBy());

        // populate nested DTOs
        if (e.getExpenseCategory() != null) {
            var c = new org.example.expenseapi.dto.ExpenseCategoryDto();
            c.setId(e.getExpenseCategory().getId());
            c.setName(e.getExpenseCategory().getName());
            c.setStatus(e.getExpenseCategory().getStatus());
            c.setLevel(e.getExpenseCategory().getLevel());
            // populate budgets for category (use budgetService)
            java.util.List<ExpenseCategoryBudgetDto> bdto = new java.util.ArrayList<>();
            try {
                Long catId = e.getExpenseCategory().getId();
                if (catId != null && budgetService != null) {
                    var budgets = budgetService.findBudgetsForCategory(catId);
                    if (budgets != null) {
                        for (var b : budgets) {
                            if (b == null) continue;
                            var eb = new ExpenseCategoryBudgetDto();
                            eb.setYear(b.getYear()); eb.setMonth(b.getMonth()); eb.setBudget(b.getBudget());
                            // map allowOverspend as provided by the entity (entity getter returns a default if null)
                            eb.setAllowOverspend(b.getAllowOverspend());
                            bdto.add(eb);
                        }
                    }
                }
            } catch (Exception ex) {
                // Defensive: log and continue without budgets rather than failing the whole request
                log.warn("Failed to load budgets for expense category id={}: {}", e.getExpenseCategory().getId(), ex.getMessage());
            }
            c.setBudgets(bdto);
            c.setParentId(e.getExpenseCategory().getParent() != null ? e.getExpenseCategory().getParent().getId() : null);
            c.setCreatedAt(e.getExpenseCategory().getCreatedAt());
            c.setCreatedBy(e.getExpenseCategory().getCreatedBy());
            c.setUpdatedAt(e.getExpenseCategory().getUpdatedAt());
            c.setUpdatedBy(e.getExpenseCategory().getUpdatedBy());
            // ensure subCategories are not included when returning an Expense
            c.setSubCategories(null);
            dto.setExpenseCategory(c);
        }

        if (e.getExpenseStatus() != null) {
            var s = new org.example.expenseapi.dto.ExpenseStatusDto();
            s.setId(e.getExpenseStatus().getId());
            s.setName(e.getExpenseStatus().getName());
            s.setIsDefault(e.getExpenseStatus().getIsDefault());
            s.setCreatedAt(e.getExpenseStatus().getCreatedAt());
            s.setCreatedBy(e.getExpenseStatus().getCreatedBy());
            s.setUpdatedAt(e.getExpenseStatus().getUpdatedAt());
            s.setUpdatedBy(e.getExpenseStatus().getUpdatedBy());
            dto.setExpenseStatus(s);
        }

        return dto;
    }

    // Validation helpers for amount

    private ResponseEntity<?> validateAmountForUpdate(BigDecimal amount) {
        // amount is optional for updates, but if provided it must be valid
        if (amount == null) return null;

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "amount must be greater than 0"));
        }

        // allow at most 2 fractional digits
        int scale = Math.max(0, amount.stripTrailingZeros().scale());
        if (scale > 2) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "amount must have at most 2 decimal places"));
        }

        return null;
    }
}
