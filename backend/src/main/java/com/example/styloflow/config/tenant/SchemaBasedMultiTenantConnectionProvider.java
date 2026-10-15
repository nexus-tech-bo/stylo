package com.example.styloflow.config.tenant;

import lombok.RequiredArgsConstructor;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.regex.Pattern;

/** Points each Hibernate connection to the tenant schema through {@code search_path}. */
@Component
@RequiredArgsConstructor
public class SchemaBasedMultiTenantConnectionProvider implements MultiTenantConnectionProvider<String> {
    // search_path cannot be bound as a parameter, so the identifier is validated before reaching SQL.
    private static final Pattern SCHEMA = Pattern.compile("^(public|tenant_\\d+)$");

    private final transient DataSource dataSource;

    @Override
    public Connection getAnyConnection() throws SQLException {
        var connection = dataSource.getConnection();
        setSearchPath(connection, CustomTenantResolver.DEFAULT_TENANT);
        return connection;
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        connection.close();
    }

    @Override
    public Connection getConnection(String tenantIdentifier) throws SQLException {
        if (!SCHEMA.matcher(tenantIdentifier).matches())
            throw new SQLException("Invalid tenant identifier: " + tenantIdentifier);
        var connection = dataSource.getConnection();
        setSearchPath(connection, tenantIdentifier + ", public");
        return connection;
    }

    @Override
    public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {
        try {
            setSearchPath(connection, CustomTenantResolver.DEFAULT_TENANT); // pooled connections must not leak a tenant
        } finally {
            connection.close();
        }
    }

    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }

    @Override
    public boolean isUnwrappableAs(Class<?> unwrapType) {
        return false;
    }

    @Override
    public <T> T unwrap(Class<T> unwrapType) {
        throw new UnsupportedOperationException("Unwrap not supported");
    }

    private static void setSearchPath(Connection connection, String path) throws SQLException {
        try (var stmt = connection.createStatement()) {
            stmt.execute("SET search_path TO " + path);
        }
    }
}
