package com.bitaspire.jdborm.query;

import com.bitaspire.jdborm.JdbORM;
import com.bitaspire.jdborm.exception.JdbOrmException;
import com.bitaspire.jdborm.schema.Table;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * Builder for DROP TABLE statements.
 * <p>
 * Supports IF EXISTS and CASCADE options.
 * </p>
 */
public class DropTableQuery implements Query {

    private final JdbORM jdborm;
    private final String table;
    private boolean ifExists;
    private boolean cascade;

    /**
     * Creates a new DROP TABLE builder for the given table.
     *
     * @param jdborm the JdbORM instance for connection management
     * @param table  the table name to drop
     */
    public DropTableQuery(JdbORM jdborm, String table) {
        this.jdborm = jdborm;
        this.table = table;
    }

    /**
     * Adds IF EXISTS to the DROP TABLE statement.
     *
     * @return this builder for chaining
     */
    public DropTableQuery ifExists() {
        this.ifExists = true;
        return this;
    }

    /**
     * Adds CASCADE to the DROP TABLE statement.
     *
     * @return this builder for chaining
     */
    public DropTableQuery cascade() {
        this.cascade = true;
        return this;
    }

    @Override
    public String toSql() {
        StringBuilder sql = new StringBuilder("DROP TABLE ");
        if (ifExists) {
            sql.append("IF EXISTS ");
        }
        sql.append(table);
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
     * Executes the DROP TABLE statement.
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
            throw new JdbOrmException("Failed to execute DROP TABLE: " + sql, e);
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
