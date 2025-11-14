package com.example.aimodel.ui.screens

import androidx.lifecycle.viewModelScope
import com.example.aimodel.core.analytics.AnalyticsScreens
import com.example.aimodel.core.analytics.AnalyticsTracker
import com.example.aimodel.core.analytics.AnalyticsViewModel
import com.example.aimodel.core.analytics.Events
import com.example.aimodel.core.analytics.Params
import com.example.aimodel.core.analytics.annotations.TrackScreen
import com.example.aimodel.core.analytics.trackFlow
import com.example.aimodel.core.analytics.trackSync
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
    val totalUserCount: Int = 0,
    val isLoading: Boolean = false
)

@HiltViewModel
@TrackScreen(AnalyticsScreens.HOME)
class HomeViewModel @Inject constructor(
    private val userService: UserService,
    analyticsTracker: AnalyticsTracker
) : AnalyticsViewModel(analyticsTracker) {

    private val _uiState = MutableStateFlow(HomeUIState())
    val uiState: StateFlow<HomeUIState> = _uiState

    private val _event = MutableStateFlow<HomeUIEvent?>(null)
    val event: StateFlow<HomeUIEvent?> = _event

    init {
        // Screen view automatically tracked via @TrackScreen annotation
        loadUsers()
        syncData()
    }

    private fun loadUsers() {
        userService.getUsers()
            .trackFlow(
                analyticsTracker = analyticsTracker,
                eventName = Events.PAGE_LOADED,
                params = mapOf(Params.SCREEN_NAME to AnalyticsScreens.HOME),
                trackPerformance = true,
                trackErrors = true,
                onData = { users -> mapOf(Params.ITEM_COUNT to users.size.toString()) }
            )
            .onEach { users ->
                _uiState.value = _uiState.value.copy(users = users)
            }
            .catch { error ->
                _event.value = HomeUIEvent.ShowSnackbar("Error loading users from local source")
            }
            .launchIn(viewModelScope)
    }

    private fun syncData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // Use trackSync extension for automatic sync event tracking
            val syncSuccessful = try {
                trackSync(
                    analyticsTracker = analyticsTracker,
                    screenName = AnalyticsScreens.HOME,
                    trigger = "auto"
                ) {
                    userService.syncUsers()
                }
            } catch (error: Exception) {
                _event.value = HomeUIEvent.ShowSnackbar("Sync failed")
                false
            }

            if (!syncSuccessful) {
                _event.value = HomeUIEvent.ShowSnackbar("Sync failed")
            }

            // Fetch total user count from API
            val totalCount = userService.getTotalUserCount()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                totalUserCount = totalCount
            )
        }
    }

    fun onEvent(event: HomeUIEvent) {
        // Handle UI events here
    }
}
