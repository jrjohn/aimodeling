package com.example.aimodel.ui.screens

import app.cash.turbine.test
import com.example.aimodel.data.model.User
import com.example.aimodel.data.repository.DataRepository
import com.example.aimodel.sync.Synchronizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var dataRepository: DataRepository
    private lateinit var synchronizer: Synchronizer
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        dataRepository = mock()
        synchronizer = mock()
        viewModel = HomeViewModel(dataRepository, synchronizer)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is correct`() = runTest {
        whenever(dataRepository.getUsers()).thenReturn(flowOf(emptyList()))
        val initialState = HomeUIState(users = emptyList(), isLoading = false)
        assertEquals(initialState, viewModel.uiState.value)
    }

    @Test
    fun `loadUsers updates state with users`() = runTest {
        val users = listOf(User(1, "John Doe"))
        whenever(dataRepository.getUsers()).thenReturn(flowOf(users))
        whenever(synchronizer.sync()).thenReturn(true)

        viewModel.uiState.test {
            // Initial state from loadUsers()
            assertEquals(HomeUIState(users = users, isLoading = false), awaitItem())
            // State after syncData() starts
            assertEquals(HomeUIState(users = users, isLoading = true), awaitItem())
            // State after syncData() finishes
            assertEquals(HomeUIState(users = users, isLoading = false), awaitItem())
        }
    }
}
