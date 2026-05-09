package com.bitaspire.jdborm.query;

/**
 * Value object holding metadata for a single JOIN clause.
 *
 * @param type    the join type (e.g. "INNER JOIN", "LEFT JOIN", "RIGHT JOIN")
 * @param table   the target table name (may include alias)
 * @param onLeft  the left-side column of the ON condition
 * @param onRight the right-side column of the ON condition
 */
record JoinClause(String type, String table, String onLeft, String onRight) {
}
