# jdborm — Fluent JDBC Query Builder

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Java 17+](https://img.shields.io/badge/Java-17%2B-blue.svg)](https://openjdk.org/projects/jdk/17/)
[![Maven Central](https://img.shields.io/maven-central/v/com.bitaspire/jdborm)](https://search.maven.org/artifact/com.bitaspire/jdborm)

A lightweight, zero-dependency Java library inspired by [Drizzle ORM](https://orm.drizzle.team).  
Write type-safe SQL queries using fluent method chaining instead of raw string concatenation.

## Quick Start

### 1. Add dependency

```groovy
implementation 'com.bitaspire:jdborm:0.1.0'
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

// INSERT — returns generated keys
var keys = db.insert("users")
    .set("name", "John")
    .set("email", "john@example.com")
    .execute();
Long id = keys.getFirst();

// UPDATE — returns affected row count
int affected = db.update("users")
    .set("name", "Jane")
    .where(eq("id", 1))
    .execute();

// DELETE — returns affected row count
int deleted = db.delete("users")
    .where(eq("id", 1))
    .execute();
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

| Builder      | Key methods                                    | Returns                   |
|--------------|------------------------------------------------|---------------------------|
| `SelectQuery`| `.from()`, `.where()`, `.join()`, `.orderBy()`, `.limit()`, `.offset()` | `List<T>` |
| `InsertQuery`| `.set(col, val)`                               | `List<Long>` (generated keys) |
| `UpdateQuery`| `.set(col, val)`, `.where()`                   | `int` (affected rows)    |
| `DeleteQuery`| `.where()`                                     | `int` (affected rows)    |

All builders support `.toSql()` and `.getParameters()` for debugging.

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
