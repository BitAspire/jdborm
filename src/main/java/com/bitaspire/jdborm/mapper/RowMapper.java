package com.bitaspire.jdborm.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * A functional interface for mapping a single row of a {@link ResultSet} to a result object.
 * <p>
 * Used by {@link com.bitaspire.jdborm.JdbORM#query(String, RowMapper, Object...)}
 * and {@link com.bitaspire.jdborm.query.SelectQuery#execute(RowMapper)} for
 * custom result mapping without reflection.
 * </p>
 *
 * @param <T> the result type
 */
@FunctionalInterface
public interface RowMapper<T> {

    /**
     * Maps the current row of the given {@link ResultSet} to an object.
     *
     * @param rs     the ResultSet positioned at the current row (caller does NOT advance it)
     * @param rowNum the number of the current row (0-based)
     * @return the mapped result object
     * @throws SQLException if a database access error occurs
     */
    T mapRow(ResultSet rs, int rowNum) throws SQLException;
}
