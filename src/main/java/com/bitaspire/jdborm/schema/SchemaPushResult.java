package com.bitaspire.jdborm.schema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Result of pushing a declarative {@link Schema} to a database.
 * <p>
 * The result records the SQL statements that were actually executed. If the
 * database was already compatible with the schema, the list is empty.
 * </p>
 */
public final class SchemaPushResult {

    private final List<String> executedSql;

    /**
     * Creates a new schema push result.
     *
     * @param executedSql SQL statements that were executed during the push
     * @throws NullPointerException if {@code executedSql} is {@code null}
     */
    public SchemaPushResult(List<String> executedSql) {
this.executedSql = Collections.unmodifiableList(new ArrayList<String>(Objects.requireNonNull(executedSql, "executedSql")));
    }

    /**
     * Returns SQL statements that were executed during the push.
     *
     * @return an immutable list of executed SQL statements
     */
    public List<String> executedSql() {
        return Collections.unmodifiableList(executedSql);
    }

    /**
     * Returns the number of executed SQL statements.
     *
     * @return the executed statement count
     */
    public int statementsExecuted() {
        return executedSql.size();
    }

    /**
     * Returns whether the database was changed by the schema push.
     *
     * @return {@code true} if at least one SQL statement was executed
     */
    public boolean changed() {
        return !executedSql.isEmpty();
    }
}
