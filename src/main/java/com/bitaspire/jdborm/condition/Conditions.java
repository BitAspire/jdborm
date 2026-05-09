package com.bitaspire.jdborm.condition;

import com.bitaspire.jdborm.schema.Column;

/**
 * Static factory for constructing {@link Condition} objects.
 * <p>
 * Designed for static imports to enable a concise, DSL-like syntax:
 * </p>
 * <pre>{@code
 * import static com.bitaspire.jdborm.condition.Conditions.*;
 *
 * where(eq("age", 18).and(gt("score", 100)))
 *
 * // Type-safe variants using Column&lt;T&gt;:
 * Column&lt;Integer&gt; AGE = Column.of("age");
 * where(eq(AGE, 18));
 * }</pre>
 */
public final class Conditions {

    private Conditions() {
    }

    // ── String-based (legacy) ────────────────────────────────────────────

    /** Equal to: {@code column = ?}. */
    public static Condition eq(String column, Object value) {
        return new SimpleCondition(column, "=", value);
    }

    /** Not equal to: {@code column <> ?}. */
    public static Condition ne(String column, Object value) {
        return new SimpleCondition(column, "<>", value);
    }

    /** Greater than: {@code column > ?}. */
    public static Condition gt(String column, Object value) {
        return new SimpleCondition(column, ">", value);
    }

    /** Greater than or equal: {@code column >= ?}. */
    public static Condition gte(String column, Object value) {
        return new SimpleCondition(column, ">=", value);
    }

    /** Less than: {@code column < ?}. */
    public static Condition lt(String column, Object value) {
        return new SimpleCondition(column, "<", value);
    }

    /** Less than or equal: {@code column <= ?}. */
    public static Condition lte(String column, Object value) {
        return new SimpleCondition(column, "<=", value);
    }

    /** LIKE pattern match: {@code column LIKE ?}. */
    public static Condition like(String column, Object value) {
        return new SimpleCondition(column, "LIKE", value);
    }

    /** IN list: {@code column IN (?, ?, ...)}. */
    @SuppressWarnings("varargs")
    public static Condition in(String column, Object... values) {
        return new InCondition(column, values);
    }

    /** BETWEEN range: {@code column BETWEEN ? AND ?}. */
    public static Condition between(String column, Object start, Object end) {
        return new BetweenCondition(column, start, end);
    }

    /** IS NULL check: {@code column IS NULL}. */
    public static Condition isNull(String column) {
        return new IsNullCondition(column, false);
    }

    /** IS NOT NULL check: {@code column IS NOT NULL}. */
    public static Condition isNotNull(String column) {
        return new IsNullCondition(column, true);
    }

    // ── Type-safe Column&lt;T&gt; overloads ─────────────────────────────

    /** Equal to: {@code column = ?} (type-safe). */
    public static <T> Condition eq(Column<T> column, T value) {
        return new SimpleCondition(column.qualifiedName(), "=", value);
    }

    /** Not equal to: {@code column <> ?} (type-safe). */
    public static <T> Condition ne(Column<T> column, T value) {
        return new SimpleCondition(column.qualifiedName(), "<>", value);
    }

    /** Greater than: {@code column > ?} (type-safe). */
    public static <T> Condition gt(Column<T> column, T value) {
        return new SimpleCondition(column.qualifiedName(), ">", value);
    }

    /** Greater than or equal: {@code column >= ?} (type-safe). */
    public static <T> Condition gte(Column<T> column, T value) {
        return new SimpleCondition(column.qualifiedName(), ">=", value);
    }

    /** Less than: {@code column < ?} (type-safe). */
    public static <T> Condition lt(Column<T> column, T value) {
        return new SimpleCondition(column.qualifiedName(), "<", value);
    }

    /** Less than or equal: {@code column <= ?} (type-safe). */
    public static <T> Condition lte(Column<T> column, T value) {
        return new SimpleCondition(column.qualifiedName(), "<=", value);
    }

    /** LIKE pattern match: {@code column LIKE ?} (type-safe). */
    public static <T> Condition like(Column<T> column, T value) {
        return new SimpleCondition(column.qualifiedName(), "LIKE", value);
    }

    /** IN list: {@code column IN (?, ?, ...)} (type-safe). */
    @SafeVarargs
    public static <T> Condition in(Column<T> column, T... values) {
        return new InCondition(column.qualifiedName(), values);
    }

    /** BETWEEN range: {@code column BETWEEN ? AND ?} (type-safe). */
    public static <T> Condition between(Column<T> column, T start, T end) {
        return new BetweenCondition(column.qualifiedName(), start, end);
    }

    /** IS NULL check: {@code column IS NULL} (type-safe). */
    public static <T> Condition isNull(Column<T> column) {
        return new IsNullCondition(column.qualifiedName(), false);
    }

    /** IS NOT NULL check: {@code column IS NOT NULL} (type-safe). */
    public static <T> Condition isNotNull(Column<T> column) {
        return new IsNullCondition(column.qualifiedName(), true);
    }

    // ── Compound conditions ──────────────────────────────────────────────

    /** Combines multiple conditions with AND. */
    public static Condition and(Condition... conditions) {
        Condition result = conditions[0];
        for (int i = 1; i < conditions.length; i++) {
            result = new AndCondition(result, conditions[i]);
        }
        return result;
    }

    /** Combines multiple conditions with OR. */
    public static Condition or(Condition... conditions) {
        Condition result = conditions[0];
        for (int i = 1; i < conditions.length; i++) {
            result = new OrCondition(result, conditions[i]);
        }
        return result;
    }

    /** Negates a condition: {@code NOT (...)}. */
    public static Condition not(Condition condition) {
        return new NotCondition(condition);
    }

    /**
     * Creates a raw SQL fragment condition that is appended verbatim.
     * Use with caution — no escaping or parameterisation is applied.
     */
    public static Condition raw(String sql) {
        return new RawCondition(sql);
    }
}
