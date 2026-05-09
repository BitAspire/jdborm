package com.bitaspire.jdborm.query;

import com.bitaspire.jdborm.JdbORM;
import com.bitaspire.jdborm.exception.JdbOrmException;
import com.bitaspire.jdborm.condition.Condition;
import com.bitaspire.jdborm.mapper.ResultMapper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Builder for SELECT queries.
 * <p>
 * Supports selecting specific columns (or all with {@code *}), FROM clause,
 * WHERE with conditions, ORDER BY (ASC/DESC), LIMIT, OFFSET, and JOINs
 * (INNER, LEFT, RIGHT).
 * </p>
 */
public class SelectQuery implements Query {

    private final JdbORM jdborm;
    private final String[] columns;
    private String table;
    private Condition where;
    private final List<String> orderByColumns = new ArrayList<>();
    private final List<Boolean> orderByDirections = new ArrayList<>();
    private Integer limit;
    private Integer offset;
    private final List<JoinClause> joins = new ArrayList<>();

    /**
     * Creates a new SELECT query builder.
     *
     * @param jdborm  the JdbORM instance for connection management
     * @param columns columns to select; empty means {@code *}
     */
    public SelectQuery(JdbORM jdborm, String... columns) {
        this.jdborm = jdborm;
        this.columns = columns;
    }

    /**
     * Sets the table to select from.
     *
     * @param table the table name (can include alias, e.g. {@code "users u"})
     * @return this builder for chaining
     */
    public SelectQuery from(String table) {
        this.table = table;
        return this;
    }

    /**
     * Adds a WHERE clause with the given condition.
     *
     * @param condition the filter condition
     * @return this builder for chaining
     */
    public SelectQuery where(Condition condition) {
        this.where = condition;
        return this;
    }

    /**
     * Adds an ORDER BY clause for the given columns in ascending order.
     *
     * @param columns column names to order by
     * @return this builder for chaining
     */
    public SelectQuery orderBy(String... columns) {
        for (String col : columns) {
            this.orderByColumns.add(col);
            this.orderByDirections.add(true);
        }
        return this;
    }

    /**
     * Adds an ORDER BY clause for the given columns in descending order.
     *
     * @param columns column names to order by
     * @return this builder for chaining
     */
    public SelectQuery orderByDesc(String... columns) {
        for (String col : columns) {
            this.orderByColumns.add(col);
            this.orderByDirections.add(false);
        }
        return this;
    }

    /**
     * Sets the maximum number of rows to return.
     *
     * @param limit the row limit
     * @return this builder for chaining
     */
    public SelectQuery limit(int limit) {
        this.limit = limit;
        return this;
    }

    /**
     * Sets the number of rows to skip before returning results.
     *
     * @param offset the row offset
     * @return this builder for chaining
     */
    public SelectQuery offset(int offset) {
        this.offset = offset;
        return this;
    }

    /**
     * Adds an INNER JOIN clause.
     *
     * @param table   the table to join (can include alias)
     * @param onLeft  the left side of the ON condition
     * @param onRight the right side of the ON condition
     * @return this builder for chaining
     */
    public SelectQuery join(String table, String onLeft, String onRight) {
        joins.add(new JoinClause("INNER JOIN", table, onLeft, onRight));
        return this;
    }

    /**
     * Adds a LEFT JOIN clause.
     *
     * @param table   the table to join (can include alias)
     * @param onLeft  the left side of the ON condition
     * @param onRight the right side of the ON condition
     * @return this builder for chaining
     */
    public SelectQuery leftJoin(String table, String onLeft, String onRight) {
        joins.add(new JoinClause("LEFT JOIN", table, onLeft, onRight));
        return this;
    }

    /**
     * Adds a RIGHT JOIN clause.
     *
     * @param table   the table to join (can include alias)
     * @param onLeft  the left side of the ON condition
     * @param onRight the right side of the ON condition
     * @return this builder for chaining
     */
    public SelectQuery rightJoin(String table, String onLeft, String onRight) {
        joins.add(new JoinClause("RIGHT JOIN", table, onLeft, onRight));
        return this;
    }

    @Override
    public String toSql() {
        if (table == null) {
            throw new JdbOrmException("Table not specified. Call from() first.");
        }

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");

        if (columns.length == 0) {
            sql.append("*");
        } else {
            for (int i = 0; i < columns.length; i++) {
                if (i > 0) sql.append(", ");
                sql.append(columns[i]);
            }
        }

        sql.append(" FROM ").append(table);

        for (JoinClause join : joins) {
            sql.append(" ").append(join.type())
                    .append(" ").append(join.table())
                    .append(" ON ").append(join.onLeft())
                    .append(" = ").append(join.onRight());
        }

        if (where != null) {
            sql.append(" WHERE ");
            List<Object> params = new ArrayList<>();
            where.appendTo(sql, params);
        }

        if (!orderByColumns.isEmpty()) {
            sql.append(" ORDER BY ");
            for (int i = 0; i < orderByColumns.size(); i++) {
                if (i > 0) sql.append(", ");
                sql.append(orderByColumns.get(i));
                sql.append(orderByDirections.get(i) ? " ASC" : " DESC");
            }
        }

        if (limit != null) {
            sql.append(" LIMIT ").append(limit);
        }

        if (offset != null) {
            sql.append(" OFFSET ").append(offset);
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
     * Executes the SELECT query and maps the result rows to a list of objects
     * of the given type.
     *
     * @param type the target class for result row mapping
     * @param <T>  the result type
     * @return list of mapped results (never null)
     * @throws JdbOrmException if SQL execution or mapping fails
     */
    public <T> List<T> execute(Class<T> type) {
        Connection conn = jdborm.getConnection();
        try {
            return executeWithConnection(conn, type);
        } finally {
            if (jdborm.isUseDataSource() && conn != null) {
                try {
                    conn.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    private <T> List<T> executeWithConnection(Connection conn, Class<T> type) {
        String sql = toSql();
        List<Object> params = getParameters();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                ResultMapper mapper = new ResultMapper();
                return mapper.mapAll(rs, type);
            }
        } catch (SQLException e) {
            throw new JdbOrmException("Failed to execute SELECT: " + sql, e);
        }
    }
}
