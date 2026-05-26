# jdborm — AI Instructions

## Build & Test Commands

```bash
# Build the project (compile + test + jar)
./gradlew build

# Compile only
./gradlew compileJava

# Run tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.bitaspire.jdborm.SqlGenerationTest"
./gradlew test --tests "com.bitaspire.jdborm.SchemaPushIntegrationTest"

# Run a single test method
./gradlew test --tests "com.bitaspire.jdborm.SqlGenerationTest.selectAll"

# Clean build artifacts
./gradlew clean
```

## Code Style

- Java 17+, no external runtime dependencies.
- Package root: `com.bitaspire.jdborm`.
- Fluent API with method chaining, inspired by Drizzle ORM.
- Static imports are expected for `Conditions.*` in examples/tests.
- **STRICTLY REQUIRED: Write Javadoc comments on ALL public classes, methods, and fields** — no exceptions. Every public element must have complete Javadoc with `@param`, `@return`, and `@throws` where applicable.
- Records are preferred for small immutable value objects, when suitable.
- Tests use JUnit 5 + AssertJ + HSQLDB for integration tests.
- Test class naming: `*Test.java`.
- No lombok or annotation processors.
- Keep API additions zero-dependency and JDBC-friendly.

## Project Architecture

```
com.bitaspire.jdborm
├── JdbORM.java                  — Entry point; create via JdbORM.create(...), connect(...)
├── condition/
│   ├── Condition.java           — Interface; records: SimpleCondition, AndCondition, OrCondition, etc.
│   └── Conditions.java          — Static factory: eq(), gt(), like(), and(), or(), not(), raw(), etc.
├── exception/
│   └── JdbOrmException.java     — Unchecked exception wrapping SQL/ORM failures
├── mapper/
│   ├── ResultMapper.java        — ResultSet → POJO via reflection (field name matching)
│   └── RowMapper.java           — Functional interface for custom result mapping
├── query/
│   ├── SelectQuery.java         — SELECT builder
│   ├── InsertQuery.java         — INSERT/UPSERT/batch builder; execute() returns GeneratedKeys
│   ├── UpdateQuery.java         — UPDATE builder
│   ├── DeleteQuery.java         — DELETE builder
│   ├── CreateTableQuery.java    — Explicit CREATE TABLE builder
│   ├── AlterTableQuery.java     — Explicit ALTER TABLE builder
│   ├── DropTableQuery.java      — Explicit DROP TABLE builder
│   ├── CreateIndexQuery.java    — Explicit CREATE INDEX builder
│   ├── DropIndexQuery.java      — Explicit DROP INDEX builder
│   ├── TruncateQuery.java       — TRUNCATE TABLE builder
│   ├── RenameTableQuery.java    — RENAME TABLE builder
│   ├── GeneratedKeys.java       — Generated key result wrapper
│   ├── JoinClause.java          — Join value object
│   ├── RawExpression.java       — Raw SQL expression wrapper
│   └── TransactionCallback.java — Functional interface for inTransaction()
└── schema/
    ├── Table.java               — Type-safe table reference (name + optional alias)
    ├── Column.java              — Type-safe column reference
    ├── Schema.java              — Declarative schema root for Drizzle-like schema files
    ├── TableDefinition.java     — Declarative table definition
    ├── ColumnDefinition.java    — Declarative column definition and inline constraints
    ├── IndexDefinition.java     — Declarative index definition
    ├── SchemaPushResult.java    — Result of JdbORM.pushSchema(...)
    ├── Uuids.java               — UUIDv4/UUIDv7 helpers and DB default expressions
    ├── UuidVersion.java         — Supported Java UUID generation versions
    └── UuidDialect.java         — DB dialects for UUID default expressions
```

## API Reference

### JdbORM (entry point)

| Method | Returns | Description |
|--------|---------|-------------|
| `JdbORM.create(DataSource)` | `JdbORM` | Pooled connections via DataSource |
| `JdbORM.create(Connection)` | `JdbORM` | Single connection (caller manages) |
| `JdbORM.connect(url)` | `JdbORM` | Auto-connect via DriverManager without auth |
| `JdbORM.connect(url, user, pass)` | `JdbORM` | Auto-connect via DriverManager |
| `.select(String... cols)` | `SelectQuery` | Start SELECT builder |
| `.insert(String table)` | `InsertQuery` | Start INSERT builder |
| `.update(String table)` | `UpdateQuery` | Start UPDATE builder |
| `.delete(String table)` | `DeleteQuery` | Start DELETE builder |
| `.createTable(String table)` | `CreateTableQuery` | Start CREATE TABLE builder |
| `.alterTable(String table)` | `AlterTableQuery` | Start ALTER TABLE builder |
| `.dropTable(String table)` | `DropTableQuery` | Start DROP TABLE builder |
| `.createIndex(String indexName)` | `CreateIndexQuery` | Start CREATE INDEX builder |
| `.dropIndex(String indexName)` | `DropIndexQuery` | Start DROP INDEX builder |
| `.pushSchema(Schema)` | `SchemaPushResult` | Additively push declarative schema to DB |
| `.execute(String sql, Object... params)` | `int` | Raw SQL execution (affected rows) |
| `.query(String sql, RowMapper<T>, Object... params)` | `List<T>` | Raw SELECT with custom RowMapper |
| `.querySingle(String sql, Class<T>, Object... params)` | `T` | Raw SELECT, first row or null |
| `.inTransaction(TransactionCallback<T>)` | `T` | Execute callback within a transaction |
| `.getConnection()` | `Connection` | Borrow connection from pool or return direct |
| `.close()` | `void` | Close direct connection (no-op for DataSource) |

### SelectQuery

| Method | Returns | Description |
|--------|---------|-------------|
| `.from(String table)` | `SelectQuery` | Set FROM table; aliases supported (`"users u"`) |
| `.where(Condition)` | `SelectQuery` | Filter rows |
| `.orderBy(String... cols)` | `SelectQuery` | ORDER BY ASC |
| `.orderByDesc(String... cols)` | `SelectQuery` | ORDER BY DESC |
| `.limit(int)` | `SelectQuery` | LIMIT |
| `.offset(int)` | `SelectQuery` | OFFSET |
| `.join(table, onLeft, onRight)` | `SelectQuery` | INNER JOIN |
| `.leftJoin(table, onLeft, onRight)` | `SelectQuery` | LEFT JOIN |
| `.rightJoin(table, onLeft, onRight)` | `SelectQuery` | RIGHT JOIN |
| `.execute(Class<T>)` | `List<T>` | Execute and map rows to POJO via reflection |
| `.execute(RowMapper<T>)` | `List<T>` | Execute and map rows via custom RowMapper |
| `.executeScalar(Class<T>)` | `T` | Execute and return first column of first row |
| `.toSql()` | `String` | Build SQL string |
| `.getParameters()` | `List<Object>` | Get parameter values |

### InsertQuery

| Method | Returns | Description |
|--------|---------|-------------|
| `.set(String col, Object val)` | `InsertQuery` | Set column value |
| `.setRaw(String col, String expr)` | `InsertQuery` | Set raw SQL expression (e.g. `CURRENT_TIMESTAMP`) |
| `.onConflict(String... columns)` | `InsertQuery` | Set UPSERT conflict target columns |
| `.onConflictOnConstraint(String name)` | `InsertQuery` | Set UPSERT conflict target by constraint name |
| `.doNothing()` | `InsertQuery` | Complete UPSERT with `ON CONFLICT ... DO NOTHING` |
| `.doUpdateSet(String... clauses)` | `InsertQuery` | Complete UPSERT with `ON CONFLICT ... DO UPDATE SET ...` |
| `.onConflictDoNothing()` | `InsertQuery` | Deprecated legacy shortcut |
| `.onConflictDoUpdate(String... clauses)` | `InsertQuery` | Deprecated legacy shortcut |
| `.addBatch()` | `InsertQuery` | Save current values as batch row |
| `.execute()` | `GeneratedKeys` | Execute insert and return generated keys |
| `.executeBatch()` | `int[]` | Execute accumulated batch rows |

Static helpers:

| Method | Returns | Description |
|--------|---------|-------------|
| `InsertQuery.excluded(String col)` | `String` | Returns `"col = EXCLUDED.col"` |
| `InsertQuery.setClause(String col, String expr)` | `String` | Returns `"col = expr"` |

### UpdateQuery / DeleteQuery

| Builder | Method | Description |
|---------|--------|-------------|
| `UpdateQuery` | `.set(String col, Object val)` | Set column value |
| `UpdateQuery` | `.setRaw(String col, String expr)` | Set raw SQL expression |
| `UpdateQuery` | `.where(Condition)` | Filter rows to update |
| `UpdateQuery` | `.execute()` | Execute and return affected row count |
| `DeleteQuery` | `.where(Condition)` | Filter rows to delete |
| `DeleteQuery` | `.execute()` | Execute and return affected row count |

### Declarative Schema API

Use this for a Drizzle-like Java `DbSchema.java` file.

```java
public final class DbSchema {
    public static final Schema SCHEMA = Schema.create()
        .table("users", table -> table
            .column("id", "UUID", col -> col.defaultPostgresUuid().primaryKey())
            .column("email", "VARCHAR(255)", col -> col.notNull().unique())
            .index("idx_users_email", idx -> idx.on("email").unique()))
        .table("posts", table -> table
            .column("id", "UUID", col -> col.defaultPostgresUuid().primaryKey())
            .column("user_id", "UUID", col -> col.notNull().references("users(id)"))
            .column("title", "VARCHAR(200)", col -> col.notNull())
            .index("idx_posts_user_id", "user_id"));
}

SchemaPushResult result = db.pushSchema(DbSchema.SCHEMA);
```

Schema classes/methods:

| Method | Description |
|--------|-------------|
| `Schema.create()` | Create empty declarative schema |
| `Schema.define(Consumer<Schema>)` | Create and fill schema in one callback |
| `schema.table(String, Consumer<TableDefinition>)` | Add table definition |
| `schema.table(Table, Consumer<TableDefinition>)` | Add table via type-safe table reference |
| `schema.toSql()` | Preview idempotent CREATE statements |
| `db.pushSchema(schema)` | Create missing tables/columns/indexes |
| `SchemaPushResult.executedSql()` | SQL statements executed during push |
| `SchemaPushResult.changed()` | Whether push changed DB |

`pushSchema()` behavior:

- Creates missing tables with declared columns and table-level constraints.
- Adds missing columns to existing tables.
- Creates missing indexes.
- Does not drop or rewrite existing tables, columns, or indexes.
- Fails fast for table-level constraint changes on existing tables; use explicit migration SQL.
- Supports schema-qualified table names such as `PUBLIC.users`.
- Uses schema-aware JDBC metadata lookup to avoid cross-schema false positives.
- Runs transactionally where supported and performs best-effort cleanup for DDL executed before a later failure.

### Declarative TableDefinition

| Method | Description |
|--------|-------------|
| `.column(String name, String definition)` | Add column using full type/constraint fragment |
| `.column(String name, String type, Consumer<ColumnDefinition>)` | Add and configure column |
| `.column(Column<?> column, String definition)` | Type-safe column overload |
| `.primaryKey(String... columns)` | Add table-level primary key |
| `.unique(String... columns)` | Add table-level unique constraint |
| `.foreignKey(String column, String reference)` | Add table-level FK |
| `.check(String expression)` | Add table-level CHECK |
| `.constraint(String sql)` | Add raw table-level constraint |
| `.index(String name, String... columns)` | Add non-unique index |
| `.index(String name, Consumer<IndexDefinition>)` | Add and configure index |

### Declarative ColumnDefinition

| Method | SQL fragment |
|--------|--------------|
| `.notNull()` | `NOT NULL` |
| `.primaryKey()` | `PRIMARY KEY` |
| `.unique()` | `UNIQUE` |
| `.autoIncrement()` | `AUTO_INCREMENT` |
| `.generatedByDefaultAsIdentity()` | `GENERATED BY DEFAULT AS IDENTITY` |
| `.defaultExpression(String expr)` | `DEFAULT expr` |
| `.defaultPostgresUuid()` | `DEFAULT gen_random_uuid()` |
| `.defaultUuid(UuidDialect)` | DB-specific UUID default |
| `.references(String ref)` | `REFERENCES ref` |
| `.check(String expr)` | `CHECK (expr)` |
| `.constraint(String sql)` | raw inline constraint |

### UUID helpers

| Method | Description |
|--------|-------------|
| `Uuids.v4()` | Generate random Java UUIDv4 |
| `Uuids.randomV4()` | Alias target for UUIDv4 generation |
| `Uuids.v7()` | Generate time-ordered Java UUIDv7 (RFC 9562) |
| `Uuids.timeOrderedV7()` | Alias target for UUIDv7 generation |
| `Uuids.generate(UuidVersion.V4/V7)` | Generate requested UUID version |
| `Uuids.defaultExpression(UuidDialect.POSTGRES)` | `gen_random_uuid()` |
| `Uuids.defaultExpression(UuidDialect.MYSQL)` | `UUID()` |
| `Uuids.defaultExpression(UuidDialect.HSQLDB)` | `UUID()` |

UUIDv10 is not a standardized UUID version; prefer UUIDv7 for time-ordered IDs.

### Conditions API (static import)

| Method | SQL output |
|--------|------------|
| `eq("col", val)` | `col = ?` |
| `ne("col", val)` | `col <> ?` |
| `gt("col", val)` | `col > ?` |
| `gte("col", val)` | `col >= ?` |
| `lt("col", val)` | `col < ?` |
| `lte("col", val)` | `col <= ?` |
| `like("col", val)` | `col LIKE ?` |
| `in("col", v1, v2)` | `col IN (?, ?)` |
| `between("col", a, b)` | `col BETWEEN ? AND ?` |
| `isNull("col")` | `col IS NULL` |
| `isNotNull("col")` | `col IS NOT NULL` |
| `not(cond)` | `NOT (cond)` |
| `cond1.and(cond2)` | `cond1 AND cond2` |
| `cond1.or(cond2)` | `cond1 OR cond2` |
| `and(cond1, cond2, ...)` | `cond1 AND cond2 AND ...` |
| `or(cond1, cond2, ...)` | `cond1 OR cond2 OR ...` |
| `raw("sql")` | Raw SQL fragment |

### Query interface

All query types implement `Query`:

- `.toSql()` — builds the parameterized SQL string.
- `.getParameters()` — returns the list of `?` values in order.

### ResultMapper

Reflection-based mapping:

- Matches `ResultSet` column names to Java object fields.
- Case-insensitive.
- Converts underscores to camelCase (`first_name` → `firstName`).
- Supports primitives, wrappers, String, and enums.

### RowMapper

```java
@FunctionalInterface
public interface RowMapper<T> {
    T mapRow(ResultSet rs, int rowNum) throws SQLException;
}
```

### TransactionCallback

```java
@FunctionalInterface
public interface TransactionCallback<T> {
    T execute(JdbORM jdborm);
}
```

### GeneratedKeys

Returned by `InsertQuery.execute()`:

- `.get(String name)` — get key by name.
- `.getFirst()` — get first generated key.
- `.asMap()` — all keys as map.

## End-to-end Usage

```java
import static com.bitaspire.jdborm.condition.Conditions.*;

JdbORM db = JdbORM.create(dataSource);

// SELECT with conditions and ordering
List<User> users = db.select("id", "name", "email")
    .from("users")
    .where(eq("age", 18).and(gt("score", 100)))
    .orderBy("name")
    .limit(10)
    .execute(User.class);

// SELECT with custom RowMapper
List<User> mapped = db.select("*").from("users")
    .execute((rs, i) -> new User(rs.getLong("id"), rs.getString("name")));

// Scalar result
Long count = db.select("count(*)").from("users").executeScalar(Long.class);

// INSERT with UUIDv7 generated in Java
var keys = db.insert("users")
    .set("id", Uuids.v7())
    .set("name", "John")
    .setRaw("created_at", "CURRENT_TIMESTAMP")
    .execute();

// Batch INSERT
InsertQuery insert = db.insert("users");
insert.set("name", "Alice").addBatch();
insert.set("name", "Bob").addBatch();
insert.executeBatch();

// UPDATE
int affected = db.update("users")
    .set("name", "Jane")
    .setRaw("updated_at", "CURRENT_TIMESTAMP")
    .where(eq("id", 1))
    .execute();

// DELETE
int deleted = db.delete("users")
    .where(eq("id", 1))
    .execute();

// JOIN
List<Post> posts = db.select("u.id", "p.title")
    .from("users u")
    .join("posts p", "p.user_id", "u.id")
    .leftJoin("comments c", "c.post_id", "p.id")
    .execute(Post.class);

// Declarative schema push
SchemaPushResult result = db.pushSchema(DbSchema.SCHEMA);

// Transaction
db.inTransaction(tx -> {
    tx.insert("users").set("name", "John").execute();
    tx.update("accounts").set("balance", 100).where(eq("id", 1)).execute();
    return null;
});

// Raw SQL
db.execute("UPDATE users SET name = ? WHERE id = ?", "John", 1);
```
