package org.example.expenseapi.tenant;

/**
 * Simple ThreadLocal-based holder for the current request tenant id.
 * Set by authentication/filter layer and cleared after request completes.
 */
public final class TenantContextHolder {
    private static final ThreadLocal<String> CONTEXT = new ThreadLocal<>();

    private TenantContextHolder() {}

    public static void setTenant(String tenantId) {
        CONTEXT.set(tenantId);
    }

    public static String getTenant() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}

