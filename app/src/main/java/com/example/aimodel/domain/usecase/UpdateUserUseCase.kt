package com.example.aimodel.domain.usecase

import com.example.aimodel.core.common.RetryPolicy
import com.example.aimodel.data.model.User
import com.example.aimodel.domain.service.UserService
import com.example.aimodel.domain.validation.UserValidator
import timber.log.Timber
import javax.inject.Inject

/**
 * Use case for updating an existing user with validation and business rules
 */
class UpdateUserUseCase @Inject constructor(
    private val userService: UserService,
    private val validator: UserValidator,
    private val retryPolicy: RetryPolicy
) {
    /**
     * Updates an existing user with validation and retry logic
     *
     * @param user The user with updated data
     * @return Result containing success or error
     */
    suspend operator fun invoke(user: User): Result<User> {
        Timber.d("UpdateUserUseCase: Updating user ${user.id}")

        // 1. Validate user data
        validator.validateForUpdate(user).onFailure { error ->
            Timber.w("UpdateUserUseCase: Validation failed - ${error.message}")
            return Result.failure(error)
        }

        // 2. Update timestamp and version for optimistic locking
        val updatedUser = user.copy(
            updatedAt = System.currentTimeMillis(),
            version = user.version + 1
        )

        // 3. Execute with retry logic
        return retryPolicy.executeWithRetry(
            shouldRetry = { exception -> RetryPolicy.isNetworkError(exception) }
        ) {
            val success = userService.updateUser(updatedUser)
            if (success) {
                Timber.d("UpdateUserUseCase: User updated successfully")
                updatedUser
            } else {
                Timber.e("UpdateUserUseCase: Failed to update user")
                throw Exception("Failed to update user")
            }
        }
    }
}
