# Release Process — jdborm

When the user asks to release a new version, follow these steps **in order**.

## Step 1: Update version

Update `version` in `build.gradle.kts`:
```kotlin
version = "X.Y.Z"
```

Update version references in `README.md`:
- Gradle dependency: `implementation("com.github.BitAspire:jdborm:X.Y.Z")`
- Maven dependency: `<version>X.Y.Z</version>`
- Feature section header if present: `## Features added in vX.Y.Z`

## Step 2: Build

```bash
./gradlew clean build
```

Verify: `BUILD SUCCESSFUL`

## Step 3: Commit, tag & release

```bash
# Delete old tag if re-releasing same version
git tag -d X.Y.Z 2>$null
git push origin --delete X.Y.Z 2>$null

# Clean up old GitHub Release if re-releasing
gh release delete X.Y.Z --yes 2>$null

# Commit and push
git add -A
git commit -m "Bump version to X.Y.Z"
git push
git tag X.Y.Z
git push origin X.Y.Z

# Create GitHub Release (NOT draft, with JARs attached)
gh release create X.Y.Z --title "vX.Y.Z" --notes "Release vX.Y.Z of jdborm — lightweight JDBC ORM library with fluent API." "build\libs\jdborm-X.Y.Z.jar" "build\libs\jdborm-X.Y.Z-sources.jar" "build\libs\jdborm-X.Y.Z-javadoc.jar"
```
