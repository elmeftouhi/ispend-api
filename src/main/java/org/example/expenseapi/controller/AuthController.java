package org.example.expenseapi.controller;

import jakarta.annotation.PostConstruct;
import org.example.expenseapi.dto.AuthRequest;
import org.example.expenseapi.dto.AuthResponse;
import org.example.expenseapi.dto.UserCreateRequest;
import org.example.expenseapi.security.JwtUtil;
import org.example.expenseapi.security.JwtBlacklistService;
import org.example.expenseapi.service.UserService;
import org.example.expenseapi.model.User;
import org.example.expenseapi.model.UserStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.example.expenseapi.dto.UserDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/v1/auth")
public class AuthController {

    private final AuthenticationConfiguration authenticationConfiguration;
    private AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final JwtBlacklistService jwtBlacklistService;
    private final UserService userService;

    @Autowired
    public AuthController(AuthenticationConfiguration authenticationConfiguration, JwtUtil jwtUtil, JwtBlacklistService jwtBlacklistService, UserService userService) {
        this.authenticationConfiguration = authenticationConfiguration;
        this.jwtUtil = jwtUtil;
        this.jwtBlacklistService = jwtBlacklistService;
        this.userService = userService;
    }

    // Backwards-compatible constructor used by tests or other callers that don't supply UserService
    public AuthController(AuthenticationConfiguration authenticationConfiguration, JwtUtil jwtUtil, JwtBlacklistService jwtBlacklistService) {
        this(authenticationConfiguration, jwtUtil, jwtBlacklistService, null);
    }

    @PostConstruct
    public void init() {
        try {
            this.authenticationManager = authenticationConfiguration.getAuthenticationManager();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            // After successful authentication, check the user's status if UserService is available
            if (this.userService != null) {
                Optional<User> userOpt = userService.findByEmail(request.getEmail());
                if (userOpt.isPresent() && userOpt.get().getStatus() == UserStatus.INACTIVE) {
                    Map<String, String> body = Map.of("error", "User account is inactive");
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
                }
            }

            String token = jwtUtil.generateToken(auth.getName());

            // Register the issued token for the user so it can be revoked later
            if (jwtBlacklistService != null) {
                long exp = jwtUtil.getExpirationMillis(token);
                jwtBlacklistService.registerTokenForUser(auth.getName(), token, exp);
            }

            return ResponseEntity.ok(new AuthResponse(token));
        } catch (AuthenticationException ex) {
            // Return a user-friendly message instead of throwing an exception
            Map<String, String> body = Map.of("error", "Invalid email or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
        }
    }

    @Operation(summary = "Register a new user", description = "Creates a new user account. Returns the created user's id.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User created", content = @Content(mediaType = "application/json", schema = @Schema(example = "{\"id\":1}"))),
            @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
            @ApiResponse(responseCode = "409", description = "Email already in use", content = @Content),
            @ApiResponse(responseCode = "503", description = "Registration service unavailable", content = @Content)
    })
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody UserCreateRequest request) {
        if (request.getEmail() != null && userService != null && userService.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Email already in use"));
        }

        if (userService == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", "Registration service unavailable"));
        }

        User user = new User();
        user.setFirstname(request.getFirstname());
        user.setLastname(request.getLastname());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        // Default to ACTIVE if not provided
        user.setStatus(request.getStatus() != null ? request.getStatus() : UserStatus.ACTIVE);

        User created = userService.createUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", created.getId()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            long exp = jwtUtil.getExpirationMillis(token);
            // Blacklist token if valid and not already expired
            if (exp > System.currentTimeMillis()) {
                jwtBlacklistService.blacklistToken(token, exp);
            }
        }
        // Always return 204 No Content to avoid revealing state
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get current user", description = "Returns the currently authenticated user's information.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authenticated user info"),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content)
    })
    @GetMapping("/me")
    public ResponseEntity<?> me() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }

        String email;
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            email = ((UserDetails) principal).getUsername();
        } else {
            email = principal.toString();
        }

        if (userService == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", "User service unavailable"));
        }

        Optional<User> userOpt = userService.findByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
        }

        User user = userOpt.get();
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setFirstname(user.getFirstname());
        dto.setLastname(user.getLastname());
        dto.setEmail(user.getEmail());
        dto.setStatus(user.getStatus());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setCreatedBy(user.getCreatedBy());
        dto.setUpdatedAt(user.getUpdatedAt());
        dto.setUpdatedBy(user.getUpdatedBy());

        return ResponseEntity.ok(dto);
    }

}
