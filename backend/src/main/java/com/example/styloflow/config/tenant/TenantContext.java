package com.example.styloflow.config.tenant;

/**
 * Holds the current tenant schema ({@code tenant_<businessId>}) for the running thread.
 * Set it before a transaction opens and always clear it in {@code finally}.
 */
public final class TenantContext {
    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setCurrentTenant(String schema) {
        CURRENT_TENANT.set(schema);
    }

    public static String getCurrentTenant() {
        return CURRENT_TENANT.get();
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
