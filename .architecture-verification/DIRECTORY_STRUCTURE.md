# Architecture Verification Directory Structure

## 📁 Complete Directory Layout

```
.architecture-verification/
│
├── README.md                           # Main documentation
├── DIRECTORY_STRUCTURE.md              # This file
│
├── config/                             # Configuration files
│   ├── verification-config.yaml        # Main verification settings
│   ├── severity-levels.yaml            # Severity level definitions
│   └── exclusions.yaml                 # File/pattern exclusions
│
├── rules/                              # Verification rules (YAML)
│   ├── architecture/                   # Architecture-specific rules
│   │   ├── clean-architecture.yaml     # Clean Architecture rules
│   │   ├── mvvm-pattern.yaml           # MVVM pattern compliance ✅
│   │   ├── dependency-rules.yaml       # Dependency direction rules
│   │   └── offline-first.yaml          # Offline-first pattern rules
│   │
│   ├── coding-style/                   # Code style rules
│   │   ├── kotlin-conventions.yaml     # Kotlin conventions ✅
│   │   ├── naming-conventions.yaml     # Naming patterns
│   │   ├── code-organization.yaml      # File/class organization
│   │   └── formatting.yaml             # Code formatting rules
│   │
│   ├── annotations/                    # Annotation usage rules
│   │   ├── required-annotations.yaml   # Required annotations
│   │   ├── hilt-annotations.yaml       # Hilt DI annotations
│   │   └── custom-annotations.yaml     # Custom annotation patterns
│   │
│   ├── testing/                        # Testing rules
│   │   ├── test-coverage.yaml          # Coverage requirements
│   │   ├── test-naming.yaml            # Test naming conventions
│   │   └── test-structure.yaml         # Test organization
│   │
│   ├── documentation/                  # Documentation rules
│   │   ├── kdoc-requirements.yaml      # KDoc comment rules
│   │   └── architecture-docs.yaml      # Architecture doc checks
│   │
│   └── security/                       # Security rules
│       ├── secrets-detection.yaml      # Hardcoded secrets
│       ├── input-validation.yaml       # User input validation
│       └── security-best-practices.yaml # Security patterns
│
├── templates/                          # Code templates
│   ├── ViewModel.kt.template           # ViewModel template ✅
│   ├── Repository.kt.template          # Repository template
│   ├── Service.kt.template             # Service template
│   ├── Screen.kt.template              # Composable screen template
│   └── Test.kt.template                # Unit test template
│
├── docs/                               # Additional documentation
│   ├── CUSTOM_RULES.md                 # How to create rules ✅
│   ├── CONFIGURATION_GUIDE.md          # Configuration reference
│   ├── RULE_SCHEMA.md                  # Rule file schema
│   └── EXAMPLES.md                     # Rule examples
│
└── reports/                            # Generated reports (gitignored)
    ├── .gitignore
    └── (generated .md, .html, .json reports)
```

---

## 📄 File Descriptions

### Configuration Files (`config/`)

#### `verification-config.yaml` ✅
**Purpose:** Main configuration file controlling which checks are enabled

**Contents:**
- Global settings (fail on error, parallel checks, etc.)
- Feature flags (experimental checks, auto-fix)
- Check category enablement
- Individual rule configuration
- File patterns and scan directories
- Reporting settings

**Example:**
```yaml
checks:
  architecture:
    enabled: true
    checks:
      mvvm_pattern:
        enabled: true
        severity: ERROR
```

#### `severity-levels.yaml` ✅
**Purpose:** Defines severity levels and their behavior

**Contents:**
- Severity level definitions (CRITICAL, ERROR, WARNING, INFO)
- Rule-specific severity overrides
- Violation thresholds
- Progressive severity rules

**Example:**
```yaml
levels:
  ERROR:
    fail_build: true
    color: red
    icon: "❌"
```

#### `exclusions.yaml` ✅
**Purpose:** Files and patterns to exclude from verification

**Contents:**
- Global exclusions (build/, generated/)
- Check-specific exclusions
- Legacy code exclusions
- Third-party code exclusions

**Example:**
```yaml
global:
  directories:
    - "**/build/**"
    - "**/generated/**"
```

---

### Rule Files (`rules/`)

#### Architecture Rules (`rules/architecture/`)

##### `mvvm-pattern.yaml` ✅
**12 Rules** enforcing MVVM Input/Output pattern:
- MVVM_001: Input sealed interface required
- MVVM_002: Output sealed interface required
- MVVM_003: State data class required
- MVVM_004: Effect sealed interface required
- MVVM_005: onEvent method required
- MVVM_006: StateFlow for State
- MVVM_007: Channel for Effects
- MVVM_008: @HiltViewModel annotation
- MVVM_009: Constructor injection
- MVVM_010: Extend ViewModel
- MVVM_011: Immutable State
- MVVM_012: Private event handlers

**Status:** ✅ Implemented

##### `clean-architecture.yaml` (To be created)
Rules for Clean Architecture:
- Layer separation
- Dependency direction
- No Android in domain
- Module boundaries

##### `dependency-rules.yaml` (To be created)
Dependency injection rules:
- Constructor injection required
- No service locators
- Hilt module configuration

##### `offline-first.yaml` (To be created)
Offline-first pattern rules:
- Repository pattern compliance
- Flow usage for reactive data
- Error handling patterns

---

#### Coding Style Rules (`rules/coding-style/`)

##### `kotlin-conventions.yaml` ✅
**30+ Rules** for Kotlin conventions:
- Naming conventions (PascalCase, camelCase, etc.)
- Code organization
- Function design
- Null safety
- Lambda usage
- Collections
- Documentation
- Formatting

**Status:** ✅ Implemented

##### Other style files (To be created)
- `naming-conventions.yaml`: Detailed naming rules
- `code-organization.yaml`: File structure rules
- `formatting.yaml`: Code formatting rules

---

#### Annotation Rules (`rules/annotations/`)

**To be created:**
- `required-annotations.yaml`: Required annotations per file type
- `hilt-annotations.yaml`: Hilt DI annotation rules
- `custom-annotations.yaml`: Custom annotation patterns

---

#### Testing Rules (`rules/testing/`)

**To be created:**
- `test-coverage.yaml`: Coverage requirements
- `test-naming.yaml`: Test naming conventions
- `test-structure.yaml`: AAA pattern, setup/teardown

---

#### Documentation Rules (`rules/documentation/`)

**To be created:**
- `kdoc-requirements.yaml`: KDoc comment requirements
- `architecture-docs.yaml`: Architecture doc checks

---

#### Security Rules (`rules/security/`)

**To be created:**
- `secrets-detection.yaml`: Hardcoded secrets detection
- `input-validation.yaml`: User input validation
- `security-best-practices.yaml`: Security patterns

---

### Templates (`templates/`)

#### `ViewModel.kt.template` ✅
Complete ViewModel template with:
- Proper Input/Output pattern
- Hilt annotations
- Analytics integration
- State and Effect handling
- Event handling
- Documentation

**Variables to replace:**
- `{{package_name}}`
- `{{ClassName}}`
- `{{dependency_name}}`
- `{{DependencyType}}`
- `{{ItemType}}`
- etc.

**Status:** ✅ Implemented

#### Other templates (To be created)
- `Repository.kt.template`: Repository pattern template
- `Service.kt.template`: Domain service template
- `Screen.kt.template`: Compose screen template
- `Test.kt.template`: Unit test template

---

### Documentation (`docs/`)

#### `CUSTOM_RULES.md` ✅
Comprehensive guide for creating custom rules:
- Rule file structure
- Rule categories
- Check types
- Severity levels
- Creating new rules
- Advanced features
- Best practices
- Examples

**Status:** ✅ Implemented

#### Other docs (To be created)
- `CONFIGURATION_GUIDE.md`: Detailed config reference
- `RULE_SCHEMA.md`: YAML schema specification
- `EXAMPLES.md`: Complete rule examples

---

### Reports (`reports/`)

**Purpose:** Generated verification reports (gitignored)

**Generated files:**
- `verification-report-{timestamp}.md`: Detailed report
- `verification-summary-{timestamp}.json`: Machine-readable summary
- `violations-{timestamp}.csv`: Violations list

**Cleanup:** Old reports auto-deleted after 30 days

---

## 🎯 Implementation Status

### ✅ Completed

1. **Directory Structure** - Created
2. **Configuration Files** - All 3 files created
3. **MVVM Pattern Rules** - Complete with 12 rules
4. **Kotlin Conventions Rules** - Complete with 30+ rules
5. **ViewModel Template** - Complete template
6. **Custom Rules Guide** - Comprehensive documentation
7. **Main README** - Complete overview

### 🔄 To Be Created

1. **Additional Rule Files:**
   - `clean-architecture.yaml`
   - `dependency-rules.yaml`
   - `offline-first.yaml`
   - `naming-conventions.yaml`
   - `code-organization.yaml`
   - `formatting.yaml`
   - `required-annotations.yaml`
   - `hilt-annotations.yaml`
   - `custom-annotations.yaml`
   - All testing rules
   - All documentation rules
   - All security rules

2. **Additional Templates:**
   - `Repository.kt.template`
   - `Service.kt.template`
   - `Screen.kt.template`
   - `Test.kt.template`

3. **Additional Documentation:**
   - `CONFIGURATION_GUIDE.md`
   - `RULE_SCHEMA.md`
   - `EXAMPLES.md`

---

## 🚀 Usage

### View Configuration

```bash
# View main config
cat .architecture-verification/config/verification-config.yaml

# View severity levels
cat .architecture-verification/config/severity-levels.yaml

# View exclusions
cat .architecture-verification/config/exclusions.yaml
```

### View Rules

```bash
# View MVVM rules
cat .architecture-verification/rules/architecture/mvvm-pattern.yaml

# View Kotlin convention rules
cat .architecture-verification/rules/coding-style/kotlin-conventions.yaml
```

### Use Templates

```bash
# Copy ViewModel template
cp .architecture-verification/templates/ViewModel.kt.template \
   app/src/main/java/com/example/arcana/ui/MyViewModel.kt

# Replace placeholders
sed -i '' 's/{{ClassName}}/My/g' app/src/main/java/com/example/arcana/ui/MyViewModel.kt
```

### Read Documentation

```bash
# Custom rules guide
cat .architecture-verification/docs/CUSTOM_RULES.md

# This file
cat .architecture-verification/DIRECTORY_STRUCTURE.md
```

---

## 📋 Next Steps

1. **Create Additional Rules:** Fill in the remaining rule files
2. **Create Templates:** Add more code templates
3. **Integrate with Plugin:** Update Gradle plugin to read YAML configs
4. **Add Auto-fix:** Implement auto-fix capabilities
5. **Create Web UI:** Build web dashboard for reports

---

## 🔗 Related Documentation

- [Main README](.architecture-verification/README.md)
- [Custom Rules Guide](docs/CUSTOM_RULES.md)
- [Architecture Documentation](../../docs/ARCHITECTURE.md)
- [ViewModel Pattern](../../docs/VIEWMODEL_PATTERN.md)
- [Architecture Verification](../../docs/ARCHITECTURE_VERIFICATION.md)

---

**Last Updated:** 2025-11-14
**Version:** 1.0
**Status:** 🟢 Foundation Complete
