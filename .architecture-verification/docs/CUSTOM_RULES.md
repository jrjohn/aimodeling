# Custom Rules Guide

This guide explains how to create custom verification rules for your project.

---

## Rule File Structure

All rule files are YAML files located in `.architecture-verification/rules/`.

### Basic Rule Structure

```yaml
version: "1.0"
category: architecture|coding_style|annotations|testing|documentation|security
subcategory: your_subcategory

description: |
  Brief description of what these rules enforce

rules:
  - id: CATEGORY_###
    name: "Human-readable rule name"
    severity: CRITICAL|ERROR|WARNING|INFO
    enabled: true
    description: "Detailed description"
    check:
      # Check configuration
    message: "Error message shown when violated"
    suggestion: |
      How to fix the violation
    reference: "docs/REFERENCE.md#section"
    examples:
      good: ["Example 1", "Example 2"]
      bad: ["Bad example 1", "Bad example 2"]
```

---

## Rule Categories

### 1. Architecture Rules

**Location:** `.architecture-verification/rules/architecture/`

**Purpose:** Enforce architectural patterns and constraints

**Example:**
```yaml
- id: ARCH_001
  name: "No Android dependencies in domain layer"
  severity: ERROR
  check:
    file_pattern: "**/domain/**/*.kt"
    must_not_contain:
      - "import android."
      - "import androidx."
  message: "Domain layer must not have Android dependencies"
```

### 2. Coding Style Rules

**Location:** `.architecture-verification/rules/coding-style/`

**Purpose:** Enforce coding conventions and style

**Example:**
```yaml
- id: STYLE_001
  name: "Class names use PascalCase"
  severity: WARNING
  check:
    file_pattern: "**/*.kt"
    class_naming: "^[A-Z][a-zA-Z0-9]*$"
  message: "Class names should use PascalCase"
```

### 3. Annotation Rules

**Location:** `.architecture-verification/rules/annotations/`

**Purpose:** Verify correct annotation usage

**Example:**
```yaml
- id: ANNO_001
  name: "ViewModels must have @HiltViewModel"
  severity: ERROR
  check:
    file_pattern: "**/*ViewModel.kt"
    must_contain: "@HiltViewModel"
  message: "ViewModels must use @HiltViewModel annotation"
```

### 4. Testing Rules

**Location:** `.architecture-verification/rules/testing/`

**Purpose:** Define testing requirements

**Example:**
```yaml
- id: TEST_001
  name: "ViewModels must have tests"
  severity: WARNING
  check:
    source_pattern: "**/*ViewModel.kt"
    requires_test_file: true
  message: "Missing test file for ViewModel"
```

---

## Check Types

### Pattern Matching

```yaml
check:
  file_pattern: "**/*ViewModel.kt"
  must_contain: "sealed interface Input"
  must_not_contain: "var state"
  must_contain_pattern: "fun\\s+onEvent\\s*\\("
```

### Naming Conventions

```yaml
check:
  class_naming: "^[A-Z][a-zA-Z0-9]*$"
  function_naming: "^[a-z][a-zA-Z0-9]*$"
  property_naming: "^[a-z][a-zA-Z0-9]*$"
  constant_naming: "^[A-Z][A-Z0-9_]*$"
```

### Code Structure

```yaml
check:
  max_file_length: 500
  max_function_length: 50
  max_parameters: 5
  max_nesting_depth: 4
```

### Dependency Checks

```yaml
check:
  file_pattern: "**/domain/**/*.kt"
  forbidden_imports:
    - "android.*"
    - "androidx.*"
  required_imports:
    - "javax.inject.Inject"
```

### Within Scope

```yaml
check:
  must_contain: "data class State"
  within_interface: "Output"
  within_class: true
```

---

## Severity Levels

### CRITICAL
- **Use for:** Security issues, data loss risks
- **Behavior:** Fails build immediately
- **Example:** Hardcoded secrets, SQL injection

### ERROR
- **Use for:** Architecture violations
- **Behavior:** Fails build after collecting all errors
- **Example:** Missing required patterns, wrong dependencies

### WARNING
- **Use for:** Style violations, missing best practices
- **Behavior:** Shows warning, doesn't fail build
- **Example:** Missing tests, naming conventions

### INFO
- **Use for:** Suggestions and tips
- **Behavior:** Informational only
- **Example:** Code organization hints

---

## Creating a New Rule

### Step 1: Create Rule File

Create a new YAML file in the appropriate category directory:

```bash
touch .architecture-verification/rules/your-category/your-rule.yaml
```

### Step 2: Define Rule

```yaml
version: "1.0"
category: your_category
subcategory: your_subcategory

rules:
  - id: YOUR_001
    name: "Your rule name"
    severity: ERROR
    enabled: true
    description: "What this rule checks"
    check:
      file_pattern: "**/*.kt"
      must_contain: "your pattern"
    message: "Violation message"
    suggestion: |
      How to fix this
```

### Step 3: Enable in Config

Add to `.architecture-verification/config/verification-config.yaml`:

```yaml
checks:
  your_category:
    enabled: true
    checks:
      your_rule:
        enabled: true
        severity: ERROR
```

### Step 4: Test Your Rule

```bash
./gradlew verifyArchitecture
```

---

## Advanced Rule Features

### Conditional Checks

```yaml
check:
  file_pattern: "**/*ViewModel.kt"
  conditions:
    - if_contains: "sealed interface Input"
      then_must_contain: "fun onEvent(input: Input)"
```

### Multiple Patterns

```yaml
check:
  any_of:
    - must_contain: "StateFlow<State>"
    - must_contain: "Flow<State>"
  message: "State must be exposed as Flow"
```

### Exclusions

```yaml
check:
  file_pattern: "**/*.kt"
  must_contain: "import"
  exclude_patterns:
    - "**/generated/**"
    - "**/*Test.kt"
```

### Custom Validators

```yaml
check:
  custom_validator: "checkMvvmPattern"
  validator_params:
    require_all: true
    strict_mode: false
```

---

## Rule Templates

### Architecture Rule Template

```yaml
- id: ARCH_###
  name: "Descriptive rule name"
  severity: ERROR
  enabled: true
  description: "What architectural principle this enforces"
  check:
    file_pattern: "**/*.kt"
    # Your checks
  message: "Clear violation message"
  suggestion: |
    Step-by-step fix instructions
  reference: "docs/ARCHITECTURE.md#relevant-section"
  examples:
    correct: |
      // Correct implementation
    incorrect: |
      // Incorrect implementation
```

### Style Rule Template

```yaml
- id: STYLE_###
  name: "Style rule name"
  severity: WARNING
  enabled: true
  check:
    file_pattern: "**/*.kt"
    # Style checks
  message: "Style violation message"
  auto_fix: true  # If auto-fixable
```

### Test Rule Template

```yaml
- id: TEST_###
  name: "Test requirement"
  severity: WARNING
  enabled: true
  check:
    source_pattern: "**/*ViewModel.kt"
    requires_test_file: true
    test_file_pattern: "**/*ViewModelTest.kt"
  message: "Missing test coverage"
```

---

## Best Practices

### 1. Clear Messages
```yaml
# Good
message: "ViewModel must have 'sealed interface Input' to receive user events"

# Bad
message: "Missing Input"
```

### 2. Actionable Suggestions
```yaml
suggestion: |
  Add the following to your ViewModel:

  sealed interface Input {
      data object LoadData : Input
  }
```

### 3. Provide Examples
```yaml
examples:
  good:
    - "sealed interface Input { }"
    - "fun onEvent(input: Input) { }"
  bad:
    - "fun loadData() { }"  # Should use Input
```

### 4. Reference Documentation
```yaml
reference: "docs/VIEWMODEL_PATTERN.md#input-interface"
```

### 5. Use Appropriate Severity
- Don't make everything ERROR
- Use WARNING for style issues
- Use INFO for suggestions

---

## Testing Your Rules

### 1. Create Test File

Create a test Kotlin file that violates your rule:

```kotlin
// test-files/ViolatesRule.kt
class MyViewModel {
    // Missing Input interface
    // Missing Output interface
}
```

### 2. Run Verification

```bash
./gradlew verifyArchitecture
```

### 3. Verify Output

Check that your rule correctly identifies the violation.

### 4. Test Positive Case

Create a compliant file and verify it passes.

---

## Rule Maintenance

### Updating Rules

1. Edit the rule YAML file
2. Update version if needed
3. Test changes
4. Update documentation
5. Commit changes

### Deprecating Rules

```yaml
- id: OLD_001
  name: "Old rule"
  enabled: false
  deprecated: true
  deprecated_since: "2025-01-01"
  replacement: "NEW_001"
  message: "This rule is deprecated, use NEW_001 instead"
```

### Rule Versioning

```yaml
version: "1.1"  # Increment when rules change significantly
changes:
  - version: "1.1"
    date: "2025-01-15"
    changes:
      - "Added ARCH_005"
      - "Deprecated ARCH_001"
```

---

## Troubleshooting

### Rule Not Running

1. Check if enabled in `verification-config.yaml`
2. Check file pattern matches your files
3. Check severity level configuration
4. Run with `--debug` flag

### False Positives

1. Add exclusions in rule or `exclusions.yaml`
2. Refine pattern matching
3. Add conditional checks

### Performance Issues

1. Use more specific file patterns
2. Enable parallel checks
3. Limit check scope
4. Cache results

---

## Examples

See the following example rule files:

- `rules/architecture/mvvm-pattern.yaml` - MVVM pattern rules
- `rules/coding-style/kotlin-conventions.yaml` - Kotlin style rules
- `rules/annotations/required-annotations.yaml` - Annotation rules
- `rules/testing/test-coverage.yaml` - Testing rules

---

## Resources

- [Verification Configuration Reference](../config/README.md)
- [Architecture Documentation](../../docs/ARCHITECTURE.md)
- [ViewModel Pattern Guide](../../docs/VIEWMODEL_PATTERN.md)

---

**Last Updated:** 2025-11-14
