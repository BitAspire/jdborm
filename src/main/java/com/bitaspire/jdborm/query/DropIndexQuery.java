package com.bitaspire.jdborm.query;

import com.bitaspire.jdborm.JdbORM;
import com.bitaspire.jdborm.exception.JdbOrmException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * Builder for DROP INDEX statements.
 * <p>
 * Supports IF EXISTS and CASCADE options.
 * </p>
 */
public class DropIndexQuery implements Query {

    private final JdbORM jdborm;
    private final String indexName;
    private String table;
    private boolean ifExists;
    private boolean cascade;

    /**
     * Creates a new DROP INDEX builder for the given index name.
     *
     * @param jdborm    the JdbORM instance for connection management
     * @param indexName the name of the index to drop
     */
    public DropIndexQuery(JdbORM jdborm, String indexName) {
        this.jdborm = jdborm;
        this.indexName = indexName;
    }

    /**
     * Sets the table that owns the index (required by some databases).
     *
     * @param table the table name
     * @return this builder for chaining
     */
    public DropIndexQuery on(String table) {
        this.table = table;
        return this;
    }

    /**
     * Adds IF EXISTS to the DROP INDEX statement.
     *
     * @return this builder for chaining
     */
    public DropIndexQuery ifExists() {
        this.ifExists = true;
        return this;
    }

    /**
     * Adds CASCADE to the DROP INDEX statement.
     *
     * @return this builder for chaining
     */
    public DropIndexQuery cascade() {
        this.cascade = true;
        return this;
    }

    @Override
    public String toSql() {
        StringBuilder sql = new StringBuilder("DROP INDEX ");
        if (ifExists) {
            sql.append("IF EXISTS ");
        }
        if (table != null) {
            sql.append(table).append(".");
        }
        sql.append(indexName);
        if (cascade) {
            sql.append(" CASCADE");
        }
        return sql.toString();
    }

    @Override
    public List<Object> getParameters() {
        return List.of();
    }

    /**
     * Executes the DROP INDEX statement.
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
            throw new JdbOrmException("Failed to execute DROP INDEX: " + sql, e);
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
