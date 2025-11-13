package com.example.aimodel.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aimodel.data.model.User
import com.example.aimodel.domain.service.UserService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

// Input Events (from UI to ViewModel)
sealed interface UserEvent {
    data object LoadInitial : UserEvent
    data object LoadNextPage : UserEvent
    data object Refresh : UserEvent
    data class CreateUser(val user: User) : UserEvent
    data class UpdateUser(val user: User) : UserEvent
    data class DeleteUser(val user: User) : UserEvent
    data class GoToPage(val page: Int) : UserEvent
    data object GoToNextPage : UserEvent
    data object GoToPreviousPage : UserEvent
}

// Output Events (from ViewModel to UI)
sealed interface UserEffect {
    data class ShowError(val message: String) : UserEffect
    data class ShowSuccess(val message: String) : UserEffect
}

data class UserUiState(
    val users: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val currentPage: Int = 1,
    val totalPages: Int = 1
)

@HiltViewModel
class UserViewModel @Inject constructor(
    private val userService: UserService
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserUiState())
    val uiState: StateFlow<UserUiState> = _uiState.asStateFlow()

    private val _effect = Channel<UserEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        onEvent(UserEvent.LoadInitial)
    }

    fun onEvent(event: UserEvent) {
        when (event) {
            is UserEvent.LoadInitial -> loadUsers()
            is UserEvent.LoadNextPage -> loadNextPage()
            is UserEvent.Refresh -> refresh()
            is UserEvent.CreateUser -> createUser(event.user)
            is UserEvent.UpdateUser -> updateUser(event.user)
            is UserEvent.DeleteUser -> deleteUser(event.user)
            is UserEvent.GoToPage -> goToPage(event.page)
            is UserEvent.GoToNextPage -> goToNextPage()
            is UserEvent.GoToPreviousPage -> goToPreviousPage()
        }
    }

    private fun loadUsers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            userService.getUsersPage(1).fold(
                onSuccess = { (users, totalPages) ->
                    _uiState.update {
                        it.copy(
                            users = users,
                            currentPage = 1,
                            totalPages = totalPages,
                            isLoading = false
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isLoading = false) }
                    _effect.send(UserEffect.ShowError(error.message ?: "Failed to load users"))
                }
            )
        }
    }

    private fun loadNextPage() {
        val currentState = _uiState.value
        Timber.d("loadNextPage called - currentPage: ${currentState.currentPage}, totalPages: ${currentState.totalPages}, isLoadingMore: ${currentState.isLoadingMore}")

        if (currentState.isLoadingMore || currentState.currentPage >= currentState.totalPages) {
            Timber.d("loadNextPage skipped - already loading or no more pages")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            val nextPage = currentState.currentPage + 1
            Timber.d("Loading page $nextPage")

            userService.getUsersPage(nextPage).fold(
                onSuccess = { (newUsers, totalPages) ->
                    Timber.d("Successfully loaded ${newUsers.size} users from page $nextPage")
                    _uiState.update {
                        it.copy(
                            users = it.users + newUsers,
                            currentPage = nextPage,
                            totalPages = totalPages,
                            isLoadingMore = false
                        )
                    }
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to load page $nextPage")
                    _uiState.update { it.copy(isLoadingMore = false) }
                    _effect.send(UserEffect.ShowError(error.message ?: "Failed to load more users"))
                }
            )
        }
    }

    private fun createUser(user: User) {
        viewModelScope.launch {
            Timber.d("Creating user: name=${user.name}, email=${user.email}, avatar=${user.avatar}")
            val success = userService.createUser(user)
            if (success) {
                _effect.send(UserEffect.ShowSuccess("User created successfully"))
                loadUsers() // Refresh the list
            } else {
                _effect.send(UserEffect.ShowError("Failed to create user"))
            }
        }
    }

    private fun updateUser(user: User) {
        viewModelScope.launch {
            Timber.d("Updating user ${user.id}: name=${user.name}, email=${user.email}, avatar=${user.avatar}")
            val success = userService.updateUser(user)
            if (success) {
                _effect.send(UserEffect.ShowSuccess("User updated successfully"))
                loadUsers() // Refresh the list
            } else {
                _effect.send(UserEffect.ShowError("Failed to update user"))
            }
        }
    }

    private fun deleteUser(user: User) {
        viewModelScope.launch {
            Timber.d("Deleting user ${user.id}: ${user.name}")
            val success = userService.deleteUser(user.id)
            if (success) {
                // Remove from local state immediately for better UX
                _uiState.update {
                    it.copy(users = it.users.filter { u -> u.id != user.id })
                }
                _effect.send(UserEffect.ShowSuccess("User deleted successfully"))
                Timber.d("User ${user.id} deleted successfully")
            } else {
                _effect.send(UserEffect.ShowError("Failed to delete user"))
            }
        }
    }

    private fun refresh() {
        loadUsers()
    }

    private fun goToNextPage() {
        val currentState = _uiState.value
        if (currentState.currentPage < currentState.totalPages && !currentState.isLoading) {
            loadSpecificPage(currentState.currentPage + 1)
        }
    }

    private fun goToPreviousPage() {
        val currentState = _uiState.value
        if (currentState.currentPage > 1 && !currentState.isLoading) {
            loadSpecificPage(currentState.currentPage - 1)
        }
    }

    private fun goToPage(page: Int) {
        val currentState = _uiState.value
        if (page in 1..currentState.totalPages && !currentState.isLoading) {
            loadSpecificPage(page)
        }
    }

    private fun loadSpecificPage(page: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            Timber.d("Loading specific page $page")

            userService.getUsersPage(page).fold(
                onSuccess = { (users, totalPages) ->
                    Timber.d("Successfully loaded ${users.size} users from page $page")
                    _uiState.update {
                        it.copy(
                            users = users,
                            currentPage = page,
                            totalPages = totalPages,
                            isLoading = false
                        )
                    }
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to load page $page")
                    _uiState.update { it.copy(isLoading = false) }
                    _effect.send(UserEffect.ShowError(error.message ?: "Failed to load page $page"))
                }
            )
        }
    }
}
