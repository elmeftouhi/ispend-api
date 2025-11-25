package org.example.expenseapi.controller;

import jakarta.annotation.PostConstruct;
import org.example.expenseapi.dto.AuthRequest;
import org.example.expenseapi.dto.AuthResponse;
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
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

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

}
