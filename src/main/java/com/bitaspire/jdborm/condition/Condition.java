package com.bitaspire.jdborm.condition;

import java.util.List;

/**
 * Represents a single SQL condition (e.g. part of a WHERE clause).
 * <p>
 * Implementations append their SQL fragment and parameter values to the
 * provided builder and list. Compound conditions (AND, OR, NOT) can be
 * composed using the default methods or via {@link Conditions}.
 * </p>
 */
public interface Condition {

    /**
     * Appends the SQL representation of this condition to the given builder
     * and adds parameter values to the list.
     *
     * @param sql    the SQL string builder
     * @param params the list of parameter values to populate
     */
    void appendTo(StringBuilder sql, List<Object> params);

    /**
     * Returns the combining operator (AND/OR) used by compound conditions,
     * or {@code null} for leaf conditions.
     *
     * @return the combining operator, or {@code null}
     */
    default String combiningOperator() {
        return null;
    }

    /**
     * Combines this condition with another using AND.
     *
     * @param other the other condition
     * @return a new compound AND condition
     */
    default Condition and(Condition other) {
        return Conditions.and(this, other);
    }

    /**
     * Combines this condition with another using OR.
     *
     * @param other the other condition
     * @return a new compound OR condition
     */
    default Condition or(Condition other) {
        return Conditions.or(this, other);
    }
}

/**
 * A simple condition: {@code column operator ?}.
 */
record SimpleCondition(String column, String operator, Object value) implements Condition {
    @Override
    public void appendTo(StringBuilder sql, List<Object> params) {
        sql.append(column).append(" ").append(operator).append(" ?");
        params.add(value);
    }
}

/**
 * Compound condition combining two child conditions with AND.
 * Adds parentheses around the right child if its combining operator differs
 * from AND.
 */
record AndCondition(Condition left, Condition right) implements Condition {
    @Override
    public void appendTo(StringBuilder sql, List<Object> params) {
        appendChild(sql, params, left);
        sql.append(" AND ");
        appendChild(sql, params, right);
    }

    private void appendChild(StringBuilder sql, List<Object> params, Condition child) {
        String childOp = child.combiningOperator();
        if (childOp != null && !"AND".equals(childOp)) {
            sql.append("(");
            child.appendTo(sql, params);
            sql.append(")");
        } else {
            child.appendTo(sql, params);
        }
    }

    @Override
    public String combiningOperator() {
        return "AND";
    }
}

/**
 * Compound condition combining two child conditions with OR.
 * Adds parentheses around the right child if its combining operator differs
 * from OR.
 */
record OrCondition(Condition left, Condition right) implements Condition {
    @Override
    public void appendTo(StringBuilder sql, List<Object> params) {
        appendChild(sql, params, left);
        sql.append(" OR ");
        appendChild(sql, params, right);
    }

    private void appendChild(StringBuilder sql, List<Object> params, Condition child) {
        String childOp = child.combiningOperator();
        if (childOp != null && !"OR".equals(childOp)) {
            sql.append("(");
            child.appendTo(sql, params);
            sql.append(")");
        } else {
            child.appendTo(sql, params);
        }
    }

    @Override
    public String combiningOperator() {
        return "OR";
    }
}

/**
 * Negation condition: {@code NOT (...)}.
 */
record NotCondition(Condition inner) implements Condition {
    @Override
    public void appendTo(StringBuilder sql, List<Object> params) {
        sql.append("NOT (");
        inner.appendTo(sql, params);
        sql.append(")");
    }
}

/**
 * IN condition: {@code column IN (?, ?, ...)}.
 */
record InCondition(String column, Object[] values) implements Condition {
    @Override
    public void appendTo(StringBuilder sql, List<Object> params) {
        sql.append(column).append(" IN (");
        for (int i = 0; i < values.length; i++) {
            if (i > 0) sql.append(", ");
            sql.append("?");
            params.add(values[i]);
        }
        sql.append(")");
    }
}

/**
 * BETWEEN condition: {@code column BETWEEN ? AND ?}.
 */
record BetweenCondition(String column, Object start, Object end) implements Condition {
    @Override
    public void appendTo(StringBuilder sql, List<Object> params) {
        sql.append(column).append(" BETWEEN ? AND ?");
        params.add(start);
        params.add(end);
    }
}

/**
 * IS NULL / IS NOT NULL condition.
 */
record IsNullCondition(String column, boolean negated) implements Condition {
    @Override
    public void appendTo(StringBuilder sql, List<Object> params) {
        sql.append(column).append(negated ? " IS NOT NULL" : " IS NULL");
    }
}

/**
 * Raw SQL fragment condition — appended verbatim without any parameters.
 */
record RawCondition(String sqlFragment) implements Condition {
    @Override
    public void appendTo(StringBuilder sql, List<Object> params) {
        sql.append(sqlFragment);
    }
}
