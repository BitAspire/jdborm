package com.bitaspire.jdborm.schema;

/**
 * Supported UUID versions for JdbORM schema helpers and Java-side UUID generation.
 * <p>
 * UUID version 4 is random. UUID version 7 is time-ordered and defined by
 * RFC 9562, making it a good default for database primary keys and indexes.
 * </p>
 */
public enum UuidVersion {

    /**
     * Random UUID as defined by RFC 4122/RFC 9562.
     */
    V4,

    /**
     * Time-ordered UUID as defined by RFC 9562.
     */
    V7
}
