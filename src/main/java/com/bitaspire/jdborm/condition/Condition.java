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
final class SimpleCondition implements Condition {
    private final String column;
    private final String operator;
    private final Object value;

    SimpleCondition(String column, String operator, Object value) {
        this.column = column;
        this.operator = operator;
        this.value = value;
    }

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
final class AndCondition implements Condition {
    private final Condition left;
    private final Condition right;

    AndCondition(Condition left, Condition right) {
        this.left = left;
        this.right = right;
    }

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
final class OrCondition implements Condition {
    private final Condition left;
    private final Condition right;

    OrCondition(Condition left, Condition right) {
        this.left = left;
        this.right = right;
    }

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
final class NotCondition implements Condition {
    private final Condition inner;

    NotCondition(Condition inner) {
        this.inner = inner;
    }

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
final class InCondition implements Condition {
    private final String column;
    private final Object[] values;

    InCondition(String column, Object[] values) {
        this.column = column;
        this.values = values;
    }

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
final class BetweenCondition implements Condition {
    private final String column;
    private final Object start;
    private final Object end;

    BetweenCondition(String column, Object start, Object end) {
        this.column = column;
        this.start = start;
        this.end = end;
    }

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
final class IsNullCondition implements Condition {
    private final String column;
    private final boolean negated;

    IsNullCondition(String column, boolean negated) {
        this.column = column;
        this.negated = negated;
    }

    @Override
    public void appendTo(StringBuilder sql, List<Object> params) {
        sql.append(column).append(negated ? " IS NOT NULL" : " IS NULL");
    }
}

/**
 * Raw SQL fragment condition — appended verbatim without any parameters.
 */
final class RawCondition implements Condition {
    private final String sqlFragment;

    RawCondition(String sqlFragment) {
        this.sqlFragment = sqlFragment;
    }

    @Override
    public void appendTo(StringBuilder sql, List<Object> params) {
        sql.append(sqlFragment);
    }
}
