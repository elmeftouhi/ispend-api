package org.example.expenseapi.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.expenseapi.tenant.TenantContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final JwtBlacklistService jwtBlacklistService;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserDetailsService userDetailsService, JwtBlacklistService jwtBlacklistService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.jwtBlacklistService = jwtBlacklistService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        String username = null;
        String token = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            // If token is blacklisted, reject immediately
            if (jwtBlacklistService != null && jwtBlacklistService.isBlacklisted(token)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
            if (jwtUtil.validateToken(token)) {
                username = jwtUtil.getUsernameFromToken(token);
            }
        }

        try {
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                if (jwtUtil.validateToken(token)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());

                    // Extract tenant from token and set into context holder
                    String tenantId = jwtUtil.getTenantFromToken(token);
                    if (tenantId != null && !tenantId.isBlank()) {
                        TenantContextHolder.setTenant(tenantId);
                        // Also attach tenant info to authentication details for downstream/AuditorAware
                        Map<String, Object> details = new HashMap<>();
                        details.put(JwtUtil.CLAIM_TENANT, tenantId);
                        authToken.setDetails(details);
                    }

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            // Clear tenant context to avoid leakage between requests
            TenantContextHolder.clear();
        }
    }
}
