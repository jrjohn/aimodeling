package com.example.aimodel.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aimodel.data.model.User
import com.example.aimodel.domain.service.UserService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UserUiState(
    val users: List<User> = emptyList(),
    val isLoading: Boolean = false,
)

@HiltViewModel
class UserViewModel @Inject constructor(
    private val userService: UserService
) : ViewModel() {

    val uiState: StateFlow<UserUiState> = userService
        .getUsers()
        .map { UserUiState(users = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UserUiState(isLoading = true),
        )

    fun createUser(name: String, job: String) {
        viewModelScope.launch {
            userService.createUser(name, job)
        }
    }

    fun updateUser(user: User, name: String, job: String) {
        viewModelScope.launch {
            userService.updateUser(user.id, name, job)
        }
    }

    fun deleteUser(user: User) {
        viewModelScope.launch {
            userService.deleteUser(user.id)
        }
    }
}
