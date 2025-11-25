package org.example.expenseapi.controller;

import org.example.expenseapi.dto.ExpenseStatusCreateRequest;
import org.example.expenseapi.dto.ExpenseStatusDto;
import org.example.expenseapi.dto.ExpenseStatusUpdateRequest;
import org.example.expenseapi.model.ExpenseStatus;
import org.example.expenseapi.service.ExpenseStatusService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/expense-statuses")
public class ExpenseStatusController {

    private final ExpenseStatusService service;

    public ExpenseStatusController(ExpenseStatusService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody ExpenseStatusCreateRequest req) {
        if (req.getName() != null && service.findByName(req.getName()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Status name already in use"));
        }

        ExpenseStatus s = new ExpenseStatus();
        s.setName(req.getName());
        s.setIsDefault(req.getIsDefault() != null ? req.getIsDefault() : false);

        ExpenseStatus created = service.createExpenseStatus(s);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        var opt = service.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Status not found"));
        }
        return ResponseEntity.ok(toDto(opt.get()));
    }

    @GetMapping
    public List<ExpenseStatusDto> list() {
        return service.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody ExpenseStatusUpdateRequest req) {
        var opt = service.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Status not found"));
        }

        // If the client is trying to change the name, ensure no other entity already uses it
        if (req.getName() != null) {
            var byName = service.findByName(req.getName());
            if (byName.isPresent() && !byName.get().getId().equals(id)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Status name already in use"));
            }
        }

        ExpenseStatus update = new ExpenseStatus();
        update.setName(req.getName());
        update.setIsDefault(req.getIsDefault());

        ExpenseStatus saved = service.updateExpenseStatus(id, update);
        return ResponseEntity.ok(toDto(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        var opt = service.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Status not found"));
        }
        service.deleteExpenseStatus(id);
        return ResponseEntity.noContent().build();
    }

    private ExpenseStatusDto toDto(ExpenseStatus s) {
        ExpenseStatusDto dto = new ExpenseStatusDto();
        dto.setId(s.getId());
        dto.setName(s.getName());
        dto.setIsDefault(s.getIsDefault());
        dto.setCreatedAt(s.getCreatedAt());
        dto.setCreatedBy(s.getCreatedBy());
        dto.setUpdatedAt(s.getUpdatedAt());
        dto.setUpdatedBy(s.getUpdatedBy());
        return dto;
    }
}
