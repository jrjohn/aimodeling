# Architecture Verification Configuration

This directory contains all configuration files, rules, and templates for the automated architecture verification system.

---

## 📁 Directory Structure

```
.architecture-verification/
├── README.md                   # This file
├── config/                     # Configuration files
│   ├── verification-config.yaml    # Main verification configuration
│   ├── severity-levels.yaml        # Warning/error severity configuration
│   └── exclusions.yaml             # Files/patterns to exclude from checks
├── rules/                      # Verification rules
│   ├── architecture/               # Architecture-specific rules
│   │   ├── clean-architecture.yaml
│   │   ├── mvvm-pattern.yaml
│   │   └── dependency-rules.yaml
│   ├── coding-style/               # Code style rules
│   │   ├── kotlin-conventions.yaml
│   │   ├── naming-conventions.yaml
│   │   └── code-organization.yaml
│   ├── annotations/                # Annotation usage rules
│   │   ├── required-annotations.yaml
│   │   └── annotation-patterns.yaml
│   └── testing/                    # Testing rules
│       ├── test-coverage.yaml
│       └── test-naming.yaml
├── templates/                  # Code templates and examples
│   ├── ViewModel.kt.template
│   ├── Repository.kt.template
│   └── Service.kt.template
└── reports/                    # Generated reports (gitignored)
    └── .gitkeep
```

---

## 🔧 Configuration Files

### 1. Main Configuration (`config/verification-config.yaml`)

Controls which verification checks are enabled and their behavior.

### 2. Severity Levels (`config/severity-levels.yaml`)

Defines severity levels for different types of violations:
- `CRITICAL` - Fails build immediately
- `ERROR` - Fails build after collecting all errors
- `WARNING` - Shows warning but doesn't fail build
- `INFO` - Informational only

### 3. Exclusions (`config/exclusions.yaml`)

Specify files, directories, or patterns to exclude from verification.

---

## 📋 Rule Categories

### Architecture Rules (`rules/architecture/`)

Define project architecture patterns and constraints:
- Clean Architecture layer boundaries
- MVVM pattern compliance
- Dependency direction rules
- Module boundaries

### Coding Style Rules (`rules/coding-style/`)

Enforce coding conventions:
- Kotlin coding conventions
- Naming patterns
- File organization
- Code formatting standards

### Annotation Rules (`rules/annotations/`)

Verify proper annotation usage:
- Required annotations (e.g., `@HiltViewModel`)
- Annotation combinations
- Custom annotation patterns

### Testing Rules (`rules/testing/`)

Define testing requirements:
- Test coverage requirements
- Test naming conventions
- Test structure patterns

---

## 🚀 Usage

### Enable/Disable Checks

Edit `config/verification-config.yaml`:

```yaml
checks:
  architecture:
    clean-architecture: true
    mvvm-pattern: true
  coding-style:
    kotlin-conventions: true
    naming-conventions: true
```

### Add Custom Rules

1. Create a new YAML file in the appropriate `rules/` subdirectory
2. Define your rules following the schema
3. Reference it in `verification-config.yaml`

### Exclude Files

Edit `config/exclusions.yaml`:

```yaml
exclude:
  files:
    - "**/generated/**"
    - "**/build/**"
  patterns:
    - "**/*Test.kt"  # Exclude all test files from certain checks
```

---

## 📖 Documentation

- [Configuration Reference](config/README.md)
- [Rule Schema](rules/README.md)
- [Template Guide](templates/README.md)
- [Custom Rules Guide](docs/CUSTOM_RULES.md)

---

**Note:** All reports are generated in the `reports/` directory and are automatically gitignored.
