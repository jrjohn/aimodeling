# Arcana Android - Complete Project Documentation & Generation Prompt

> **Purpose**: This document serves as the complete specification for regenerating the Arcana Android application. It consolidates all architecture documentation, implementation guides, configurations, and project knowledge into one comprehensive reference.

**Project**: Arcana Android
**Package**: `com.example.arcana`
**Repository**: arcana-android
**Last Updated**: November 2024

---

## Table of Contents

1. [Core App Generation Prompt](#1-core-app-generation-prompt)
2. [Architecture Documentation](#2-architecture-documentation)
3. [Input/Output ViewModel Pattern](#3-inputoutput-viewmodel-pattern)
4. [Input Validation](#4-input-validation)
5. [Analytics System](#5-analytics-system)
6. [Offline-First Implementation](#6-offline-first-implementation)
7. [Pagination & Caching](#7-pagination--caching)
8. [UI Theme - Arcana](#8-ui-theme---arcana)
9. [Configuration Snapshot](#9-configuration-snapshot)
10. [API Documentation Setup](#10-api-documentation-setup)
11. [Testing Strategy](#11-testing-strategy)
12. [Key Improvements & Fixes](#12-key-improvements--fixes)

---

## 1. Core App Generation Prompt

### Project Overview

**Arcana Android** is a production-ready Android application demonstrating:
- **Clean Architecture** with clear layer separation
- **Offline-First** design for seamless user experience
- **Input/Output ViewModel Pattern** for structured state management
- **AOP Analytics** for comprehensive behavior tracking
- **Real-time Input Validation** following Android best practices
- **Arcana Theme** with mystical purple/gold aesthetic

### Core Requirements

| Category | Technology | Version/Details |
|----------|-----------|-----------------|
| **Language** | Kotlin | 2.2.21 |
| **Async** | Coroutines | 1.10.2 |
| **UI** | Jetpack Compose | BOM 2025.11.00 |
| **DI** | Hilt | 2.57.2 |
| **Navigation** | Navigation Compose | 2.9.6 |
| **Theme** | Arcana (Purple/Gold) | Custom |
| **Local DB** | Room | 2.8.3 |
| **Network** | Ktorfit + Ktor | 2.6.4 / 3.3.2 |
| **Background** | WorkManager | 2.11.0 |
| **Testing** | JUnit, Mockito, Turbine | 256/256 passing |
| **Logging** | Timber | 5.0.1 |
| **Documentation** | Dokka + Mermaid | Auto-generated |

### Architecture Patterns

#### **MVVM with Input/Output Pattern**

All ViewModels MUST follow this structured pattern:

```kotlin
@HiltViewModel
class UserViewModel @Inject constructor(
    private val userService: UserService,
    private val stringProvider: StringProvider,
    analyticsTracker: AnalyticsTracker
) : AnalyticsViewModel(analyticsTracker) {

    // ============================================
    // Input - Events from UI to ViewModel
    // ============================================
    sealed interface Input {
        data object LoadInitial : Input
        data object LoadNextPage : Input
        data object Refresh : Input
        data class CreateUser(val user: User) : Input
        data class UpdateUser(val user: User) : Input
        data class DeleteUser(val user: User) : Input
    }

    // ============================================
    // Output - State and Effects to UI
    // ============================================
    sealed interface Output {
        /**
         * State - Represents current UI state for binding
         */
        data class State(
            val users: List<User> = emptyList(),
            val isLoading: Boolean = false,
            val currentPage: Int = 1,
            val totalPages: Int = 1
        )

        /**
         * Effect - One-time events from ViewModel to UI
         */
        sealed interface Effect {
            data class ShowError(val message: String) : Effect
            data class ShowSuccess(val message: String) : Effect
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
            is Input.LoadInitial -> loadUsers()
            is Input.LoadNextPage -> loadNextPage()
            is Input.Refresh -> refresh()
            is Input.CreateUser -> createUser(input.user)
            is Input.UpdateUser -> updateUser(input.user)
            is Input.DeleteUser -> deleteUser(input.user)
        }
    }

    private fun loadUsers() { /* implementation */ }
    private fun createUser(user: User) { /* implementation */ }
}
```

**UI Integration Pattern:**

```kotlin
@Composable
fun UserScreen(viewModel: UserViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle one-time effects
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is UserViewModel.Output.Effect.ShowError ->
                    snackbarHostState.showSnackbar(effect.message)
                is UserViewModel.Output.Effect.ShowSuccess ->
                    snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) {
        // Render UI based on state
        if (state.isLoading) {
            CircularProgressIndicator()
        } else {
            LazyColumn {
                items(state.users) { user ->
                    UserItem(user = user)
                }
            }
        }

        // Send events to ViewModel
        Button(onClick = { viewModel.onEvent(UserViewModel.Input.Refresh) }) {
            Text("Refresh")
        }
    }
}
```

#### **Offline-First Architecture**

```
User Action (UI)
    ↓
ViewModel
    ↓
Service Layer
    ↓
Repository (Offline-First)
    ├─ Online:  API → Update Local → Cache → UI
    └─ Offline: Local → Queue Change → Optimistic UI
                    ↓
            Background Sync (When Online)
                    ↓
            Process Queue → API → Refresh Local
```

**Key Components:**
- `NetworkMonitor`: Observes connectivity status
- `UserChange` Entity: Queues offline CRUD operations
- `OfflineFirstDataRepository`: Immediate local updates + queued sync
- `SyncWorker`: Processes offline queue and fetches latest data

#### **Repository Pattern**

```kotlin
interface UserRepository {
    fun getUsers(): Flow<List<User>>
    suspend fun getUsersPage(page: Int): Result<Pair<List<User>, Int>>
    suspend fun createUser(user: User): Boolean
    suspend fun updateUser(user: User): Boolean
    suspend fun deleteUser(id: Int): Boolean
    suspend fun syncUsers(): Boolean
}

class OfflineFirstDataRepository @Inject constructor(
    private val networkDataSource: UserNetworkDataSource,
    private val localDataSource: UserLocalDataSource,
    private val networkMonitor: NetworkMonitor
) : UserRepository {
    // Orchestrates local + network + sync logic
}
```

### Layer Structure

#### **Presentation Layer**
- `ui/screens/` - Compose UI + ViewModels
- `ui/theme/` - Arcana theme (colors, typography)
- `domain/validation/` - Input validators (UserValidator)

#### **Domain Layer**
- `domain/service/` - Business logic (UserService)
- `domain/model/` - Value objects (EmailAddress)

#### **Data Layer**
- `data/repository/` - Offline-first repository
- `data/local/` - Room database + DAOs
- `data/remote/` - Ktorfit API services
- `data/worker/` - WorkManager sync workers

#### **Core/Infrastructure**
- `core/analytics/` - AOP analytics system
- `core/common/` - Utilities, StringProvider, RetryPolicy
- `di/` - Hilt modules

---

## 2. Architecture Documentation

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Presentation Layer                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │   Compose    │→ │  ViewModels  │→ │  UI States   │      │
│  │     UI       │  │  (Input/Out) │  │              │      │
│  └──────┬───────┘  └──────────────┘  └──────────────┘      │
│         ↓                                                    │
│  ┌──────────────┐                                           │
│  │  Validators  │                                           │
│  └──────────────┘                                           │
└────────────────────────┬────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────────┐
│                      Domain Layer                           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │   Services   │→ │Business Logic│→ │Domain Models │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└────────────────────────┬────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────────┐
│                       Data Layer                            │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ Repository   │→ │  Room DB     │  │  Remote API  │      │
│  │(Offline-1st) │  │   (Local)    │  │   (Ktorfit)  │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
```

### Key Principles

1. **Unidirectional Data Flow**: Events flow up, State flows down
2. **Single Source of Truth**: Room database is the source
3. **Separation of Concerns**: Clear layer boundaries
4. **Dependency Rule**: Inner layers know nothing about outer layers
5. **Testability**: Pure functions, injected dependencies

---

## 3. Input/Output ViewModel Pattern

### Pattern Benefits

1. **Clear Contract**: Input (what user does) vs Output (what UI shows)
2. **Type Safety**: Compiler-checked sealed interfaces
3. **Testability**: Easy to mock inputs and verify outputs
4. **Maintainability**: New events/states don't break existing code
5. **Scalability**: Pattern works for simple and complex screens

### Complete Implementation Guide

See `docs/VIEWMODEL_PATTERN.md` for full implementation details including:
- Pattern structure
- Best practices
- Migration guide
- Testing strategies

**Key Rules:**
- ✅ All events as `Input` sealed interface
- ✅ UI state as `Output.State` data class
- ✅ One-time events as `Output.Effect` sealed interface
- ✅ Single `onEvent(input: Input)` method
- ✅ State via `StateFlow`, Effects via `Channel`

---

## 4. Input Validation

### Real-Time Validation Pattern

Following Android's official Compose validation guide:

```kotlin
@Composable
fun UserDialog() {
    var firstName by remember { mutableStateOf("") }
    var firstNameTouched by remember { mutableStateOf(false) }

    // Efficient validation with derivedStateOf
    val firstNameError by remember {
        derivedStateOf {
            when {
                !firstNameTouched -> null
                firstName.isBlank() -> "First name is required"
                !UserValidator.isValidName(firstName) ->
                    "First name is too long (max 100 characters)"
                else -> null
            }
        }
    }

    OutlinedTextField(
        value = firstName,
        onValueChange = {
            firstName = it
            firstNameTouched = true
        },
        isError = firstNameError != null,
        supportingText = firstNameError?.let { { Text(it) } }
    )

    val isFormValid by remember {
        derivedStateOf {
            firstNameError == null &&
            lastNameError == null &&
            emailError == null
        }
    }

    Button(
        onClick = { viewModel.onEvent(UserViewModel.Input.CreateUser(user)) },
        enabled = isFormValid
    ) {
        Text("Create")
    }
}
```

### Validation Rules

| Field | Rules | Errors |
|-------|-------|--------|
| First Name | Required, max 100 chars | "Required", "Too long" |
| Last Name | Required, max 100 chars | "Required", "Too long" |
| Email | Required, RFC-compliant | "Required", "Invalid format" |
| Avatar | Required selection | Pre-validated |

### Validators Location

- **Presentation Layer**: `domain/validation/UserValidator.kt`
- Used directly by UI for real-time feedback
- Domain layer also uses for business logic validation

---

## 5. Analytics System

### AOP-Based Analytics

**Declarative tracking with annotations:**

```kotlin
@TrackScreen(AnalyticsScreens.HOME)
class HomeViewModel @Inject constructor(
    analyticsTracker: AnalyticsTracker
) : AnalyticsViewModel(analyticsTracker) {

    fun loadUsers() {
        userService.getUsers()
            .trackFlow(
                analyticsTracker = analyticsTracker,
                eventName = Events.PAGE_LOADED,
                trackPerformance = true,
                trackErrors = true
            )
            .collect { users -> /* ... */ }
    }

    fun createUser(user: User) {
        trackCrudOperation(
            operation = CrudOperation.CREATE,
            entity = "User",
            params = mapOf(Params.USER_NAME to user.name)
        ) {
            userService.createUser(user)
        }
    }
}
```

### Features

- ✅ **Screen View Tracking**: Automatic via `@TrackScreen`
- ✅ **Performance Metrics**: Page load times, operation duration
- ✅ **Error Tracking**: Comprehensive error logging
- ✅ **Offline Support**: Events persisted locally, uploaded when online
- ✅ **Batch Upload**: Efficient batch uploads every 6 hours
- ✅ **Zero Boilerplate**: ~70% less analytics code

---

## 6. Offline-First Implementation

### Architecture Flow

```
CREATE User (Offline)
    ↓
Repository.createUser()
    ├─ Insert to Room immediately (optimistic UI)
    ├─ Queue UserChange (type=CREATE)
    └─ Return success
    ↓
UI shows new user instantly
    ↓
[Network becomes available]
    ↓
SyncWorker runs
    ├─ Process queued changes
    ├─ POST to API
    ├─ Delete from queue if success
    └─ Fetch latest data
```

### Key Components

**1. NetworkMonitor**
```kotlin
interface NetworkMonitor {
    val isOnline: StateFlow<Boolean>
}
```

**2. UserChange Entity**
```kotlin
@Entity(tableName = "user_changes")
data class UserChange(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int?,
    val changeType: ChangeType, // CREATE, UPDATE, DELETE
    val userData: String?,
    val timestamp: Long
)
```

**3. OfflineFirstDataRepository**
- Immediate local updates
- Queue changes when offline
- Sync when online

**4. SyncWorker**
- Processes offline queue
- Fetches latest data
- Runs periodically via WorkManager

---

## 7. Pagination & Caching

### Pagination Strategy

```kotlin
Output.State(
    val userPages: Map<Int, List<User>> = emptyMap(),
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false
) {
    val users: List<User>
        get() = userPages[currentPage] ?: emptyList()

    val allUsers: List<User>
        get() = userPages.entries
            .sortedBy { it.key }
            .flatMap { it.value }
}
```

### Cache System

**LRU Cache with TTL:**

```kotlin
class LruTtlCache<K, V>(
    maxSize: Int,
    private val ttlMillis: Long
) {
    private val cache = LruCache<K, CacheEntry<V>>(maxSize)

    data class CacheEntry<V>(
        val value: V,
        val timestamp: Long
    )

    fun get(key: K): V? {
        val entry = cache.get(key) ?: return null
        if (System.currentTimeMillis() - entry.timestamp > ttlMillis) {
            cache.remove(key)
            return null
        }
        return entry.value
    }
}
```

**Features:**
- LRU eviction policy
- TTL expiration (5 minutes default)
- Event-driven invalidation
- Automatic cleanup

---

## 8. UI Theme - Arcana

### Color Palette

```kotlin
// Arcana Theme Colors
val ArcanaPurple = Color(0xFF6B46C1)        // Deep mystical purple
val ArcanaIndigo = Color(0xFF4C1D95)        // Dark indigo
val ArcanaViolet = Color(0xFF8B5CF6)        // Bright violet
val ArcanaGold = Color(0xFFFBBF24)          // Arcane gold
val ArcanaAmber = Color(0xFFF59E0B)         // Mystical amber
val ArcanaCyan = Color(0xFF06B6D4)          // Magical cyan
val ArcanaBackground = Color(0xFF1A0B2E)    // Deep purple background
val ArcanaBackgroundLight = Color(0xFF2D1B4E)
val ArcanaGlow = Color(0xFFA78BFA)          // Glowing purple
```

### HomeScreen Example

```kotlin
@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        ArcanaBackground,
                        ArcanaIndigo,
                        ArcanaPurple
                    )
                )
            )
    ) {
        // Decorative glowing orbs
        Box(
            modifier = Modifier
                .size(200.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ArcanaViolet.copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Content with gold accents
        Text(
            text = "✨ Arcana ✨",
            color = ArcanaGold,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
```

### Custom Launcher Icon

**Background**: Deep purple gradient with mystical circles and stars
**Foreground**: Golden "A" with arcane circle and symbols
**Theme**: Mystical, magical, professional

---

## 9. Configuration Snapshot

### Library Versions (gradle/libs.versions.toml)

```toml
[versions]
android-gradle-plugin = "8.13.0"
kotlin = "2.2.21"
compose-bom = "2025.11.00"
hilt = "2.57.2"
navigation-compose = "2.9.6"
coroutines = "1.10.2"
room = "2.8.3"
ktorfit = "2.6.4"
ktor = "3.3.2"
work = "2.11.0"
ksp = "2.3.2"
```

### app/build.gradle.kts Key Settings

```kotlin
android {
    namespace = "com.example.arcana"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.arcana"
        minSdk = 28
        targetSdk = 36
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.ktorfit.lib)

    testImplementation(libs.junit)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.turbine)
}
```

---

## 10. API Documentation Setup

### Dokka Configuration

```kotlin
plugins {
    id("org.jetbrains.dokka") version "2.0.0"
}

tasks.dokkaHtml {
    moduleName.set("Arcana Android")
    outputDirectory.set(file("build/docs/api"))

    dokkaSourceSets {
        configureEach {
            includeNonPublic.set(false)
            skipEmptyPackages.set(true)
        }
    }
}
```

### Generate Documentation

```bash
# Generate API docs (auto-copied to docs/api/)
./gradlew generateApiDocs

# Generate architecture diagrams
./gradlew generateMermaidDiagrams

# Output locations:
# - docs/api/index.html
# - docs/diagrams/*.png
```

### Architecture Diagrams

Located in `docs/architecture/*.mmd` (Mermaid format):
1. Overall Architecture
2. Clean Architecture Layers
3. Caching System
4. Data Flow
5. Offline-First Sync
6. Dependency Graph

---

## 11. Testing Strategy

### Test Coverage: 100% Business Logic

**256/256 tests passing** across:

- **Domain Validation**: 79 tests (UserValidator, EmailAddress)
- **Error Handling**: 52 tests (AppError, RetryPolicy)
- **Retry Policy**: 26 tests
- **UI Layer**: 49 tests (ViewModels, Screens)
- **Service Layer**: 25 tests
- **Repository Layer**: 25 tests

### Test Structure

```kotlin
@Test
fun `createUser should emit success effect when successful`() = runTest {
    // Given
    whenever(userService.createUser(testUser))
        .thenReturn(true)

    // When
    viewModel.onEvent(UserViewModel.Input.CreateUser(testUser))

    // Then
    viewModel.effect.test {
        advanceUntilIdle()
        val effect = awaitItem()
        assertTrue(effect is UserViewModel.Output.Effect.ShowSuccess)
    }
}
```

### Testing Tools

- **JUnit 4**: Test framework
- **Mockito + Mockito-Kotlin**: Mocking
- **Coroutines Test**: Async testing with `runTest`
- **Turbine**: Flow testing

---

## 12. Key Improvements & Fixes

### Implemented Improvements

1. **Input/Output Pattern**: Structured ViewModel architecture
2. **Offline-First**: Complete offline CRUD with sync
3. **Pagination**: Infinite scroll with page caching
4. **Real-time Validation**: derivedStateOf for efficiency
5. **AOP Analytics**: Declarative tracking system
6. **Arcana Theme**: Professional mystical UI
7. **Error Handling**: RetryPolicy with exponential backoff
8. **Cache System**: LRU + TTL for performance
9. **API Documentation**: Auto-generated Dokka docs
10. **Testing**: 100% coverage for business logic

### Bug Fixes Applied

- **Cache Fix**: LRU cache with proper TTL and invalidation
- **Sync Fix**: HomeScreen sync properly queued and executed
- **Pagination Fix**: Offline pagination with proper page tracking
- **User Count Fix**: Accurate total count from API
- **Validation Fix**: Real-time validation without unnecessary re-renders

---

## Summary

This document consolidates all project documentation and serves as the complete specification for:

1. **Generating** a new Arcana Android application
2. **Understanding** the architecture and patterns
3. **Implementing** features following established patterns
4. **Testing** with proper coverage and tools
5. **Documenting** with auto-generated API docs

**Key Patterns to Follow:**
- ✅ Input/Output ViewModels
- ✅ Offline-First Repository
- ✅ Real-time Input Validation
- ✅ AOP Analytics
- ✅ Arcana Theme Consistency

**References:**
- Architecture Diagrams: `docs/diagrams/*.png`
- ViewModel Pattern: `docs/VIEWMODEL_PATTERN.md`
- API Docs: `docs/api/index.html` (after build)
- Mermaid Sources: `docs/architecture/*.mmd`

---

*Generated from all project .md files on November 14, 2024*
