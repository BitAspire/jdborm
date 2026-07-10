package com.bitaspire.jdborm.query;

import java.util.Objects;

/**
 * An immutable value object for a raw SQL expression used in {@code setRaw()} calls.
 * <p>
 * When passed as a value in {@link InsertQuery#setRaw(String, String)} or
 * {@link UpdateQuery#setRaw(String, String)}, the expression is inserted
 * verbatim into the generated SQL instead of a {@code ?} placeholder.
 * </p>
 *
 */
public final class RawExpression {

    private final String expression;

    /**
     * Creates a raw SQL expression.
     *
     * @param expression the raw SQL expression
     */
    public RawExpression(String expression) {
        this.expression = expression;
    }

    /**
     * Returns the raw SQL expression.
     *
     * @return the raw SQL expression
     */
    public String expression() {
        return expression;
    }

    /**
     * Compares this expression with another value object.
     *
     * @param other the value to compare
     * @return {@code true} when both expressions are equal
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof RawExpression)) {
            return false;
        }
        RawExpression that = (RawExpression) other;
        return Objects.equals(expression, that.expression);
    }

    /**
     * Returns a hash code derived from the expression.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(expression);
    }

    /**
     * Returns a string representation of this expression.
     *
     * @return a string representation of this expression
     */
    @Override
    public String toString() {
        return "RawExpression[expression=" + expression + "]";
    }
}
