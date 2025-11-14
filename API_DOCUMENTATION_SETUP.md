# API Documentation Setup

## Overview

This project now automatically generates comprehensive API documentation using **Dokka** (Kotlin's documentation engine) whenever you build the project.

---

## Features

✅ **Automatic Generation**: Documentation is generated automatically when you run `./gradlew assemble` or `./gradlew assembleDebug`
✅ **HTML Output**: Beautiful, browsable HTML documentation
✅ **Comprehensive**: Documents all public classes, functions, properties, and their relationships
✅ **Kotlin-First**: Native support for Kotlin-specific features (extension functions, data classes, etc.)
✅ **Android-Aware**: Understands Android SDK types and components
✅ **Search & Navigation**: Full-text search and package/class navigation

---

## Usage

### Automatic Generation (During Build)

The documentation is automatically generated when you build the project:

```bash
./gradlew assembleDebug
```

or

```bash
./gradlew assembleRelease
```

After the build completes, you'll see a message like:

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📚 API Documentation Generated Successfully!
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  Location: /path/to/project/app/build/docs/api
  Format:   HTML

  To view the documentation:
  open /path/to/project/app/build/docs/api/index.html
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

### Manual Generation (Without Building)

To generate documentation without building the entire project:

```bash
./gradlew generateApiDocs
```

This is faster if you only need to update the documentation.

### Build With Documentation (Explicit)

Use this task to explicitly build with documentation:

```bash
./gradlew assembleWithDocs
```

---

## Viewing the Documentation

### Option 1: Open in Browser

```bash
open app/build/docs/api/index.html
```

Or navigate to: `app/build/docs/api/index.html` in your file browser and double-click.

### Option 2: Local Server (Recommended for Better Experience)

```bash
cd app/build/docs/api
python3 -m http.server 8000
```

Then open http://localhost:8000 in your browser.

---

## Documentation Structure

The generated documentation includes:

### Package Documentation
- Overview of each package
- List of classes, interfaces, and objects in the package

### Class Documentation
- Class description
- Constructor parameters
- Properties with types and descriptions
- Functions with parameters, return types, and descriptions
- Inherited members
- Extension functions

### Navigation Features
- **Search**: Full-text search across all documented code
- **Package Tree**: Hierarchical view of all packages
- **Class Hierarchy**: Shows inheritance relationships
- **Source Links**: (Can be configured to link to GitHub)

---

## Configuration

### Location

Documentation configuration is in `app/build.gradle.kts`:

```kotlin
dokka {
    moduleName.set("AI Model Android App")

    dokkaPublications.html {
        outputDirectory.set(layout.buildDirectory.dir("docs/api"))
        suppressObviousFunctions.set(false)
    }

    dokkaSourceSets.configureEach {
        suppressGeneratedFiles.set(false)

        // Skip test sources
        suppressedFiles.from(
            fileTree(projectDir.resolve("src/test")),
            fileTree(projectDir.resolve("src/androidTest"))
        )
    }
}
```

### Customization Options

You can customize the documentation by editing `app/build.gradle.kts`:

#### Change Module Name
```kotlin
moduleName.set("Your App Name")
```

#### Change Output Directory
```kotlin
dokkaPublications.html {
    outputDirectory.set(file("docs/api"))
}
```

#### Add Source Links to GitHub
Uncomment and configure in `dokkaSourceSets.configureEach`:
```kotlin
sourceLink {
    localDirectory.set(projectDir.resolve("src"))
    remoteUrl.set(uri("https://github.com/yourorg/yourrepo/tree/main/app/src").toURL())
    remoteLineSuffix.set("#L")
}
```

#### Include Custom Documentation
Uncomment in `dokkaSourceSets.configureEach`:
```kotlin
includes.from("Module.md")
```

Then create `app/Module.md` with custom overview documentation.

---

## Writing Good Documentation

### KDoc Format

Document your public APIs using KDoc comments:

```kotlin
/**
 * Brief description of the class or function.
 *
 * More detailed explanation if needed. Can span
 * multiple lines and include examples.
 *
 * @param userId The unique identifier for the user
 * @param includeDetails Whether to include full user details
 * @return User object if found, null otherwise
 * @throws NetworkException If the network request fails
 * @see UserRepository for more details
 */
suspend fun getUser(userId: Int, includeDetails: Boolean = false): User? {
    // Implementation
}
```

### Best Practices

1. **Every Public API**: Document all public classes, functions, and properties
2. **Be Specific**: Use specific parameter and return value descriptions
3. **Include Examples**: Add code examples for complex functions
4. **Link Related Items**: Use `@see` to link to related classes/functions
5. **Document Exceptions**: Use `@throws` for all exceptions that can be thrown
6. **Explain Why**: Document the purpose and use cases, not just what it does

### Example

```kotlin
/**
 * Repository for managing user data with offline-first architecture.
 *
 * This repository implements a caching strategy with automatic sync:
 * - Reads from local database when offline
 * - Syncs with remote API when online
 * - Resolves conflicts using last-write-wins strategy
 *
 * Example usage:
 * ```kotlin
 * val users = userRepository.getUsers().first()
 * userRepository.syncData()
 * ```
 *
 * @property userDao Local database access object
 * @property networkDataSource Remote API data source
 * @see CachingDataRepository for caching implementation
 */
class OfflineFirstDataRepository @Inject constructor(
    private val userDao: UserDao,
    private val networkDataSource: UserNetworkDataSource
) : DataRepository {
    // ...
}
```

---

## Build Integration

### How It Works

1. **Trigger**: When you run `./gradlew assemble` (or any variant like `assembleDebug`)
2. **Dokka Runs**: The `dokkaGeneratePublicationHtml` task executes
3. **Documentation Generated**: HTML files are created in `app/build/docs/api/`
4. **Success Message**: Custom task prints the location of the documentation

### Tasks Created

- `dokkaGeneratePublicationHtml`: Core Dokka task that generates HTML
- `generateApiDocs`: Wrapper task that runs Dokka and prints success message
- `assembleWithDocs`: Convenience task that builds and generates docs explicitly

---

## Continuous Integration

### GitHub Actions Example

Add this to your CI workflow to publish documentation:

```yaml
- name: Generate API Documentation
  run: ./gradlew generateApiDocs

- name: Deploy Documentation to GitHub Pages
  uses: peaceiris/actions-gh-pages@v3
  with:
    github_token: ${{ secrets.GITHUB_TOKEN }}
    publish_dir: ./app/build/docs/api
```

---

## Dependencies Added

### gradle/libs.versions.toml
```toml
[versions]
dokka = "2.1.0"

[plugins]
dokka = { id = "org.jetbrains.dokka", version.ref = "dokka" }
```

### build.gradle.kts (root)
```kotlin
plugins {
    alias(libs.plugins.dokka) apply false
}
```

### app/build.gradle.kts
```kotlin
plugins {
    alias(libs.plugins.dokka)
}
```

---

## Troubleshooting

### Documentation Not Generated

**Problem**: `./gradlew assemble` doesn't create docs

**Solution**: Run `./gradlew assembleDebug` or `./gradlew generateApiDocs` explicitly

### Empty Documentation

**Problem**: Documentation is generated but shows no content

**Solution**: Ensure your classes have KDoc comments and are `public`

### Build Fails with Dokka Error

**Problem**: Build fails during documentation generation

**Solution**:
1. Check that all KDoc syntax is correct
2. Remove `includes.from("Module.md")` if Module.md has syntax errors
3. Run with `--stacktrace` to see detailed error

### Documentation Too Large

**Problem**: Documentation generation is slow or files are huge

**Solution**:
1. Suppress generated files: `suppressGeneratedFiles.set(true)`
2. Suppress obvious functions: `suppressObviousFunctions.set(true)`
3. Exclude packages: Add to `suppressedFiles.from()`

---

## Output Files

The documentation is generated at:
```
app/build/docs/api/
├── index.html          # Main entry point
├── navigation.html     # Package/class navigation
├── scripts/            # JavaScript for search and navigation
├── styles/             # CSS styling
├── images/             # Icons and images
└── [package-name]/     # Per-package documentation
```

---

## Version

- **Dokka Version**: 2.1.0
- **Format**: HTML (Dokka V2)
- **Gradle Plugin**: Kotlin Gradle DSL

---

## Resources

- [Dokka Documentation](https://kotlinlang.org/docs/dokka-introduction.html)
- [KDoc Syntax Guide](https://kotlinlang.org/docs/kotlin-doc.html)
- [Dokka Gradle Plugin](https://kotlinlang.org/docs/dokka-gradle.html)

---

## Future Enhancements

Possible improvements:
- [ ] Add custom CSS styling
- [ ] Generate PDF documentation
- [ ] Add code coverage badges
- [ ] Auto-publish to GitHub Pages
- [ ] Generate Javadoc format for Java compatibility
- [ ] Add architecture diagrams

---

**Documentation Generated Automatically** ✨
Every time you build the project, the documentation stays up-to-date with your code!
