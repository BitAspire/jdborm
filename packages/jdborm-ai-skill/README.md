# jdborm AI Skill

AI assistant skill for [jdborm](https://github.com/BitAspire/jdbc-orm-manager), a zero-dependency Java 8+ fluent JDBC query builder inspired by Drizzle ORM.

The skill gives coding assistants practical knowledge of:

- SELECT / INSERT / UPDATE / DELETE builders
- Parameterized conditions and joins
- Batch inserts and UPSERT / `ON CONFLICT`
- Raw SQL and transactions
- Explicit DDL builders
- Declarative schema push with `Schema`, `TableDefinition`, `ColumnDefinition`, and `IndexDefinition`
- UUID helpers (`Uuids.v4()`, `Uuids.v7()`, database default helpers)
- ResultSet-to-POJO mapping and custom `RowMapper<T>` usage

## Installation

```bash
npx jdborm-ai-skill
```

This copies `SKILL.md` into `.agents/skills/jdborm/` so your AI assistant can reference the current jdborm API.

## Quick example

```java
import static com.bitaspire.jdborm.condition.Conditions.*;

JdbORM db = JdbORM.create(dataSource);

List<User> users = db.select("id", "email")
    .from("users")
    .where(eq("active", true))
    .execute(User.class);
```

## Declarative schema example

```java
public final class DbSchema {
    public static final Schema SCHEMA = Schema.create()
        .table("users", table -> table
            .column("id", "UUID", col -> col.defaultPostgresUuid().primaryKey())
            .column("email", "VARCHAR(255)", col -> col.notNull().unique())
            .index("idx_users_email", idx -> idx.on("email").unique()));
}

SchemaPushResult result = db.pushSchema(DbSchema.SCHEMA);
```

## Source

The complete jdborm library is at:

[https://github.com/BitAspire/jdbc-orm-manager](https://github.com/BitAspire/jdbc-orm-manager)

## License

MIT
