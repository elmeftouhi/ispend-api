package org.example.expenseapi.config;

import org.example.expenseapi.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.authentication.AnonymousAuthenticationToken;

import java.util.Optional;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Allow public and health endpoints and OpenAPI/Swagger UI endpoints
                .requestMatchers(
                        "/public/**",
                        "/actuator/health",
                        "/v3/api-docs/**",
                        "/v3/api-docs.yaml",
                        "/swagger-ui.html",
                        "/swagger-ui/index.html",
                        "/swagger-ui/**"
                ).permitAll()
                .requestMatchers(HttpMethod.POST, "/v1/auth/**", "/v1/users/**").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form.disable());

        // allow frames for h2-console if present
        http.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        // register JWT filter
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // AuditorAware implementation that reads the username from the SecurityContext
    @Bean
    public AuditorAware<String> auditorProvider() {
        return new AuditorAware<String>() {
            @Override
            public Optional<String> getCurrentAuditor() {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                // If no authentication or not authenticated, return system
                if (authentication == null || !authentication.isAuthenticated()) {
                    return Optional.of("system");
                }
                // Handle anonymous authentication explicitly (it may report authenticated=true)
                if (authentication instanceof AnonymousAuthenticationToken) {
                    return Optional.of("system");
                }
                Object principal = authentication.getPrincipal();
                // If principal is UserDetails, use the username (Java 8 compatible)
                if (principal instanceof UserDetails) {
                    UserDetails ud = (UserDetails) principal;
                    return Optional.ofNullable(ud.getUsername());
                }
                // If principal is the anonymousUser string, fallback to system
                if (principal instanceof String) {
                    String s = (String) principal;
                    if ("anonymousUser".equals(s)) {
                        return Optional.of("system");
                    }
                    // otherwise return the string principal
                    return Optional.of(s);
                }
                // Fallback to principal.toString()
                return Optional.of(principal.toString());
            }
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}
