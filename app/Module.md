# Module Arcana Android

## Overview

This is a modern Android application built with Kotlin and Jetpack Compose, demonstrating best practices for Android development including:

- **Clean Architecture**: Separation of concerns with data, domain, and UI layers
- **Offline-First**: Full offline support with local database and background sync
- **MVVM Pattern**: ViewModel-based state management with StateFlow
- **Dependency Injection**: Using Hilt for dependency management
- **Reactive Programming**: Kotlin Coroutines and Flow for asynchronous operations
- **Modern UI**: Jetpack Compose for declarative UI development

## Architecture Layers

### Data Layer (`com.example.arcana.data`)
- **Repository Pattern**: Manages data sources (local database, network API)
- **Caching Strategy**: LRU cache with TTL for optimal performance
- **Offline Support**: SQLite Room database for local persistence
- **Network**: Ktor client with Ktorfit for REST API communication

### Domain Layer (`com.example.arcana.domain`)
- **Services**: Business logic and application services for data operations
- **Models**: Core business entities and value objects
- **Validation**: Input validators for domain entities

### UI Layer (`com.example.arcana.ui`)
- **Jetpack Compose**: Modern declarative UI framework
- **ViewModels**: State management and business logic coordination
- **Navigation**: Navigation Compose for app navigation

### Core Layer (`com.example.arcana.core`)
- **Common Utilities**: Shared utilities and helpers
- **String Provider**: Abstraction for string resources
- **Network Monitor**: Connectivity state monitoring

### Sync Layer (`com.example.arcana.sync`)
- **Background Sync**: WorkManager-based background synchronization
- **Conflict Resolution**: Last-write-wins strategy for data conflicts
- **Cache Invalidation**: Event-driven cache management

## Key Features

### Cache Management
The application implements a sophisticated caching system with:
- **LRU Cache**: Least-recently-used eviction policy
- **TTL Support**: 5-minute time-to-live for cached data
- **Event-Based Invalidation**: SharedFlow-based cache invalidation events
- **Synchronous Sync**: Deterministic cache updates during sync

### Offline-First Architecture
- All user data synced and available offline
- Pagination works offline using local database
- Background sync when network becomes available
- Conflict resolution for concurrent edits

### Data Synchronization
- Fetches all pages of data during sync
- Stores complete dataset in local database
- Handles create, update, delete operations offline
- Queues offline changes and syncs when online

## Technology Stack

- **Language**: Kotlin 2.2.21
- **UI Framework**: Jetpack Compose
- **DI**: Hilt (Dagger)
- **Database**: Room 2.8.3
- **Networking**: Ktor 3.3.2 + Ktorfit 2.6.4
- **Background Work**: WorkManager 2.11.0
- **Async**: Coroutines 1.10.2
- **Navigation**: Navigation Compose 2.9.6
- **Logging**: Timber 5.0.1
- **Image Loading**: Coil 2.7.0

## Package Structure

```
com.example.arcana
├── core/                   # Core utilities and common code
│   └── common/            # Common helpers
├── data/                   # Data layer
│   ├── local/             # Room database and DAOs
│   ├── model/             # Data models
│   ├── network/           # Network data sources
│   ├── remote/            # API service and DTOs
│   └── repository/        # Repository implementations
├── di/                     # Dependency injection modules
├── domain/                 # Domain layer
│   └── service/           # Domain services
├── sync/                   # Synchronization logic
└── ui/                     # UI layer
    └── screens/           # Compose screens and ViewModels
```

## Getting Started

### Building the Project
Use the following command to build:
```
./gradlew assembleDebug
```

This will compile the source code, run unit tests, and generate this API documentation.

### Running Tests
```
./gradlew test
```

### Generating Documentation Manually
```
./gradlew generateApiDocs
```

Documentation will be generated at: `app/build/docs/api/index.html`

## Documentation Conventions

All public APIs should be documented with KDoc comments including parameter descriptions, return values, and exceptions thrown.

## License

This project is developed for educational and demonstration purposes.
