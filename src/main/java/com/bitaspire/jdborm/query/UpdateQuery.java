package com.bitaspire.jdborm.query;

import com.bitaspire.jdborm.JdbORM;
import com.bitaspire.jdborm.exception.JdbOrmException;
import com.bitaspire.jdborm.condition.Condition;
import com.bitaspire.jdborm.schema.Column;

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
 * Use {@link #set(String, Object)} (string-based) or
 * {@link #set(Column, Object)} (type-safe) to specify columns to update,
 * optionally {@link #setRaw(String, String)} for raw SQL expressions,
 * and {@link #where(Condition)} to filter which rows are affected.
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
     * Sets a column to a new value (string-based).
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
     * Sets a column to a new value (type-safe).
     * The compiler ensures the value type matches the column type.
     *
     * @param column the {@link Column} reference
     * @param value  the new value (must match the column's type)
     * @param <T>    the column's value type
     * @return this builder for chaining
     */
    public <T> UpdateQuery set(Column<T> column, T value) {
        values.put(column.qualifiedName(), value);
        return this;
    }

    /**
     * Sets a column to a raw SQL expression instead of a parameterised value (string-based).
     * <p>
     * The expression is inserted verbatim into the SQL. Useful for expressions
     * like {@code "counter + 1"}, {@code "NOW()"}, or {@code "DEFAULT"}.
     * </p>
     *
     * @param column     the column name
     * @param expression the raw SQL expression (e.g. {@code "NOW()"})
     * @return this builder for chaining
     */
    public UpdateQuery setRaw(String column, String expression) {
        values.put(column, new RawExpression(expression));
        return this;
    }

    /**
     * Sets a column to a raw SQL expression instead of a parameterised value (type-safe).
     *
     * @param column     the {@link Column} reference
     * @param expression the raw SQL expression (e.g. {@code "counter + 1"})
     * @param <T>        the column's value type
     * @return this builder for chaining
     */
    public <T> UpdateQuery setRaw(Column<T> column, String expression) {
        values.put(column.qualifiedName(), new RawExpression(expression));
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
            sql.append(columns.get(i)).append(" = ");
            Object val = values.get(columns.get(i));
            if (val instanceof RawExpression expr) {
                sql.append(expr.expression());
            } else {
                sql.append("?");
            }
        }

        if (where != null) {
            sql.append(" WHERE ");
            where.appendTo(sql, new ArrayList<>());
        }

        return sql.toString();
    }

    @Override
    public List<Object> getParameters() {
        List<Object> params = new ArrayList<>();
        for (Object val : values.values()) {
            if (!(val instanceof RawExpression)) {
                params.add(val);
            }
        }
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
