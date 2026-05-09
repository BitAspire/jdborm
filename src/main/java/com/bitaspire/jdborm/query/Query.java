package com.bitaspire.jdborm.query;

import java.util.List;

/**
 * Common interface for all query types (SELECT, INSERT, UPDATE, DELETE).
 * <p>
 * Implementations provide the generated SQL string and the list of
 * parameter values to be used with a {@link java.sql.PreparedStatement}.
 * </p>
 */
public interface Query {

    /**
     * Returns the generated SQL string with {@code ?} placeholders for parameters.
     *
     * @return the SQL string
     */
    String toSql();

    /**
     * Returns the list of parameter values corresponding to the {@code ?}
     * placeholders in the SQL string, in order.
     *
     * @return list of parameter values
     */
    List<Object> getParameters();
}
