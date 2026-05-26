package com.bitaspire.jdborm.schema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Declarative definition of a database index used by {@link Schema}.
 * <p>
 * Index definitions are normally created through
 * {@link TableDefinition#index(String, java.util.function.Consumer)} so the
 * owning table can be filled automatically.
 * </p>
 */
public final class IndexDefinition {

    private final String name;
    private String table;
    private final List<String> columns = new ArrayList<>();
    private boolean unique;
    private String method;

    /**
     * Creates a new index definition.
     *
     * @param name the database index name
     * @throws NullPointerException if {@code name} is {@code null}
     */
    public IndexDefinition(String name) {
        this.name = Objects.requireNonNull(name, "name");
    }

    /**
     * Returns the database index name.
     *
     * @return the index name
     */
    public String name() {
        return name;
    }

    /**
     * Returns the table on which this index is created.
     *
     * @return the table name, or {@code null} if it has not been assigned yet
     */
    public String table() {
        return table;
    }

    /**
     * Returns the indexed columns in declaration order.
     *
     * @return an immutable list of column names
     */
    public List<String> columns() {
        return Collections.unmodifiableList(columns);
    }

    /**
     * Returns whether this index is unique.
     *
     * @return {@code true} for a unique index, otherwise {@code false}
     */
    public boolean isUnique() {
        return unique;
    }

    /**
     * Returns the optional index method.
     *
     * @return the index method, or {@code null} if none was configured
     */
    public String method() {
        return method;
    }

    /**
     * Sets the table on which this index is created.
     *
     * @param table the database table name
     * @return this index definition for chaining
     * @throws NullPointerException if {@code table} is {@code null}
     */
    public IndexDefinition onTable(String table) {
        this.table = Objects.requireNonNull(table, "table");
        return this;
    }

    /**
     * Sets the indexed columns.
     *
     * @param columns the column names to index
     * @return this index definition for chaining
     * @throws NullPointerException if {@code columns} is {@code null}
     */
    public IndexDefinition on(String... columns) {
        Objects.requireNonNull(columns, "columns");
        this.columns.clear();
        for (String column : columns) {
            this.columns.add(Objects.requireNonNull(column, "column"));
        }
        return this;
    }

    /**
     * Sets the indexed columns using type-safe column references.
     *
     * @param columns the column references to index
     * @return this index definition for chaining
     * @throws NullPointerException if {@code columns} is {@code null}
     */
    public IndexDefinition on(Column<?>... columns) {
        Objects.requireNonNull(columns, "columns");
        this.columns.clear();
        for (Column<?> column : columns) {
            this.columns.add(Objects.requireNonNull(column, "column").name());
        }
        return this;
    }

    /**
     * Marks this index as unique.
     *
     * @return this index definition for chaining
     */
    public IndexDefinition uniqueIndex() {
        this.unique = true;
        return this;
    }

    /**
     * Marks this index as unique.
     * <p>
     * This alias is provided for a natural DSL style: {@code idx.on("email").unique()}.
     * </p>
     *
     * @return this index definition for chaining
     */
    public IndexDefinition unique() {
        return uniqueIndex();
    }

    /**
     * Sets the index method, for example {@code BTREE}, {@code HASH}, or {@code GIN}.
     *
     * @param method the SQL index method
     * @return this index definition for chaining
     * @throws NullPointerException if {@code method} is {@code null}
     */
    public IndexDefinition using(String method) {
        this.method = Objects.requireNonNull(method, "method");
        return this;
    }

    /**
     * Converts this index definition to a CREATE INDEX SQL statement.
     *
     * @return the CREATE INDEX SQL statement with {@code IF NOT EXISTS}
     */
    public String toSql() {
        return toSql(true);
    }

    /**
     * Converts this index definition to a CREATE INDEX SQL statement.
     *
     * @param ifNotExists whether {@code IF NOT EXISTS} should be included
     * @return the CREATE INDEX SQL statement
     * @throws IllegalStateException if no table or columns were configured
     */
    public String toSql(boolean ifNotExists) {
        if (table == null || table.isBlank()) {
            throw new IllegalStateException("Index table must be configured");
        }
        if (columns.isEmpty()) {
            throw new IllegalStateException("Index must contain at least one column");
        }

        StringBuilder sql = new StringBuilder("CREATE ");
        if (unique) {
            sql.append("UNIQUE ");
        }
        sql.append("INDEX ");
        if (ifNotExists) {
            sql.append("IF NOT EXISTS ");
        }
        sql.append(name).append(" ON ").append(table);
        if (method != null) {
            sql.append(" USING ").append(method);
        }
        sql.append(" (").append(String.join(", ", columns)).append(")");
        return sql.toString();
    }
}
