package org.example.expenseapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.expenseapi.dto.AuthRequest;
import org.example.expenseapi.model.User;
import org.example.expenseapi.model.UserStatus;
import org.example.expenseapi.security.JwtBlacklistService;
import org.example.expenseapi.security.JwtUtil;
import org.example.expenseapi.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class AuthControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final AuthenticationConfiguration authenticationConfiguration = mock(AuthenticationConfiguration.class);
    private final AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
    private final JwtUtil jwtUtil = mock(JwtUtil.class);
    private final JwtBlacklistService jwtBlacklistService = mock(JwtBlacklistService.class);
    private final UserService userService = mock(UserService.class);

    private AuthController authController;

    @BeforeEach
    public void setup() throws Exception {
        when(authenticationConfiguration.getAuthenticationManager()).thenReturn(authenticationManager);

        authController = new AuthController(authenticationConfiguration, jwtUtil, jwtBlacklistService, userService);
        // call PostConstruct init to set the authenticationManager field
        authController.init();

        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
    }

    @AfterEach
    public void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void login_success_shouldReturnToken() throws Exception {
        String email = "user@example.com";
        String password = "secret";
        String generatedToken = "dummy-token";

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(email);
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtUtil.generateToken(email)).thenReturn(generatedToken);

        AuthRequest request = new AuthRequest();
        request.setEmail(email);
        request.setPassword(password);

        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").value(generatedToken));

        verify(authenticationManager, times(1)).authenticate(any());
        verify(jwtUtil, times(1)).generateToken(email);
    }

    @Test
    public void logout_withValidToken_shouldBlacklistAndReturnNoContent() throws Exception {
        String token = "valid-token";
        long exp = System.currentTimeMillis() + 60_000L;

        when(jwtUtil.getExpirationMillis(token)).thenReturn(exp);

        mockMvc.perform(post("/v1/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        verify(jwtUtil, times(1)).getExpirationMillis(token);
        verify(jwtBlacklistService, times(1)).blacklistToken(token, exp);
    }

    @Test
    public void me_authenticated_shouldReturnUserInfo() throws Exception {
        String email = "user@example.com";

        // Set up SecurityContext with an authenticated user
        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                email, "password", Collections.emptyList());
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authToken);

        User user = new User();
        user.setId(1L);
        user.setFirstname("Alice");
        user.setLastname("Smith");
        user.setEmail(email);
        user.setStatus(UserStatus.ACTIVE);

        when(userService.findByEmail(email)).thenReturn(Optional.of(user));

        mockMvc.perform(get("/v1/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstname").value("Alice"))
                .andExpect(jsonPath("$.lastname").value("Smith"))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(userService, times(1)).findByEmail(email);
    }

    @Test
    public void me_userNotFound_shouldReturn404() throws Exception {
        String email = "unknown@example.com";

        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                email, "password", Collections.emptyList());
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authToken);

        when(userService.findByEmail(email)).thenReturn(Optional.empty());

        mockMvc.perform(get("/v1/auth/me"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("User not found"));
    }
}
