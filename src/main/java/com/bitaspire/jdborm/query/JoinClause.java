package com.bitaspire.jdborm.query;

/**
 * Value object holding metadata for a single JOIN clause.
 *
 * @param type    the join type (e.g. "INNER JOIN", "LEFT JOIN", "RIGHT JOIN")
 * @param table   the target table name (may include alias)
 * @param onLeft  the left-side column of the ON condition
 * @param onRight the right-side column of the ON condition
 */
final class JoinClause {

    private final String type;
    private final String table;
    private final String onLeft;
    private final String onRight;

    JoinClause(String type, String table, String onLeft, String onRight) {
        this.type = type;
        this.table = table;
        this.onLeft = onLeft;
        this.onRight = onRight;
    }

    String type() {
        return type;
    }

    String table() {
        return table;
    }

    String onLeft() {
        return onLeft;
    }

    String onRight() {
        return onRight;
    }
}
