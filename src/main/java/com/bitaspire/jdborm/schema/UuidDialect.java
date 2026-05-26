package com.bitaspire.jdborm.schema;

/**
 * Database dialects supported by UUID default expression helpers.
 */
public enum UuidDialect {

    /**
     * PostgreSQL using the {@code pgcrypto} extension's {@code gen_random_uuid()} function.
     */
    POSTGRES,

    /**
     * MySQL using the built-in {@code UUID()} function.
     */
    MYSQL,

    /**
     * HSQLDB using the built-in {@code UUID()} function.
     */
    HSQLDB
}
