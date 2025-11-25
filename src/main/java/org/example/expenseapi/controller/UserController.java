package org.example.expenseapi.controller;

import org.example.expenseapi.dto.UserCreateRequest;
import org.example.expenseapi.dto.UserDto;
import org.example.expenseapi.dto.UserUpdateRequest;
import org.example.expenseapi.model.User;
import org.example.expenseapi.model.UserStatus;
import org.example.expenseapi.application.service.UserApplicationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/users")
public class UserController {

    private final UserApplicationService userService;

    public UserController(UserApplicationService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody UserCreateRequest request) {
        // Return friendly error if the email is already used
        if (request.getEmail() != null && userService.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Email already in use"));
        }

        User user = new User();
        user.setFirstname(request.getFirstname());
        user.setLastname(request.getLastname());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        user.setStatus(request.getStatus());

        User created = userService.createUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUser(@PathVariable Long id) {
        var opt = userService.findById(id);
        if (opt.isPresent()) {
            return ResponseEntity.ok(toDto(opt.get()));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "User not found"));
        }
    }

    @GetMapping
    public List<UserDto> listUsers() {
        return userService.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody UserUpdateRequest request) {
        var opt = userService.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
        }

        User user = new User();
        user.setFirstname(request.getFirstname());
        user.setLastname(request.getLastname());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        user.setStatus(request.getStatus());

        User updated = userService.updateUser(id, user);
        return ResponseEntity.ok(toDto(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        var opt = userService.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
        }
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> toggleStatus(@PathVariable Long id) {
        var opt = userService.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
        }

        User existing = opt.get();
        UserStatus current = existing.getStatus();
        UserStatus newStatus = (current == UserStatus.ACTIVE)
                ? UserStatus.INACTIVE
                : UserStatus.ACTIVE;
        // Only set the status field so updateUser won't modify or re-encode the password
        User update = new User();
        update.setStatus(newStatus);

        User updated = userService.updateUser(id, update);
        return ResponseEntity.ok(toDto(updated));
    }

    private UserDto toDto(User u) {
        UserDto dto = new UserDto();
        dto.setId(u.getId());
        dto.setFirstname(u.getFirstname());
        dto.setLastname(u.getLastname());
        dto.setEmail(u.getEmail());
        dto.setStatus(u.getStatus());
        // Map audit fields from BasicEntity
        dto.setCreatedAt(u.getCreatedAt());
        dto.setCreatedBy(u.getCreatedBy());
        dto.setUpdatedAt(u.getUpdatedAt());
        dto.setUpdatedBy(u.getUpdatedBy());
        return dto;
    }
}
