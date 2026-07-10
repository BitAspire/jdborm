# jdborm — Fluent JDBC Query Builder

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Java 8+](https://img.shields.io/badge/Java-8%2B-blue.svg)](https://openjdk.org/projects/jdk/8/)
[![](https://jitpack.io/v/BitAspire/jdborm.svg)](https://jitpack.io/#BitAspire/jdborm)

**jdborm** is a lightweight, zero-dependency Java 8+ library inspired by [Drizzle ORM](https://orm.drizzle.team).
It gives you a fluent JDBC API for queries, DDL, transactions, and declarative schema push — without annotation processors, runtime magic, or heavyweight ORM state.

```java
import static com.bitaspire.jdborm.condition.Conditions.*;

JdbORM db = JdbORM.create(dataSource);

List<User> users = db.select("id", "name", "email")
    .from("users")
    .where(eq("active", true).and(gte("age", 18)))
    .orderBy("name")
    .limit(20)
    .execute(User.class);
```

## AI assistant skill

jdborm ships an AI skill so coding assistants can learn the API in your project:

```bash
npx jdborm-ai-skill
```

This installs `.agents/skills/jdborm/SKILL.md` with examples and API notes.

## Why jdborm?

- **Zero runtime dependencies** — just Java 8+ and JDBC.
- **Fluent SQL builders** — SELECT, INSERT, UPDATE, DELETE, joins, ordering, limits, and raw SQL escape hatches.
- **Parameterized conditions** — safer SQL generation with ordered `?` parameters.
- **Custom or reflection mapping** — map rows with `RowMapper<T>` or simple POJO field matching.
- **DDL helpers** — create/alter/drop tables and indexes with a fluent API.
- **Declarative schema push** — define tables in Java and push missing tables, columns, and indexes to the database.
- **UUID helpers** — database UUID defaults plus Java-side UUIDv4/UUIDv7 generation.

## Requirements

- Java 8+ at runtime
- Java 17+ to build this checkout with the included Gradle 9 wrapper
- JDBC driver for your database
- No external runtime dependencies from jdborm itself

## Installation

### Gradle

```kotlin
repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("com.github.BitAspire:jdborm:0.6.0")
}
```

### Maven

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
    <version>0.6.0</version>
</dependency>
```

## Quick start

```java
import com.bitaspire.jdborm.JdbORM;

import java.util.List;

import static com.bitaspire.jdborm.condition.Conditions.*;

JdbORM db = JdbORM.create(dataSource);

List<User> activeUsers = db.select("id", "name", "email")
    .from("users")
    .where(eq("active", true))
    .orderBy("name")
    .execute(User.class);

InsertQuery.GeneratedKeys keys = db.insert("users")
    .set("name", "Alice")
    .set("email", "alice@example.com")
    .execute();

int affected = db.update("users")
    .set("name", "Alice Smith")
    .where(eq("email", "alice@example.com"))
    .execute();

int deleted = db.delete("users")
    .where(eq("active", false))
    .execute();
```

## Defining schema in Java

For a Drizzle-like workflow, create a dedicated schema class such as `DbSchema.java`:

```java
package com.example.app.db;

import com.bitaspire.jdborm.schema.Schema;

public final class DbSchema {

    private DbSchema() {
    }

    public static final Schema SCHEMA = Schema.create()
        .table("users", table -> table
            .column("id", "UUID", col -> col.defaultPostgresUuid().primaryKey())
            .column("email", "VARCHAR(255)", col -> col.notNull().unique())
            .column("name", "VARCHAR(100)", col -> col.notNull())
            .column("created_at", "TIMESTAMP", col -> col.defaultExpression("CURRENT_TIMESTAMP"))
            .index("idx_users_email", idx -> idx.on("email").unique()))

        .table("posts", table -> table
            .column("id", "UUID", col -> col.defaultPostgresUuid().primaryKey())
            .column("user_id", "UUID", col -> col.notNull().references("users(id)"))
            .column("title", "VARCHAR(200)", col -> col.notNull())
            .column("content", "TEXT")
            .column("created_at", "TIMESTAMP", col -> col.defaultExpression("CURRENT_TIMESTAMP"))
            .index("idx_posts_user_id", "user_id"));
}
```

Push it during application/plugin startup:

```java
JdbORM db = JdbORM.create(dataSource);

SchemaPushResult result = db.pushSchema(DbSchema.SCHEMA);
if (result.changed()) {
    result.executedSql().forEach(System.out::println);
}
```

### `pushSchema()` behavior

`pushSchema()` is intentionally **additive and safe by default**:

| Declared object | If missing | If already exists |
|-----------------|------------|-------------------|
| Table | Created with all declared columns and table-level constraints | Kept as-is |
| Column | Added via `ALTER TABLE ... ADD COLUMN` | Kept as-is; existing type/default is not rewritten |
| Index | Created | Kept as-is |
| Table-level constraint | Created with a new table | Fails fast on existing tables; use explicit migration SQL |

Additional notes:

- Schema-qualified names are supported, for example `PUBLIC.users`.
- Metadata lookup is schema-aware and avoids confusing tables/indexes across schemas.
- The push runs transactionally where the JDBC/database combination supports transactional DDL, with best-effort cleanup for DDL changes made before a later failure.
- `schema.toSql()` returns idempotent preview statements using `CREATE TABLE IF NOT EXISTS` and `CREATE INDEX IF NOT EXISTS`.

## UUID helpers

### Database-generated UUID defaults

```java
import com.bitaspire.jdborm.schema.Schema;
import com.bitaspire.jdborm.schema.UuidDialect;

Schema schema = Schema.create()
    .table("users", table -> table
        // PostgreSQL: requires pgcrypto extension for gen_random_uuid()
        .column("id", "UUID", col -> col.defaultPostgresUuid().primaryKey())

        // MySQL/HSQLDB-style UUID() default
        .column("external_id", "CHAR(36)", col -> col.defaultUuid(UuidDialect.MYSQL)));
```

### Java-generated UUIDs

```java
import com.bitaspire.jdborm.schema.Uuids;

UUID randomId = Uuids.v4();
UUID orderedId = Uuids.v7(); // RFC 9562 time-ordered UUID

db.insert("users")
    .set("id", orderedId)
    .set("email", "alice@example.com")
    .execute();
```

## Query examples

### Conditions

```java
List<User> users = db.select("*")
    .from("users")
    .where(and(
        eq("status", "active"),
        or(eq("role", "admin"), eq("role", "moderator")),
        not(eq("banned", true))
    ))
    .execute(User.class);
```

Common condition helpers via `import static com.bitaspire.jdborm.condition.Conditions.*;`:

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
| `and(c1, c2, ...)` | `c1 AND c2 AND ...` |
| `or(c1, c2, ...)` | `c1 OR c2 OR ...` |
| `raw("sql")` | Raw SQL fragment |

### Joins

```java
List<Post> posts = db.select("u.id", "u.name", "p.title")
    .from("users u")
    .join("posts p", "p.user_id", "u.id")
    .leftJoin("comments c", "c.post_id", "p.id")
    .execute(Post.class);
```

### Custom row mapping

```java
List<User> users = db.select("id", "name")
    .from("users")
    .execute((rs, rowNum) -> new User(
        rs.getLong("id"),
        rs.getString("name")
    ));
```

### Scalar query

```java
Long count = db.select("count(*)")
    .from("users")
    .executeScalar(Long.class);
```

### Batch insert

```java
InsertQuery insert = db.insert("users");
insert.set("name", "Alice").addBatch();
insert.set("name", "Bob").addBatch();
int[] counts = insert.executeBatch();
```

### Upsert / ON CONFLICT

```java
db.insert("users")
    .set("id", Uuids.v7())
    .set("email", "alice@example.com")
    .set("name", "Alice")
    .onConflict("email")
    .doUpdateSet(
        InsertQuery.excluded("name"),
        InsertQuery.setClause("updated_at", "CURRENT_TIMESTAMP")
    )
    .execute();
```

### Transactions

```java
db.inTransaction(tx -> {
    tx.insert("users").set("name", "John").execute();
    tx.update("accounts").set("balance", 100).where(eq("id", 1)).execute();
    return null;
});
```

### Raw SQL

```java
int updated = db.execute("UPDATE users SET name = ? WHERE id = ?", "John", 1);

List<User> users = db.query(
    "SELECT * FROM users WHERE id = ?",
    (rs, rowNum) -> new User(rs.getLong("id"), rs.getString("name")),
    1
);
```

## DDL builder examples

If you prefer explicit DDL commands over declarative schema push:

```java
db.createTable("users")
    .ifNotExists()
    .column("id", "UUID PRIMARY KEY")
    .column("email", "VARCHAR(255) NOT NULL UNIQUE")
    .execute();

db.alterTable("users")
    .addColumnIfNotExists("created_at", "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    .execute();

db.createIndex("idx_users_email")
    .on("users", "email")
    .unique()
    .ifNotExists()
    .execute();
```

## API overview

| Area | Main entry points |
|------|-------------------|
| Query builders | `select()`, `insert()`, `update()`, `delete()` |
| Raw SQL | `execute()`, `query()`, `querySingle()` |
| Transactions | `inTransaction()` |
| DDL builders | `createTable()`, `alterTable()`, `dropTable()`, `createIndex()`, `dropIndex()` |
| Declarative schema | `Schema.create()`, `pushSchema()`, `SchemaPushResult` |
| UUID helpers | `Uuids.v4()`, `Uuids.v7()`, `defaultPostgresUuid()`, `defaultUuid()` |
| Debugging | every query supports `toSql()` and `getParameters()` |

## Build and test

```bash
./gradlew build
./gradlew test
```

## License

MIT
