---
name: jdborm
description: Fluent JDBC query builder for Java. Provides Drizzle-like fluent API for SELECT, INSERT, UPDATE, DELETE with type-safe conditions and joins.
---

# jdborm Skill

This skill provides deep knowledge of the [jdborm](https://github.com/BitAspire/jdbc-orm-manager) library — a fluent JDBC query builder for Java 17+.

## API Overview

### Entry point: `JdbORM`

```java
JdbORM db = JdbORM.create(dataSource);
```

### SELECT
```java
db.select("id", "name").from("users")
    .where(eq("age", 18).and(gt("score", 100)))
    .orderBy("name").limit(10)
    .execute(User.class);
```

### INSERT / UPDATE / DELETE
```java
db.insert("users").set("name", "John").execute();         // → List<Long>
db.update("users").set("name", "Jane").where(eq("id",1)).execute(); // → int
db.delete("users").where(eq("id", 1)).execute();          // → int
```

### JOINs
```java
db.select("u.id", "p.title").from("users u")
    .join("posts p", "p.user_id", "u.id")
    .leftJoin("comments c", "c.post_id", "p.id")
    .execute(Post.class);
```

### Conditions (static import `Conditions.*`)
`eq`, `ne`, `gt`, `gte`, `lt`, `lte`, `like`, `in`, `between`, `isNull`, `isNotNull`, `not`, `and`, `or`, `raw`

Compound: `cond1.and(cond2)`, `cond1.or(cond2)`, `and(cond1, cond2, ...)`, `or(cond1, cond2, ...)`

### Query interface
All queries implement `toSql()` and `getParameters()`.

### ResultMapper
Reflection-based: `ResultSet` column names → Java fields (case-insensitive, underscores→camelCase). Supports primitives, wrappers, String, enums.

## Package structure
```
com.bitaspire.jdborm
├── JdbORM.java
├── condition/Condition.java, Conditions.java
├── exception/JdbOrmException.java
├── mapper/ResultMapper.java
├── query/SelectQuery.java, InsertQuery.java, UpdateQuery.java, DeleteQuery.java, JoinClause.java
└── schema/Column.java
```

For full details, see AGENTS.md and AI_REFERENCE.md shipped with the library in `META-INF/jdborm/`.
