package org.example.expenseapi.tenant;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hibernate.Session;
import org.hibernate.Filter;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class HibernateTenantFilter extends OncePerRequestFilter {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String tenant = TenantContextHolder.getTenant();
        Session session = null;
        Filter filter = null;
        try {
            if (entityManager != null) {
                session = entityManager.unwrap(Session.class);
                if (session != null && tenant != null && !tenant.isBlank()) {
                    filter = session.enableFilter("tenantFilter");
                    filter.setParameter("tenant", tenant);
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            // disable the filter (if enabled) to avoid leakage
            if (session != null) {
                try {
                    if (session.getEnabledFilter("tenantFilter") != null) {
                        session.disableFilter("tenantFilter");
                    }
                } catch (Exception ignore) {
                }
            }
        }
    }
}

