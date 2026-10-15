package com.example.styloflow.config.tenant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

/** Creates and migrates tenant schemas; on startup migrates every existing business schema. */
@Slf4j
@Service
@Order(0)
@RequiredArgsConstructor
public class SchemaProvisioningService implements ApplicationRunner {
    private static final String TENANT_MIGRATIONS = "classpath:db/migration/tenant";

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    /**
     * Schema name of a business.
     *
     * @param businessId id of the business
     * @return {@code tenant_<businessId>}
     */
    public static String schemaName(Long businessId) {
        return "tenant_" + businessId;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.queryForList("SELECT id FROM public.businesses ORDER BY id", Long.class)
                .forEach(this::provision);
    }

    /**
     * Creates the business schema if missing and applies pending tenant migrations. Idempotent.
     * Call it outside any transaction, after the business row is committed.
     *
     * @param businessId id of the business in {@code public.businesses}
     * @return the schema name
     */
    public String provision(Long businessId) {
        var schema = schemaName(businessId);
        var result = Flyway.configure()
                .dataSource(dataSource)
                .schemas(schema)
                .createSchemas(true)
                .locations(TENANT_MIGRATIONS)
                .load()
                .migrate();
        if (result.migrationsExecuted > 0)
            log.info("Schema {} migrated to version {}", schema, result.targetSchemaVersion);
        return schema;
    }
}
