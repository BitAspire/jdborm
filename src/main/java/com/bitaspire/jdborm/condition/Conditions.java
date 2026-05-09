package com.bitaspire.jdborm.condition;

/**
 * Static factory for constructing {@link Condition} objects.
 * <p>
 * Designed for static imports to enable a concise, DSL-like syntax:
 * </p>
 * <pre>{@code
 * import static com.bitaspire.jdborm.condition.Conditions.*;
 *
 * where(eq("age", 18).and(gt("score", 100)))
 * }</pre>
 */
public final class Conditions {

    private Conditions() {
    }

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
