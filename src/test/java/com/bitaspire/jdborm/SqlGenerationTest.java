package com.bitaspire.jdborm;

import com.bitaspire.jdborm.condition.Conditions;
import com.bitaspire.jdborm.query.AlterTableQuery;
import com.bitaspire.jdborm.query.CreateIndexQuery;
import com.bitaspire.jdborm.query.CreateTableQuery;
import com.bitaspire.jdborm.query.DeleteQuery;
import com.bitaspire.jdborm.query.DropIndexQuery;
import com.bitaspire.jdborm.query.DropTableQuery;
import com.bitaspire.jdborm.query.InsertQuery;
import com.bitaspire.jdborm.query.RenameTableQuery;
import com.bitaspire.jdborm.query.SelectQuery;
import com.bitaspire.jdborm.query.TruncateQuery;
import com.bitaspire.jdborm.query.UpdateQuery;
import com.bitaspire.jdborm.schema.Column;
import com.bitaspire.jdborm.schema.Table;
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

    private static final Table USERS = Table.of("users");
    private static final Column<Integer> AGE = Column.of("age");
    private static final Column<String> NAME = Column.of("name");
    private static final Column<String> EMAIL = Column.of("email");
    private static final Column<Integer> ID = Column.of("id");
    private static final Column<String> ROLE = Column.of("role");
    private static final Column<Integer> SCORE = Column.of("score");
    private static final Column<String> STATUS = Column.of("status");
    private static final Column<Boolean> ACTIVE = Column.of("active");
    private static final Column<String> LAST_NAME = Column.of("last_name");
    private static final Column<String> FIRST_NAME = Column.of("first_name");
    private static final Column<?> CREATED_AT = Column.of("created_at");
    private static final Column<?> DELETED_AT = Column.of("deleted_at");

    private static final Table USERS_U = Table.of("users", "u");
    private static final Column<?> U_ID = USERS_U.column(ID);
    private static final Column<String> U_NAME = USERS_U.column(NAME);

    private static final Table POSTS_P = Table.of("posts", "p");
    private static final Column<?> P_USER_ID = POSTS_P.column(Column.of("user_id"));
    private static final Column<String> P_TITLE = POSTS_P.column(Column.of("title"));


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

    // ── Type-safe tests ──────────────────────────────────────────────────

    @Test
    void typeSafeSelectAll() {
        SelectQuery q = new SelectQuery(null);
        q.from(USERS);
        assertEquals("SELECT * FROM users", q.toSql());
    }

    @Test
    void typeSafeSelectColumns() {
        SelectQuery q = new SelectQuery(null, ID.qualifiedName(), NAME.qualifiedName(), EMAIL.qualifiedName());
        q.from(USERS);
        assertEquals("SELECT id, name, email FROM users", q.toSql());
    }

    @Test
    void typeSafeSelectWithWhere() {
        SelectQuery q = new SelectQuery(null, ID.qualifiedName(), NAME.qualifiedName());
        q.from(USERS);
        q.where(eq(AGE, 18));
        assertEquals("SELECT id, name FROM users WHERE age = ?", q.toSql());
        assertEquals(List.of(18), q.getParameters());
    }

    @Test
    void typeSafeSelectWithAndCondition() {
        SelectQuery q = new SelectQuery(null);
        q.from(USERS);
        q.where(eq(AGE, 18).and(gt(SCORE, 100)));
        assertEquals("SELECT * FROM users WHERE age = ? AND score > ?", q.toSql());
        assertEquals(List.of(18, 100), q.getParameters());
    }

    @Test
    void typeSafeSelectWithOrCondition() {
        SelectQuery q = new SelectQuery(null);
        q.from(USERS);
        q.where(eq(ROLE, "admin").or(eq(ROLE, "moderator")));
        assertEquals("SELECT * FROM users WHERE role = ? OR role = ?", q.toSql());
        assertEquals(List.of("admin", "moderator"), q.getParameters());
    }

    @Test
    void typeSafeSelectWithNestedAndOr() {
        SelectQuery q = new SelectQuery(null);
        q.from(USERS);
        q.where(eq(AGE, 18).and(eq(ROLE, "user").or(eq(ROLE, "moderator"))));
        assertEquals("SELECT * FROM users WHERE age = ? AND (role = ? OR role = ?)", q.toSql());
        assertEquals(List.of(18, "user", "moderator"), q.getParameters());
    }

    @Test
    void typeSafeSelectWithOrderBy() {
        SelectQuery q = new SelectQuery(null);
        q.from(USERS).orderBy(NAME);
        assertEquals("SELECT * FROM users ORDER BY name ASC", q.toSql());
    }

    @Test
    void typeSafeSelectWithOrderByDesc() {
        SelectQuery q = new SelectQuery(null);
        q.from(USERS).orderByDesc(CREATED_AT);
        assertEquals("SELECT * FROM users ORDER BY created_at DESC", q.toSql());
    }

    @Test
    void typeSafeSelectWithJoin() {
        SelectQuery q = new SelectQuery(null, U_ID.qualifiedName(), U_NAME.qualifiedName(), P_TITLE.qualifiedName());
        q.from(USERS_U);
        q.join(POSTS_P, P_USER_ID, U_ID);
        assertEquals("SELECT u.id, u.name, p.title FROM users u INNER JOIN posts p ON p.user_id = u.id", q.toSql());
    }

    @Test
    void typeSafeSelectWithLeftJoin() {
        SelectQuery q = new SelectQuery(null);
        q.from(USERS_U);
        q.leftJoin(POSTS_P, P_USER_ID, U_ID);
        assertEquals("SELECT * FROM users u LEFT JOIN posts p ON p.user_id = u.id", q.toSql());
    }

    @Test
    void typeSafeSelectWithRightJoin() {
        SelectQuery q = new SelectQuery(null);
        q.from(USERS_U);
        q.rightJoin(POSTS_P, P_USER_ID, U_ID);
        assertEquals("SELECT * FROM users u RIGHT JOIN posts p ON p.user_id = u.id", q.toSql());
    }

    @Test
    void typeSafeInsert() {
        InsertQuery q = new InsertQuery(null, "users");
        q.set(NAME, "John").set(EMAIL, "john@example.com");
        assertEquals("INSERT INTO users (name, email) VALUES (?, ?)", q.toSql());
        assertEquals(List.of("John", "john@example.com"), q.getParameters());
    }

    @Test
    void typeSafeUpdate() {
        UpdateQuery q = new UpdateQuery(null, "users");
        q.set(NAME, "Jane").where(eq(ID, 1));
        assertEquals("UPDATE users SET name = ? WHERE id = ?", q.toSql());
        assertEquals(List.of("Jane", 1), q.getParameters());
    }

    @Test
    void typeSafeDelete() {
        DeleteQuery q = new DeleteQuery(null, "users");
        q.where(eq(ID, 1));
        assertEquals("DELETE FROM users WHERE id = ?", q.toSql());
        assertEquals(List.of(1), q.getParameters());
    }

    @Test
    void typeSafeConditionsIn() {
        SelectQuery q = new SelectQuery(null);
        q.from(USERS);
        q.where(in(ID, 1, 2, 3));
        assertEquals("SELECT * FROM users WHERE id IN (?, ?, ?)", q.toSql());
        assertEquals(List.of(1, 2, 3), q.getParameters());
    }

    @Test
    void typeSafeConditionsBetween() {
        SelectQuery q = new SelectQuery(null);
        q.from(USERS);
        q.where(between(AGE, 18, 65));
        assertEquals("SELECT * FROM users WHERE age BETWEEN ? AND ?", q.toSql());
        assertEquals(List.of(18, 65), q.getParameters());
    }

    @Test
    void typeSafeConditionsIsNull() {
        SelectQuery q = new SelectQuery(null);
        q.from(USERS);
        q.where(isNull(DELETED_AT));
        assertEquals("SELECT * FROM users WHERE deleted_at IS NULL", q.toSql());
    }

    @Test
    void typeSafeConditionsIsNotNull() {
        SelectQuery q = new SelectQuery(null);
        q.from(USERS);
        q.where(isNotNull(EMAIL));
        assertEquals("SELECT * FROM users WHERE email IS NOT NULL", q.toSql());
    }

    @Test
    void typeSafeConditionsNot() {
        SelectQuery q = new SelectQuery(null);
        q.from(USERS);
        q.where(not(eq(STATUS, "banned")));
        assertEquals("SELECT * FROM users WHERE NOT (status = ?)", q.toSql());
        assertEquals(List.of("banned"), q.getParameters());
    }

    @Test
    void typeSafeConditionsAndFactory() {
        SelectQuery q = new SelectQuery(null);
        q.from(USERS);
        q.where(and(eq(AGE, 18), eq(ACTIVE, true), gt(SCORE, 50)));
        assertEquals("SELECT * FROM users WHERE age = ? AND active = ? AND score > ?", q.toSql());
        assertEquals(List.of(18, true, 50), q.getParameters());
    }

    @Test
    void typeSafeMultipleOrderBy() {
        SelectQuery q = new SelectQuery(null);
        q.from(USERS).orderBy(LAST_NAME, FIRST_NAME).orderByDesc(CREATED_AT);
        assertEquals("SELECT * FROM users ORDER BY last_name ASC, first_name ASC, created_at DESC", q.toSql());
    }

    @Test
    void typeSafeTableFromViaJdbOrmEntryPoint() {
        SelectQuery q = new SelectQuery(null, ID.qualifiedName(), NAME.qualifiedName());
        q.from(USERS);
        assertEquals("SELECT id, name FROM users", q.toSql());
    }

    // ── setRaw tests ──────────────────────────────────────────────────────

    @Test
    void insertWithSetRaw() {
        InsertQuery q = new InsertQuery(null, "users");
        q.set("name", "John").setRaw("created_at", "NOW()");
        assertEquals("INSERT INTO users (name, created_at) VALUES (?, NOW())", q.toSql());
        assertEquals(List.of("John"), q.getParameters());
    }

    @Test
    void insertWithSetRawTypeSafe() {
        InsertQuery q = new InsertQuery(null, "users");
        q.set(NAME, "John").setRaw(CREATED_AT, "NOW()");
        assertEquals("INSERT INTO users (name, created_at) VALUES (?, NOW())", q.toSql());
        assertEquals(List.of("John"), q.getParameters());
    }

    @Test
    void updateWithSetRaw() {
        UpdateQuery q = new UpdateQuery(null, "users");
        q.set("name", "John").setRaw("updated_at", "NOW()").where(eq("id", 1));
        assertEquals("UPDATE users SET name = ?, updated_at = NOW() WHERE id = ?", q.toSql());
        assertEquals(List.of("John", 1), q.getParameters());
    }

    @Test
    void updateWithSetRawTypeSafe() {
        UpdateQuery q = new UpdateQuery(null, "users");
        q.set(NAME, "John").setRaw(CREATED_AT, "NOW()").where(eq(ID, 1));
        assertEquals("UPDATE users SET name = ?, created_at = NOW() WHERE id = ?", q.toSql());
        assertEquals(List.of("John", 1), q.getParameters());
    }

    // ── ON CONFLICT tests ─────────────────────────────────────────────────

    @Test
    void insertOnConflictDoNothing() {
        InsertQuery q = new InsertQuery(null, "users");
        q.set("name", "John").onConflictDoNothing();
        assertEquals("INSERT INTO users (name) VALUES (?) ON CONFLICT DO NOTHING", q.toSql());
        assertEquals(List.of("John"), q.getParameters());
    }

    @Test
    void insertOnConflictDoUpdate() {
        InsertQuery q = new InsertQuery(null, "users");
        q.set("name", "John").set("email", "john@example.com")
                .onConflictDoUpdate("name = EXCLUDED.name", "email = EXCLUDED.email");
        assertEquals("INSERT INTO users (name, email) VALUES (?, ?) ON CONFLICT DO UPDATE SET name = EXCLUDED.name, email = EXCLUDED.email", q.toSql());
        assertEquals(List.of("John", "john@example.com"), q.getParameters());
    }

    @Test
    void insertOnConflictDoUpdateTypeSafe() {
        InsertQuery q = new InsertQuery(null, "users");
        q.set(NAME, "John").onConflictDoUpdate("name = EXCLUDED.name");
        assertEquals("INSERT INTO users (name) VALUES (?) ON CONFLICT DO UPDATE SET name = EXCLUDED.name", q.toSql());
        assertEquals(List.of("John"), q.getParameters());
    }

    // ── Batch INSERT tests ────────────────────────────────────────────────

    @Test
    void insertBatch() {
        InsertQuery q = new InsertQuery(null, "users");
        q.set("name", "Alice").addBatch();
        q.set("name", "Bob").addBatch();
        assertEquals("INSERT INTO users (name) VALUES (?), (?)", q.toSql());
        assertEquals(List.of("Alice", "Bob"), q.getParameters());
    }

    @Test
    void insertBatchWithSetRaw() {
        InsertQuery q = new InsertQuery(null, "users");
        q.set("name", "Alice").setRaw("created_at", "NOW()").addBatch();
        q.set("name", "Bob").setRaw("created_at", "NOW()").addBatch();
        assertEquals("INSERT INTO users (name, created_at) VALUES (?, NOW()), (?, NOW())", q.toSql());
        assertEquals(List.of("Alice", "Bob"), q.getParameters());
    }

    @Test
    void insertBatchWithOnConflictDoNothing() {
        InsertQuery q = new InsertQuery(null, "users");
        q.set("name", "Alice").addBatch();
        q.set("name", "Bob").addBatch();
        q.onConflictDoNothing();
        assertEquals("INSERT INTO users (name) VALUES (?), (?) ON CONFLICT DO NOTHING", q.toSql());
        assertEquals(List.of("Alice", "Bob"), q.getParameters());
    }

    @Test
    void typeSafeQualifiedColumnsInJoin() {
        SelectQuery q = new SelectQuery(null, U_ID.qualifiedName(), U_NAME.qualifiedName(), P_TITLE.qualifiedName());
        q.from(USERS_U);
        q.join(POSTS_P, P_USER_ID, U_ID);
        q.where(eq(U_NAME, "Alice"));
        assertEquals("SELECT u.id, u.name, p.title FROM users u INNER JOIN posts p ON p.user_id = u.id WHERE u.name = ?", q.toSql());
        assertEquals(List.of("Alice"), q.getParameters());
    }

    // ── DDL: CREATE TABLE tests ────────────────────────────────────────────

    @Test
    void createTableBasic() {
        CreateTableQuery q = new CreateTableQuery(null, "users");
        q.column("id", "BIGINT AUTO_INCREMENT PRIMARY KEY")
                .column("name", "VARCHAR(100) NOT NULL")
                .column("email", "VARCHAR(255) NOT NULL UNIQUE");
        assertEquals("CREATE TABLE users (id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(100) NOT NULL, email VARCHAR(255) NOT NULL UNIQUE)", q.toSql());
    }

    @Test
    void createTableWithIfNotExists() {
        CreateTableQuery q = new CreateTableQuery(null, "users");
        q.column("id", "INTEGER PRIMARY KEY").ifNotExists();
        assertEquals("CREATE TABLE IF NOT EXISTS users (id INTEGER PRIMARY KEY)", q.toSql());
    }

    @Test
    void createTableWithTableConstraints() {
        CreateTableQuery q = new CreateTableQuery(null, "orders");
        q.column("id", "INTEGER")
                .column("user_id", "INTEGER NOT NULL")
                .column("order_code", "VARCHAR(50) NOT NULL")
                .primaryKey("id")
                .foreignKey("user_id", "users(id)")
                .unique("order_code")
                .check("id > 0");
        assertEquals("CREATE TABLE orders (id INTEGER, user_id INTEGER NOT NULL, order_code VARCHAR(50) NOT NULL, PRIMARY KEY (id), FOREIGN KEY (user_id) REFERENCES users(id), UNIQUE (order_code), CHECK (id > 0))", q.toSql());
    }

    @Test
    void createTableTypeSafeColumn() {
        CreateTableQuery q = new CreateTableQuery(null, "users");
        q.column(ID, "INTEGER PRIMARY KEY").column(NAME, "VARCHAR(100) NOT NULL");
        assertEquals("CREATE TABLE users (id INTEGER PRIMARY KEY, name VARCHAR(100) NOT NULL)", q.toSql());
    }

    // ── DDL: ALTER TABLE tests ─────────────────────────────────────────────

    @Test
    void alterTableAddColumn() {
        AlterTableQuery q = new AlterTableQuery(null, "users");
        q.addColumn("age", "INTEGER");
        assertEquals("ALTER TABLE users ADD COLUMN age INTEGER", q.toSql());
    }

    @Test
    void alterTableMultiple() {
        AlterTableQuery q = new AlterTableQuery(null, "users");
        q.addColumn("age", "INTEGER DEFAULT 0")
                .dropColumn("old_field")
                .modifyColumn("name", "VARCHAR(200)")
                .renameColumn("email", "email_address");
        assertEquals("ALTER TABLE users ADD COLUMN age INTEGER DEFAULT 0, ALTER TABLE users DROP COLUMN old_field, ALTER TABLE users MODIFY COLUMN name VARCHAR(200), ALTER TABLE users RENAME COLUMN email TO email_address", q.toSql());
    }

    @Test
    void alterTableConstraints() {
        AlterTableQuery q = new AlterTableQuery(null, "orders");
        q.addPrimaryKey("id")
                .addForeignKey("user_id", "users(id)")
                .addUnique("order_code")
                .addCheck("amount > 0")
                .dropConstraint("old_constraint")
                .dropPrimaryKey();
        assertEquals(
                "ALTER TABLE orders ADD PRIMARY KEY (id), ALTER TABLE orders ADD FOREIGN KEY (user_id) REFERENCES users(id), ALTER TABLE orders ADD UNIQUE (order_code), ALTER TABLE orders ADD CHECK (amount > 0), ALTER TABLE orders DROP CONSTRAINT old_constraint, ALTER TABLE orders DROP PRIMARY KEY",
                q.toSql());
    }

    @Test
    void alterTableAddColumnTypeSafe() {
        AlterTableQuery q = new AlterTableQuery(null, "users");
        q.addColumn(AGE, "INTEGER DEFAULT 0");
        assertEquals("ALTER TABLE users ADD COLUMN age INTEGER DEFAULT 0", q.toSql());
    }

    @Test
    void alterTableDropColumnTypeSafe() {
        AlterTableQuery q = new AlterTableQuery(null, "users");
        q.dropColumn(AGE);
        assertEquals("ALTER TABLE users DROP COLUMN age", q.toSql());
    }

    @Test
    void alterTableAddColumnIfNotExists() {
        AlterTableQuery q = new AlterTableQuery(null, "users");
        q.addColumnIfNotExists("age", "INTEGER");
        assertEquals("ALTER TABLE users ADD COLUMN IF NOT EXISTS age INTEGER", q.toSql());
    }

    @Test
    void alterTableAddColumnIfNotExistsTypeSafe() {
        AlterTableQuery q = new AlterTableQuery(null, "users");
        q.addColumnIfNotExists(AGE, "INTEGER DEFAULT 0");
        assertEquals("ALTER TABLE users ADD COLUMN IF NOT EXISTS age INTEGER DEFAULT 0", q.toSql());
    }

    // ── DDL: DROP TABLE tests ──────────────────────────────────────────────

    @Test
    void dropTable() {
        DropTableQuery q = new DropTableQuery(null, "users");
        assertEquals("DROP TABLE users", q.toSql());
    }

    @Test
    void dropTableIfExists() {
        DropTableQuery q = new DropTableQuery(null, "users");
        q.ifExists();
        assertEquals("DROP TABLE IF EXISTS users", q.toSql());
    }

    @Test
    void dropTableCascade() {
        DropTableQuery q = new DropTableQuery(null, "users");
        q.cascade();
        assertEquals("DROP TABLE users CASCADE", q.toSql());
    }

    @Test
    void dropTableIfExistsCascade() {
        DropTableQuery q = new DropTableQuery(null, "users");
        q.ifExists().cascade();
        assertEquals("DROP TABLE IF EXISTS users CASCADE", q.toSql());
    }

    // ── DDL: TRUNCATE TABLE tests ──────────────────────────────────────────

    @Test
    void truncateTable() {
        TruncateQuery q = new TruncateQuery(null, "users");
        assertEquals("TRUNCATE TABLE users", q.toSql());
    }

    // ── DDL: RENAME TABLE tests ────────────────────────────────────────────

    @Test
    void renameTable() {
        RenameTableQuery q = new RenameTableQuery(null, "users", "customers");
        assertEquals("RENAME TABLE users TO customers", q.toSql());
    }

    // ── DDL: CREATE INDEX tests ────────────────────────────────────────────

    @Test
    void createIndex() {
        CreateIndexQuery q = new CreateIndexQuery(null, "idx_users_email");
        q.on("users", "email");
        assertEquals("CREATE INDEX idx_users_email ON users (email)", q.toSql());
    }

    @Test
    void createIndexUnique() {
        CreateIndexQuery q = new CreateIndexQuery(null, "idx_users_email");
        q.on("users", "email").unique();
        assertEquals("CREATE UNIQUE INDEX idx_users_email ON users (email)", q.toSql());
    }

    @Test
    void createIndexIfNotExists() {
        CreateIndexQuery q = new CreateIndexQuery(null, "idx_users_email");
        q.on("users", "email").ifNotExists();
        assertEquals("CREATE INDEX IF NOT EXISTS idx_users_email ON users (email)", q.toSql());
    }

    @Test
    void createIndexUsingMethod() {
        CreateIndexQuery q = new CreateIndexQuery(null, "idx_users_email");
        q.on("users", "email").using("HASH");
        assertEquals("CREATE INDEX idx_users_email ON users USING HASH (email)", q.toSql());
    }

    @Test
    void createIndexMultipleColumns() {
        CreateIndexQuery q = new CreateIndexQuery(null, "idx_users_name");
        q.on("users", "last_name", "first_name");
        assertEquals("CREATE INDEX idx_users_name ON users (last_name, first_name)", q.toSql());
    }

    @Test
    void createIndexTypeSafeColumns() {
        CreateIndexQuery q = new CreateIndexQuery(null, "idx_users_age_score");
        q.on("users", AGE, SCORE);
        assertEquals("CREATE INDEX idx_users_age_score ON users (age, score)", q.toSql());
    }

    // ── DDL: DROP INDEX tests ──────────────────────────────────────────────

    @Test
    void dropIndex() {
        DropIndexQuery q = new DropIndexQuery(null, "idx_users_email");
        assertEquals("DROP INDEX idx_users_email", q.toSql());
    }

    @Test
    void dropIndexIfExists() {
        DropIndexQuery q = new DropIndexQuery(null, "idx_users_email");
        q.ifExists();
        assertEquals("DROP INDEX IF EXISTS idx_users_email", q.toSql());
    }

    @Test
    void dropIndexOnTable() {
        DropIndexQuery q = new DropIndexQuery(null, "idx_users_email");
        q.on("users");
        assertEquals("DROP INDEX users.idx_users_email", q.toSql());
    }

    @Test
    void dropIndexCascade() {
        DropIndexQuery q = new DropIndexQuery(null, "idx_users_email");
        q.cascade();
        assertEquals("DROP INDEX idx_users_email CASCADE", q.toSql());
    }
}
