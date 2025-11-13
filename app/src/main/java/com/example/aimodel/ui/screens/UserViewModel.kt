package com.example.aimodel.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aimodel.R
import com.example.aimodel.core.common.StringProvider
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
    val userPages: Map<Int, List<User>> = emptyMap(), // All loaded pages
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val currentPage: Int = 1,
    val totalPages: Int = 1
) {
    /**
     * Returns all users from loaded pages in order
     */
    val allUsers: List<User>
        get() = userPages.entries
            .sortedBy { it.key }
            .flatMap { it.value }

    /**
     * Returns users for the current page
     */
    val users: List<User>
        get() = userPages[currentPage] ?: emptyList()

    /**
     * Checks if a specific page is loaded
     */
    fun isPageLoaded(page: Int): Boolean = userPages.containsKey(page)

    /**
     * Gets the number of loaded pages
     */
    val loadedPagesCount: Int
        get() = userPages.size
}

@HiltViewModel
class UserViewModel @Inject constructor(
    private val userService: UserService,
    private val stringProvider: StringProvider
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
                            userPages = mapOf(1 to users), // Reset cache with page 1
                            currentPage = 1,
                            totalPages = totalPages,
                            isLoading = false
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isLoading = false) }
                    _effect.send(UserEffect.ShowError(
                        error.message ?: stringProvider.getString(R.string.error_failed_load_users)
                    ))
                }
            )
        }
    }

    private fun loadNextPage() {
        // Atomically check and set loading flag to prevent race conditions
        var shouldLoad = false
        var nextPage = 1

        _uiState.update { currentState ->
            Timber.d("loadNextPage called - currentPage: ${currentState.currentPage}, totalPages: ${currentState.totalPages}, isLoadingMore: ${currentState.isLoadingMore}")

            if (currentState.isLoadingMore || currentState.currentPage >= currentState.totalPages) {
                Timber.d("loadNextPage skipped - already loading or no more pages")
                currentState // Return unchanged state
            } else {
                shouldLoad = true
                nextPage = currentState.currentPage + 1
                currentState.copy(isLoadingMore = true) // Set loading flag
            }
        }

        // Only proceed if we successfully set the loading flag
        if (!shouldLoad) return

        viewModelScope.launch {
            Timber.d("Loading page $nextPage")

            userService.getUsersPage(nextPage).fold(
                onSuccess = { (newUsers, totalPages) ->
                    Timber.d("Successfully loaded ${newUsers.size} users from page $nextPage")
                    _uiState.update {
                        it.copy(
                            userPages = it.userPages + (nextPage to newUsers), // Add page to cache
                            currentPage = nextPage,
                            totalPages = totalPages,
                            isLoadingMore = false
                        )
                    }
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to load page $nextPage")
                    _uiState.update { it.copy(isLoadingMore = false) }
                    _effect.send(UserEffect.ShowError(
                        error.message ?: stringProvider.getString(R.string.error_failed_load_more_users)
                    ))
                }
            )
        }
    }

    private fun createUser(user: User) {
        viewModelScope.launch {
            Timber.d("Creating user: name=${user.name}, email=${user.email}, avatar=${user.avatar}")
            val success = userService.createUser(user)
            if (success) {
                _effect.send(UserEffect.ShowSuccess(
                    stringProvider.getString(R.string.user_created_success)
                ))
                loadUsers() // Refresh the list
            } else {
                _effect.send(UserEffect.ShowError(
                    stringProvider.getString(R.string.user_create_failed)
                ))
            }
        }
    }

    private fun updateUser(user: User) {
        viewModelScope.launch {
            Timber.d("Updating user ${user.id}: name=${user.name}, email=${user.email}, avatar=${user.avatar}")
            val success = userService.updateUser(user)
            if (success) {
                _effect.send(UserEffect.ShowSuccess(
                    stringProvider.getString(R.string.user_updated_success)
                ))
                loadUsers() // Refresh the list
            } else {
                _effect.send(UserEffect.ShowError(
                    stringProvider.getString(R.string.user_update_failed)
                ))
            }
        }
    }

    private fun deleteUser(user: User) {
        viewModelScope.launch {
            Timber.d("Deleting user ${user.id}: ${user.name}")
            val success = userService.deleteUser(user.id)
            if (success) {
                // Remove from all cached pages for better UX
                _uiState.update { state ->
                    val updatedPages = state.userPages.mapValues { (_, users) ->
                        users.filter { u -> u.id != user.id }
                    }
                    state.copy(userPages = updatedPages)
                }
                _effect.send(UserEffect.ShowSuccess(
                    stringProvider.getString(R.string.user_deleted_success)
                ))
                Timber.d("User ${user.id} deleted successfully")
            } else {
                _effect.send(UserEffect.ShowError(
                    stringProvider.getString(R.string.user_delete_failed)
                ))
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
        val currentState = _uiState.value

        // Check if page is already cached
        if (currentState.isPageLoaded(page)) {
            Timber.d("Page $page already loaded, switching to it")
            _uiState.update { it.copy(currentPage = page) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            Timber.d("Loading specific page $page")

            userService.getUsersPage(page).fold(
                onSuccess = { (users, totalPages) ->
                    Timber.d("Successfully loaded ${users.size} users from page $page")
                    _uiState.update {
                        it.copy(
                            userPages = it.userPages + (page to users), // Add page to cache
                            currentPage = page,
                            totalPages = totalPages,
                            isLoading = false
                        )
                    }
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to load page $page")
                    _uiState.update { it.copy(isLoading = false) }
                    _effect.send(UserEffect.ShowError(
                        error.message ?: stringProvider.getString(R.string.error_failed_load_page, page)
                    ))
                }
            )
        }
    }
}
