package com.bitaspire.jdborm.schema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Declarative database schema definition inspired by Drizzle's schema files.
 * <p>
 * A schema groups table and index definitions so they can be converted to SQL
 * or pushed to a database via {@link com.bitaspire.jdborm.JdbORM#pushSchema(Schema)}.
 * The push operation is intentionally additive: it creates missing tables,
 * missing columns, and missing indexes without dropping existing objects.
 * </p>
 *
 * <pre>{@code
 * Schema schema = Schema.create()
 *     .table("users", table -> table
 *         .column("id", "INTEGER", col -> col.generatedByDefaultAsIdentity().primaryKey())
 *         .column("email", "VARCHAR(255)", col -> col.notNull().unique())
 *         .index("idx_users_email", idx -> idx.on("email").unique()));
 *
 * db.pushSchema(schema);
 * }</pre>
 */
public final class Schema {

    private final List<TableDefinition> tables = new ArrayList<>();

    private Schema() {
    }

    /**
     * Creates an empty schema definition.
     *
     * @return a new schema definition
     */
    public static Schema create() {
        return new Schema();
    }

    /**
     * Creates a schema definition and applies the given builder callback.
     *
     * @param builder the callback used to define tables and indexes
     * @return the populated schema definition
     * @throws NullPointerException if {@code builder} is {@code null}
     */
    public static Schema define(Consumer<Schema> builder) {
        Objects.requireNonNull(builder, "builder");
        Schema schema = new Schema();
        builder.accept(schema);
        return schema;
    }

    /**
     * Adds a table definition to this schema.
     *
     * @param name    the database table name
     * @param builder the callback used to define columns, constraints, and indexes
     * @return this schema for chaining
     * @throws NullPointerException if {@code name} or {@code builder} is {@code null}
     */
    public Schema table(String name, Consumer<TableDefinition> builder) {
        Objects.requireNonNull(builder, "builder");
        TableDefinition table = new TableDefinition(name);
        builder.accept(table);
        tables.add(table);
        return this;
    }

    /**
     * Adds a table definition to this schema using a type-safe table reference.
     *
     * @param table   the table reference whose database name should be used
     * @param builder the callback used to define columns, constraints, and indexes
     * @return this schema for chaining
     * @throws NullPointerException if {@code table} or {@code builder} is {@code null}
     */
    public Schema table(Table table, Consumer<TableDefinition> builder) {
        Objects.requireNonNull(table, "table");
        return table(table.name(), builder);
    }

    /**
     * Adds an already constructed table definition to this schema.
     *
     * @param table the table definition to add
     * @return this schema for chaining
     * @throws NullPointerException if {@code table} is {@code null}
     */
    public Schema addTable(TableDefinition table) {
        tables.add(Objects.requireNonNull(table, "table"));
        return this;
    }

    /**
     * Returns all table definitions in declaration order.
     *
     * @return an immutable list of table definitions
     */
    public List<TableDefinition> tables() {
        return Collections.unmodifiableList(tables);
    }

    /**
     * Converts the schema to idempotent SQL statements.
     * <p>
     * Table statements include {@code IF NOT EXISTS}; index statements include
     * {@code IF NOT EXISTS}. These statements are useful for previews and simple
     * bootstrapping. For additive diffing against an existing database, use
     * {@link com.bitaspire.jdborm.JdbORM#pushSchema(Schema)}.
     * </p>
     *
     * @return SQL statements needed to create the declared schema
     */
    public List<String> toSql() {
        List<String> statements = new ArrayList<>();
        for (TableDefinition table : tables) {
            statements.add(table.toSql(true));
            for (IndexDefinition index : table.indexes()) {
                statements.add(index.toSql(true));
            }
        }
        return statements;
    }
}
