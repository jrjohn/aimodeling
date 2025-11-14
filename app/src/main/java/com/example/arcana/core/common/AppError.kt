package com.example.arcana.core.common

import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Sealed hierarchy for application errors with user-friendly messages
 */
sealed class AppError {
    abstract val message: String
    abstract val throwable: Throwable?

    /**
     * Network-related errors
     */
    data class NetworkError(
        override val message: String,
        val isRetryable: Boolean = true,
        override val throwable: Throwable? = null
    ) : AppError()

    /**
     * Validation errors for user input
     */
    data class ValidationError(
        val field: String,
        override val message: String,
        override val throwable: Throwable? = null
    ) : AppError()

    /**
     * Server-side errors (4xx, 5xx)
     */
    data class ServerError(
        val code: Int,
        override val message: String,
        override val throwable: Throwable? = null
    ) : AppError()

    /**
     * Data conflict errors (optimistic locking failures)
     */
    data class ConflictError(
        override val message: String,
        override val throwable: Throwable? = null
    ) : AppError()

    /**
     * Authentication/Authorization errors
     */
    data class AuthError(
        override val message: String,
        override val throwable: Throwable? = null
    ) : AppError()

    /**
     * Unknown/unexpected errors
     */
    data class UnknownError(
        override val message: String = "An unexpected error occurred",
        override val throwable: Throwable
    ) : AppError()

    companion object {
        /**
         * Converts a generic exception into a specific AppError type
         */
        fun fromException(exception: Throwable): AppError {
            return when (exception) {
                is IOException, is SocketTimeoutException, is UnknownHostException -> {
                    NetworkError(
                        message = when (exception) {
                            is UnknownHostException -> "No internet connection"
                            is SocketTimeoutException -> "Connection timed out"
                            else -> "Network error: ${exception.message}"
                        },
                        isRetryable = true,
                        throwable = exception
                    )
                }
                else -> UnknownError(
                    message = exception.message ?: "An unexpected error occurred",
                    throwable = exception
                )
            }
        }

        /**
         * Creates a network error for offline scenarios
         */
        fun noConnection(): NetworkError {
            return NetworkError(
                message = "No internet connection. Changes will be synced when online.",
                isRetryable = true
            )
        }

        /**
         * Creates a validation error
         */
        fun validation(field: String, message: String): ValidationError {
            return ValidationError(field = field, message = message)
        }

        /**
         * Creates a server error from HTTP response code
         */
        fun fromHttpCode(code: Int, message: String? = null): AppError {
            return when (code) {
                in 400..499 -> {
                    when (code) {
                        401, 403 -> AuthError(message ?: "Authentication required")
                        409 -> ConflictError(message ?: "Data conflict detected")
                        else -> ServerError(code, message ?: "Client error: $code")
                    }
                }
                in 500..599 -> ServerError(code, message ?: "Server error: $code")
                else -> UnknownError(message ?: "Unexpected HTTP code: $code", Exception(message))
            }
        }
    }
}

/**
 * Extension to get user-friendly error message
 */
fun AppError.getUserMessage(): String {
    return when (this) {
        is AppError.NetworkError -> {
            if (isRetryable) "$message Please try again." else message
        }
        is AppError.ValidationError -> message
        is AppError.ServerError -> {
            when (code) {
                in 500..599 -> "Server is experiencing issues. Please try again later."
                else -> message
            }
        }
        is AppError.ConflictError -> "$message Please refresh and try again."
        is AppError.AuthError -> "$message Please sign in again."
        is AppError.UnknownError -> "Something went wrong. Please try again."
    }
}

/**
 * Extension to check if error is retryable
 */
fun AppError.isRetryable(): Boolean {
    return when (this) {
        is AppError.NetworkError -> isRetryable
        is AppError.ServerError -> code in 500..599
        is AppError.ConflictError -> true
        else -> false
    }
}
