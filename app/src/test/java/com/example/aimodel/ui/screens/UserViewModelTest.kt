package com.example.aimodel.ui.screens

import app.cash.turbine.test
import com.example.aimodel.R
import com.example.aimodel.core.analytics.AnalyticsTracker
import com.example.aimodel.core.common.StringProvider
import com.example.aimodel.data.model.User
import com.example.aimodel.domain.service.UserService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class UserViewModelTest {

    private lateinit var viewModel: UserViewModel
    private lateinit var userService: UserService
    private lateinit var stringProvider: StringProvider
    private lateinit var analyticsTracker: AnalyticsTracker
    private val testDispatcher = StandardTestDispatcher()

    private val testUsers = listOf(
        User(id = 1, firstName = "John", lastName = "Doe", email = "john@example.com", avatar = "avatar1.jpg"),
        User(id = 2, firstName = "Jane", lastName = "Smith", email = "jane@example.com", avatar = "avatar2.jpg")
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        userService = mock()
        stringProvider = mock()
        analyticsTracker = mock()

        // Mock string resources
        whenever(stringProvider.getString(R.string.user_created_success)).thenReturn("User created successfully")
        whenever(stringProvider.getString(R.string.user_create_failed)).thenReturn("Failed to create user")
        whenever(stringProvider.getString(R.string.user_updated_success)).thenReturn("User updated successfully")
        whenever(stringProvider.getString(R.string.user_update_failed)).thenReturn("Failed to update user")
        whenever(stringProvider.getString(R.string.user_deleted_success)).thenReturn("User deleted successfully")
        whenever(stringProvider.getString(R.string.user_delete_failed)).thenReturn("Failed to delete user")
        whenever(stringProvider.getString(R.string.error_failed_load_users)).thenReturn("Failed to load users")
        whenever(stringProvider.getString(R.string.error_failed_load_more_users)).thenReturn("Failed to load more users")
        whenever(stringProvider.getString(eq(R.string.error_failed_load_page), any())).thenAnswer { invocation ->
            "Failed to load page ${invocation.getArgument<Int>(1)}"
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial load should fetch users and update state`() = runTest {
        // Given
        whenever(userService.getUsersPage(1)).thenReturn(Result.success(Pair(testUsers, 5)))

        // When
        viewModel = UserViewModel(userService, stringProvider, analyticsTracker)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(testUsers, state.users)
        assertEquals(1, state.currentPage)
        assertEquals(5, state.totalPages)
        assertFalse(state.isLoading)
        verify(userService).getUsersPage(1)
    }

    @Test
    fun `initial load failure should emit error effect`() = runTest {
        // Given
        val errorMessage = "Network error"
        whenever(userService.getUsersPage(1)).thenReturn(Result.failure(Exception(errorMessage)))

        // When
        viewModel = UserViewModel(userService, stringProvider, analyticsTracker)

        // Then
        viewModel.effect.test {
            advanceUntilIdle()
            val effect = awaitItem()
            assertTrue(effect is UserEffect.ShowError)
            assertEquals(errorMessage, (effect as UserEffect.ShowError).message)
        }
    }

    @Test
    fun `LoadNextPage event should append users and update page`() = runTest {
        // Given
        val page1Users = listOf(testUsers[0])
        val page2Users = listOf(testUsers[1])
        whenever(userService.getUsersPage(1)).thenReturn(Result.success(Pair(page1Users, 5)))
        whenever(userService.getUsersPage(2)).thenReturn(Result.success(Pair(page2Users, 5)))

        viewModel = UserViewModel(userService, stringProvider, analyticsTracker)
        advanceUntilIdle()

        // When
        viewModel.onEvent(UserEvent.LoadNextPage)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(page2Users, state.users) // Current page users
        assertEquals(page1Users + page2Users, state.allUsers) // All loaded users
        assertEquals(2, state.currentPage)
        assertFalse(state.isLoadingMore)
        verify(userService).getUsersPage(2)
    }

    @Test
    fun `LoadNextPage should not load if already loading`() = runTest {
        // Given
        whenever(userService.getUsersPage(1)).thenReturn(Result.success(Pair(testUsers, 5)))
        whenever(userService.getUsersPage(2)).thenReturn(Result.success(Pair(emptyList(), 5)))

        viewModel = UserViewModel(userService, stringProvider, analyticsTracker)
        advanceUntilIdle()

        // When - trigger LoadNextPage once, should work
        viewModel.onEvent(UserEvent.LoadNextPage)

        // Trigger again while still loading (before advanceUntilIdle)
        viewModel.onEvent(UserEvent.LoadNextPage)  // Should be ignored

        advanceUntilIdle()

        // Then - getUsersPage(2) should only be called once despite two events
        verify(userService, org.mockito.kotlin.times(1)).getUsersPage(2)
    }

    @Test
    fun `LoadNextPage should not load if on last page`() = runTest {
        // Given
        whenever(userService.getUsersPage(1)).thenReturn(Result.success(Pair(testUsers, 1)))
        viewModel = UserViewModel(userService, stringProvider, analyticsTracker)
        advanceUntilIdle()

        // When
        viewModel.onEvent(UserEvent.LoadNextPage)
        advanceUntilIdle()

        // Then - should not try to load page 2
        verify(userService).getUsersPage(1)  // Only initial load
    }

    @Test
    fun `CreateUser event should create user and refresh list`() = runTest {
        // Given
        val newUser = User(id = 0, firstName = "New", lastName = "User", email = "new@example.com")
        whenever(userService.getUsersPage(1)).thenReturn(Result.success(Pair(testUsers, 5)))
        whenever(userService.createUser(newUser)).thenReturn(true)

        viewModel = UserViewModel(userService, stringProvider, analyticsTracker)
        advanceUntilIdle()

        // When
        viewModel.onEvent(UserEvent.CreateUser(newUser))

        // Then
        viewModel.effect.test {
            advanceUntilIdle()
            val effect = awaitItem()
            assertTrue(effect is UserEffect.ShowSuccess)
            assertEquals("User created successfully", (effect as UserEffect.ShowSuccess).message)
        }

        advanceUntilIdle()
        verify(userService).createUser(newUser)
        // getUsersPage(1) called at least twice: initial + refresh
        verify(userService, org.mockito.kotlin.atLeast(2)).getUsersPage(1)
    }

    @Test
    fun `CreateUser failure should emit error effect`() = runTest {
        // Given
        val newUser = User(id = 0, firstName = "New", lastName = "User", email = "new@example.com")
        whenever(userService.getUsersPage(1)).thenReturn(Result.success(Pair(testUsers, 5)))
        whenever(userService.createUser(newUser)).thenReturn(false)

        viewModel = UserViewModel(userService, stringProvider, analyticsTracker)
        advanceUntilIdle()

        // When
        viewModel.onEvent(UserEvent.CreateUser(newUser))

        // Then
        viewModel.effect.test {
            advanceUntilIdle()
            val effect = awaitItem()
            assertTrue(effect is UserEffect.ShowError)
            assertEquals("Failed to create user", (effect as UserEffect.ShowError).message)
        }
    }

    @Test
    fun `UpdateUser event should update user and refresh list`() = runTest {
        // Given
        val updatedUser = testUsers[0].copy(firstName = "Updated")
        whenever(userService.getUsersPage(1)).thenReturn(Result.success(Pair(testUsers, 5)))
        whenever(userService.updateUser(updatedUser)).thenReturn(true)

        viewModel = UserViewModel(userService, stringProvider, analyticsTracker)
        advanceUntilIdle()

        // When
        viewModel.onEvent(UserEvent.UpdateUser(updatedUser))

        // Then
        viewModel.effect.test {
            advanceUntilIdle()
            val effect = awaitItem()
            assertTrue(effect is UserEffect.ShowSuccess)
            assertEquals("User updated successfully", (effect as UserEffect.ShowSuccess).message)
        }

        advanceUntilIdle()
        verify(userService).updateUser(updatedUser)
    }

    @Test
    fun `UpdateUser failure should emit error effect`() = runTest {
        // Given
        val updatedUser = testUsers[0].copy(firstName = "Updated")
        whenever(userService.getUsersPage(1)).thenReturn(Result.success(Pair(testUsers, 5)))
        whenever(userService.updateUser(updatedUser)).thenReturn(false)

        viewModel = UserViewModel(userService, stringProvider, analyticsTracker)
        advanceUntilIdle()

        // When
        viewModel.onEvent(UserEvent.UpdateUser(updatedUser))

        // Then
        viewModel.effect.test {
            advanceUntilIdle()
            val effect = awaitItem()
            assertTrue(effect is UserEffect.ShowError)
            assertEquals("Failed to update user", (effect as UserEffect.ShowError).message)
        }
    }

    @Test
    fun `DeleteUser event should remove user from state and emit success`() = runTest {
        // Given
        val userToDelete = testUsers[0]
        whenever(userService.getUsersPage(1)).thenReturn(Result.success(Pair(testUsers, 5)))
        whenever(userService.deleteUser(userToDelete.id)).thenReturn(true)

        viewModel = UserViewModel(userService, stringProvider, analyticsTracker)
        advanceUntilIdle()

        // When
        viewModel.onEvent(UserEvent.DeleteUser(userToDelete))

        // Then
        viewModel.effect.test {
            advanceUntilIdle()
            val effect = awaitItem()
            assertTrue(effect is UserEffect.ShowSuccess)
            assertEquals("User deleted successfully", (effect as UserEffect.ShowSuccess).message)
        }

        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertFalse(state.users.contains(userToDelete))
        verify(userService).deleteUser(userToDelete.id)
    }

    @Test
    fun `DeleteUser failure should emit error effect`() = runTest {
        // Given
        val userToDelete = testUsers[0]
        whenever(userService.getUsersPage(1)).thenReturn(Result.success(Pair(testUsers, 5)))
        whenever(userService.deleteUser(userToDelete.id)).thenReturn(false)

        viewModel = UserViewModel(userService, stringProvider, analyticsTracker)
        advanceUntilIdle()

        // When
        viewModel.onEvent(UserEvent.DeleteUser(userToDelete))

        // Then
        viewModel.effect.test {
            advanceUntilIdle()
            val effect = awaitItem()
            assertTrue(effect is UserEffect.ShowError)
            assertEquals("Failed to delete user", (effect as UserEffect.ShowError).message)
        }
    }

    @Test
    fun `Refresh event should reload first page`() = runTest {
        // Given
        val initialUsers = listOf(testUsers[0])
        val refreshedUsers = testUsers
        whenever(userService.getUsersPage(1))
            .thenReturn(Result.success(Pair(initialUsers, 5)))
            .thenReturn(Result.success(Pair(refreshedUsers, 5)))

        viewModel = UserViewModel(userService, stringProvider, analyticsTracker)
        advanceUntilIdle()

        // When
        viewModel.onEvent(UserEvent.Refresh)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(refreshedUsers, state.users)
        assertEquals(1, state.currentPage)
    }

    @Test
    fun `GoToPage event should load specific page`() = runTest {
        // Given
        val page3Users = listOf(testUsers[0])
        whenever(userService.getUsersPage(1)).thenReturn(Result.success(Pair(testUsers, 10)))
        whenever(userService.getUsersPage(3)).thenReturn(Result.success(Pair(page3Users, 10)))

        viewModel = UserViewModel(userService, stringProvider, analyticsTracker)
        advanceUntilIdle()

        // When
        viewModel.onEvent(UserEvent.GoToPage(3))
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(page3Users, state.users)
        assertEquals(3, state.currentPage)
        verify(userService).getUsersPage(3)
    }

    @Test
    fun `GoToPage should not load invalid page number`() = runTest {
        // Given
        whenever(userService.getUsersPage(1)).thenReturn(Result.success(Pair(testUsers, 5)))
        viewModel = UserViewModel(userService, stringProvider, analyticsTracker)
        advanceUntilIdle()

        // When - try to go to page 100 (invalid)
        viewModel.onEvent(UserEvent.GoToPage(100))
        advanceUntilIdle()

        // Then - should not attempt to load
        verify(userService).getUsersPage(1)  // Only initial load
    }

    @Test
    fun `GoToNextPage event should load next page when available`() = runTest {
        // Given
        whenever(userService.getUsersPage(1)).thenReturn(Result.success(Pair(testUsers, 5)))
        whenever(userService.getUsersPage(2)).thenReturn(Result.success(Pair(testUsers, 5)))

        viewModel = UserViewModel(userService, stringProvider, analyticsTracker)
        advanceUntilIdle()

        // When
        viewModel.onEvent(UserEvent.GoToNextPage)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(2, state.currentPage)
        verify(userService).getUsersPage(2)
    }

    @Test
    fun `GoToPreviousPage event should load previous page when available`() = runTest {
        // Given
        whenever(userService.getUsersPage(1)).thenReturn(Result.success(Pair(testUsers, 5)))
        whenever(userService.getUsersPage(2)).thenReturn(Result.success(Pair(testUsers, 5)))

        viewModel = UserViewModel(userService, stringProvider, analyticsTracker)
        advanceUntilIdle()

        // Navigate to page 2 first
        viewModel.onEvent(UserEvent.GoToPage(2))
        advanceUntilIdle()

        // When - go back to page 1
        viewModel.onEvent(UserEvent.GoToPreviousPage)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(1, state.currentPage)
    }

    @Test
    fun `loading state should be false after initial load completes`() = runTest {
        // Given
        whenever(userService.getUsersPage(any())).thenReturn(Result.success(Pair(testUsers, 5)))

        // When
        viewModel = UserViewModel(userService, stringProvider, analyticsTracker)
        advanceUntilIdle()

        // Then - after completion, loading should be false
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(testUsers, viewModel.uiState.value.users)
    }
}
