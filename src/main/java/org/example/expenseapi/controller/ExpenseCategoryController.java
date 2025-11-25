package org.example.expenseapi.controller;

import org.example.expenseapi.application.service.ExpenseCategoryApplicationService;
import org.example.expenseapi.dto.ExpenseCategoryCreateRequest;
import org.example.expenseapi.dto.ExpenseCategoryDto;
import org.example.expenseapi.dto.ExpenseCategoryUpdateRequest;
import org.example.expenseapi.model.ExpenseCategory;
import org.example.expenseapi.model.ExpenseCategoryStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/v1/expense-categories")
public class ExpenseCategoryController {

    private final ExpenseCategoryApplicationService service;
    private final org.example.expenseapi.service.ExpenseBudgetService budgetService;
    private static final Logger log = LoggerFactory.getLogger(ExpenseCategoryController.class);

    public ExpenseCategoryController(ExpenseCategoryApplicationService service, org.example.expenseapi.service.ExpenseBudgetService budgetService) {
        this.service = service;
        this.budgetService = budgetService;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody ExpenseCategoryCreateRequest req) {
        log.info("Create expense category request received: name={}", req.getName());
        // check name conflict
        if (req.getName() != null && service.findByName(req.getName()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Category name already in use"));
        }

        ExpenseCategory c = new ExpenseCategory();
        c.setName(req.getName());
        c.setStatus(req.getStatus() != null ? req.getStatus() : ExpenseCategoryStatus.ACTIVE);
        c.setLevel(req.getLevel());
        if (req.getParentId() != null) {
            var parentOpt = service.findById(req.getParentId());
            if (parentOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Parent category not found"));
            }
            c.setParent(parentOpt.get());
        }

        ExpenseCategory created = service.create(c);
        log.debug("Created expense category: id={} name={}", created.getId(), created.getName());
        var ymCreated = java.time.YearMonth.now();
        var statusMapCreated = budgetService.getBudgetStatusForCategories(java.util.List.of(created.getId()), ymCreated.getYear(), ymCreated.getMonthValue());
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(created, statusMapCreated));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        log.info("Get expense category request: id={}", id);
        var existingCategory = service.findById(id);
        if (existingCategory.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Category not found"));
        }

        ExpenseCategory current = existingCategory.get();

        log.info("Getting subcategories for category id={}", current.getId());
        List<ExpenseCategory> allCategories = service.findAll();
        List<ExpenseCategory> subsEntities = allCategories.stream()
                .filter(ch -> ch.getParent() != null && ch.getParent().getId() != null && ch.getParent().getId().equals(current.getId()))
                .toList();
        List<Long> idsToCheck = new ArrayList<>();
        idsToCheck.add(current.getId());
        for (var s : subsEntities) idsToCheck.add(s.getId());

        var ym = java.time.YearMonth.now();
        var statusMap = budgetService.getBudgetStatusForCategories(idsToCheck, ym.getYear(), ym.getMonthValue());

        ExpenseCategoryDto dto = toDto(current, statusMap);
        List<ExpenseCategoryDto> subs = subsEntities.stream()
                .map(e -> toDto(e, statusMap))
                .collect(Collectors.toList());
        log.debug("Returning category id={} with {} subcategories", current.getId(), subs.size());
        dto.setSubCategories(subs);
        return ResponseEntity.ok(dto);
    }

    @GetMapping
    public List<ExpenseCategoryDto> list() {
        log.info("List expense categories request");
        // Return only top-level parent categories and include their direct children as `subCategories`.
        List<ExpenseCategory> all = service.findAll();
        // prepare list of all ids we will need to check budgets for (parents + their children)
        List<Long> ids = all.stream().map(ExpenseCategory::getId).collect(Collectors.toList());
        // also include children ids
        for (var c : all) {
            if (c.getParent() != null && c.getParent().getId() != null) ids.add(c.getId());
        }
        // dedupe ids
        ids = new java.util.ArrayList<>(new java.util.LinkedHashSet<>(ids));

        var ym = java.time.YearMonth.now();
        var statusMap = budgetService.getBudgetStatusForCategories(ids, ym.getYear(), ym.getMonthValue());

        var result = all.stream()
                .filter(c -> c.getParent() == null)
                .map(parent -> {
                    ExpenseCategoryDto dto = toDto(parent, statusMap);
                    List<ExpenseCategoryDto> subs = all.stream()
                            .filter(ch -> ch.getParent() != null && ch.getParent().getId() != null && ch.getParent().getId().equals(parent.getId()))
                            .map(ch -> toDto(ch, statusMap))
                            .collect(Collectors.toList());
                    dto.setSubCategories(subs);
                    return dto;
                })
                .collect(Collectors.toList());
        log.debug("Returning {} top-level categories", result.size());
        return result;
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody ExpenseCategoryUpdateRequest req) {
        log.info("Update expense category request: id={} name={}", id, req.getName());
        var opt = service.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Category not found"));
        }
        ExpenseCategory update = new ExpenseCategory();
        update.setName(req.getName());
        update.setStatus(req.getStatus());
        update.setLevel(req.getLevel());
        if (req.getParentId() != null) {
            var parentOpt = service.findById(req.getParentId());
            if (parentOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Parent category not found"));
            }
            update.setParent(parentOpt.get());
        }

        ExpenseCategory saved = service.update(id, update);

        // process optional budget updates included in the update request
        if (req.getBudgets() != null && !req.getBudgets().isEmpty()) {
            for (var b : req.getBudgets()) {
                try {
                    // require year/month to locate budget
                    if (b.getYear() == null || b.getMonth() == null) continue;
                    // find existing budget entity
                    var optBudget = budgetService.findBudgetForCategoryMonth(saved.getId(), b.getYear(), b.getMonth());
                    if (optBudget.isPresent()) {
                        var existingBudget = optBudget.get();
                        // if allowOverspend provided, update it
                        if (b.getAllowOverspend() != null) {
                            budgetService.setAllowOverspendForBudget(existingBudget.getId(), b.getAllowOverspend());
                        }
                    } else {
                        // if no existing budget and allowOverspend provided along with a budget value, create the budget then set flag
                        if (b.getBudget() != null) {
                            var created = budgetService.setBudget(saved.getId(), b.getYear(), b.getMonth(), b.getBudget());
                            if (b.getAllowOverspend() != null) {
                                budgetService.setAllowOverspendForBudget(created.getId(), b.getAllowOverspend());
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }

        var ymSaved = java.time.YearMonth.now();
        var statusMapSaved = budgetService.getBudgetStatusForCategories(java.util.List.of(saved.getId()), ymSaved.getYear(), ymSaved.getMonthValue());
        log.debug("Updated category id={}", saved.getId());
        return ResponseEntity.ok(toDto(saved, statusMapSaved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        var opt = service.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Category not found"));
        }
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> toggleStatus(@PathVariable Long id) {
        log.info("Toggle status request for category id={}", id);
        var opt = service.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Category not found"));
        }
        ExpenseCategory existing = opt.get();
        ExpenseCategoryStatus current = existing.getStatus();
        ExpenseCategoryStatus newStatus = (current == ExpenseCategoryStatus.ACTIVE) ? ExpenseCategoryStatus.INACTIVE : ExpenseCategoryStatus.ACTIVE;
        ExpenseCategory update = new ExpenseCategory();
        update.setStatus(newStatus);
        ExpenseCategory saved = service.update(id, update);
        var statusMapToggled = budgetService.getBudgetStatusForCategories(java.util.List.of(saved.getId()), java.time.YearMonth.now().getYear(), java.time.YearMonth.now().getMonthValue());
        log.debug("Toggled status for category id={} newStatus={}", saved.getId(), saved.getStatus());
        return ResponseEntity.ok(toDto(saved, statusMapToggled));
    }

    @GetMapping("/{id}/budget-status")
    public ResponseEntity<?> getBudgetStatus(@PathVariable Long id) {
        log.info("Budget status request for category id={}", id);
        var opt = service.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Category not found"));
        }
        var ym = java.time.YearMonth.now();
        // use single-item batch to keep consistent behavior
        var map = budgetService.getBudgetStatusForCategories(java.util.List.of(id), ym.getYear(), ym.getMonthValue());
        var status = map.get(id);
        // return whatever the service produced; if no budget exists, status will be present but with budget==null
        log.debug("Budget status for id={} -> {}", id, status);
        return ResponseEntity.ok(status);
    }

    private ExpenseCategoryDto toDto(ExpenseCategory c, java.util.Map<Long, org.example.expenseapi.dto.BudgetStatus> statusMap) {
        ExpenseCategoryDto dto = new ExpenseCategoryDto();
        dto.setId(c.getId());
        dto.setName(c.getName());
        dto.setStatus(c.getStatus());
        dto.setLevel(c.getLevel());

        java.util.List<org.example.expenseapi.dto.ExpenseCategoryBudgetDto> bdto = new java.util.ArrayList<>();
        var budgets = budgetService.findBudgetsForCategory(c.getId());
        for (var b : budgets) {
            var eb = new org.example.expenseapi.dto.ExpenseCategoryBudgetDto();
            eb.setYear(b.getYear()); eb.setMonth(b.getMonth()); eb.setBudget(b.getBudget());
            // map allowOverspend so the DTO reflects entity value (entity getter returns default if null)
            eb.setAllowOverspend(b.getAllowOverspend());
            bdto.add(eb);
        }
        dto.setBudgets(bdto);
        dto.setParentId(c.getParent() != null ? c.getParent().getId() : null);
        dto.setCreatedAt(c.getCreatedAt());
        dto.setCreatedBy(c.getCreatedBy());
        dto.setUpdatedAt(c.getUpdatedAt());
        dto.setUpdatedBy(c.getUpdatedBy());
        // Populate budgetStatus only if the service returned a budget entry (budget != null)
        try {
            var st = statusMap == null ? null : statusMap.get(c.getId());
            if (st != null && st.getBudget() != null) {
                dto.setBudgetStatus(st);
            } else {
                dto.setBudgetStatus(null);
            }
        } catch (Exception ignored) {
            dto.setBudgetStatus(null);
        }
        return dto;
    }
}
