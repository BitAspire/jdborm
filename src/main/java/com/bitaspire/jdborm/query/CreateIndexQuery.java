package com.bitaspire.jdborm.query;

import com.bitaspire.jdborm.JdbORM;
import com.bitaspire.jdborm.exception.JdbOrmException;
import com.bitaspire.jdborm.schema.Column;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Builder for CREATE INDEX statements.
 * <p>
 * Supports UNIQUE indexes, custom index methods, and IF NOT EXISTS.
 * </p>
 */
public class CreateIndexQuery implements Query {

    private final JdbORM jdborm;
    private final String indexName;
    private String table;
    private final List<String> columns;
    private boolean unique;
    private boolean ifNotExists;
    private String method;

    /**
     * Creates a new CREATE INDEX builder for the given index name.
     *
     * @param jdborm    the JdbORM instance for connection management
     * @param indexName the name of the index to create
     */
    public CreateIndexQuery(JdbORM jdborm, String indexName) {
        this.jdborm = jdborm;
        this.indexName = indexName;
        this.columns = new ArrayList<>();
    }

    /**
     * Sets the table on which to create the index.
     *
     * @param table   the table name
     * @param columns the column(s) to index
     * @return this builder for chaining
     */
    public CreateIndexQuery on(String table, String... columns) {
        this.table = table;
        this.columns.addAll(List.of(columns));
        return this;
    }

    /**
     * Sets the table and column using type-safe references.
     *
     * @param table   the table name
     * @param columns the column(s) to index
     * @return this builder for chaining
     */
    public CreateIndexQuery on(String table, Column<?>... columns) {
        this.table = table;
        for (Column<?> col : columns) {
            this.columns.add(col.name());
        }
        return this;
    }

    /**
     * Makes the index UNIQUE.
     *
     * @return this builder for chaining
     */
    public CreateIndexQuery unique() {
        this.unique = true;
        return this;
    }

    /**
     * Adds IF NOT EXISTS to the CREATE INDEX statement.
     *
     * @return this builder for chaining
     */
    public CreateIndexQuery ifNotExists() {
        this.ifNotExists = true;
        return this;
    }

    /**
     * Sets the index method (e.g. "BTREE", "HASH", "GIST").
     *
     * @param method the index method
     * @return this builder for chaining
     */
    public CreateIndexQuery using(String method) {
        this.method = method;
        return this;
    }

    @Override
    public String toSql() {
        StringBuilder sql = new StringBuilder("CREATE ");
        if (unique) {
            sql.append("UNIQUE ");
        }
        sql.append("INDEX ");
        if (ifNotExists) {
            sql.append("IF NOT EXISTS ");
        }
        sql.append(indexName).append(" ON ").append(table);
        if (method != null) {
            sql.append(" USING ").append(method);
        }
        sql.append(" (").append(String.join(", ", columns)).append(")");
        return sql.toString();
    }

    @Override
    public List<Object> getParameters() {
        return List.of();
    }

    /**
     * Executes the CREATE INDEX statement.
     *
     * @throws JdbOrmException if SQL execution fails
     */
    public void execute() {
        String sql = toSql();
        Connection conn = jdborm.getConnection();
        try {
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new JdbOrmException("Failed to execute CREATE INDEX: " + sql, e);
        } finally {
            if (jdborm.isUseDataSource() && conn != null) {
                try {
                    conn.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }
}
