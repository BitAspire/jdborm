# Build & Test Commands

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

# Code Style

- Java 17+, no external runtime dependencies
- Package: `com.bitaspire.jdborm`
- Fluent API with method chaining (Drizzle-like)
- Static imports for `Conditions.*`
- Write Javadoc comments on all public classes, methods, and fields
- Records for value objects (conditions, join clauses)
- Tests use JUnit 5 + AssertJ + HSQLDB for integration tests
- Test class naming: `*Test.java`
- No lombok or annotation processors
