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
- Write Javadoc comments on all public classes, methods, and fields
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
│   └── ResultMapper.java    — ResultSet → POJO via reflection (field name matching)
├── query/
│   ├── SelectQuery.java     — SELECT builder: from(), where(), orderBy(), limit(), offset(), join/leftJoin/rightJoin(), execute(Class)
│   ├── InsertQuery.java     — INSERT builder: set(), execute() returns List<Long> (generated keys)
│   ├── UpdateQuery.java     — UPDATE builder: set(), where(), execute() returns int (affected rows)
│   ├── DeleteQuery.java     — DELETE builder: where(), execute() returns int
│   └── JoinClause.java      — Record: type, table, onLeft, onRight
└── schema/
    └── Column.java          — Type-safe column reference (table name + column name + type token)
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
| `.execute(Class<T>)` | `List<T>` | Execute and map rows to POJO |
| `.toSql()` | `String` | Build SQL string (for debugging) |
| `.getParameters()` | `List<Object>` | Get parameter values |

### InsertQuery

| Method | Returns | Description |
|--------|---------|-------------|
| `.set(String col, Object val)` | `InsertQuery` | Set column value |
| `.execute()` | `List<Long>` | Execute; returns generated keys |

### UpdateQuery

| Method | Returns | Description |
|--------|---------|-------------|
| `.set(String col, Object val)` | `UpdateQuery` | Set column value |
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

## Typické použití (end-to-end)

```java
// 1. Vytvoření instance
JdbORM db = JdbORM.create(dataSource);

// 2. SELECT s podmínkami a řazením
List<User> users = db.select("id", "name", "email")
    .from("users")
    .where(eq("age", 18).and(gt("score", 100)))
    .orderBy("name")
    .limit(10)
    .execute(User.class);

// 3. INSERT
List<Long> keys = db.insert("users")
    .set("name", "John")
    .set("email", "john@example.com")
    .execute();

// 4. UPDATE
int affected = db.update("users")
    .set("name", "Jane")
    .where(eq("id", 1))
    .execute();

// 5. DELETE
int deleted = db.delete("users")
    .where(eq("id", 1))
    .execute();

// 6. JOIN
List<Post> posts = db.select("u.id", "p.title")
    .from("users u")
    .join("posts p", "p.user_id", "u.id")
    .leftJoin("comments c", "c.post_id", "p.id")
    .execute(Post.class);

// 7. Složené podmínky
where(and(
    eq("status", "active"),
    or(eq("role", "admin"), eq("role", "moderator")),
    not(eq("banned", true))
));
```
