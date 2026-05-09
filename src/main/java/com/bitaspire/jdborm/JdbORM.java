package com.bitaspire.jdborm;

import com.bitaspire.jdborm.query.DeleteQuery;
import com.bitaspire.jdborm.query.InsertQuery;
import com.bitaspire.jdborm.query.SelectQuery;
import com.bitaspire.jdborm.query.UpdateQuery;
import com.bitaspire.jdborm.exception.JdbOrmException;
import com.bitaspire.jdborm.schema.Column;
import com.bitaspire.jdborm.schema.Table;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;

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

    private JdbORM(DataSource dataSource) {
        this.dataSource = dataSource;
        this.connection = null;
        this.useDataSource = true;
    }

    private JdbORM(Connection connection) {
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
     * @param url      the JDBC connection URL (e.g. {@code "jdbc:hsqldb:mem:mydb"})
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
     * @param url the JDBC connection URL (e.g. {@code "jdbc:hsqldb:mem:mydb"})
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
