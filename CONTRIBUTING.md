# Contributing to jdborm

Thank you for your interest in improving jdborm. Bug reports, feature proposals, documentation improvements, and pull requests are all welcome.

## Before you begin

- Check the open [issues](https://github.com/BitAspire/jdborm/issues) and pull requests first to avoid duplicating work.
- For a substantial change or a new public API, please open an issue first. Briefly describe the problem, proposed solution, and any compatibility implications.
- The project targets Java 8+ and must not add runtime dependencies. The API should remain JDBC-friendly and fluent for method chaining.

## Set up your development environment

You need JDK 17 or newer to build this checkout. The library itself remains compatible with Java 8.

```bash
git clone https://github.com/BitAspire/jdborm.git
cd jdborm
./gradlew build
```

On Windows, use `gradlew.bat` instead of `./gradlew`.

Useful commands:

```bash
./gradlew compileJava                         # compile only
./gradlew test                                # run all tests
./gradlew test --tests "com.bitaspire.jdborm.SqlGenerationTest"  # run one test class
./gradlew build                               # compile, test, and create artifacts
```

## Submitting a change

1. Create a branch from the current main branch, for example `fix/select-parameters` or `feature/schema-indexes`.
2. Make one small, focused change. Please keep unrelated formatting and refactoring in separate pull requests.
3. Add or update tests that cover the new or fixed behavior. Name test classes `*Test.java`; integration tests may use HSQLDB.
4. Update the README or other examples whenever user-facing API behavior changes.
5. Run at least `./gradlew test`; we recommend `./gradlew build` before opening a pull request.
6. Open a pull request with a clear title and description: what changed, why, how you verified it, and whether it affects compatibility.

## Style and compatibility

- Follow the existing Java style and the package structure under `com.bitaspire.jdborm`.
- Every public class, method, and field must have complete Javadoc. Include `@param`, `@return`, and `@throws` tags where applicable.
- Prefer `record` for small immutable value objects where appropriate.
- New SQL behavior must use parameterized values wherever possible. Raw SQL should remain an explicit escape hatch.
- Do not remove or change existing public API without prior discussion in an issue.

## Reporting bugs and proposing features

Please include the following in an issue:

- the jdborm, JDK, database, and JDBC driver versions you use;
- a short reproducible example with the expected and actual result;
- a stack trace or generated SQL, if available;
- for feature requests, a concrete use case and ideally a sketch of the proposed API.

## Community conduct

Be respectful and constructive. Contributions are reviewed for their value, clarity, tests, documentation, and long-term maintainability.

By submitting a pull request, you confirm that you have the right to contribute the code under the project's [MIT License](LICENSE).
