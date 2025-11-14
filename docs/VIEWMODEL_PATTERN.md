# ViewModel Input/Output Pattern

## Overview

All ViewModels in the Arcana Android app follow a structured **Input/Output pattern** for clear separation of concerns and better code organization.

## Pattern Structure

```kotlin
class SomeViewModel @Inject constructor(
    // dependencies
) : AnalyticsViewModel(analyticsTracker) {

    // ============================================
    // Input - Events from UI to ViewModel
    // ============================================
    sealed interface Input {
        data object LoadData : Input
        data class UpdateItem(val item: Item) : Input
        data class DeleteItem(val id: Int) : Input
    }

    // ============================================
    // Output - State and Effects to UI
    // ============================================
    sealed interface Output {
        /**
         * State - Represents the current UI state for binding
         */
        data class State(
            val items: List<Item> = emptyList(),
            val isLoading: Boolean = false,
            val error: String? = null
        )

        /**
         * Effect - One-time events from ViewModel to UI
         */
        sealed interface Effect {
            data class ShowError(val message: String) : Effect
            data class ShowSuccess(val message: String) : Effect
            data class Navigate(val route: String) : Effect
        }
    }

    // ============================================
    // State & Effect Channels
    // ============================================
    private val _state = MutableStateFlow(Output.State())
    val state: StateFlow<Output.State> = _state.asStateFlow()

    private val _effect = Channel<Output.Effect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    // ============================================
    // Event Handler
    // ============================================
    fun onEvent(input: Input) {
        when (input) {
            is Input.LoadData -> loadData()
            is Input.UpdateItem -> updateItem(input.item)
            is Input.DeleteItem -> deleteItem(input.id)
        }
    }

    // ============================================
    // Private Methods
    // ============================================
    private fun loadData() {
        // Implementation
    }
}
```

## Components

### 1. Input (Events)

**Purpose**: Represents user actions or UI events sent from the View to the ViewModel.

**Characteristics**:
- Sealed interface containing all possible user inputs
- Immutable data structures
- Clear intent naming (LoadData, CreateUser, etc.)

**Example**:
```kotlin
sealed interface Input {
    data object LoadInitial : Input
    data class CreateUser(val user: User) : Input
    data class UpdateUser(val user: User) : Input
    data class DeleteUser(val user: User) : Input
}
```

**Usage in UI**:
```kotlin
// In Composable
Button(onClick = { viewModel.onEvent(UserViewModel.Input.CreateUser(user)) }) {
    Text("Create User")
}
```

### 2. Output.State (UI State)

**Purpose**: Represents the current state of the UI for data binding.

**Characteristics**:
- Immutable data class
- Contains all data needed to render the UI
- Computed properties for derived data
- Exposed as `StateFlow` for reactive updates

**Example**:
```kotlin
data class State(
    val users: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val currentPage: Int = 1,
    val totalPages: Int = 1
) {
    // Computed property
    val hasMorePages: Boolean
        get() = currentPage < totalPages
}
```

**Usage in UI**:
```kotlin
// In Composable
val state by viewModel.state.collectAsState()

if (state.isLoading) {
    CircularProgressIndicator()
} else {
    LazyColumn {
        items(state.users) { user ->
            UserItem(user)
        }
    }
}
```

### 3. Output.Effect (One-time Events)

**Purpose**: Represents one-time side effects that should not be part of the state (navigation, snackbars, dialogs).

**Characteristics**:
- Sealed interface for type-safety
- Consumed once and not retained
- Exposed as `Flow` from a `Channel`
- Perfect for navigation, showing toasts, etc.

**Example**:
```kotlin
sealed interface Effect {
    data class ShowError(val message: String) : Effect
    data class ShowSuccess(val message: String) : Effect
    data class NavigateToDetail(val userId: Int) : Effect
}
```

**Usage in UI**:
```kotlin
// In Composable
val snackbarHostState = remember { SnackbarHostState() }

LaunchedEffect(Unit) {
    viewModel.effect.collect { effect ->
        when (effect) {
            is UserViewModel.Output.Effect.ShowError -> {
                snackbarHostState.showSnackbar(effect.message)
            }
            is UserViewModel.Output.Effect.ShowSuccess -> {
                snackbarHostState.showSnackbar(effect.message)
            }
        }
    }
}
```

## Benefits

### 1. Clear Separation of Concerns
- **Input**: What the user can do
- **Output.State**: What the UI shows
- **Output.Effect**: What happens once (side effects)

### 2. Type Safety
- All events and effects are type-safe sealed interfaces
- Compiler ensures all cases are handled
- No magic strings or integers

### 3. Testability
- Easy to test: send Input, verify State and Effect
- No need to mock Compose or Android framework
- Clear contract between ViewModel and UI

### 4. Maintainability
- New developers can quickly understand the contract
- Adding new events is straightforward
- Refactoring is safer with compile-time checks

### 5. Scalability
- Pattern scales well for complex screens
- Easy to add new inputs/outputs without breaking existing code
- Clear boundaries prevent spaghetti code

## Examples in Codebase

### UserViewModel
```kotlin
// Input events
viewModel.onEvent(UserViewModel.Input.LoadInitial)
viewModel.onEvent(UserViewModel.Input.CreateUser(user))
viewModel.onEvent(UserViewModel.Input.DeleteUser(user))

// State binding
val state by viewModel.state.collectAsState()
Text("Total: ${state.totalPages}")

// Effect handling
LaunchedEffect(Unit) {
    viewModel.effect.collect { effect ->
        when (effect) {
            is UserViewModel.Output.Effect.ShowError -> { /* ... */ }
            is UserViewModel.Output.Effect.ShowSuccess -> { /* ... */ }
        }
    }
}
```

### HomeViewModel
```kotlin
// Input events
viewModel.onEvent(HomeViewModel.Input.LoadUsers)
viewModel.onEvent(HomeViewModel.Input.Refresh)

// State binding
val state by viewModel.state.collectAsState()
Text("Users: ${state.users.size}")

// Effect handling
LaunchedEffect(Unit) {
    viewModel.effect.collect { effect ->
        when (effect) {
            is HomeViewModel.Output.Effect.ShowSnackbar -> {
                snackbarHostState.showSnackbar(effect.message)
            }
        }
    }
}
```

## Best Practices

### 1. Keep State Immutable
```kotlin
// ✅ Good
_state.update { it.copy(isLoading = true) }

// ❌ Bad
_state.value.isLoading = true // Won't work, data class is immutable
```

### 2. Use Computed Properties for Derived Data
```kotlin
// ✅ Good
data class State(
    val users: List<User> = emptyList()
) {
    val userCount: Int get() = users.size
    val hasUsers: Boolean get() = users.isNotEmpty()
}

// ❌ Bad - storing derived data
data class State(
    val users: List<User> = emptyList(),
    val userCount: Int = 0,  // Can get out of sync
    val hasUsers: Boolean = false  // Redundant
)
```

### 3. Use Effects for One-Time Events Only
```kotlin
// ✅ Good - navigation is one-time
sealed interface Effect {
    data class NavigateToDetail(val id: Int) : Effect
}

// ❌ Bad - loading state should be in State, not Effect
sealed interface Effect {
    data object ShowLoading : Effect  // Wrong!
}
```

### 4. Name Inputs with Action Verbs
```kotlin
// ✅ Good
sealed interface Input {
    data object LoadUsers : Input
    data class CreateUser(val user: User) : Input
    data object Refresh : Input
}

// ❌ Bad
sealed interface Input {
    data object Users : Input  // What about users?
    data class User(val user: User) : Input  // Create? Update?
}
```

### 5. Keep ViewModel Logic Private
```kotlin
class UserViewModel {
    // ✅ Good - private methods
    private fun loadUsers() { /* ... */ }
    private fun createUser(user: User) { /* ... */ }

    // Public interface
    fun onEvent(input: Input) {
        when (input) {
            is Input.LoadUsers -> loadUsers()
            is Input.CreateUser -> createUser(input.user)
        }
    }
}
```

## Migration Guide

If you have an existing ViewModel, follow these steps:

1. **Create Input sealed interface** from existing function calls
2. **Create Output.State** from existing state class
3. **Create Output.Effect** for one-time events
4. **Replace direct function calls** with `onEvent(Input.X)`
5. **Update UI** to use `viewModel.state` and `viewModel.effect`

### Before
```kotlin
class OldViewModel {
    val uiState: StateFlow<UiState>

    fun loadUsers() { /* ... */ }
    fun createUser(user: User) { /* ... */ }
}

// UI
viewModel.loadUsers()
val uiState by viewModel.uiState.collectAsState()
```

### After
```kotlin
class NewViewModel {
    sealed interface Input {
        data object LoadUsers : Input
        data class CreateUser(val user: User) : Input
    }

    sealed interface Output {
        data class State(/* ... */)
        sealed interface Effect { /* ... */ }
    }

    val state: StateFlow<Output.State>
    val effect: Flow<Output.Effect>

    fun onEvent(input: Input)
}

// UI
viewModel.onEvent(NewViewModel.Input.LoadUsers)
val state by viewModel.state.collectAsState()
```

## Conclusion

The Input/Output pattern provides a clean, maintainable, and scalable architecture for ViewModels in the Arcana Android app. It enforces clear contracts, improves testability, and makes the codebase easier to understand and maintain.
