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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Builder for INSERT queries.
 * <p>
 * Supports setting column values via {@link #set(String, Object)} (string-based)
 * or {@link #set(Column, Object)} (type-safe), raw SQL expression via
 * {@link #setRaw(String, String)}, {@code ON CONFLICT} handling, and batch
 * insertion via {@link #addBatch()}/{@link #executeBatch()}.
 * </p>
 */
public class InsertQuery implements Query {

    private final JdbORM jdborm;
    private final String table;
    private final Map<String, Object> values = new LinkedHashMap<>();
    private String onConflictAction;
    private String[] conflictColumns;
    private String conflictConstraint;
    private final List<Map<String, Object>> batchRows = new ArrayList<>();
    private boolean inBatch;

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

    /**
     * Sets a column to a raw SQL expression instead of a parameterised value (string-based).
     * <p>
     * The expression is inserted verbatim into the SQL. Useful for database functions
     * like {@code NOW()}, {@code DEFAULT}, or {@code GEN_RANDOM_UUID()}.
     * </p>
     *
     * @param column     the column name
     * @param expression the raw SQL expression (e.g. {@code "NOW()"})
     * @return this builder for chaining
     */
    public InsertQuery setRaw(String column, String expression) {
        values.put(column, new RawExpression(expression));
        return this;
    }

    /**
     * Sets a column to a raw SQL expression instead of a parameterised value (type-safe).
     *
     * @param column     the {@link Column} reference
     * @param expression the raw SQL expression (e.g. {@code "NOW()"})
     * @param <T>        the column's value type
     * @return this builder for chaining
     */
    public <T> InsertQuery setRaw(Column<T> column, String expression) {
        values.put(column.qualifiedName(), new RawExpression(expression));
        return this;
    }

    /**
     * Adds an {@code ON CONFLICT DO NOTHING} clause to the INSERT.
     * <p>
     * When a conflict occurs (e.g. on a unique constraint), no error is thrown
     * and the row is simply not inserted.
     * </p>
     *
     * @return this builder for chaining
     * @deprecated Use {@link #onConflict(String...)} followed by {@link #doNothing()}
     *             or {@link #onConflictOnConstraint(String)} followed by {@link #doNothing()} instead.
     */
    @Deprecated
    public InsertQuery onConflictDoNothing() {
        this.onConflictAction = "DO NOTHING";
        return this;
    }

    /**
     * Adds an {@code ON CONFLICT DO UPDATE SET ...} clause to the INSERT.
     * <p>
     * When a conflict occurs, the specified SET clauses are applied as an update.
     * Each clause should be a string like {@code "col = EXCLUDED.col"}.
     * </p>
     *
     * @param setClauses one or more SET clause strings (at least one required)
     * @return this builder for chaining
     * @throws JdbOrmException if no set clauses are provided
     * @deprecated Use {@link #onConflict(String...)} followed by {@link #doUpdateSet(String...)}
     *             or {@link #onConflictOnConstraint(String)} followed by {@link #doUpdateSet(String...)} instead.
     */
    @Deprecated
    public InsertQuery onConflictDoUpdate(String... setClauses) {
        if (setClauses.length == 0) {
            throw new JdbOrmException("At least one SET clause required for ON CONFLICT DO UPDATE");
        }
        StringBuilder sb = new StringBuilder("DO UPDATE SET ");
        for (int i = 0; i < setClauses.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(setClauses[i]);
        }
        this.onConflictAction = sb.toString();
        return this;
    }

    /**
     * Sets the conflict target columns for an {@code ON CONFLICT} clause.
     * <p>
     * Must be followed by {@link #doNothing()} or {@link #doUpdateSet(String...)}.
     * </p>
     *
     * @param columns the conflict target column names (e.g. {@code "unique_id"})
     * @return this builder for chaining
     */
    public InsertQuery onConflict(String... columns) {
        this.conflictColumns = columns;
        this.conflictConstraint = null;
        return this;
    }

    /**
     * Sets the conflict target constraint name for an {@code ON CONFLICT ON CONSTRAINT} clause.
     * <p>
     * Must be followed by {@link #doNothing()} or {@link #doUpdateSet(String...)}.
     * </p>
     *
     * @param constraintName the constraint name (e.g. {@code "pk_visitors"})
     * @return this builder for chaining
     */
    public InsertQuery onConflictOnConstraint(String constraintName) {
        this.conflictConstraint = constraintName;
        this.conflictColumns = null;
        return this;
    }

    /**
     * Completes the {@code ON CONFLICT} clause with {@code DO NOTHING}.
     * <p>
     * Must be called after {@link #onConflict(String...)} or
     * {@link #onConflictOnConstraint(String)}.
     * </p>
     *
     * @return this builder for chaining
     * @throws JdbOrmException if no conflict target was set before this call
     */
    public InsertQuery doNothing() {
        if (conflictColumns == null && conflictConstraint == null) {
            throw new JdbOrmException("onConflict(columns) or onConflictOnConstraint(name) must be called before doNothing()");
        }
        this.onConflictAction = "DO NOTHING";
        return this;
    }

    /**
     * Completes the {@code ON CONFLICT} clause with {@code DO UPDATE SET ...}.
     * <p>
     * Must be called after {@link #onConflict(String...)} or
     * {@link #onConflictOnConstraint(String)}.
     * </p>
     *
     * @param setClauses SET clause strings (e.g. {@code "name = EXCLUDED.name"})
     * @return this builder for chaining
     * @throws JdbOrmException if no set clauses are provided
     * @throws JdbOrmException if no conflict target was set before this call
     */
    public InsertQuery doUpdateSet(String... setClauses) {
        if (setClauses.length == 0) {
            throw new JdbOrmException("At least one SET clause required for ON CONFLICT DO UPDATE");
        }
        if (conflictColumns == null && conflictConstraint == null) {
            throw new JdbOrmException("onConflict(columns) or onConflictOnConstraint(name) must be called before doUpdateSet()");
        }
        StringBuilder sb = new StringBuilder("DO UPDATE SET ");
        for (int i = 0; i < setClauses.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(setClauses[i]);
        }
        this.onConflictAction = sb.toString();
        return this;
    }

    /**
     * Returns a SET clause using the PostgreSQL {@code EXCLUDED} pseudo-table.
     * <p>
     * Shortcut for {@code setClause(column, "EXCLUDED." + column)}.
     * </p>
     *
     * @param column the column name
     * @return a SET clause string like {@code "last_known_name = EXCLUDED.last_known_name"}
     */
    public static String excluded(String column) {
        return column + " = EXCLUDED." + column;
    }

    /**
     * Returns a SET clause string for use with {@link #doUpdateSet(String...)}.
     *
     * @param column     the column name
     * @param expression the expression to set (e.g. {@code "EXCLUDED.name"})
     * @return a SET clause string like {@code "first_seen = LEAST(visitors.first_seen, EXCLUDED.first_seen)"}
     */
    public static String setClause(String column, String expression) {
        return column + " = " + expression;
    }

    /**
     * Adds the current values as a batch row and clears the value map for the next row.
     * <p>
     * After calling this method once, subsequent calls to {@link #execute()} will
     * generate a multi-row INSERT. Use {@link #executeBatch()} instead for
     * {@link java.sql.PreparedStatement} batch execution.
     * </p>
     *
     * @return this builder for chaining
     * @throws JdbOrmException if no values have been set
     */
    public InsertQuery addBatch() {
        if (values.isEmpty()) {
            throw new JdbOrmException("No values to add to batch. Call set() first.");
        }
        batchRows.add(new LinkedHashMap<>(values));
        values.clear();
        inBatch = true;
        return this;
    }

    private List<String> unifiedColumns() {
        if (batchRows.isEmpty()) {
            return new ArrayList<>(values.keySet());
        }
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (Map<String, Object> row : batchRows) {
            seen.addAll(row.keySet());
        }
        seen.addAll(values.keySet());
        return new ArrayList<>(seen);
    }

    @Override
    public String toSql() {
        List<String> columns = unifiedColumns();
        if (columns.isEmpty()) {
            throw new JdbOrmException("No values specified for INSERT");
        }

        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ").append(table).append(" (");

        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append(columns.get(i));
        }

        sql.append(") VALUES ");
        int rowCount = !batchRows.isEmpty() ? batchRows.size() : 1;
        for (int r = 0; r < rowCount; r++) {
            if (r > 0) sql.append(", ");
            sql.append("(");
            Map<String, Object> row = !batchRows.isEmpty() ? batchRows.get(r) : values;
            for (int i = 0; i < columns.size(); i++) {
                if (i > 0) sql.append(", ");
                String colName = columns.get(i);
                Object val = row.get(colName);
                if (val instanceof RawExpression expr) {
                    sql.append(expr.expression());
                } else {
                    sql.append("?");
                }
            }
            sql.append(")");
        }

        if (onConflictAction != null) {
            sql.append(" ON CONFLICT");
            if (conflictConstraint != null) {
                sql.append(" ON CONSTRAINT ").append(conflictConstraint);
            } else if (conflictColumns != null && conflictColumns.length > 0) {
                sql.append(" (");
                for (int i = 0; i < conflictColumns.length; i++) {
                    if (i > 0) sql.append(", ");
                    sql.append(conflictColumns[i]);
                }
                sql.append(")");
            }
            sql.append(" ").append(onConflictAction);
        }

        return sql.toString();
    }

    @Override
    public List<Object> getParameters() {
        List<Object> params = new ArrayList<>();
        List<String> columns = unifiedColumns();
        List<Map<String, Object>> rows = !batchRows.isEmpty() ? batchRows : List.of(values);
        for (Map<String, Object> row : rows) {
            for (String colName : columns) {
                Object val = row.get(colName);
                if (!(val instanceof RawExpression)) {
                    params.add(val);
                }
            }
        }
        return params;
    }

    /**
     * Executes the INSERT statement and returns the generated keys.
     * <p>
     * If batch rows have been added via {@link #addBatch()}, this generates
     * a multi-row INSERT and returns all generated keys.
     * </p>
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
                while (rs.next()) {
                    keys.add("GENERATED_KEY", rs.getObject(1));
                }
            }
            return keys;
        } catch (SQLException e) {
            throw new JdbOrmException("Failed to execute INSERT: " + sql, e);
        }
    }

    /**
     * Executes the accumulated batch rows using {@link PreparedStatement#executeBatch()}.
     * <p>
     * Call {@link #addBatch()} at least once before invoking this method.
     * </p>
     *
     * @return an array of update counts (one per batch row)
     * @throws JdbOrmException if no batch rows have been added or execution fails
     */
    public int[] executeBatch() {
        if (batchRows.isEmpty()) {
            throw new JdbOrmException("No batch rows added. Call addBatch() first.");
        }

        Connection conn = jdborm.getConnection();
        try {
            return executeBatchWithConnection(conn);
        } finally {
            if (jdborm.isUseDataSource() && conn != null) {
                try {
                    conn.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    private int[] executeBatchWithConnection(Connection conn) {
        String sql = toSql();
        List<String> columns = unifiedColumns();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (Map<String, Object> row : batchRows) {
                int idx = 1;
                for (String colName : columns) {
                    Object val = row.get(colName);
                    if (val != null && !(val instanceof RawExpression)) {
                        stmt.setObject(idx++, val);
                    }
                }
                stmt.addBatch();
            }
            return stmt.executeBatch();
        } catch (SQLException e) {
            throw new JdbOrmException("Failed to execute batch INSERT", e);
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
