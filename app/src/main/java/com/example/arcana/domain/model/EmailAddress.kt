package com.example.arcana.domain.model

import com.example.arcana.core.common.AppError

/**
 * Value object representing a validated email address
 */
@JvmInline
value class EmailAddress private constructor(val value: String) {

    companion object {
        private val EMAIL_REGEX = """^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}$""".toRegex()

        /**
         * Creates an EmailAddress from a string after validation
         *
         * @param email The email string to validate
         * @return Result containing EmailAddress or ValidationError
         */
        fun create(email: String): Result<EmailAddress> {
            val trimmed = email.trim()

            return when {
                trimmed.isEmpty() -> Result.failure(
                    Exception(AppError.validation("email", "Email address is required").message)
                )
                !trimmed.matches(EMAIL_REGEX) -> Result.failure(
                    Exception(AppError.validation("email", "Invalid email address format").message)
                )
                trimmed.length > 254 -> Result.failure(
                    Exception(AppError.validation("email", "Email address is too long").message)
                )
                else -> Result.success(EmailAddress(trimmed))
            }
        }

        /**
         * Creates an EmailAddress without validation (use only with trusted data)
         */
        fun createUnsafe(email: String): EmailAddress {
            return EmailAddress(email)
        }
    }

    override fun toString(): String = value
}
