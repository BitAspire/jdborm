package com.bitaspire.jdborm.schema;

/**
 * A table reference with an optional alias for use in type-safe queries.
 * <p>
 * Pass a {@code Table} instance to {@link com.bitaspire.jdborm.JdbORM#select(Column[])},
 * {@link com.bitaspire.jdborm.JdbORM#insert(Table)}, etc. to get type-safe
 * query building. Use {@link #column(Column)} to qualify a column with this
 * table's name or alias.
 * </p>
 *
 * <pre>{@code
 * Table USERS = Table.of("users");
 * Column<Integer> ID = Column.of("id");
 *
 * db.select(ID).from(USERS).where(eq(ID, 1));
 * }</pre>
 */
public final class Table {

    private final String name;
    private final String alias;

    private Table(String name, String alias) {
        this.name = name;
        this.alias = alias;
    }

    /**
     * Creates a new table reference with the given name.
     *
     * @param name the table name as it appears in the database
     * @return a new Table instance
     */
    public static Table of(String name) {
        return new Table(name, null);
    }

    /**
     * Creates a new table reference with a name and alias.
     *
     * @param name  the table name as it appears in the database
     * @param alias the alias to use in queries
     * @return a new Table instance
     */
    public static Table of(String name, String alias) {
        return new Table(name, alias);
    }

    /**
     * Returns the table name without alias.
     *
     * @return the table name
     */
    public String name() {
        return name;
    }

    /**
     * Returns the alias, or {@code null} if no alias was set.
     *
     * @return the alias, or {@code null}
     */
    public String alias() {
        return alias;
    }

    /**
     * Returns the SQL reference string: the table name followed by the alias
     * if one is set (e.g. {@code "users u"}).
     *
     * @return the SQL reference string
     */
    public String reference() {
        return alias != null ? name + " " + alias : name;
    }

    /**
     * Qualifies the given column with this table's alias (if set) or name.
     *
     * @param col the column to qualify
     * @param <T> the column's value type
     * @return a new Column qualified with this table's alias or name
     */
    public <T> Column<T> column(Column<T> col) {
        return col.qualifiedBy(alias != null ? alias : name);
    }

    @Override
    public String toString() {
        return reference();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Table table)) return false;
        return reference().equals(table.reference());
    }

    @Override
    public int hashCode() {
        return reference().hashCode();
    }
}
