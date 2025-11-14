package com.example.arcana.core.common

/**
 * Error and Warning codes for the application
 *
 * Error codes start with 'E' followed by 4 digits
 * Warning codes start with 'W' followed by 4 digits
 *
 * Code ranges:
 * - E1000-E1999: Network errors
 * - E2000-E2999: Validation errors
 * - E3000-E3999: Server errors
 * - E4000-E4999: Authentication/Authorization errors
 * - E5000-E5999: Data/Conflict errors
 * - E6000-E6999: Database errors
 * - E9000-E9999: Unknown/System errors
 *
 * - W1000-W1999: Network warnings
 * - W2000-W2999: Validation warnings
 * - W3000-W3999: Data warnings
 */
sealed class ErrorCode(
    val code: String,
    val description: String,
    val category: String
) {
    // ========== ERROR CODES ==========

    // Network Errors (E1000-E1999)
    object E1000_NO_CONNECTION : ErrorCode(
        "E1000",
        "No internet connection available",
        "Network"
    )

    object E1001_CONNECTION_TIMEOUT : ErrorCode(
        "E1001",
        "Connection attempt timed out",
        "Network"
    )

    object E1002_UNKNOWN_HOST : ErrorCode(
        "E1002",
        "Unable to resolve host address",
        "Network"
    )

    object E1003_NETWORK_IO : ErrorCode(
        "E1003",
        "Network I/O error occurred",
        "Network"
    )

    object E1004_SSL_ERROR : ErrorCode(
        "E1004",
        "SSL/TLS connection error",
        "Network"
    )

    // Validation Errors (E2000-E2999)
    object E2000_VALIDATION_FAILED : ErrorCode(
        "E2000",
        "Input validation failed",
        "Validation"
    )

    object E2001_INVALID_EMAIL : ErrorCode(
        "E2001",
        "Invalid email address format",
        "Validation"
    )

    object E2002_INVALID_NAME : ErrorCode(
        "E2002",
        "Invalid name format or length",
        "Validation"
    )

    object E2003_REQUIRED_FIELD : ErrorCode(
        "E2003",
        "Required field is missing",
        "Validation"
    )

    object E2004_FIELD_TOO_LONG : ErrorCode(
        "E2004",
        "Field value exceeds maximum length",
        "Validation"
    )

    object E2005_FIELD_TOO_SHORT : ErrorCode(
        "E2005",
        "Field value below minimum length",
        "Validation"
    )

    // Server Errors (E3000-E3999)
    object E3000_SERVER_ERROR : ErrorCode(
        "E3000",
        "Internal server error",
        "Server"
    )

    object E3001_BAD_REQUEST : ErrorCode(
        "E3001",
        "Bad request - invalid parameters",
        "Server"
    )

    object E3002_NOT_FOUND : ErrorCode(
        "E3002",
        "Resource not found",
        "Server"
    )

    object E3003_SERVICE_UNAVAILABLE : ErrorCode(
        "E3003",
        "Service temporarily unavailable",
        "Server"
    )

    object E3004_RATE_LIMITED : ErrorCode(
        "E3004",
        "Rate limit exceeded",
        "Server"
    )

    // Authentication/Authorization Errors (E4000-E4999)
    object E4000_AUTH_REQUIRED : ErrorCode(
        "E4000",
        "Authentication required",
        "Authentication"
    )

    object E4001_UNAUTHORIZED : ErrorCode(
        "E4001",
        "Invalid credentials",
        "Authentication"
    )

    object E4002_FORBIDDEN : ErrorCode(
        "E4002",
        "Access forbidden - insufficient permissions",
        "Authentication"
    )

    object E4003_SESSION_EXPIRED : ErrorCode(
        "E4003",
        "Session has expired",
        "Authentication"
    )

    object E4004_TOKEN_INVALID : ErrorCode(
        "E4004",
        "Invalid or malformed token",
        "Authentication"
    )

    // Data/Conflict Errors (E5000-E5999)
    object E5000_DATA_CONFLICT : ErrorCode(
        "E5000",
        "Data conflict detected",
        "Data"
    )

    object E5001_STALE_DATA : ErrorCode(
        "E5001",
        "Data has been modified by another user",
        "Data"
    )

    object E5002_DUPLICATE_ENTRY : ErrorCode(
        "E5002",
        "Duplicate entry detected",
        "Data"
    )

    object E5003_CONSTRAINT_VIOLATION : ErrorCode(
        "E5003",
        "Database constraint violation",
        "Data"
    )

    // Database Errors (E6000-E6999)
    object E6000_DATABASE_ERROR : ErrorCode(
        "E6000",
        "Database operation failed",
        "Database"
    )

    object E6001_QUERY_FAILED : ErrorCode(
        "E6001",
        "Database query execution failed",
        "Database"
    )

    object E6002_TRANSACTION_FAILED : ErrorCode(
        "E6002",
        "Database transaction failed",
        "Database"
    )

    object E6003_MIGRATION_FAILED : ErrorCode(
        "E6003",
        "Database migration failed",
        "Database"
    )

    // Unknown/System Errors (E9000-E9999)
    object E9000_UNKNOWN : ErrorCode(
        "E9000",
        "Unknown error occurred",
        "System"
    )

    object E9001_UNEXPECTED_STATE : ErrorCode(
        "E9001",
        "Unexpected application state",
        "System"
    )

    object E9002_NULL_POINTER : ErrorCode(
        "E9002",
        "Null pointer exception",
        "System"
    )

    object E9003_SERIALIZATION_ERROR : ErrorCode(
        "E9003",
        "Data serialization/deserialization failed",
        "System"
    )

    // ========== WARNING CODES ==========

    // Network Warnings (W1000-W1999)
    object W1000_SLOW_CONNECTION : ErrorCode(
        "W1000",
        "Network connection is slow",
        "Network"
    )

    object W1001_OFFLINE_MODE : ErrorCode(
        "W1001",
        "Operating in offline mode",
        "Network"
    )

    object W1002_SYNC_PENDING : ErrorCode(
        "W1002",
        "Data synchronization pending",
        "Network"
    )

    // Validation Warnings (W2000-W2999)
    object W2000_INCOMPLETE_DATA : ErrorCode(
        "W2000",
        "Data is incomplete but acceptable",
        "Validation"
    )

    object W2001_DEPRECATED_FORMAT : ErrorCode(
        "W2001",
        "Using deprecated data format",
        "Validation"
    )

    // Data Warnings (W3000-W3999)
    object W3000_STALE_CACHE : ErrorCode(
        "W3000",
        "Cache data may be stale",
        "Data"
    )

    object W3001_PARTIAL_SYNC : ErrorCode(
        "W3001",
        "Partial data synchronization completed",
        "Data"
    )

    object W3002_DATA_TRUNCATED : ErrorCode(
        "W3002",
        "Data was truncated to fit limits",
        "Data"
    )

    companion object {
        /**
         * Get all error codes
         */
        fun getAllErrorCodes(): List<ErrorCode> {
            return listOf(
                // Network Errors
                E1000_NO_CONNECTION, E1001_CONNECTION_TIMEOUT, E1002_UNKNOWN_HOST,
                E1003_NETWORK_IO, E1004_SSL_ERROR,

                // Validation Errors
                E2000_VALIDATION_FAILED, E2001_INVALID_EMAIL, E2002_INVALID_NAME,
                E2003_REQUIRED_FIELD, E2004_FIELD_TOO_LONG, E2005_FIELD_TOO_SHORT,

                // Server Errors
                E3000_SERVER_ERROR, E3001_BAD_REQUEST, E3002_NOT_FOUND,
                E3003_SERVICE_UNAVAILABLE, E3004_RATE_LIMITED,

                // Auth Errors
                E4000_AUTH_REQUIRED, E4001_UNAUTHORIZED, E4002_FORBIDDEN,
                E4003_SESSION_EXPIRED, E4004_TOKEN_INVALID,

                // Data Errors
                E5000_DATA_CONFLICT, E5001_STALE_DATA, E5002_DUPLICATE_ENTRY,
                E5003_CONSTRAINT_VIOLATION,

                // Database Errors
                E6000_DATABASE_ERROR, E6001_QUERY_FAILED, E6002_TRANSACTION_FAILED,
                E6003_MIGRATION_FAILED,

                // System Errors
                E9000_UNKNOWN, E9001_UNEXPECTED_STATE, E9002_NULL_POINTER,
                E9003_SERIALIZATION_ERROR
            )
        }

        /**
         * Get all warning codes
         */
        fun getAllWarningCodes(): List<ErrorCode> {
            return listOf(
                // Network Warnings
                W1000_SLOW_CONNECTION, W1001_OFFLINE_MODE, W1002_SYNC_PENDING,

                // Validation Warnings
                W2000_INCOMPLETE_DATA, W2001_DEPRECATED_FORMAT,

                // Data Warnings
                W3000_STALE_CACHE, W3001_PARTIAL_SYNC, W3002_DATA_TRUNCATED
            )
        }

        /**
         * Get all codes (errors + warnings)
         */
        fun getAllCodes(): List<ErrorCode> {
            return getAllErrorCodes() + getAllWarningCodes()
        }

        /**
         * Get codes by category
         */
        fun getCodesByCategory(category: String): List<ErrorCode> {
            return getAllCodes().filter { it.category == category }
        }
    }
}
