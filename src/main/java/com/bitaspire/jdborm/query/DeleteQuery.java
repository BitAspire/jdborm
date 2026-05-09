package com.bitaspire.jdborm.query;

import com.bitaspire.jdborm.JdbORM;
import com.bitaspire.jdborm.exception.JdbOrmException;
import com.bitaspire.jdborm.condition.Condition;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Builder for DELETE queries.
 * <p>
 * Optionally accepts a WHERE clause via {@link #where(Condition)} to
 * filter which rows to delete.
 * </p>
 */
public class DeleteQuery implements Query {

    private final JdbORM jdborm;
    private final String table;
    private Condition where;

    /**
     * Creates a new DELETE query builder for the given table.
     *
     * @param jdborm the JdbORM instance for connection management
     * @param table  the target table name
     */
    public DeleteQuery(JdbORM jdborm, String table) {
        this.jdborm = jdborm;
        this.table = table;
    }

    /**
     * Adds a WHERE clause to filter which rows to delete.
     *
     * @param condition the filter condition
     * @return this builder for chaining
     */
    public DeleteQuery where(Condition condition) {
        this.where = condition;
        return this;
    }

    @Override
    public String toSql() {
        StringBuilder sql = new StringBuilder();
        sql.append("DELETE FROM ").append(table);

        if (where != null) {
            sql.append(" WHERE ");
            where.appendTo(sql, new ArrayList<>());
        }

        return sql.toString();
    }

    @Override
    public List<Object> getParameters() {
        List<Object> params = new ArrayList<>();
        if (where != null) {
            where.appendTo(new StringBuilder(), params);
        }
        return params;
    }

    /**
     * Executes the DELETE statement.
     *
     * @return the number of affected rows
     * @throws JdbOrmException if SQL execution fails
     */
    public int execute() {
        Connection conn = jdborm.getConnection();
        try {
            return executeWithConnection(conn);
        } finally {
            if (jdborm.isUseDataSource() && conn != null) {
                try {
                    conn.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    private int executeWithConnection(Connection conn) {
        String sql = toSql();
        List<Object> params = getParameters();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            return stmt.executeUpdate();
        } catch (SQLException e) {
            throw new JdbOrmException("Failed to execute DELETE: " + sql, e);
        }
    }
}
