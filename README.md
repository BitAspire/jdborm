# jdborm — Fluent JDBC Query Builder

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Java 17+](https://img.shields.io/badge/Java-17%2B-blue.svg)](https://openjdk.org/projects/jdk/17/)
[![](https://jitpack.io/v/BitAspire/jdborm.svg)](https://jitpack.io/#BitAspire/jdborm)

A lightweight, zero-dependency Java library inspired by [Drizzle ORM](https://orm.drizzle.team).  
Write type-safe SQL queries using fluent method chaining instead of raw string concatenation.

## Quick Start

### 1. Add dependency

**Gradle:**

```kotlin
repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("com.github.BitAspire:jdborm:0.3.1")
}
```

**Maven:**

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.BitAspire</groupId>
    <artifactId>jdborm</artifactId>
    <version>0.3.1</version>
</dependency>
```

### 2. Use it

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

// Custom RowMapper (no reflection)
List<User> users = db.select("*").from("users")
    .execute((rs, i) -> new User(rs.getLong("id"), rs.getString("name")));

// Scalar result (e.g. count)
Long count = db.select("count(*)").from("users").executeScalar(Long.class);

// INSERT with raw expression + ON CONFLICT
var keys = db.insert("users")
    .set("name", "John")
    .setRaw("created_at", "NOW())
    .onConflictDoNothing()
    .execute();

// Batch INSERT
InsertQuery ins = db.insert("users");
ins.set("name", "Alice").addBatch();
ins.set("name", "Bob").addBatch();
ins.executeBatch();

// UPDATE with raw expression
int affected = db.update("users")
    .set("name", "Jane")
    .setRaw("updated_at", "NOW())
    .where(eq("id", 1))
    .execute();

// DELETE
int deleted = db.delete("users")
    .where(eq("id", 1))
    .execute();

// Transaction
db.inTransaction(tx -> {
    tx.insert("users").set("name", "John").execute();
    tx.update("accounts").set("balance", 100).where(eq("id", 1)).execute();
    return null;
});

// Raw SQL
db.execute("UPDATE users SET name = ? WHERE id = ?", "John", 1);
List<User> users = db.query("SELECT * FROM users WHERE id = ?",
    (rs, i) -> new User(rs.getLong("id"), rs.getString("name")), 1);
```

## Conditions API

| Method | SQL output | Example |
|--------|------------|---------|
| `eq("col", val)` | `col = ?` | `eq("status", "active")` |
| `ne("col", val)` | `col <> ?` | `ne("age", 18)` |
| `gt("col", val)` | `col > ?` | `gt("price", 100)` |
| `gte("col", val)` | `col >= ?` | `gte("score", 50)` |
| `lt("col", val)` | `col < ?` | `lt("age", 21)` |
| `lte("col", val)` | `col <= ?` | `lte("rating", 5)` |
| `like("col", val)` | `col LIKE ?` | `like("name", "%john%")` |
| `in("col", v1, v2)` | `col IN (?, ?)` | `in("id", 1, 2, 3)` |
| `between("col", a, b)` | `col BETWEEN ? AND ?` | `between("price", 10, 50)` |
| `isNull("col")` | `col IS NULL` | `isNull("deleted_at")` |
| `isNotNull("col")` | `col IS NOT NULL` | `isNotNull("email")` |
| `not(cond)` | `NOT (cond)` | `not(eq("banned", true))` |
| `cond1.and(cond2)` | `cond1 AND cond2` | `eq("a", 1).and(eq("b", 2))` |
| `cond1.or(cond2)` | `cond1 OR cond2` | `eq("a", 1).or(eq("b", 2))` |
| `and(c1, c2, ...)` | `c1 AND c2 AND ...` | `and(eq("a",1), eq("b",2))` |
| `or(c1, c2, ...)` | `c1 OR c2 OR ...` | `or(eq("a",1), eq("b",2))` |
| `raw("sql")` | Raw SQL fragment | `raw("NOW()")` |

### Compound conditions example

```java
where(and(
    eq("status", "active"),
    or(eq("role", "admin"), eq("role", "moderator")),
    not(eq("banned", true))
));
```

## Joins

```java
List<Post> posts = db.select("u.id", "p.title")
    .from("users u")
    .join("posts p", "p.user_id", "u.id")
    .leftJoin("comments c", "c.post_id", "p.id")
    .rightJoin("likes l", "l.post_id", "p.id")
    .execute(Post.class);
```

## Query API overview

| Builder | Key methods | Returns |
|---------|-------------|---------|
| `JdbORM` | `.execute(sql, params)`, `.query(sql, mapper, params)`, `.inTransaction(callback)` | `int`, `List<T>`, `T` |
| `SelectQuery` | `.from()`, `.where()`, `.join()`, `.orderBy()`, `.limit()`, `.offset()` | `List<T>` / `execute(Class)`, `execute(RowMapper)`, `executeScalar(Class)` |
| `InsertQuery` | `.set()`, `.setRaw()`, `.onConflictDoNothing()`, `.onConflictDoUpdate()`, `.addBatch()` | `GeneratedKeys`, `int[]` |
| `UpdateQuery` | `.set()`, `.setRaw()`, `.where()` | `int` (affected rows) |
| `DeleteQuery` | `.where()` | `int` (affected rows) |

All builders support `.toSql()` and `.getParameters()` for debugging.

## Features added in v0.3.1

- `addColumnIfNotExists()` on `AlterTableQuery` — safe ADD COLUMN for PostgreSQL and others

## Features added in v0.3.0

- DDL schema management: `createTable()`, `alterTable()`, `dropTable()`
- `truncateTable()`, `renameTable()` for table management
- `createIndex()`, `dropIndex()` for index management
- Table-level constraints: PRIMARY KEY, FOREIGN KEY, UNIQUE, CHECK
- IF EXISTS / IF NOT EXISTS and CASCADE support
- Full type-safe API via `Table` and `Column` overloads

## Features added in v0.2.1

- Raw SQL execution (`execute()`, `query()`, `querySingle()`)
- `setRaw()` for SQL expressions in INSERT/UPDATE (`NOW()`, `counter + 1`, etc.)
- `ON CONFLICT DO NOTHING` / `ON CONFLICT DO UPDATE` on INSERT
- Custom `RowMapper` on SELECT (no reflection needed)
- `executeScalar()` for single-value results
- Batch INSERT with `addBatch()` / `executeBatch()`
- Transaction API (`inTransaction()`)

## AI Assistant Skill

jdborm comes with an AI skill that gives coding assistants deep knowledge of the entire API:

```bash
npx jdborm-ai-skill
```

Run this once in your project to install `.agents/skills/jdborm/SKILL.md` — your AI will then be able to write correct jdborm queries without being prompted about the API each time.

## Build

```bash
./gradlew build
```

## Requirements

- Java 17+
- No external runtime dependencies

## License

MIT
