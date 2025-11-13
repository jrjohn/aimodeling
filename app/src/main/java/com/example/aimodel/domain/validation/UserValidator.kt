package com.example.aimodel.domain.validation

import com.example.aimodel.R
import com.example.aimodel.core.common.AppError
import com.example.aimodel.core.common.StringProvider
import com.example.aimodel.data.model.User
import com.example.aimodel.domain.model.EmailAddress
import javax.inject.Inject

/**
 * Validator for User entities
 */
class UserValidator @Inject constructor(
    private val stringProvider: StringProvider
) {

    /**
     * Validates a User object
     *
     * @param user The user to validate
     * @return Result indicating success or validation error
     */
    fun validate(user: User): Result<Unit> {
        val errors = mutableListOf<AppError.ValidationError>()

        // Validate email
        EmailAddress.create(user.email).onFailure {
            errors.add(AppError.validation("email", it.message ?: stringProvider.getString(R.string.error_email_invalid)))
        }

        // Validate first name
        if (user.firstName.isBlank() && user.lastName.isBlank()) {
            errors.add(AppError.validation("name", stringProvider.getString(R.string.error_name_required)))
        }

        // Validate name length
        if (user.firstName.length > 100) {
            errors.add(AppError.validation("firstName", stringProvider.getString(R.string.error_first_name_too_long)))
        }

        if (user.lastName.length > 100) {
            errors.add(AppError.validation("lastName", stringProvider.getString(R.string.error_last_name_too_long)))
        }

        // Validate avatar URL format if provided
        if (user.avatar.isNotEmpty() && !isValidUrl(user.avatar)) {
            errors.add(AppError.validation("avatar", stringProvider.getString(R.string.error_avatar_invalid)))
        }

        return if (errors.isEmpty()) {
            Result.success(Unit)
        } else {
            Result.failure(Exception(errors.first().message))
        }
    }

    /**
     * Validates user data for creation (stricter rules)
     */
    fun validateForCreation(user: User): Result<Unit> {
        val errors = mutableListOf<AppError.ValidationError>()

        // Email is required for creation
        if (user.email.isEmpty()) {
            errors.add(AppError.validation("email", stringProvider.getString(R.string.error_email_required)))
        } else {
            EmailAddress.create(user.email).onFailure {
                errors.add(AppError.validation("email", it.message ?: stringProvider.getString(R.string.error_email_invalid)))
            }
        }

        // At least one name field is required
        if (user.firstName.isBlank() && user.lastName.isBlank()) {
            errors.add(AppError.validation("name", stringProvider.getString(R.string.error_name_required_create)))
        }

        return if (errors.isEmpty()) {
            validate(user)
        } else {
            Result.failure(Exception(errors.first().message))
        }
    }

    /**
     * Validates user data for update (more lenient)
     */
    fun validateForUpdate(user: User): Result<Unit> {
        // For updates, we just check basic constraints
        return validate(user)
    }

    private fun isValidUrl(url: String): Boolean {
        return try {
            url.startsWith("http://") || url.startsWith("https://")
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        /**
         * Quick validation for email only
         */
        fun validateEmail(email: String): Result<String> {
            return EmailAddress.create(email).map { it.value }
        }

        /**
         * Checks if a name is valid (not empty, reasonable length)
         */
        fun isValidName(name: String): Boolean {
            return name.isNotBlank() && name.length <= 100
        }
    }
}
