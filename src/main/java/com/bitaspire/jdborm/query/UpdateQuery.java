package com.bitaspire.jdborm.query;

import com.bitaspire.jdborm.JdbORM;
import com.bitaspire.jdborm.exception.JdbOrmException;
import com.bitaspire.jdborm.condition.Condition;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builder for UPDATE queries.
 * <p>
 * Use {@link #set(String, Object)} to specify columns to update and
 * optionally {@link #where(Condition)} to filter which rows are affected.
 * </p>
 */
public class UpdateQuery implements Query {

    private final JdbORM jdborm;
    private final String table;
    private final Map<String, Object> values = new LinkedHashMap<>();
    private Condition where;

    /**
     * Creates a new UPDATE query builder for the given table.
     *
     * @param jdborm the JdbORM instance for connection management
     * @param table  the target table name
     */
    public UpdateQuery(JdbORM jdborm, String table) {
        this.jdborm = jdborm;
        this.table = table;
    }

    /**
     * Sets a column to a new value.
     *
     * @param column the column name
     * @param value  the new value
     * @return this builder for chaining
     */
    public UpdateQuery set(String column, Object value) {
        values.put(column, value);
        return this;
    }

    /**
     * Adds a WHERE clause to filter which rows to update.
     *
     * @param condition the filter condition
     * @return this builder for chaining
     */
    public UpdateQuery where(Condition condition) {
        this.where = condition;
        return this;
    }

    @Override
    public String toSql() {
        if (values.isEmpty()) {
            throw new JdbOrmException("No values specified for UPDATE");
        }

        StringBuilder sql = new StringBuilder();
        sql.append("UPDATE ").append(table).append(" SET ");

        List<String> columns = new ArrayList<>(values.keySet());
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append(columns.get(i)).append(" = ?");
        }

        if (where != null) {
            sql.append(" WHERE ");
            where.appendTo(sql, new ArrayList<>());
        }

        return sql.toString();
    }

    @Override
    public List<Object> getParameters() {
        List<Object> params = new ArrayList<>(values.values());
        if (where != null) {
            where.appendTo(new StringBuilder(), params);
        }
        return params;
    }

    /**
     * Executes the UPDATE statement.
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
            throw new JdbOrmException("Failed to execute UPDATE: " + sql, e);
        }
    }
}
