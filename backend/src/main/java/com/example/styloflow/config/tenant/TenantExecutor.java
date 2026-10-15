package com.example.styloflow.config.tenant;

import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

/**
 * Runs code against a specific tenant schema in its own transaction. Use it where there is no request tenant
 * or a different one is needed: login, onboarding, seeders, cross-tenant platform queries.
 * Never use tenant repositories of another tenant inside an already open {@code @Transactional} method:
 * the Hibernate session stays bound to the tenant it was opened with.
 */
@Component
public class TenantExecutor {
    private final TransactionTemplate tx;

    public TenantExecutor(PlatformTransactionManager transactionManager) {
        this.tx = new TransactionTemplate(transactionManager);
        this.tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * Executes {@code work} in a new transaction bound to {@code schema}, restoring the previous tenant afterwards.
     *
     * @param schema tenant schema, e.g. {@code tenant_1} (see {@link SchemaProvisioningService#schemaName(Long)})
     * @param work   code using tenant repositories
     * @param <T>    result type
     * @return the value returned by {@code work}
     */
    public <T> T inTenant(String schema, Supplier<T> work) {
        var previous = TenantContext.getCurrentTenant();
        TenantContext.setCurrentTenant(schema);
        try {
            return tx.execute(_ -> work.get());
        } finally {
            if (previous == null) TenantContext.clear();
            else TenantContext.setCurrentTenant(previous);
        }
    }
}
