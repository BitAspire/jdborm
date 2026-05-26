package com.bitaspire.jdborm.schema;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * UUID helper functions for schema defaults and Java-side identifier generation.
 * <p>
 * Use {@link #randomV4()} when a random UUID is sufficient. Use {@link #timeOrderedV7()}
 * for new database identifiers that should preserve approximate creation order
 * in indexes. UUIDv7 is standardized by RFC 9562; UUIDv10 is not standardized.
 * </p>
 */
public final class Uuids {

    private static final SecureRandom RANDOM = new SecureRandom();

    private Uuids() {
    }

    /**
     * Generates a random UUID version 4 using {@link UUID#randomUUID()}.
     *
     * @return a random UUIDv4 value
     */
    public static UUID randomV4() {
        return UUID.randomUUID();
    }

    /**
     * Alias for {@link #randomV4()}.
     *
     * @return a random UUIDv4 value
     */
    public static UUID v4() {
        return randomV4();
    }

    /**
     * Generates a time-ordered UUID version 7 using the current system clock.
     *
     * @return a UUIDv7 value
     */
    public static UUID timeOrderedV7() {
        return timeOrderedV7(Instant.now());
    }

    /**
     * Alias for {@link #timeOrderedV7()}.
     *
     * @return a UUIDv7 value
     */
    public static UUID v7() {
        return timeOrderedV7();
    }

    /**
     * Generates a time-ordered UUID version 7 for the given timestamp.
     * <p>
     * The high 48 bits contain the Unix epoch timestamp in milliseconds,
     * followed by the UUID version bits and random data. The variant bits are
     * set to the RFC 4122/RFC 9562 variant.
     * </p>
     *
     * @param instant timestamp to encode into the UUIDv7 value
     * @return a UUIDv7 value
     * @throws NullPointerException if {@code instant} is {@code null}
     */
    public static UUID timeOrderedV7(Instant instant) {
        Objects.requireNonNull(instant, "instant");
        long timestampMillis = instant.toEpochMilli();
        long randomA = RANDOM.nextLong() & 0x0FFFL;
        long randomB = RANDOM.nextLong() & 0x3FFFFFFFFFFFFFFFL;

        long mostSignificantBits = ((timestampMillis & 0xFFFFFFFFFFFFL) << 16)
                | 0x7000L
                | randomA;
        long leastSignificantBits = 0x8000000000000000L | randomB;

        return new UUID(mostSignificantBits, leastSignificantBits);
    }

    /**
     * Generates a UUID for the requested supported UUID version.
     *
     * @param version UUID version to generate
     * @return a generated UUID value
     * @throws NullPointerException if {@code version} is {@code null}
     */
    public static UUID generate(UuidVersion version) {
        Objects.requireNonNull(version, "version");
        return switch (version) {
            case V4 -> randomV4();
            case V7 -> timeOrderedV7();
        };
    }

    /**
     * Returns a SQL default expression for random UUID generation in the given dialect.
     * <p>
     * PostgreSQL requires the {@code pgcrypto} extension for {@code gen_random_uuid()}.
     * Database-native UUIDv7 defaults are not broadly standardized, so this helper
     * intentionally exposes only database-native random UUID defaults.
     * </p>
     *
     * @param dialect database dialect to target
     * @return SQL expression that generates a UUID in the database
     * @throws IllegalArgumentException if the dialect is unsupported
     * @throws NullPointerException if {@code dialect} is {@code null}
     */
    public static String defaultExpression(UuidDialect dialect) {
        Objects.requireNonNull(dialect, "dialect");
        return switch (dialect) {
            case POSTGRES -> "gen_random_uuid()";
            case MYSQL, HSQLDB -> "UUID()";
        };
    }
}
