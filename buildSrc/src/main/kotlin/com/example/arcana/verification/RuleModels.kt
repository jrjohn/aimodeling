package com.example.arcana.verification

/**
 * Data models for YAML rule definitions
 */

data class RuleFile(
    val version: String,
    val category: String,
    val subcategory: String? = null,
    val description: String? = null,
    val rules: List<Rule> = emptyList(),
    val metadata: Metadata? = null
)

data class Rule(
    val id: String,
    val name: String,
    val severity: String,
    val enabled: Boolean = true,
    val description: String? = null,
    val check: Check,
    val message: String,
    val suggestion: String? = null,
    val reference: String? = null,
    val examples: Examples? = null
)

data class Check(
    val filePattern: String? = null,
    val mustContain: Any? = null,  // Can be String or List<String>
    val mustNotContain: Any? = null,  // Can be String or List<String>
    val mustContainPattern: String? = null,
    val withinInterface: String? = null,
    val withinClass: Boolean? = null,
    val mustBeInDirectory: String? = null,
    val excludePatterns: List<String>? = null,
    val classNaming: String? = null,
    val functionNaming: String? = null,
    val requiresTestFile: Boolean? = null,
    val testFilePattern: String? = null,
    val testFileLocation: String? = null,
    val customValidator: String? = null,

    // Additional check properties from various rule types
    val maxFileLength: Int? = null,
    val maxFunctionLength: Int? = null,
    val maxParameters: Int? = null,
    val forbiddenImports: List<String>? = null,
    val requiredImports: List<String>? = null
) {
    fun getMustContainList(): List<String> {
        return when (mustContain) {
            is String -> listOf(mustContain)
            is List<*> -> mustContain.filterIsInstance<String>()
            else -> emptyList()
        }
    }

    fun getMustNotContainList(): List<String> {
        return when (mustNotContain) {
            is String -> listOf(mustNotContain)
            is List<*> -> mustNotContain.filterIsInstance<String>()
            else -> emptyList()
        }
    }
}

data class Examples(
    val good: List<String>? = null,
    val bad: List<String>? = null,
    val correct: String? = null,
    val incorrect: String? = null
)

data class Metadata(
    val totalRules: Int? = null,
    val lastUpdated: String? = null,
    val author: String? = null,
    val version: String? = null
)

data class VerificationConfig(
    val global: GlobalSettings? = null,
    val checks: Map<String, CategoryConfig>? = null
)

data class GlobalSettings(
    val failOnError: Boolean = true,
    val parallelChecks: Boolean = true,
    val detailedReports: Boolean = true
)

data class CategoryConfig(
    val enabled: Boolean = true,
    val checks: Map<String, CheckConfig>? = null
)

data class CheckConfig(
    val enabled: Boolean = true,
    val severity: String? = null
)
