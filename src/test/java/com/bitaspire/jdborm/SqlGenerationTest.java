package com.bitaspire.jdborm;

import com.bitaspire.jdborm.condition.Conditions;
import com.bitaspire.jdborm.query.DeleteQuery;
import com.bitaspire.jdborm.query.InsertQuery;
import com.bitaspire.jdborm.query.SelectQuery;
import com.bitaspire.jdborm.query.UpdateQuery;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.bitaspire.jdborm.condition.Conditions.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SQL generation of all query types and conditions.
 * <p>
 * These tests verify the output of {@link com.bitaspire.jdborm.query.Query#toSql()}
 * and {@link com.bitaspire.jdborm.query.Query#getParameters()} without requiring
 * a database connection.
 * </p>
 */
class SqlGenerationTest {

    @Test
    void selectAllWithWildcard() {
        SelectQuery q = new SelectQuery(null);
        q.from("users");
        assertEquals("SELECT * FROM users", q.toSql());
    }

    @Test
    void selectColumns() {
        SelectQuery q = new SelectQuery(null, "id", "name", "email");
        q.from("users");
        assertEquals("SELECT id, name, email FROM users", q.toSql());
    }

    @Test
    void selectWithWhere() {
        SelectQuery q = new SelectQuery(null, "id", "name");
        q.from("users");
        q.where(eq("age", 18));
        assertEquals("SELECT id, name FROM users WHERE age = ?", q.toSql());
        assertEquals(List.of(18), q.getParameters());
    }

    @Test
    void selectWithAndCondition() {
        SelectQuery q = new SelectQuery(null);
        q.from("users");
        q.where(eq("age", 18).and(gt("score", 100)));
        assertEquals("SELECT * FROM users WHERE age = ? AND score > ?", q.toSql());
        assertEquals(List.of(18, 100), q.getParameters());
    }

    @Test
    void selectWithOrCondition() {
        SelectQuery q = new SelectQuery(null);
        q.from("users");
        q.where(eq("role", "admin").or(eq("role", "moderator")));
        assertEquals("SELECT * FROM users WHERE role = ? OR role = ?", q.toSql());
        assertEquals(List.of("admin", "moderator"), q.getParameters());
    }

    @Test
    void selectWithNestedAndOr() {
        SelectQuery q = new SelectQuery(null);
        q.from("users");
        q.where(eq("age", 18).and(eq("role", "user").or(eq("role", "moderator"))));
        assertEquals("SELECT * FROM users WHERE age = ? AND (role = ? OR role = ?)", q.toSql());
        assertEquals(List.of(18, "user", "moderator"), q.getParameters());
    }

    @Test
    void selectWithOrderBy() {
        SelectQuery q = new SelectQuery(null);
        q.from("users").orderBy("name");
        assertEquals("SELECT * FROM users ORDER BY name ASC", q.toSql());
    }

    @Test
    void selectWithOrderByDesc() {
        SelectQuery q = new SelectQuery(null);
        q.from("users").orderByDesc("created_at");
        assertEquals("SELECT * FROM users ORDER BY created_at DESC", q.toSql());
    }

    @Test
    void selectWithLimit() {
        SelectQuery q = new SelectQuery(null);
        q.from("users").limit(10);
        assertEquals("SELECT * FROM users LIMIT 10", q.toSql());
    }

    @Test
    void selectWithLimitAndOffset() {
        SelectQuery q = new SelectQuery(null);
        q.from("users").limit(10).offset(20);
        assertEquals("SELECT * FROM users LIMIT 10 OFFSET 20", q.toSql());
    }

    @Test
    void selectWithJoin() {
        SelectQuery q = new SelectQuery(null, "u.id", "u.name", "p.title");
        q.from("users u");
        q.join("posts p", "p.user_id", "u.id");
        assertEquals("SELECT u.id, u.name, p.title FROM users u INNER JOIN posts p ON p.user_id = u.id", q.toSql());
    }

    @Test
    void selectWithLeftJoin() {
        SelectQuery q = new SelectQuery(null);
        q.from("users u");
        q.leftJoin("posts p", "p.user_id", "u.id");
        assertEquals("SELECT * FROM users u LEFT JOIN posts p ON p.user_id = u.id", q.toSql());
    }

    @Test
    void insert() {
        InsertQuery q = new InsertQuery(null, "users");
        q.set("name", "John").set("email", "john@example.com");
        assertEquals("INSERT INTO users (name, email) VALUES (?, ?)", q.toSql());
        assertEquals(List.of("John", "john@example.com"), q.getParameters());
    }

    @Test
    void update() {
        UpdateQuery q = new UpdateQuery(null, "users");
        q.set("name", "Jane").where(eq("id", 1));
        assertEquals("UPDATE users SET name = ? WHERE id = ?", q.toSql());
        assertEquals(List.of("Jane", 1), q.getParameters());
    }

    @Test
    void delete() {
        DeleteQuery q = new DeleteQuery(null, "users");
        q.where(eq("id", 1));
        assertEquals("DELETE FROM users WHERE id = ?", q.toSql());
        assertEquals(List.of(1), q.getParameters());
    }

    @Test
    void conditionsIn() {
        SelectQuery q = new SelectQuery(null);
        q.from("users");
        q.where(in("id", 1, 2, 3));
        assertEquals("SELECT * FROM users WHERE id IN (?, ?, ?)", q.toSql());
        assertEquals(List.of(1, 2, 3), q.getParameters());
    }

    @Test
    void conditionsBetween() {
        SelectQuery q = new SelectQuery(null);
        q.from("users");
        q.where(between("age", 18, 65));
        assertEquals("SELECT * FROM users WHERE age BETWEEN ? AND ?", q.toSql());
        assertEquals(List.of(18, 65), q.getParameters());
    }

    @Test
    void conditionsIsNull() {
        SelectQuery q = new SelectQuery(null);
        q.from("users");
        q.where(isNull("deleted_at"));
        assertEquals("SELECT * FROM users WHERE deleted_at IS NULL", q.toSql());
    }

    @Test
    void conditionsIsNotNull() {
        SelectQuery q = new SelectQuery(null);
        q.from("users");
        q.where(isNotNull("email"));
        assertEquals("SELECT * FROM users WHERE email IS NOT NULL", q.toSql());
    }

    @Test
    void conditionsNot() {
        SelectQuery q = new SelectQuery(null);
        q.from("users");
        q.where(not(eq("status", "banned")));
        assertEquals("SELECT * FROM users WHERE NOT (status = ?)", q.toSql());
        assertEquals(List.of("banned"), q.getParameters());
    }

    @Test
    void conditionsAndFactory() {
        SelectQuery q = new SelectQuery(null);
        q.from("users");
        q.where(and(eq("age", 18), eq("active", true), gt("score", 50)));
        assertEquals("SELECT * FROM users WHERE age = ? AND active = ? AND score > ?", q.toSql());
        assertEquals(List.of(18, true, 50), q.getParameters());
    }

    @Test
    void conditionsOrFactory() {
        SelectQuery q = new SelectQuery(null);
        q.from("users");
        q.where(or(eq("role", "admin"), eq("role", "mod"), eq("role", "super")));
        assertEquals("SELECT * FROM users WHERE role = ? OR role = ? OR role = ?", q.toSql());
        assertEquals(List.of("admin", "mod", "super"), q.getParameters());
    }

    @Test
    void multipleOrderBy() {
        SelectQuery q = new SelectQuery(null);
        q.from("users").orderBy("last_name", "first_name").orderByDesc("created_at");
        assertEquals("SELECT * FROM users ORDER BY last_name ASC, first_name ASC, created_at DESC", q.toSql());
    }
}
