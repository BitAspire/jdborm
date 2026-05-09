package com.bitaspire.jdborm.query;

/**
 * A marker record for a raw SQL expression used in {@code setRaw()} calls.
 * <p>
 * When passed as a value in {@link InsertQuery#setRaw(String, String)} or
 * {@link UpdateQuery#setRaw(String, String)}, the expression is inserted
 * verbatim into the generated SQL instead of a {@code ?} placeholder.
 * </p>
 *
 * @param expression the raw SQL expression (e.g. {@code "NOW()"}, {@code "DEFAULT"})
 */
public record RawExpression(String expression) {
}
