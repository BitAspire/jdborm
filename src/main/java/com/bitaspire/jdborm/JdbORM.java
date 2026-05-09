package com.bitaspire.jdborm;

import com.bitaspire.jdborm.exception.JdbOrmException;
import com.bitaspire.jdborm.mapper.ResultMapper;
import com.bitaspire.jdborm.mapper.RowMapper;
import com.bitaspire.jdborm.query.DeleteQuery;
import com.bitaspire.jdborm.query.InsertQuery;
import com.bitaspire.jdborm.query.SelectQuery;
import com.bitaspire.jdborm.query.TransactionCallback;
import com.bitaspire.jdborm.query.UpdateQuery;
import com.bitaspire.jdborm.schema.Column;
import com.bitaspire.jdborm.schema.Table;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Entry point for the JdbORM fluent query builder API.
 * <p>
 * Create an instance via {@link #create(DataSource)} or {@link #create(Connection)},
 * then build and execute queries using the fluent methods. Both string-based and
 * type-safe ({@link Column}, {@link Table}) overloads are provided.
 * </p>
 */
public class JdbORM {

    private final DataSource dataSource;
    private final Connection connection;
    private final boolean useDataSource;

    JdbORM(DataSource dataSource) {
        this.dataSource = dataSource;
        this.connection = null;
        this.useDataSource = true;
    }

    JdbORM(Connection connection) {
        this.dataSource = null;
        this.connection = connection;
        this.useDataSource = false;
    }

    /**
     * Creates a new JdbORM instance backed by a {@link DataSource}.
     * Each executed query borrows and returns a connection from the pool.
     *
     * @param dataSource the DataSource to obtain connections from
     * @return a new JdbORM instance
     */
    public static JdbORM create(DataSource dataSource) {
        return new JdbORM(dataSource);
    }

    /**
     * Creates a new JdbORM instance backed by a specific {@link Connection}.
     * The connection is used directly and must be managed (closed) by the caller.
     *
     * @param connection the database connection to use
     * @return a new JdbORM instance
     */
    public static JdbORM create(Connection connection) {
        return new JdbORM(connection);
    }

    /**
     * Creates a new JdbORM instance connected directly to the given JDBC URL.
     * The connection is managed internally and can be closed via {@link #close()}.
     *
     * @param url      the JDBC connection URL
     * @param user     the database user name
     * @param password the database password
     * @return a new JdbORM instance with an active connection
     * @throws JdbOrmException if the connection cannot be established
     */
    public static JdbORM connect(String url, String user, String password) {
        try {
            Connection conn = DriverManager.getConnection(url, user, password);
            return new JdbORM(conn);
        } catch (SQLException e) {
            throw new JdbOrmException("Failed to connect to " + url, e);
        }
    }

    /**
     * Creates a new JdbORM instance connected directly to the given JDBC URL
     * with no authentication.
     * The connection is managed internally and can be closed via {@link #close()}.
     *
     * @param url the JDBC connection URL
     * @return a new JdbORM instance with an active connection
     * @throws JdbOrmException if the connection cannot be established
     */
    public static JdbORM connect(String url) {
        try {
            Connection conn = DriverManager.getConnection(url);
            return new JdbORM(conn);
        } catch (SQLException e) {
            throw new JdbOrmException("Failed to connect to " + url, e);
        }
    }

    /**
     * Starts building a SELECT query for the given columns (string-based).
     *
     * @param columns column names to select; if empty, selects all columns ({@code *})
     * @return a new {@link SelectQuery} builder
     */
    public SelectQuery select(String... columns) {
        return new SelectQuery(this, columns);
    }

    /**
     * Starts building a SELECT query for the given type-safe columns.
     *
     * @param columns {@link Column} references to select; if empty, selects all columns ({@code *})
     * @return a new {@link SelectQuery} builder
     */
    public SelectQuery select(Column<?>... columns) {
        String[] names = Arrays.stream(columns).map(Column::qualifiedName).toArray(String[]::new);
        return new SelectQuery(this, names);
    }

    /**
     * Starts building an INSERT query for the given table (string-based).
     *
     * @param table the target table name
     * @return a new {@link InsertQuery} builder
     */
    public InsertQuery insert(String table) {
        return new InsertQuery(this, table);
    }

    /**
     * Starts building an INSERT query for the given type-safe table.
     *
     * @param table the target {@link Table}
     * @return a new {@link InsertQuery} builder
     */
    public InsertQuery insert(Table table) {
        return new InsertQuery(this, table.reference());
    }

    /**
     * Starts building an UPDATE query for the given table (string-based).
     *
     * @param table the target table name
     * @return a new {@link UpdateQuery} builder
     */
    public UpdateQuery update(String table) {
        return new UpdateQuery(this, table);
    }

    /**
     * Starts building an UPDATE query for the given type-safe table.
     *
     * @param table the target {@link Table}
     * @return a new {@link UpdateQuery} builder
     */
    public UpdateQuery update(Table table) {
        return new UpdateQuery(this, table.reference());
    }

    /**
     * Starts building a DELETE query for the given table (string-based).
     *
     * @param table the target table name
     * @return a new {@link DeleteQuery} builder
     */
    public DeleteQuery delete(String table) {
        return new DeleteQuery(this, table);
    }

    /**
     * Starts building a DELETE query for the given type-safe table.
     *
     * @param table the target {@link Table}
     * @return a new {@link DeleteQuery} builder
     */
    public DeleteQuery delete(Table table) {
        return new DeleteQuery(this, table.reference());
    }

    /**
     * Executes a raw SQL statement (INSERT/UPDATE/DELETE/DDL) with the given parameters.
     * <p>
     * Useful for one-off operations where the fluent builder is not needed.
     * </p>
     *
     * @param sql    the SQL statement with {@code ?} placeholders
     * @param params the parameter values for the placeholders
     * @return the number of affected rows
     * @throws JdbOrmException if execution fails
     */
    public int execute(String sql, Object... params) {
        Connection conn = getConnection();
        try {
            return executeWithConnection(conn, sql, params);
        } finally {
            if (useDataSource && conn != null) {
                try {
                    conn.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    private int executeWithConnection(Connection conn, String sql, Object... params) {
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            return stmt.executeUpdate();
        } catch (SQLException e) {
            throw new JdbOrmException("Failed to execute: " + sql, e);
        }
    }

    /**
     * Executes a raw SELECT query and maps each row using the given {@link RowMapper}.
     *
     * @param sql    the SELECT statement with {@code ?} placeholders
     * @param mapper the row mapper to convert each row to the result type
     * @param params the parameter values for the placeholders
     * @param <T>    the result type
     * @return list of mapped results (never null)
     * @throws JdbOrmException if query execution or mapping fails
     */
    public <T> List<T> query(String sql, RowMapper<T> mapper, Object... params) {
        Connection conn = getConnection();
        try {
            return queryWithConnection(conn, sql, mapper, params);
        } finally {
            if (useDataSource && conn != null) {
                try {
                    conn.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    private <T> List<T> queryWithConnection(Connection conn, String sql, RowMapper<T> mapper, Object... params) {
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                List<T> results = new ArrayList<>();
                int rowNum = 0;
                while (rs.next()) {
                    results.add(mapper.mapRow(rs, rowNum++));
                }
                return results;
            }
        } catch (SQLException e) {
            throw new JdbOrmException("Failed to query: " + sql, e);
        }
    }

    /**
     * Executes a raw SELECT query and maps the first result row to the given type.
     * <p>
     * Uses reflection-based mapping ({@link ResultMapper}) internally.
     * Returns {@code null} if no rows match.
     * </p>
     *
     * @param sql    the SELECT statement with {@code ?} placeholders
     * @param type   the target class for the result
     * @param params the parameter values for the placeholders
     * @param <T>    the result type
     * @return the first mapped row, or {@code null} if no results
     * @throws JdbOrmException if query execution or mapping fails
     */
    public <T> T querySingle(String sql, Class<T> type, Object... params) {
        List<T> results = query(sql, (rs, rowNum) -> {
            ResultMapper mapper = new ResultMapper();
            return mapper.mapRow(rs, type);
        }, params);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * Executes the given callback within a transaction.
     * <p>
     * A single connection is borrowed (or reused if already on a direct connection),
     * auto-commit is set to {@code false}, and the callback receives a JdbORM instance
     * that shares that connection. If the callback completes normally the transaction
     * is committed; if any exception is thrown it is rolled back.
     * </p>
     *
     * @param callback the transactional work to execute
     * @param <T>      the return type of the callback
     * @return the value returned by the callback
     * @throws JdbOrmException if the transaction fails (the cause is the original exception)
     */
    public <T> T inTransaction(TransactionCallback<T> callback) {
        Connection conn = getConnection();
        boolean closeConn = useDataSource;
        try {
            conn.setAutoCommit(false);
            JdbORM txDb = useDataSource ? new JdbORM(conn) : this;
            T result = callback.execute(txDb);
            conn.commit();
            return result;
        } catch (Exception e) {
            try {
                conn.rollback();
            } catch (SQLException ignored) {
            }
            throw new JdbOrmException("Transaction failed", e);
        } finally {
            try {
                conn.setAutoCommit(true);
                if (closeConn) {
                    conn.close();
                }
            } catch (SQLException ignored) {
            }
        }
    }

    /**
     * Returns a database connection. If this instance was created with a
     * {@link DataSource}, a new connection is borrowed. Otherwise the
     * pre-configured connection is returned directly.
     *
     * @return a JDBC {@link Connection}
     * @throws JdbOrmException if obtaining a connection from the DataSource fails
     */
    public Connection getConnection() {
        if (useDataSource) {
            try {
                return dataSource.getConnection();
            } catch (SQLException e) {
                throw new JdbOrmException("Failed to obtain connection from DataSource", e);
            }
        }
        return connection;
    }

    /**
     * Returns whether this instance uses a {@link DataSource} for connection management.
     *
     * @return {@code true} if backed by a DataSource, {@code false} if backed by a direct Connection
     */
    public boolean isUseDataSource() {
        return useDataSource;
    }

    /**
     * Closes the underlying connection if this instance was created via
     * {@link #connect(String)} or {@link #connect(String, String, String)}.
     * Does nothing if backed by a {@link DataSource}.
     */
    public void close() {
        if (!useDataSource && connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                throw new JdbOrmException("Failed to close connection", e);
            }
        }
    }
}
