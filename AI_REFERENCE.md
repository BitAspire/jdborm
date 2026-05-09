# jdborm AI Reference

Complete API reference for AI coding assistants.

## Package: `com.bitaspire.jdborm`

### JdbORM (entry point)

```
JdbORM.create(DataSource ds)          → JdbORM  // pooled connections
JdbORM.create(Connection conn)        → JdbORM  // single connection, caller manages
JdbORM.connect(String url)            → JdbORM  // DriverManager, no auth
JdbORM.connect(String url, String user, String pass) → JdbORM

.select(String... columns)  → SelectQuery  // empty = SELECT *
.insert(String table)       → InsertQuery
.update(String table)       → UpdateQuery
.delete(String table)       → DeleteQuery

.getConnection() → Connection  // borrow from pool or return direct
.close()         → void        // close direct connection only
```

### SelectQuery

```
.from(String table)                         → SelectQuery  // alias: "users u"
.where(Condition condition)                 → SelectQuery
.orderBy(String... columns)                 → SelectQuery  // ASC
.orderByDesc(String... columns)             → SelectQuery  // DESC
.limit(int n)                               → SelectQuery
.offset(int n)                              → SelectQuery
.join(String table, String onLeft, String onRight)     → SelectQuery  // INNER JOIN
.leftJoin(String table, String onLeft, String onRight) → SelectQuery  // LEFT JOIN
.rightJoin(String table, String onLeft, String onRight) → SelectQuery  // RIGHT JOIN
.execute(Class<T> type)                     → List<T>     // maps ResultSet to POJO
.toSql()                                    → String      // parameterized SQL
.getParameters()                            → List<Object>
```

### InsertQuery

```
.set(String column, Object value) → InsertQuery
.execute()                        → List<Long>  // generated keys
```

### UpdateQuery

```
.set(String column, Object value) → UpdateQuery
.where(Condition condition)       → UpdateQuery
.execute()                        → int  // affected rows
```

### DeleteQuery

```
.where(Condition condition) → DeleteQuery
.execute()                  → int  // affected rows
```

### Conditions (static import `com.bitaspire.jdborm.condition.Conditions.*`)

```
eq("col", val)        → col = ?
ne("col", val)        → col <> ?
gt("col", val)        → col > ?
gte("col", val)       → col >= ?
lt("col", val)        → col < ?
lte("col", val)       → col <= ?
like("col", val)      → col LIKE ?
in("col", v1, v2)     → col IN (?, ?)
between("col", a, b)  → col BETWEEN ? AND ?
isNull("col")         → col IS NULL
isNotNull("col")      → col IS NOT NULL
not(Condition)        → NOT (cond)
and(Condition...)     → cond1 AND cond2 AND ...
or(Condition...)      → cond1 OR cond2 OR ...
raw("sql fragment")   → verbatim SQL

// Instance methods on Condition:
cond1.and(cond2)     → cond1 AND cond2
cond1.or(cond2)      → cond1 OR cond2
```

### Condition records (internal, `Condition.java`)

| Record | Fields | SQL output |
|--------|--------|------------|
| `SimpleCondition` | `col, op, val` | `col = ?`, `col > ?` |
| `AndCondition` | `left, right` | `cond1 AND cond2` |
| `OrCondition` | `left, right` | `cond1 OR cond2` |
| `NotCondition` | `inner` | `NOT (cond)` |
| `InCondition` | `col, values[]` | `col IN (?, ?, ...)` |
| `BetweenCondition` | `col, start, end` | `col BETWEEN ? AND ?` |
| `IsNullCondition` | `col, negated` | `col IS NULL` / `col IS NOT NULL` |
| `RawCondition` | `sqlFragment` | Verbatim |

### Query interface

All query types (Select, Insert, Update, Delete) implement:
- `toSql()` → String
- `getParameters()` → List<Object>

### ResultMapper

- Reflection-based: matches `ResultSet` column names to Java fields
- Case-insensitive matching
- Underscores → camelCase (e.g. `first_name` → field `firstName`)
- Supports: primitives, wrappers, String, enums

### Column (schema)

```
Column(String table, String name, Class<T> type)
Column.of("users", "id", Long.class)
```

### JdbOrmException

- Extends `RuntimeException`
- Wraps `SQLException`
- Constructor: `JdbOrmException(String message, SQLException cause)`

## Usage Patterns

### Basic SELECT
```java
JdbORM db = JdbORM.create(dataSource);
List<User> users = db.select("id", "name")
    .from("users")
    .where(eq("active", true))
    .execute(User.class);
```

### SELECT with JOIN
```java
List<Post> posts = db.select("u.name", "p.title")
    .from("users u")
    .join("posts p", "p.user_id", "u.id")
    .execute(Post.class);
```

### INSERT
```java
List<Long> keys = db.insert("users")
    .set("name", "Alice")
    .set("email", "alice@example.com")
    .execute();
```

### UPDATE
```java
int affected = db.update("users")
    .set("name", "Bob")
    .where(eq("id", 5))
    .execute();
```

### DELETE
```java
int deleted = db.delete("users")
    .where(eq("status", "inactive"))
    .execute();
```

### Complex conditions
```java
.where(and(
    eq("status", "active"),
    or(eq("role", "admin"), eq("role", "moderator")),
    not(eq("banned", true))
))
```

## Key conventions
- Fluent method chaining (mutable builders)
- Conditions use parameterized queries (PreparedStatement)
- No runtime dependencies beyond JDK 17+
- All query state is mutable during building
