package com.bitaspire.jdborm.query;

import com.bitaspire.jdborm.JdbORM;
import com.bitaspire.jdborm.exception.JdbOrmException;
import com.bitaspire.jdborm.schema.Column;
import com.bitaspire.jdborm.schema.Table;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Builder for CREATE TABLE statements.
 * <p>
 * Supports defining columns with type definitions, table constraints (PRIMARY KEY, FOREIGN KEY, UNIQUE),
 * and IF NOT EXISTS semantics.
 * </p>
 */
public class CreateTableQuery implements Query {

    private final JdbORM jdborm;
    private final String table;
    private final List<String> columnDefs;
    private final List<String> tableConstraints;
    private boolean ifNotExists;

    /**
     * Creates a new CREATE TABLE builder for the given table.
     *
     * @param jdborm the JdbORM instance for connection management
     * @param table  the table name to create
     */
    public CreateTableQuery(JdbORM jdborm, String table) {
        this.jdborm = jdborm;
        this.table = table;
        this.columnDefs = new ArrayList<>();
        this.tableConstraints = new ArrayList<>();
    }

    /**
     * Adds a column definition to the CREATE TABLE statement.
     *
     * @param name       the column name
     * @param definition the column type and constraints (e.g. "VARCHAR(100) NOT NULL")
     * @return this builder for chaining
     */
    public CreateTableQuery column(String name, String definition) {
        columnDefs.add(name + " " + definition);
        return this;
    }

    /**
     * Adds a column definition using a type-safe {@link Column} reference.
     *
     * @param column     the column reference
     * @param definition the column type and constraints
     * @return this builder for chaining
     */
    public CreateTableQuery column(Column<?> column, String definition) {
        columnDefs.add(column.name() + " " + definition);
        return this;
    }

    /**
     * Adds a PRIMARY KEY constraint at the table level.
     *
     * @param columns the column(s) that form the primary key
     * @return this builder for chaining
     */
    public CreateTableQuery primaryKey(String... columns) {
        tableConstraints.add("PRIMARY KEY (" + String.join(", ", columns) + ")");
        return this;
    }

    /**
     * Adds a FOREIGN KEY constraint at the table level.
     *
     * @param column    the local column
     * @param reference the referenced table and column (e.g. "users(id)")
     * @return this builder for chaining
     */
    public CreateTableQuery foreignKey(String column, String reference) {
        tableConstraints.add("FOREIGN KEY (" + column + ") REFERENCES " + reference);
        return this;
    }

    /**
     * Adds a UNIQUE constraint at the table level.
     *
     * @param columns the column(s) that should be unique
     * @return this builder for chaining
     */
    public CreateTableQuery unique(String... columns) {
        tableConstraints.add("UNIQUE (" + String.join(", ", columns) + ")");
        return this;
    }

    /**
     * Adds IF NOT EXISTS to the CREATE TABLE statement.
     *
     * @return this builder for chaining
     */
    public CreateTableQuery ifNotExists() {
        this.ifNotExists = true;
        return this;
    }

    /**
     * Adds a CHECK constraint at the table level.
     *
     * @param expression the check expression (e.g. "age >= 0")
     * @return this builder for chaining
     */
    public CreateTableQuery check(String expression) {
        tableConstraints.add("CHECK (" + expression + ")");
        return this;
    }

    @Override
    public String toSql() {
        StringBuilder sql = new StringBuilder("CREATE TABLE ");
        if (ifNotExists) {
            sql.append("IF NOT EXISTS ");
        }
        sql.append(table).append(" (");

        List<String> parts = new ArrayList<>(columnDefs);
        parts.addAll(tableConstraints);
        sql.append(String.join(", ", parts));
        sql.append(")");
        return sql.toString();
    }

    @Override
    public List<Object> getParameters() {
        return List.of();
    }

    /**
     * Executes the CREATE TABLE statement.
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
            throw new JdbOrmException("Failed to execute CREATE TABLE: " + sql, e);
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
