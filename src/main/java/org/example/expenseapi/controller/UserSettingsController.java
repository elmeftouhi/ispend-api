package org.example.expenseapi.controller;

import org.example.expenseapi.application.service.UserSettingsApplicationService;
import org.example.expenseapi.dto.UserSettingsDto;
import org.example.expenseapi.dto.UserSettingsUpdateRequest;
import org.example.expenseapi.model.UserSettings;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.util.Map;

@RestController
@RequestMapping("/v1/users/{userId}/settings")
public class UserSettingsController {

    private final UserSettingsApplicationService service;

    public UserSettingsController(UserSettingsApplicationService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<?> getSettings(@PathVariable Long userId) {
        var opt = service.findByUserId(userId);
        if (opt.isPresent()) {
            return ResponseEntity.ok(toDto(opt.get()));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Settings not found"));
        }
    }

    @PutMapping
    public ResponseEntity<?> updateSettings(@PathVariable Long userId, @RequestBody UserSettingsUpdateRequest request) {
        // Basic validation: currency non-null and length <=10, decimalDigits non-negative, weekStart reasonable
        if (request.getCurrency() != null && request.getCurrency().length() > 10) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "currency too long"));
        }
        if (request.getDecimalDigits() != null && request.getDecimalDigits() < 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "decimalDigits must be >= 0"));
        }

        String normalizedWeekStart = null;
        if (request.getWeekStart() != null) {
            try {
                // Accept e.g. "monday", "MONDAY", "Monday" — map to enum name (MONDAY)
                DayOfWeek dow = DayOfWeek.valueOf(request.getWeekStart().trim().toUpperCase());
                normalizedWeekStart = dow.name();
            } catch (IllegalArgumentException ex) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "weekStart must be a day name (e.g. MONDAY)"));
            }
        }

        // validate currencySymbolPlacement
        String placement = null;
        if (request.getCurrencySymbolPlacement() != null) {
            String p = request.getCurrencySymbolPlacement().trim().toUpperCase();
            if (!p.equals("BEFORE") && !p.equals("AFTER")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "currencySymbolPlacement must be BEFORE or AFTER"));
            }
            placement = p;
        }

        UserSettings s = new UserSettings();
        s.setCurrency(request.getCurrency());
        s.setDecimalDigits(request.getDecimalDigits());
        s.setWeekStart(normalizedWeekStart);
        s.setCurrencySymbolPlacement(placement);

        UserSettings saved = service.createOrUpdate(userId, s);
        return ResponseEntity.ok(toDto(saved));
    }

    private UserSettingsDto toDto(UserSettings s) {
        UserSettingsDto dto = new UserSettingsDto();
        dto.setId(s.getId());
        dto.setUserId(s.getUser().getId());
        dto.setCurrency(s.getCurrency());
        dto.setDecimalDigits(s.getDecimalDigits());
        dto.setWeekStart(s.getWeekStart());
        dto.setCurrencySymbolPlacement(s.getCurrencySymbolPlacement());
        return dto;
    }
}
