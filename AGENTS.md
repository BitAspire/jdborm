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

# Run a single test method
./gradlew test --tests "com.bitaspire.jdborm.SqlGenerationTest.selectAll"

# Clean build artifacts
./gradlew clean
```

## Code Style

- Java 17+, no external runtime dependencies
- Package: `com.bitaspire.jdborm`
- Fluent API with method chaining (Drizzle-like)
- Static imports for `Conditions.*`
- **STRICTLY REQUIRED: Write Javadoc comments on ALL public classes, methods, and fields** — no exceptions. Every single public element must have a complete Javadoc (`@param`, `@return`, `@throws` where applicable). Missing Javadoc is considered a bug.
- Records for value objects (conditions, join clauses)
- Tests use JUnit 5 + AssertJ + HSQLDB for integration tests
- Test class naming: `*Test.java`
- No lombok or annotation processors

## Project Architecture

```
com.bitaspire.jdborm
├── JdbORM.java              — Entry point; create via JdbORM.create(dataSource)
├── condition/
│   ├── Condition.java       — Interface; records: SimpleCondition, AndCondition, OrCondition, etc.
│   └── Conditions.java      — Static factory: eq(), gt(), like(), and(), or(), not(), raw(), etc.
├── exception/
│   └── JdbOrmException.java — Unchecked exception wrapping SQLException
├── mapper/
│   ├── ResultMapper.java    — ResultSet → POJO via reflection (field name matching)
│   └── RowMapper.java       — Functional interface for custom result mapping
├── query/
│   ├── SelectQuery.java     — SELECT builder: from(), where(), orderBy(), limit(), offset(), join/leftJoin/rightJoin(), execute(Class), execute(RowMapper), executeScalar(Class)
│   ├── InsertQuery.java     — INSERT builder: set(), setRaw(), onConflictDoNothing(), onConflictDoUpdate(), addBatch(), executeBatch(), execute() returns GeneratedKeys
│   ├── UpdateQuery.java     — UPDATE builder: set(), setRaw(), where(), execute() returns int (affected rows)
│   ├── DeleteQuery.java     — DELETE builder: where(), execute() returns int
│   ├── JoinClause.java      — Record: type, table, onLeft, onRight
│   ├── RawExpression.java   — Record: expression wrapper for setRaw()
│   └── TransactionCallback.java — Functional interface for inTransaction()
└── schema/
    └── Column.java          — Type-safe column reference (table name + column name + type token)
    └── Table.java           — Type-safe table reference (name + optional alias)
```

## API Reference

### JdbORM (entry point)

| Method | Returns | Description |
|--------|---------|-------------|
| `JdbORM.create(DataSource)` | `JdbORM` | Pooled connections via DataSource |
| `JdbORM.create(Connection)` | `JdbORM` | Single connection (caller manages) |
| `JdbORM.connect(url, user, pass)` | `JdbORM` | Auto-connect via DriverManager |
| `.select(String... cols)` | `SelectQuery` | Start SELECT builder |
| `.insert(String table)` | `InsertQuery` | Start INSERT builder |
| `.update(String table)` | `UpdateQuery` | Start UPDATE builder |
| `.delete(String table)` | `DeleteQuery` | Start DELETE builder |
| `.execute(String sql, Object... params)` | `int` | Raw SQL execution (affected rows) |
| `.query(String sql, RowMapper<T>, Object... params)` | `List<T>` | Raw SELECT with custom RowMapper |
| `.querySingle(String sql, Class<T>, Object... params)` | `T` | Raw SELECT, first row or null |
| `.inTransaction(TransactionCallback<T>)` | `T` | Execute callback within a transaction |
| `.getConnection()` | `Connection` | Borrow connection from pool or return direct |
| `.close()` | `void` | Close direct connection (no-op for DataSource) |

### SelectQuery (fluent builder)

| Method | Returns | Description |
|--------|---------|-------------|
| `.from(String table)` | `SelectQuery` | Set FROM table (supports alias: "users u") |
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
| `.toSql()` | `String` | Build SQL string (for debugging) |
| `.getParameters()` | `List<Object>` | Get parameter values |

### InsertQuery

| Method | Returns | Description |
|--------|---------|-------------|
| `.set(String col, Object val)` | `InsertQuery` | Set column value |
| `.setRaw(String col, String expr)` | `InsertQuery` | Set raw SQL expression (e.g. "NOW()") |
| `.onConflictDoNothing()` | `InsertQuery` | Add ON CONFLICT DO NOTHING |
| `.onConflictDoUpdate(String... clauses)` | `InsertQuery` | Add ON CONFLICT DO UPDATE SET ... |
| `.addBatch()` | `InsertQuery` | Save current values as batch row |
| `.execute()` | `GeneratedKeys` | Execute; returns generated keys |
| `.executeBatch()` | `int[]` | Execute accumulated batch rows |

### UpdateQuery

| Method | Returns | Description |
|--------|---------|-------------|
| `.set(String col, Object val)` | `UpdateQuery` | Set column value |
| `.setRaw(String col, String expr)` | `UpdateQuery` | Set raw SQL expression (e.g. "counter + 1") |
| `.where(Condition)` | `UpdateQuery` | Filter rows to update |
| `.execute()` | `int` | Returns affected row count |

### DeleteQuery

| Method | Returns | Description |
|--------|---------|-------------|
| `.where(Condition)` | `DeleteQuery` | Filter rows to delete |
| `.execute()` | `int` | Returns affected row count |

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
- `.toSql()` — builds the parameterized SQL string
- `.getParameters()` — returns the list of `?` values in order

### Condition records

Internal records in `Condition.java`:
- `SimpleCondition(col, op, val)` — leaf: `col = ?`, `col > ?`, etc.
- `AndCondition(left, right)` — `cond1 AND cond2`, auto-parenthesizes OR child
- `OrCondition(left, right)` — `cond1 OR cond2`, auto-parenthesizes AND child
- `NotCondition(inner)` — `NOT (cond)`
- `InCondition(col, values[])` — `col IN (?, ?, ...)`
- `BetweenCondition(col, start, end)` — `col BETWEEN ? AND ?`
- `IsNullCondition(col, negated)` — `col IS NULL` / `col IS NOT NULL`
- `RawCondition(fragment)` — verbatim SQL

### ResultMapper

Reflection-based: matches `ResultSet` column names to Java object fields (case-insensitive, underscores → camelCase). Supports primitives, wrappers, String, enums.

### RowMapper (functional interface)

```java
@FunctionalInterface
public interface RowMapper<T> {
    T mapRow(ResultSet rs, int rowNum) throws SQLException;
}
```

### TransactionCallback (functional interface)

```java
@FunctionalInterface
public interface TransactionCallback<T> {
    T execute(JdbORM jdborm);
}
```

GeneratedKeys (returned by InsertQuery.execute()):
- `.get(String name)` — get key by name
- `.getFirst()` — get first generated key
- `.asMap()` — all keys as map

## End-to-end usage

```java
// 1. Create instance
JdbORM db = JdbORM.create(dataSource);

// 2. SELECT with conditions and ordering
List<User> users = db.select("id", "name", "email")
    .from("users")
    .where(eq("age", 18).and(gt("score", 100)))
    .orderBy("name")
    .limit(10)
    .execute(User.class);

// 3. SELECT with custom RowMapper
List<User> users = db.select("*").from("users")
    .execute((rs, i) -> new User(rs.getLong("id"), rs.getString("name")));

// 4. Scalar result
Long count = db.select("count(*)").from("users").executeScalar(Long.class);

// 5. INSERT with raw expression and ON CONFLICT
var keys = db.insert("users")
    .set("name", "John")
    .setRaw("created_at", "NOW()")
    .onConflictDoNothing()
    .execute();

// 6. Batch INSERT
InsertQuery ins = db.insert("users");
ins.set("name", "Alice").addBatch();
ins.set("name", "Bob").addBatch();
ins.executeBatch();

// 7. UPDATE with raw expression
int affected = db.update("users")
    .set("name", "Jane")
    .setRaw("updated_at", "NOW()")
    .where(eq("id", 1))
    .execute();

// 8. DELETE
int deleted = db.delete("users")
    .where(eq("id", 1))
    .execute();

// 9. JOIN
List<Post> posts = db.select("u.id", "p.title")
    .from("users u")
    .join("posts p", "p.user_id", "u.id")
    .leftJoin("comments c", "c.post_id", "p.id")
    .execute(Post.class);

// 10. Transaction
db.inTransaction(tx -> {
    tx.insert("users").set("name", "John").execute();
    tx.update("accounts").set("balance", 100).where(eq("id", 1)).execute();
    return null;
});

// 11. Raw SQL
db.execute("UPDATE users SET name = ? WHERE id = ?", "John", 1);

// 12. Compound conditions
where(and(
    eq("status", "active"),
    or(eq("role", "admin"), eq("role", "moderator")),
    not(eq("banned", true))
));
```
