package com.bitaspire.jdborm.schema;

/**
 * A type-safe column reference that carries both the column name and its Java type.
 * <p>
 * Use {@link #of(String)} to create a column definition, then pass it to
 * the type-safe overloads in {@link com.bitaspire.jdborm.condition.Conditions},
 * {@link com.bitaspire.jdborm.query.SelectQuery}, {@link com.bitaspire.jdborm.query.InsertQuery},
 * and {@link com.bitaspire.jdborm.query.UpdateQuery}.
 * </p>
 *
 * <pre>{@code
 * Column<Integer> AGE = Column.of("age");
 * Column<String> NAME = Column.of("name");
 *
 * db.select(AGE, NAME).from(USERS).where(eq(AGE, 25));
 * }</pre>
 *
 * @param <T> the Java type of values stored in this column
 */
public final class Column<T> {

    private final String name;
    private final String qualifiedName;

    private Column(String name, String qualifiedName) {
        this.name = name;
        this.qualifiedName = qualifiedName;
    }

    /**
     * Creates a new column with the given SQL column name.
     *
     * @param name the column name as it appears in the database
     * @param <T>  the Java type of values stored in this column
     * @return a new Column instance
     */
    public static <T> Column<T> of(String name) {
        return new Column<>(name, name);
    }

    /**
     * Returns a new Column qualified with the given table or alias prefix
     * (e.g. {@code "u"."id"}).
     *
     * @param qualifier the table name or alias to qualify with
     * @return a new Column with the qualified name
     */
    public Column<T> qualifiedBy(String qualifier) {
        return new Column<>(name, qualifier + "." + name);
    }

    /**
     * Returns the bare column name without table qualifier.
     *
     * @return the column name
     */
    public String name() {
        return name;
    }

    /**
     * Returns the qualified column name (e.g. {@code "users"."name"} or just {@code "name"}).
     * Used when generating SQL.
     *
     * @return the qualified column name
     */
    public String qualifiedName() {
        return qualifiedName;
    }

    @Override
    public String toString() {
        return qualifiedName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Column<?> column)) return false;
        return qualifiedName.equals(column.qualifiedName);
    }

    @Override
    public int hashCode() {
        return qualifiedName.hashCode();
    }
}
