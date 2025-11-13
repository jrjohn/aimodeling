package com.example.aimodel.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aimodel.data.model.User
import com.example.aimodel.domain.service.UserService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class HomeUIEvent {
    data class ShowSnackbar(val message: String) : HomeUIEvent()
}

data class HomeUIState(
    val users: List<User> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userService: UserService
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUIState())
    val uiState: StateFlow<HomeUIState> = _uiState

    private val _event = MutableStateFlow<HomeUIEvent?>(null)
    val event: StateFlow<HomeUIEvent?> = _event

    init {
        loadUsers()
        syncData()
    }

    private fun loadUsers() {
        userService.getUsers()
            .onEach { users ->
                _uiState.value = _uiState.value.copy(users = users)
            }
            .catch {
                _event.value = HomeUIEvent.ShowSnackbar("Error loading users from local source")
            }
            .launchIn(viewModelScope)
    }

    private fun syncData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val syncSuccessful = userService.syncUsers()
            if (!syncSuccessful) {
                _event.value = HomeUIEvent.ShowSnackbar("Sync failed")
            }
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun onEvent(event: HomeUIEvent) {
        // Handle UI events here
    }
}
