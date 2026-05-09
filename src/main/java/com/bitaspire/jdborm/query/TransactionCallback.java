package com.bitaspire.jdborm.query;

import com.bitaspire.jdborm.JdbORM;

/**
 * A functional interface for executing work within a database transaction.
 * <p>
 * Used by {@link JdbORM#inTransaction(TransactionCallback)}. The callback receives
 * a {@link JdbORM} instance that shares a single connection with auto-commit disabled.
 * If the callback returns normally the transaction is committed; if it throws an
 * exception the transaction is rolled back.
 * </p>
 *
 * @param <T> the return type of the transactional work
 */
@FunctionalInterface
public interface TransactionCallback<T> {

    /**
     * Executes work within a transaction.
     *
     * @param jdborm a JdbORM instance backed by the transactional connection
     * @return the result of the transactional work
     */
    T execute(JdbORM jdborm);
}
