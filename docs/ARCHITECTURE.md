# Architecture Overview

## Table of Contents
- [Architecture Principles](#architecture-principles)
- [Architecture Diagrams](#architecture-diagrams)
- [Layer Descriptions](#layer-descriptions)
- [Key Patterns](#key-patterns)
- [Data Flow](#data-flow)
- [Analytics System](#analytics-system)

## Architecture Principles

This Android application follows **Clean Architecture** principles with an **Offline-First** approach, ensuring a robust, maintainable, and scalable codebase.

### Core Principles:
1. **Separation of Concerns** - Clear boundaries between UI, Business Logic, and Data layers
2. **Dependency Rule** - Dependencies point inward (UI → Domain → Data)
3. **Testability** - Each layer can be tested independently
4. **Offline-First** - App works without network, syncs when available
5. **Reactive Patterns** - Data flows reactively using Kotlin Flows
6. **AOP Analytics** - Cross-cutting analytics without cluttering business logic

## Architecture Diagrams

All diagrams are available in Mermaid format in `/docs/architecture/`:

1. **[Overall Architecture](architecture/01-overall-architecture.mmd)** - High-level system overview
2. **[Clean Architecture Layers](architecture/02-clean-architecture-layers.mmd)** - Layer separation and responsibilities
3. **[Analytics System](architecture/03-analytics-system.mmd)** - AOP-based analytics architecture
4. **[Data Flow](architecture/04-data-flow.mmd)** - Sequence diagram showing data flow
5. **[Offline-First Sync](architecture/05-offline-first-sync.mmd)** - Sync strategy and conflict resolution
6. **[Dependency Graph](architecture/06-dependency-graph.mmd)** - Component dependencies

### Viewing Diagrams

**Option 1: Generate PNG (Recommended)**
```bash
./gradlew generateMermaidDiagrams
```
Output: `app/build/docs/diagrams/*.png`

**Option 2: View Online**
1. Copy content from `.mmd` files
2. Paste into [Mermaid Live Editor](https://mermaid.live)

**Option 3: Import to Draw.io**
1. Open [draw.io](https://app.diagrams.net)
2. File → Import from → Mermaid
3. Select `.mmd` files

## Layer Descriptions

### 1. Presentation Layer (`ui/`)
**Responsibility**: User Interface and User Interaction

- **Jetpack Compose** for declarative UI
- **ViewModels** for UI state management (MVVM pattern)
- **UI State Classes** for immutable state
- **Navigation** using Jetpack Navigation Compose

**Key Components**:
- `HomeScreen` / `HomeViewModel` - User list overview
- `UserScreen` / `UserViewModel` - User CRUD operations
- `NavGraph` - Navigation configuration

**Patterns**:
- MVVM (Model-View-ViewModel)
- Unidirectional Data Flow (UDF)
- Single Source of Truth

### 2. Domain Layer (`domain/`)
**Responsibility**: Business Logic and Services

- **UserService** - Orchestrates user-related operations
- **Repository Interfaces** - Abstract data sources
- **Domain Models** - Business entities (User, etc.)

**Key Principle**: This layer contains **zero Android dependencies** and can be tested with pure unit tests.

### 3. Data Layer (`data/`)
**Responsibility**: Data Management and Persistence

#### Components:

**Repository (`repository/`)**:
- `OfflineFirstDataRepository` - Implements offline-first strategy
- `CacheManager` - LRU cache with TTL
- `CacheEventBus` - Cache invalidation events

**Local Storage (`local/`)**:
- Room Database for persistence
- DAOs for data access
- Entities for database schema

**Remote API (`remote/` & `network/`)**:
- Ktorfit for type-safe API calls
- Ktor Client for HTTP networking
- Network data sources

**Patterns**:
- Repository Pattern
- Offline-First Architecture
- Cache-Aside Pattern
- Event-Driven Cache Invalidation

### 4. Cross-Cutting Concerns (`core/`)

**Analytics (`core/analytics/`)**:
- AOP-based analytics tracking
- Annotations for declarative tracking
- Local persistence + batch upload
- WorkManager for background sync

**Infrastructure (`core/common/`)**:
- Dependency Injection (Hilt)
- Network monitoring
- Background workers
- Utilities

## Key Patterns

### 1. Offline-First Architecture

```
User Action → Check Network
    ├─ Online  → API Call → Update Local → Sync Cache
    └─ Offline → Save Local → Queue Change → Return Optimistically

Background Sync (When Online):
    → Get Queued Changes → Apply to API → Update Local → Clear Queue
```

**Benefits**:
- ✅ App works without internet
- ✅ Fast response times (local-first)
- ✅ Automatic sync when online
- ✅ Conflict resolution

### 2. Cache Strategy

**LRU Cache with TTL** (Least Recently Used + Time To Live):
```kotlin
CacheManager(
    maxSize = 50,        // Max 50 cached items
    ttlMillis = 5 minutes // Expire after 5 minutes
)
```

**Cache Invalidation**:
- Event-driven invalidation using `CacheEventBus`
- Automatic cleanup of stale data
- Granular invalidation (specific items vs all)

### 3. AOP Analytics System

**Aspect-Oriented Programming** for analytics:

```kotlin
@TrackScreen(AnalyticsScreens.HOME)
class HomeViewModel @Inject constructor(
    analyticsTracker: AnalyticsTracker
) : AnalyticsViewModel(analyticsTracker) {

    private fun loadData() {
        userService.getUsers()
            .trackFlow(
                analyticsTracker = analyticsTracker,
                eventName = Events.PAGE_LOADED,
                trackPerformance = true
            )
            .collect { /* ... */ }
    }
}
```

**Benefits**:
- ✅ No analytics clutter in business logic
- ✅ Declarative tracking with annotations
- ✅ Automatic performance metrics
- ✅ Centralized analytics logic

### 4. MVVM with Unidirectional Data Flow

```
User Action → ViewModel → Use Case → Repository → Data Source
                ↓
            UI State ← Flow ← Data Source
```

**Components**:
- **State**: Immutable data classes (`HomeUIState`)
- **Events**: One-time effects (`HomeUIEvent`)
- **Actions**: User interactions handled by ViewModel

## Data Flow

### Read Flow (Fetching Data)
```
UI → ViewModel → Service → Repository
    ↓
Local DB (Immediate Response)
    ↓
Cache Check
    ↓
API Call (If cache miss/expired)
    ↓
Update Local DB & Cache
    ↓
UI Updates Automatically (Flow)
```

### Write Flow (Creating/Updating Data)

**When Online**:
```
UI → ViewModel → Service → Repository
    → API Call → Success → Sync Data → Update Local
```

**When Offline**:
```
UI → ViewModel → Service → Repository
    → Save to Local → Queue Change → Return Success (Optimistic)

Later (When Online):
    Background Worker → Process Queue → Apply Changes to API
```

## Analytics System

### Architecture

The analytics system uses **Aspect-Oriented Programming (AOP)** to separate tracking concerns from business logic.

**Key Components**:

1. **Annotations** (`@TrackScreen`, `@TrackAction`, etc.)
2. **Base Class** (`AnalyticsViewModel`)
3. **Extension Functions** (`trackFlow()`, `trackSync()`, `trackCrud()`)
4. **Navigation Observer** (Automatic screen tracking)
5. **Persistent Storage** (Room database)
6. **Batch Upload** (WorkManager every 6 hours)

### Event Types

- **Screen Views** - Automatic via `@TrackScreen` + NavigationObserver
- **User Actions** - CRUD operations, clicks, etc.
- **Performance** - Page load times, operation duration
- **Errors** - Exceptions with context
- **Lifecycle** - App open/close, screen enter/exit
- **Network** - API calls, connection status

### Workflow

```
User Action → Analytics Event Created
    ↓
Persisted to Local DB
    ↓
Batched (up to 100 events)
    ↓
WorkManager (Every 6 hours)
    ↓
Upload to Analytics API
    ↓
Mark as Uploaded & Cleanup
```

## Technology Stack

### Core
- **Kotlin** - Programming language
- **Coroutines & Flow** - Async/reactive programming
- **Jetpack Compose** - UI framework

### Architecture
- **Hilt** - Dependency injection
- **Room** - Local database
- **WorkManager** - Background tasks

### Networking
- **Ktorfit** - Type-safe HTTP client
- **Ktor** - HTTP engine
- **Kotlinx Serialization** - JSON parsing

### Testing
- **JUnit 4** - Unit testing framework
- **Mockito** - Mocking
- **Turbine** - Flow testing
- **Coroutines Test** - Async testing

### Documentation
- **Dokka** - API documentation
- **Mermaid** - Architecture diagrams

## Best Practices

### 1. Dependency Injection
- All dependencies injected via Hilt
- Constructor injection preferred
- No service locators

### 2. Immutability
- UI State classes are immutable (`data class`)
- Use `copy()` for state updates
- Flows for reactive data

### 3. Error Handling
- Result types for operations that can fail
- Sealed classes for different states
- Comprehensive error tracking

### 4. Testing
- Repository tested with mocks
- ViewModel tested with fake data
- UI tested with Compose testing utilities

### 5. Performance
- LRU cache reduces API calls
- Lazy loading with pagination
- Background sync doesn't block UI
- Room database for fast local queries

## Future Enhancements

- [ ] Multi-module architecture
- [ ] Feature modules with dynamic delivery
- [ ] Offline-first for media/images
- [ ] Real-time sync with WebSockets
- [ ] Encryption for sensitive data
- [ ] Advanced conflict resolution strategies
