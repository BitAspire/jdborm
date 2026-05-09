package com.bitaspire.jdborm.query;

import com.bitaspire.jdborm.JdbORM;
import com.bitaspire.jdborm.exception.JdbOrmException;
import com.bitaspire.jdborm.schema.Column;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Builder for ALTER TABLE statements.
 * <p>
 * Supports ADD COLUMN (with optional IF NOT EXISTS), DROP COLUMN, RENAME COLUMN,
 * MODIFY COLUMN, ADD CONSTRAINT (PRIMARY KEY, FOREIGN KEY, UNIQUE, CHECK),
 * DROP CONSTRAINT, and DROP PRIMARY KEY operations.
 * </p>
 */
public class AlterTableQuery implements Query {

    private final JdbORM jdborm;
    private final String table;
    private final List<String> clauses;

    /**
     * Creates a new ALTER TABLE builder for the given table.
     *
     * @param jdborm the JdbORM instance for connection management
     * @param table  the table name to alter
     */
    public AlterTableQuery(JdbORM jdborm, String table) {
        this.jdborm = jdborm;
        this.table = table;
        this.clauses = new ArrayList<>();
    }

    /**
     * Adds a column to the table.
     *
     * @param name       the column name
     * @param definition the column type and constraints
     * @return this builder for chaining
     */
    public AlterTableQuery addColumn(String name, String definition) {
        clauses.add("ADD COLUMN " + name + " " + definition);
        return this;
    }

    /**
     * Adds a column using a type-safe {@link Column} reference.
     *
     * @param column     the column reference
     * @param definition the column type and constraints
     * @return this builder for chaining
     */
    public AlterTableQuery addColumn(Column<?> column, String definition) {
        clauses.add("ADD COLUMN " + column.name() + " " + definition);
        return this;
    }

    /**
     * Adds a column with IF NOT EXISTS (supported by PostgreSQL and others).
     *
     * @param name       the column name
     * @param definition the column type and constraints
     * @return this builder for chaining
     */
    public AlterTableQuery addColumnIfNotExists(String name, String definition) {
        clauses.add("ADD COLUMN IF NOT EXISTS " + name + " " + definition);
        return this;
    }

    /**
     * Adds a column with IF NOT EXISTS using a type-safe {@link Column} reference.
     *
     * @param column     the column reference
     * @param definition the column type and constraints
     * @return this builder for chaining
     */
    public AlterTableQuery addColumnIfNotExists(Column<?> column, String definition) {
        clauses.add("ADD COLUMN IF NOT EXISTS " + column.name() + " " + definition);
        return this;
    }

    /**
     * Drops a column from the table.
     *
     * @param name the column name to drop
     * @return this builder for chaining
     */
    public AlterTableQuery dropColumn(String name) {
        clauses.add("DROP COLUMN " + name);
        return this;
    }

    /**
     * Drops a column using a type-safe {@link Column} reference.
     *
     * @param column the column to drop
     * @return this builder for chaining
     */
    public AlterTableQuery dropColumn(Column<?> column) {
        clauses.add("DROP COLUMN " + column.name());
        return this;
    }

    /**
     * Renames a column.
     *
     * @param oldName the current column name
     * @param newName the new column name
     * @return this builder for chaining
     */
    public AlterTableQuery renameColumn(String oldName, String newName) {
        clauses.add("RENAME COLUMN " + oldName + " TO " + newName);
        return this;
    }

    /**
     * Modifies a column's definition.
     *
     * @param name       the column name
     * @param definition the new column type and constraints
     * @return this builder for chaining
     */
    public AlterTableQuery modifyColumn(String name, String definition) {
        clauses.add("MODIFY COLUMN " + name + " " + definition);
        return this;
    }

    /**
     * Adds a PRIMARY KEY constraint.
     *
     * @param columns the column(s) that form the primary key
     * @return this builder for chaining
     */
    public AlterTableQuery addPrimaryKey(String... columns) {
        clauses.add("ADD PRIMARY KEY (" + String.join(", ", columns) + ")");
        return this;
    }

    /**
     * Adds a FOREIGN KEY constraint.
     *
     * @param column    the local column
     * @param reference the referenced table and column (e.g. "users(id)")
     * @return this builder for chaining
     */
    public AlterTableQuery addForeignKey(String column, String reference) {
        clauses.add("ADD FOREIGN KEY (" + column + ") REFERENCES " + reference);
        return this;
    }

    /**
     * Adds a UNIQUE constraint.
     *
     * @param columns the column(s) that should be unique
     * @return this builder for chaining
     */
    public AlterTableQuery addUnique(String... columns) {
        clauses.add("ADD UNIQUE (" + String.join(", ", columns) + ")");
        return this;
    }

    /**
     * Adds a CHECK constraint.
     *
     * @param expression the check expression (e.g. "age >= 0")
     * @return this builder for chaining
     */
    public AlterTableQuery addCheck(String expression) {
        clauses.add("ADD CHECK (" + expression + ")");
        return this;
    }

    /**
     * Drops a constraint by name.
     *
     * @param constraintName the name of the constraint to drop
     * @return this builder for chaining
     */
    public AlterTableQuery dropConstraint(String constraintName) {
        clauses.add("DROP CONSTRAINT " + constraintName);
        return this;
    }

    /**
     * Drops the PRIMARY KEY constraint.
     *
     * @return this builder for chaining
     */
    public AlterTableQuery dropPrimaryKey() {
        clauses.add("DROP PRIMARY KEY");
        return this;
    }

    @Override
    public String toSql() {
        if (clauses.isEmpty()) {
            return "ALTER TABLE " + table;
        }
        StringBuilder sql = new StringBuilder();
        for (int i = 0; i < clauses.size(); i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append("ALTER TABLE ").append(table).append(" ").append(clauses.get(i));
        }
        return sql.toString();
    }

    @Override
    public List<Object> getParameters() {
        return List.of();
    }

    /**
     * Executes all ALTER TABLE clauses.
     *
     * @throws JdbOrmException if SQL execution fails
     */
    public void execute() {
        String sql = toSql();
        Connection conn = jdborm.getConnection();
        try {
            String[] statements = sql.split(", (?=ALTER TABLE)");
            for (String stmtSql : statements) {
                try (PreparedStatement stmt = conn.prepareStatement(stmtSql)) {
                    stmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new JdbOrmException("Failed to execute ALTER TABLE: " + sql, e);
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
