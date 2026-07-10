# jdborm AI Reference

Complete API reference for AI coding assistants.

## Package root

`com.bitaspire.jdborm`

## Core principles

- Java 8+.
- Zero external runtime dependencies.
- Fluent mutable builders.
- JDBC-friendly APIs; no annotation processors or entity manager state.
- Prefer static imports for `com.bitaspire.jdborm.condition.Conditions.*`.
- All public API additions must have Javadoc.

## Entry point: `JdbORM`

```java
JdbORM.create(DataSource ds)                       // pooled connections
JdbORM.create(Connection conn)                     // caller-managed connection
JdbORM.connect(String url)                         // DriverManager, no auth
JdbORM.connect(String url, String user, String pw) // DriverManager with auth

.select(String... columns)        -> SelectQuery
.select(Column<?>... columns)     -> SelectQuery
.insert(String table)             -> InsertQuery
.insert(Table table)              -> InsertQuery
.update(String table)             -> UpdateQuery
.update(Table table)              -> UpdateQuery
.delete(String table)             -> DeleteQuery
.delete(Table table)              -> DeleteQuery

.createTable(String table)        -> CreateTableQuery
.alterTable(String table)         -> AlterTableQuery
.dropTable(String table)          -> DropTableQuery
.truncateTable(String table)      -> TruncateQuery
.renameTable(oldName, newName)    -> RenameTableQuery
.createIndex(String indexName)    -> CreateIndexQuery
.dropIndex(String indexName)      -> DropIndexQuery

.pushSchema(Schema schema)        -> SchemaPushResult
.execute(String sql, Object... params) -> int
.query(String sql, RowMapper<T>, Object... params) -> List<T>
.querySingle(String sql, Class<T>, Object... params) -> T
.inTransaction(TransactionCallback<T>) -> T
.getConnection() -> Connection
.close() -> void
```

## Query builders

### SelectQuery

```java
.from(String table)                         -> SelectQuery // alias: "users u"
.from(Table table)                          -> SelectQuery
.where(Condition condition)                 -> SelectQuery
.orderBy(String... columns)                 -> SelectQuery // ASC
.orderBy(Column<?>... columns)              -> SelectQuery
.orderByDesc(String... columns)             -> SelectQuery // DESC
.orderByDesc(Column<?>... columns)          -> SelectQuery
.limit(int n)                               -> SelectQuery
.offset(int n)                              -> SelectQuery
.join(String table, String onLeft, String onRight)      -> SelectQuery
.leftJoin(String table, String onLeft, String onRight)  -> SelectQuery
.rightJoin(String table, String onLeft, String onRight) -> SelectQuery
.execute(Class<T> type)                     -> List<T>
.execute(RowMapper<T> mapper)               -> List<T>
.executeScalar(Class<T> type)               -> T
toSql()                                     -> String
getParameters()                             -> List<Object>
```

### InsertQuery

```java
.set(String column, Object value)            -> InsertQuery
.set(Column<T> column, T value)              -> InsertQuery
.setRaw(String column, String expression)    -> InsertQuery
.setRaw(Column<?> column, String expression) -> InsertQuery
.onConflict(String... columns)               -> InsertQuery
.onConflictOnConstraint(String name)         -> InsertQuery
.doNothing()                                 -> InsertQuery // requires conflict target
.doUpdateSet(String... clauses)              -> InsertQuery // requires conflict target
.onConflictDoNothing()                       -> InsertQuery // legacy shortcut
.onConflictDoUpdate(String... clauses)       -> InsertQuery // legacy shortcut
.addBatch()                                  -> InsertQuery
.execute()                                   -> GeneratedKeys
.executeBatch()                              -> int[]

InsertQuery.excluded("name")                -> "name = EXCLUDED.name"
InsertQuery.setClause("updated_at", "CURRENT_TIMESTAMP") -> "updated_at = CURRENT_TIMESTAMP"
```

### UpdateQuery / DeleteQuery

```java
// UpdateQuery
.set(String column, Object value)            -> UpdateQuery
.set(Column<T> column, T value)              -> UpdateQuery
.setRaw(String column, String expression)    -> UpdateQuery
.where(Condition condition)                  -> UpdateQuery
.execute()                                   -> int

// DeleteQuery
.where(Condition condition)                  -> DeleteQuery
.execute()                                   -> int
```

## Conditions

Static import:

```java
import static com.bitaspire.jdborm.condition.Conditions.*;
```

Helpers:

```java
eq("col", val)        -> col = ?
ne("col", val)        -> col <> ?
gt("col", val)        -> col > ?
gte("col", val)       -> col >= ?
lt("col", val)        -> col < ?
lte("col", val)       -> col <= ?
like("col", val)      -> col LIKE ?
in("col", v1, v2)     -> col IN (?, ?)
between("col", a, b)  -> col BETWEEN ? AND ?
isNull("col")         -> col IS NULL
isNotNull("col")      -> col IS NOT NULL
not(condition)         -> NOT (condition)
and(c1, c2, ...)       -> c1 AND c2 AND ...
or(c1, c2, ...)        -> c1 OR c2 OR ...
raw("sql fragment")    -> verbatim SQL

condition.and(other)
condition.or(other)
```

Type-safe overloads accept `Column<T>` where applicable.

## Declarative schema API

Use a Java schema file similar to Drizzle's `schema.ts`:

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

### Schema root

```java
Schema.create()                                      -> Schema
Schema.define(Consumer<Schema> builder)              -> Schema
schema.table(String name, Consumer<TableDefinition>) -> Schema
schema.table(Table table, Consumer<TableDefinition>) -> Schema
schema.toSql()                                       -> List<String>
```

### `pushSchema()` behavior

- Creates missing tables with columns and table-level constraints.
- Adds missing columns to existing tables.
- Creates missing indexes.
- Does not drop tables, drop columns, rewrite existing columns, or rewrite existing indexes.
- Fails fast for table-level constraints on existing tables because constraint diffing is not supported.
- Supports schema-qualified table names such as `PUBLIC.users`.
- Uses schema-aware metadata lookup.
- Runs transactionally when supported and performs best-effort cleanup after partial DDL failure.

### TableDefinition

```java
.column(String name, String definition)                       -> TableDefinition
.column(String name, String type, Consumer<ColumnDefinition>) -> TableDefinition
.column(Column<?> column, String definition)                  -> TableDefinition
.column(Column<?> column, String type, Consumer<ColumnDefinition>) -> TableDefinition
.primaryKey(String... columns)                                -> TableDefinition
.unique(String... columns)                                    -> TableDefinition
.foreignKey(String column, String reference)                  -> TableDefinition
.check(String expression)                                     -> TableDefinition
.constraint(String rawSql)                                    -> TableDefinition
.index(String name, String... columns)                        -> TableDefinition
.index(String name, Consumer<IndexDefinition>)                -> TableDefinition
```

### ColumnDefinition

```java
.notNull()                          -> NOT NULL
.primaryKey()                       -> PRIMARY KEY
.unique()                           -> UNIQUE
.autoIncrement()                    -> AUTO_INCREMENT
.generatedByDefaultAsIdentity()     -> GENERATED BY DEFAULT AS IDENTITY
.defaultExpression(String expr)     -> DEFAULT expr
.defaultPostgresUuid()              -> DEFAULT gen_random_uuid()
.defaultUuid(UuidDialect dialect)   -> dialect-specific UUID default
.references(String ref)             -> REFERENCES ref
.check(String expr)                 -> CHECK (expr)
.constraint(String rawSql)          -> raw inline constraint
```

### IndexDefinition

```java
.on(String... columns)              -> IndexDefinition
.on(Column<?>... columns)           -> IndexDefinition
.unique()                           -> IndexDefinition
.uniqueIndex()                      -> IndexDefinition
.using(String method)               -> IndexDefinition
.toSql()                            -> String
```

## UUID helpers

```java
Uuids.v4()                          -> UUID // random UUIDv4
Uuids.randomV4()                    -> UUID
Uuids.v7()                          -> UUID // RFC 9562 time-ordered UUIDv7
Uuids.timeOrderedV7()               -> UUID
Uuids.timeOrderedV7(Instant)        -> UUID
Uuids.generate(UuidVersion.V4)      -> UUID
Uuids.generate(UuidVersion.V7)      -> UUID
Uuids.defaultExpression(UuidDialect.POSTGRES) -> "gen_random_uuid()"
Uuids.defaultExpression(UuidDialect.MYSQL)    -> "UUID()"
Uuids.defaultExpression(UuidDialect.HSQLDB)   -> "UUID()"
```

UUIDv10 is not standardized. Prefer UUIDv7 for time-ordered Java-generated IDs.

## DDL builders

```java
db.createTable("users")
    .ifNotExists()
    .column("id", "UUID PRIMARY KEY")
    .column("email", "VARCHAR(255) NOT NULL UNIQUE")
    .execute();

db.alterTable("users")
    .addColumn("age", "INTEGER")
    .addColumnIfNotExists("created_at", "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    .dropColumn("old_field")
    .renameColumn("email", "email_address")
    .modifyColumn("name", "VARCHAR(200)")
    .addPrimaryKey("id")
    .addForeignKey("user_id", "users(id)")
    .addUnique("email")
    .addCheck("age >= 0")
    .dropConstraint("old_constraint")
    .dropPrimaryKey()
    .execute();

db.dropTable("users").ifExists().cascade().execute();
db.truncateTable("users").execute();
db.renameTable("users", "customers").execute();

db.createIndex("idx_users_email")
    .on("users", "email")
    .unique()
    .ifNotExists()
    .using("HASH")
    .execute();

db.dropIndex("idx_users_email").on("users").ifExists().cascade().execute();
```

## Mapping

### ResultMapper

- Reflection-based `ResultSet` to POJO mapping.
- Matches column labels to fields or setters.
- Case-insensitive.
- Converts snake_case to camelCase.
- Supports primitives, wrappers, String, and enums.

### RowMapper

```java
@FunctionalInterface
public interface RowMapper<T> {
    T mapRow(ResultSet rs, int rowNum) throws SQLException;
}
```

## GeneratedKeys

Returned by `InsertQuery.execute()`:

```java
keys.get("id")
keys.getFirst()
keys.asMap()
```

## Common usage patterns

```java
// SELECT
List<User> users = db.select("id", "name")
    .from("users")
    .where(eq("active", true))
    .execute(User.class);

// Custom mapper
List<User> mapped = db.select("id", "name")
    .from("users")
    .execute((rs, rowNum) -> new User(rs.getLong("id"), rs.getString("name")));

// INSERT with UUIDv7
InsertQuery.GeneratedKeys keys = db.insert("users")
    .set("id", Uuids.v7())
    .set("email", "alice@example.com")
    .execute();

// UPSERT
InsertQuery.GeneratedKeys upsertKeys = db.insert("users")
    .set("email", "alice@example.com")
    .set("name", "Alice")
    .onConflict("email")
    .doUpdateSet(InsertQuery.excluded("name"))
    .execute();

// Transaction
db.inTransaction(tx -> {
    tx.insert("users").set("name", "John").execute();
    tx.update("accounts").set("balance", 100).where(eq("id", 1)).execute();
    return null;
});
```

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
