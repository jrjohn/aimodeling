package com.example.aimodel.domain.usecase

import com.example.aimodel.core.common.AppError
import com.example.aimodel.core.common.RetryPolicy
import com.example.aimodel.data.model.User
import com.example.aimodel.domain.service.UserService
import com.example.aimodel.domain.validation.UserValidator
import timber.log.Timber
import javax.inject.Inject

/**
 * Use case for creating a new user with validation and business rules
 */
class CreateUserUseCase @Inject constructor(
    private val userService: UserService,
    private val validator: UserValidator,
    private val retryPolicy: RetryPolicy
) {
    /**
     * Creates a new user with validation and retry logic
     *
     * @param user The user to create
     * @return Result containing success or error
     */
    suspend operator fun invoke(user: User): Result<User> {
        Timber.d("CreateUserUseCase: Creating user ${user.email}")

        // 1. Validate user data
        validator.validateForCreation(user).onFailure { error ->
            Timber.w("CreateUserUseCase: Validation failed - ${error.message}")
            return Result.failure(error)
        }

        // 2. Apply business rules
        if (user.email.endsWith("@test.com") || user.email.endsWith("@temp.com")) {
            Timber.w("CreateUserUseCase: Temporary email not allowed")
            return Result.failure(
                Exception(AppError.validation("email", "Temporary email addresses are not allowed").message)
            )
        }

        // 3. Execute with retry logic for network operations
        return retryPolicy.executeWithRetry(
            shouldRetry = { exception -> RetryPolicy.isNetworkError(exception) }
        ) {
            val success = userService.createUser(user)
            if (success) {
                Timber.d("CreateUserUseCase: User created successfully")
                user
            } else {
                Timber.e("CreateUserUseCase: Failed to create user")
                throw Exception("Failed to create user")
            }
        }
    }
}
