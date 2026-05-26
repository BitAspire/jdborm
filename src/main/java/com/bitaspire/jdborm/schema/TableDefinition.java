package com.bitaspire.jdborm.schema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Declarative definition of a database table used by {@link Schema}.
 * <p>
 * A table definition contains column definitions, table-level constraints, and
 * indexes that belong to the table.
 * </p>
 */
public final class TableDefinition {

    private final String name;
    private final List<ColumnDefinition> columns = new ArrayList<>();
    private final List<String> constraints = new ArrayList<>();
    private final List<IndexDefinition> indexes = new ArrayList<>();

    /**
     * Creates a new table definition.
     *
     * @param name the database table name
     * @throws NullPointerException if {@code name} is {@code null}
     */
    public TableDefinition(String name) {
        this.name = Objects.requireNonNull(name, "name");
    }

    /**
     * Returns the database table name.
     *
     * @return the table name
     */
    public String name() {
        return name;
    }

    /**
     * Returns the column definitions in declaration order.
     *
     * @return an immutable list of column definitions
     */
    public List<ColumnDefinition> columns() {
        return Collections.unmodifiableList(columns);
    }

    /**
     * Returns the table-level constraints in declaration order.
     *
     * @return an immutable list of table-level constraint fragments
     */
    public List<String> constraints() {
        return Collections.unmodifiableList(constraints);
    }

    /**
     * Returns the index definitions in declaration order.
     *
     * @return an immutable list of index definitions
     */
    public List<IndexDefinition> indexes() {
        return Collections.unmodifiableList(indexes);
    }

    /**
     * Adds a column using a full SQL definition fragment.
     *
     * @param name       the database column name
     * @param definition the SQL type and inline constraints, for example {@code "VARCHAR(255) NOT NULL"}
     * @return this table definition for chaining
     * @throws NullPointerException if {@code name} or {@code definition} is {@code null}
     */
    public TableDefinition column(String name, String definition) {
        columns.add(new ColumnDefinition(name, definition));
        return this;
    }

    /**
     * Adds a column using a type-safe column reference and full SQL definition fragment.
     *
     * @param column     the type-safe column reference
     * @param definition the SQL type and inline constraints
     * @return this table definition for chaining
     * @throws NullPointerException if {@code column} or {@code definition} is {@code null}
     */
    public TableDefinition column(Column<?> column, String definition) {
        Objects.requireNonNull(column, "column");
        return column(column.name(), definition);
    }

    /**
     * Adds a column and configures it through a column builder callback.
     *
     * @param name    the database column name
     * @param type    the SQL column type
     * @param builder the callback used to add inline constraints
     * @return this table definition for chaining
     * @throws NullPointerException if {@code name}, {@code type}, or {@code builder} is {@code null}
     */
    public TableDefinition column(String name, String type, Consumer<ColumnDefinition> builder) {
        Objects.requireNonNull(builder, "builder");
        ColumnDefinition column = new ColumnDefinition(name, type);
        builder.accept(column);
        columns.add(column);
        return this;
    }

    /**
     * Adds a type-safe column and configures it through a column builder callback.
     *
     * @param column  the type-safe column reference
     * @param type    the SQL column type
     * @param builder the callback used to add inline constraints
     * @return this table definition for chaining
     * @throws NullPointerException if {@code column}, {@code type}, or {@code builder} is {@code null}
     */
    public TableDefinition column(Column<?> column, String type, Consumer<ColumnDefinition> builder) {
        Objects.requireNonNull(column, "column");
        return column(column.name(), type, builder);
    }

    /**
     * Adds a table-level primary key constraint.
     *
     * @param columns the columns that make up the primary key
     * @return this table definition for chaining
     * @throws NullPointerException if {@code columns} is {@code null}
     */
    public TableDefinition primaryKey(String... columns) {
        Objects.requireNonNull(columns, "columns");
        constraints.add("PRIMARY KEY (" + String.join(", ", columns) + ")");
        return this;
    }

    /**
     * Adds a table-level unique constraint.
     *
     * @param columns the columns that must be unique together
     * @return this table definition for chaining
     * @throws NullPointerException if {@code columns} is {@code null}
     */
    public TableDefinition unique(String... columns) {
        Objects.requireNonNull(columns, "columns");
        constraints.add("UNIQUE (" + String.join(", ", columns) + ")");
        return this;
    }

    /**
     * Adds a table-level foreign key constraint.
     *
     * @param column    the local column name
     * @param reference the referenced table and columns, for example {@code "users(id)"}
     * @return this table definition for chaining
     * @throws NullPointerException if {@code column} or {@code reference} is {@code null}
     */
    public TableDefinition foreignKey(String column, String reference) {
        constraints.add("FOREIGN KEY (" + Objects.requireNonNull(column, "column") + ") REFERENCES "
                + Objects.requireNonNull(reference, "reference"));
        return this;
    }

    /**
     * Adds a table-level check constraint.
     *
     * @param expression the check expression without surrounding parentheses
     * @return this table definition for chaining
     * @throws NullPointerException if {@code expression} is {@code null}
     */
    public TableDefinition check(String expression) {
        constraints.add("CHECK (" + Objects.requireNonNull(expression, "expression") + ")");
        return this;
    }

    /**
     * Adds a raw table-level constraint fragment.
     *
     * @param constraint the raw SQL constraint fragment
     * @return this table definition for chaining
     * @throws NullPointerException if {@code constraint} is {@code null}
     */
    public TableDefinition constraint(String constraint) {
        constraints.add(Objects.requireNonNull(constraint, "constraint"));
        return this;
    }

    /**
     * Adds an index using a builder callback.
     *
     * @param name    the database index name
     * @param builder the callback used to configure columns, uniqueness, and method
     * @return this table definition for chaining
     * @throws NullPointerException if {@code name} or {@code builder} is {@code null}
     */
    public TableDefinition index(String name, Consumer<IndexDefinition> builder) {
        Objects.requireNonNull(builder, "builder");
        IndexDefinition index = new IndexDefinition(name).onTable(this.name);
        builder.accept(index);
        indexes.add(index);
        return this;
    }

    /**
     * Adds a non-unique index on the given columns.
     *
     * @param name    the database index name
     * @param columns the column names to index
     * @return this table definition for chaining
     * @throws NullPointerException if {@code name} or {@code columns} is {@code null}
     */
    public TableDefinition index(String name, String... columns) {
        indexes.add(new IndexDefinition(name).onTable(this.name).on(columns));
        return this;
    }

    /**
     * Converts this table definition to a CREATE TABLE SQL statement with {@code IF NOT EXISTS}.
     *
     * @return the CREATE TABLE SQL statement
     */
    public String toSql() {
        return toSql(true);
    }

    /**
     * Converts this table definition to a CREATE TABLE SQL statement.
     *
     * @param ifNotExists whether {@code IF NOT EXISTS} should be included
     * @return the CREATE TABLE SQL statement
     * @throws IllegalStateException if the table has no columns
     */
    public String toSql(boolean ifNotExists) {
        if (columns.isEmpty()) {
            throw new IllegalStateException("Table must contain at least one column");
        }

        StringBuilder sql = new StringBuilder("CREATE TABLE ");
        if (ifNotExists) {
            sql.append("IF NOT EXISTS ");
        }
        sql.append(name).append(" (");

        List<String> parts = new ArrayList<>();
        for (ColumnDefinition column : columns) {
            parts.add(column.toSql());
        }
        parts.addAll(constraints);

        sql.append(String.join(", ", parts));
        sql.append(")");
        return sql.toString();
    }
}
