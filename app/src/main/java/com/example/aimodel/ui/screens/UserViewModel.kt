package com.example.aimodel.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aimodel.data.model.User
import com.example.aimodel.domain.service.UserService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class UserUiState(
    val users: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val error: String? = null
)

@HiltViewModel
class UserViewModel @Inject constructor(
    private val userService: UserService
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserUiState())
    val uiState: StateFlow<UserUiState> = _uiState.asStateFlow()

    init {
        loadUsers()
    }

    private fun loadUsers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
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
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to load users"
                        )
                    }
                }
            )
        }
    }

    fun loadNextPage() {
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
                    _uiState.update {
                        it.copy(
                            isLoadingMore = false,
                            error = error.message ?: "Failed to load more users"
                        )
                    }
                }
            )
        }
    }

    fun createUser(user: User) {
        viewModelScope.launch {
            Timber.d("Creating user: name=${user.name}, email=${user.email}, avatar=${user.avatar}")
            userService.createUser(user)
            loadUsers() // Refresh the list
        }
    }

    fun updateUser(user: User) {
        viewModelScope.launch {
            Timber.d("Updating user ${user.id}: name=${user.name}, email=${user.email}, avatar=${user.avatar}")
            userService.updateUser(user)
            loadUsers() // Refresh the list
        }
    }

    fun deleteUser(user: User) {
        viewModelScope.launch {
            Timber.d("Deleting user ${user.id}: ${user.name}")
            userService.deleteUser(user.id)
            // Remove from local state immediately for better UX
            _uiState.update {
                it.copy(users = it.users.filter { u -> u.id != user.id })
            }
            Timber.d("User ${user.id} deleted successfully")
        }
    }

    fun refresh() {
        loadUsers()
    }

    fun goToNextPage() {
        val currentState = _uiState.value
        if (currentState.currentPage < currentState.totalPages && !currentState.isLoading) {
            loadSpecificPage(currentState.currentPage + 1)
        }
    }

    fun goToPreviousPage() {
        val currentState = _uiState.value
        if (currentState.currentPage > 1 && !currentState.isLoading) {
            loadSpecificPage(currentState.currentPage - 1)
        }
    }

    fun goToPage(page: Int) {
        val currentState = _uiState.value
        if (page in 1..currentState.totalPages && !currentState.isLoading) {
            loadSpecificPage(page)
        }
    }

    private fun loadSpecificPage(page: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
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
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to load page $page"
                        )
                    }
                }
            )
        }
    }
}
