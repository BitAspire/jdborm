package com.bitaspire.jdborm.query;

import com.bitaspire.jdborm.JdbORM;
import com.bitaspire.jdborm.exception.JdbOrmException;
import com.bitaspire.jdborm.schema.Column;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builder for INSERT queries.
 * <p>
 * Use {@link #set(String, Object)} (string-based) or
 * {@link #set(Column, Object)} (type-safe) to add column-value pairs,
 * then {@link #execute()} to run the insert and retrieve auto-generated keys.
 * </p>
 */
public class InsertQuery implements Query {

    private final JdbORM jdborm;
    private final String table;
    private final Map<String, Object> values = new LinkedHashMap<>();

    /**
     * Creates a new INSERT query builder for the given table.
     *
     * @param jdborm the JdbORM instance for connection management
     * @param table  the target table name
     */
    public InsertQuery(JdbORM jdborm, String table) {
        this.jdborm = jdborm;
        this.table = table;
    }

    /**
     * Sets a column value for the INSERT (string-based).
     *
     * @param column the column name
     * @param value  the value to insert
     * @return this builder for chaining
     */
    public InsertQuery set(String column, Object value) {
        values.put(column, value);
        return this;
    }

    /**
     * Sets a column value for the INSERT (type-safe).
     * The compiler ensures the value type matches the column type.
     *
     * @param column the {@link Column} reference
     * @param value  the value to insert (must match the column's type)
     * @param <T>    the column's value type
     * @return this builder for chaining
     */
    public <T> InsertQuery set(Column<T> column, T value) {
        values.put(column.qualifiedName(), value);
        return this;
    }

    @Override
    public String toSql() {
        if (values.isEmpty()) {
            throw new JdbOrmException("No values specified for INSERT");
        }

        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ").append(table).append(" (");

        List<String> columns = new ArrayList<>(values.keySet());
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append(columns.get(i));
        }

        sql.append(") VALUES (");
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append("?");
        }
        sql.append(")");

        return sql.toString();
    }

    @Override
    public List<Object> getParameters() {
        return new ArrayList<>(values.values());
    }

    /**
     * Executes the INSERT statement and returns the generated keys.
     *
     * @return a {@link GeneratedKeys} object containing any auto-generated keys
     * @throws JdbOrmException if SQL execution fails
     */
    public GeneratedKeys execute() {
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

    private GeneratedKeys executeWithConnection(Connection conn) {
        String sql = toSql();
        List<Object> params = getParameters();

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            stmt.executeUpdate();

            GeneratedKeys keys = new GeneratedKeys();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    keys.add("GENERATED_KEY", rs.getObject(1));
                }
            }
            return keys;
        } catch (SQLException e) {
            throw new JdbOrmException("Failed to execute INSERT: " + sql, e);
        }
    }

    /**
     * Holds auto-generated keys returned by the database after an INSERT.
     */
    public static class GeneratedKeys {
        private final Map<String, Object> keys = new LinkedHashMap<>();

        void add(String name, Object value) {
            keys.put(name, value);
        }

        /**
         * Retrieves a generated key by name.
         *
         * @param name the key name
         * @param <T>  the expected type
         * @return the key value, or {@code null} if not present
         */
        @SuppressWarnings("unchecked")
        public <T> T get(String name) {
            return (T) keys.get(name);
        }

        /**
         * Retrieves the first generated key, or {@code null} if no keys were generated.
         *
         * @param <T> the expected type
         * @return the first key value, or {@code null}
         */
        @SuppressWarnings("unchecked")
        public <T> T getFirst() {
            return keys.isEmpty() ? null : (T) keys.values().iterator().next();
        }

        /**
         * Returns all generated keys as a map.
         *
         * @return a new map containing all key-value pairs
         */
        public Map<String, Object> asMap() {
            return new LinkedHashMap<>(keys);
        }
    }
}
