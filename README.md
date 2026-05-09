# jdborm — Fluent JDBC Query Builder

A Java library inspired by [Drizzle ORM](https://orm.drizzle.team).  
Write SQL queries using fluent method chaining instead of raw strings.

## Quick Example

```java
import static com.bitaspire.jdborm.condition.Conditions.*;

JdbORM db = JdbORM.create(dataSource);

// SELECT with conditions
List<User> users = db.select("id", "name", "email")
    .from("users")
    .where(eq("age", 18).and(gt("score", 100)))
    .orderBy("name")
    .limit(10)
    .execute(User.class);

// INSERT
var keys = db.insert("users")
    .set("name", "John")
    .set("email", "john@example.com")
    .execute();
Long id = keys.getFirst();

// UPDATE
int affected = db.update("users")
    .set("name", "Jane")
    .where(eq("id", 1))
    .execute();

// DELETE
int deleted = db.delete("users")
    .where(eq("id", 1))
    .execute();
```

## Conditions API

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

## Joins

```java
db.select("u.id", "p.title")
    .from("users u")
    .join("posts p", "p.user_id", "u.id")
    .leftJoin("comments c", "c.post_id", "p.id")
    .execute(...);
```

## Build

```bash
./gradlew build
```
