package com.bitaspire.jdborm.exception;

/**
 * Unchecked exception thrown throughout the JdbORM library for all
 * error conditions, including SQL failures, missing table names,
 * missing values, and reflection errors.
 */
public class JdbOrmException extends RuntimeException {

    /**
     * Creates a new exception with a detail message.
     *
     * @param message the detail message
     */
    public JdbOrmException(String message) {
        super(message);
    }

    /**
     * Creates a new exception with a detail message and cause.
     *
     * @param message the detail message
     * @param cause   the underlying cause
     */
    public JdbOrmException(String message, Throwable cause) {
        super(message, cause);
    }
}
