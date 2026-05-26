---
name: jdborm
description: Fluent JDBC query builder for Java. Provides Drizzle-like SELECT/INSERT/UPDATE/DELETE builders, DDL helpers, declarative schema push, UUID helpers, type-safe conditions, joins, transactions, and ResultSet mapping.
---

# jdborm Skill

This skill provides deep knowledge of the [jdborm](https://github.com/BitAspire/jdbc-orm-manager) library — a lightweight, zero-dependency Java 17+ fluent JDBC query builder inspired by Drizzle ORM.

## Core rules

- Package root: `com.bitaspire.jdborm`.
- Use `import static com.bitaspire.jdborm.condition.Conditions.*;` for conditions.
- Public API additions require Javadoc on every public class, method, and field.
- Keep additions zero-dependency and JDBC-friendly.
- Prefer explicit SQL fragments for dialect-specific DDL/defaults.

## Entry point

```java
JdbORM db = JdbORM.create(dataSource);
JdbORM db = JdbORM.create(connection);
JdbORM db = JdbORM.connect("jdbc:...");
JdbORM db = JdbORM.connect("jdbc:...", "user", "pass");
```

## Query examples

### SELECT

```java
List<User> users = db.select("id", "name", "email")
    .from("users")
    .where(eq("active", true).and(gte("age", 18)))
    .orderBy("name")
    .limit(20)
    .execute(User.class);
```

### Custom mapper

```java
List<User> users = db.select("id", "name")
    .from("users")
    .execute((rs, rowNum) -> new User(rs.getLong("id"), rs.getString("name")));
```

### INSERT / UPDATE / DELETE

```java
var keys = db.insert("users")
    .set("id", Uuids.v7())
    .set("email", "alice@example.com")
    .execute();

int affected = db.update("users")
    .set("name", "Alice")
    .where(eq("email", "alice@example.com"))
    .execute();

int deleted = db.delete("users")
    .where(eq("active", false))
    .execute();
```

### UPSERT / ON CONFLICT

```java
db.insert("users")
    .set("email", "alice@example.com")
    .set("name", "Alice")
    .onConflict("email")
    .doUpdateSet(InsertQuery.excluded("name"))
    .execute();
```

### JOINs

```java
db.select("u.id", "u.name", "p.title")
    .from("users u")
    .join("posts p", "p.user_id", "u.id")
    .leftJoin("comments c", "c.post_id", "p.id")
    .execute(Post.class);
```

### Transactions

```java
db.inTransaction(tx -> {
    tx.insert("users").set("name", "John").execute();
    tx.update("accounts").set("balance", 100).where(eq("id", 1)).execute();
    return null;
});
```

## Conditions

Static import `com.bitaspire.jdborm.condition.Conditions.*`.

Available helpers:

- `eq`, `ne`, `gt`, `gte`, `lt`, `lte`
- `like`, `in`, `between`
- `isNull`, `isNotNull`
- `not`, `and`, `or`, `raw`
- instance combinators: `cond1.and(cond2)`, `cond1.or(cond2)`

All normal condition helpers produce parameterized SQL and ordered `?` parameters.

## Declarative schema push

Use a Java `DbSchema.java` file, similar to Drizzle's `schema.ts`:

```java
public final class DbSchema {
    private DbSchema() {
    }

    public static final Schema SCHEMA = Schema.create()
        .table("users", table -> table
            .column("id", "UUID", col -> col.defaultPostgresUuid().primaryKey())
            .column("email", "VARCHAR(255)", col -> col.notNull().unique())
            .column("created_at", "TIMESTAMP", col -> col.defaultExpression("CURRENT_TIMESTAMP"))
            .index("idx_users_email", idx -> idx.on("email").unique()))
        .table("posts", table -> table
            .column("id", "UUID", col -> col.defaultPostgresUuid().primaryKey())
            .column("user_id", "UUID", col -> col.notNull().references("users(id)"))
            .column("title", "VARCHAR(200)", col -> col.notNull())
            .index("idx_posts_user_id", "user_id"));
}

SchemaPushResult result = db.pushSchema(DbSchema.SCHEMA);
```

### `pushSchema()` behavior

- Creates missing tables with all declared columns and table-level constraints.
- Adds missing columns to existing tables.
- Creates missing indexes.
- Does not drop tables/columns/indexes or rewrite existing definitions.
- Fails fast for table-level constraints on existing tables; use explicit migration SQL.
- Supports schema-qualified names such as `PUBLIC.users`.
- Uses schema-aware JDBC metadata lookup.
- Runs transactionally when supported, with best-effort cleanup after partial DDL failure.

### Schema DSL

```java
Schema.create()
Schema.define(schema -> { ... })
schema.table("users", table -> { ... })
schema.toSql()

// TableDefinition
table.column("id", "UUID", col -> col.defaultPostgresUuid().primaryKey())
table.column("email", "VARCHAR(255)", col -> col.notNull().unique())
table.primaryKey("id")
table.unique("email")
table.foreignKey("user_id", "users(id)")
table.check("age >= 0")
table.constraint("CONSTRAINT ...")
table.index("idx_users_email", idx -> idx.on("email").unique())

// ColumnDefinition
col.notNull()
col.primaryKey()
col.unique()
col.autoIncrement()
col.generatedByDefaultAsIdentity()
col.defaultExpression("CURRENT_TIMESTAMP")
col.defaultPostgresUuid()
col.defaultUuid(UuidDialect.POSTGRES)
col.references("users(id)")
col.check("age >= 0")
col.constraint("...")
```

## UUID helpers

```java
UUID id4 = Uuids.v4();          // random UUIDv4
UUID id7 = Uuids.v7();          // RFC 9562 time-ordered UUIDv7
UUID id = Uuids.generate(UuidVersion.V7);

String pg = Uuids.defaultExpression(UuidDialect.POSTGRES); // gen_random_uuid()
String mysql = Uuids.defaultExpression(UuidDialect.MYSQL); // UUID()
```

UUIDv10 is not a standardized UUID version. Prefer UUIDv7 for time-ordered Java-generated IDs.

## Explicit DDL builders

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

## Query interface

All query builders implement:

- `toSql()`
- `getParameters()`

## Result mapping

- `execute(Class<T>)` uses reflection-based `ResultMapper`.
- Column names match Java fields case-insensitively.
- `first_name` maps to `firstName`.
- Use `execute(RowMapper<T>)` for custom mapping and no reflection.

## Package structure

```text
com.bitaspire.jdborm
├── JdbORM.java
├── condition/Condition.java, Conditions.java
├── exception/JdbOrmException.java
├── mapper/ResultMapper.java, RowMapper.java
├── query/SelectQuery.java, InsertQuery.java, UpdateQuery.java, DeleteQuery.java, DDL builders, GeneratedKeys.java
└── schema/Table.java, Column.java, Schema.java, TableDefinition.java, ColumnDefinition.java, IndexDefinition.java, SchemaPushResult.java, Uuids.java
```

For full details, see `AGENTS.md` and `AI_REFERENCE.md` shipped with the library in `META-INF/jdborm/`.
