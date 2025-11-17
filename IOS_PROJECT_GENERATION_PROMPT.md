# Arcana iOS - Complete Project Generation Prompt

> Generate a modern iOS application with **Clean Architecture**, **Offline-First** design, and **Comprehensive Analytics** using SwiftUI and modern iOS technologies.

---

## 🎯 Project Overview

Create an iOS version of the Arcana Android app that maintains the same architecture, patterns, and features while using iOS-native technologies. The app should be production-ready with 100% test coverage for business logic.

**Project Name**: Arcana iOS
**Bundle ID**: com.example.arcana.ios
**Minimum iOS Version**: iOS 16.0
**Language**: Swift 5.9+
**UI Framework**: SwiftUI
**Architecture**: Clean Architecture + MVVM + Input/Output Pattern

---

## 📋 Core Requirements

### Features to Implement

#### Core Functionality
- ✅ **User Management** - Complete CRUD operations (Create, Read, Update, Delete)
- ✅ **Offline-First** - Full functionality without internet connection
- ✅ **Auto-Sync** - Background synchronization when online
- ✅ **Smart Caching** - LRU cache with TTL for optimal performance
- ✅ **Pagination** - Efficient data loading with page navigation
- ✅ **Real-time Updates** - Reactive UI with Combine/AsyncSequence

#### Advanced Features
- 📊 **Analytics Tracking** - Comprehensive user behavior tracking with error codes
- 🔄 **Background Sync** - BackgroundTasks framework for automatic sync
- 📱 **Modern UI** - Beautiful SwiftUI interface with Arcana theme
- ✅ **Input Validation** - Real-time form validation with user-friendly errors
- 🎯 **Type-Safe Navigation** - NavigationStack with type-safe routing
- 💾 **Persistent Storage** - Core Data / SwiftData
- 🌐 **RESTful API** - URLSession + async/await
- 🧪 **Well-Tested** - 100% test coverage for business logic
- 🏗️ **Input/Output Pattern** - Clean ViewModel architecture
- 📝 **Error Code System** - Standardized error codes (E####, W####)

---

## 🏗 Architecture

### Clean Architecture Layers

```
┌─────────────────────────────────────────────────────────────┐
│                   Presentation Layer                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │   SwiftUI    │→ │  ViewModels  │→ │  ViewStates  │       │
│  │    Views     │  │   (MVVM)     │  │  (Published) │       │
│  └──────┬───────┘  └──────────────┘  └──────────────┘       │
│         ↓                                                   │
│  ┌──────────────┐                                           │
│  │  Validation  │                                           │
│  │   & Value    │                                           │
│  │   Objects    │                                           │
│  └──────────────┘                                           │
└────────────────────────┬────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────────┐
│                      Domain Layer                           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │   Services   │→ │Business Logic│→ │Domain Models │       │
│  │  (Protocols) │  │ (Use Cases)  │  │  (Structs)   │       │
│  └──────────────┘  └──────────────┘  └──────────────┘       │
└────────────────────────┬────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────────┐
│                       Data Layer                            │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │ Repository   │→ │  Core Data   │  │  URLSession  │       │
│  │(Offline-1st) │  │  (SwiftData) │  │   (async)    │       │
│  └──────────────┘  └──────────────┘  └──────────────┘       │
└─────────────────────────────────────────────────────────────┘
```

### Key Patterns

#### 1. Input/Output ViewModel Pattern

```swift
@MainActor
final class UserViewModel: ObservableObject {

    // MARK: - Input
    enum Input {
        case loadInitial
        case createUser(User)
        case updateUser(User)
        case deleteUser(User)
        case refresh
    }

    // MARK: - Output
    struct Output {
        @Published var users: [User] = []
        @Published var isLoading: Bool = false
        @Published var errorMessage: String?
    }

    enum Effect {
        case showError(AppError)
        case showSuccess(String)
        case navigateTo(Route)
    }

    @Published private(set) var state = Output()
    let effects = PassthroughSubject<Effect, Never>()

    private let userService: UserService
    private let analyticsTracker: AnalyticsTracker
    private var cancellables = Set<AnyCancellable>()

    init(userService: UserService, analyticsTracker: AnalyticsTracker) {
        self.userService = userService
        self.analyticsTracker = analyticsTracker
    }

    func send(_ input: Input) {
        Task {
            switch input {
            case .loadInitial:
                await loadUsers()
            case .createUser(let user):
                await createUser(user)
            case .updateUser(let user):
                await updateUser(user)
            case .deleteUser(let user):
                await deleteUser(user)
            case .refresh:
                await refresh()
            }
        }
    }

    private func loadUsers() async {
        state.isLoading = true
        defer { state.isLoading = false }

        do {
            let users = try await userService.getUsers()
            state.users = users
            analyticsTracker.trackEvent(.pageLoaded, params: ["count": users.count])
        } catch {
            let appError = AppError.from(error)
            effects.send(.showError(appError))
            analyticsTracker.trackAppError(appError)
        }
    }
}
```

#### 2. Offline-First Repository

```swift
protocol UserRepository {
    func getUsers() async throws -> [User]
    func createUser(_ user: User) async throws -> User
    func updateUser(_ user: User) async throws -> User
    func deleteUser(_ user: User) async throws
}

final class OfflineFirstUserRepository: UserRepository {
    private let localDataSource: LocalUserDataSource
    private let remoteDataSource: RemoteUserDataSource
    private let syncManager: SyncManager
    private let cache: LRUCache<String, User>

    func getUsers() async throws -> [User] {
        // Try cache first
        if let cached = cache.getAll(), !cached.isEmpty {
            return cached
        }

        // Try local database
        let localUsers = try await localDataSource.getUsers()
        if !localUsers.isEmpty {
            cache.setAll(localUsers)

            // Sync in background
            Task.detached {
                try? await self.syncUsers()
            }

            return localUsers
        }

        // Fetch from remote
        do {
            let remoteUsers = try await remoteDataSource.getUsers()
            try await localDataSource.saveUsers(remoteUsers)
            cache.setAll(remoteUsers)
            return remoteUsers
        } catch {
            throw AppError.from(error)
        }
    }

    func createUser(_ user: User) async throws -> User {
        // Optimistic update
        try await localDataSource.createUser(user)
        cache.set(user, forKey: user.id)

        do {
            // Try remote
            let createdUser = try await remoteDataSource.createUser(user)
            try await localDataSource.updateUser(createdUser)
            cache.set(createdUser, forKey: createdUser.id)
            return createdUser
        } catch {
            // Queue for sync if offline
            if case .networkError = AppError.from(error) {
                await syncManager.queueChange(.create(user))
                return user
            }
            throw error
        }
    }
}
```

#### 3. Error Code System

```swift
// ErrorCode.swift
enum ErrorCode {
    // Network Errors (E1000-E1999)
    case E1000_NO_CONNECTION
    case E1001_CONNECTION_TIMEOUT
    case E1002_UNKNOWN_HOST
    case E1003_NETWORK_IO
    case E1004_SSL_ERROR

    // Validation Errors (E2000-E2999)
    case E2000_VALIDATION_FAILED
    case E2001_INVALID_EMAIL
    case E2002_INVALID_NAME
    case E2003_REQUIRED_FIELD
    case E2004_FIELD_TOO_LONG
    case E2005_FIELD_TOO_SHORT

    // Server Errors (E3000-E3999)
    case E3000_SERVER_ERROR
    case E3001_BAD_REQUEST
    case E3002_NOT_FOUND
    case E3003_SERVICE_UNAVAILABLE
    case E3004_RATE_LIMITED

    // Auth Errors (E4000-E4999)
    case E4000_AUTH_REQUIRED
    case E4001_UNAUTHORIZED
    case E4002_FORBIDDEN
    case E4003_SESSION_EXPIRED
    case E4004_TOKEN_INVALID

    // Data Errors (E5000-E5999)
    case E5000_DATA_CONFLICT
    case E5001_STALE_DATA
    case E5002_DUPLICATE_ENTRY
    case E5003_CONSTRAINT_VIOLATION

    // Database Errors (E6000-E6999)
    case E6000_DATABASE_ERROR
    case E6001_QUERY_FAILED
    case E6002_TRANSACTION_FAILED
    case E6003_MIGRATION_FAILED

    // System Errors (E9000-E9999)
    case E9000_UNKNOWN
    case E9001_UNEXPECTED_STATE
    case E9002_NULL_POINTER
    case E9003_SERIALIZATION_ERROR

    // Warnings (W1000-W3999)
    case W1000_SLOW_CONNECTION
    case W1001_OFFLINE_MODE
    case W1002_SYNC_PENDING
    case W2000_INCOMPLETE_DATA
    case W2001_DEPRECATED_FORMAT
    case W3000_STALE_CACHE
    case W3001_PARTIAL_SYNC
    case W3002_DATA_TRUNCATED

    var code: String {
        return String(describing: self).components(separatedBy: "_").first ?? ""
    }

    var description: String {
        switch self {
        case .E1000_NO_CONNECTION: return "No internet connection available"
        case .E1001_CONNECTION_TIMEOUT: return "Connection attempt timed out"
        case .E1002_UNKNOWN_HOST: return "Unable to resolve host address"
        // ... all other cases
        default: return "Unknown error"
        }
    }

    var category: String {
        let numericCode = code.filter { $0.isNumber }
        guard let firstDigit = numericCode.first?.wholeNumberValue else { return "Unknown" }

        switch firstDigit {
        case 1: return "Network"
        case 2: return "Validation"
        case 3: return "Server"
        case 4: return "Authentication"
        case 5: return "Data"
        case 6: return "Database"
        case 9: return "System"
        default: return "Unknown"
        }
    }
}

// AppError.swift
enum AppError: Error {
    case networkError(ErrorCode, message: String, isRetryable: Bool, underlyingError: Error?)
    case validationError(ErrorCode, field: String, message: String)
    case serverError(ErrorCode, statusCode: Int, message: String)
    case authError(ErrorCode, message: String)
    case conflictError(ErrorCode, message: String)
    case unknownError(ErrorCode, message: String, underlyingError: Error)

    var errorCode: ErrorCode {
        switch self {
        case .networkError(let code, _, _, _): return code
        case .validationError(let code, _, _): return code
        case .serverError(let code, _, _): return code
        case .authError(let code, _): return code
        case .conflictError(let code, _): return code
        case .unknownError(let code, _, _): return code
        }
    }

    static func from(_ error: Error) -> AppError {
        if let appError = error as? AppError {
            return appError
        }

        if let urlError = error as? URLError {
            switch urlError.code {
            case .notConnectedToInternet, .networkConnectionLost:
                return .networkError(.E1000_NO_CONNECTION, message: "No internet connection", isRetryable: true, underlyingError: urlError)
            case .timedOut:
                return .networkError(.E1001_CONNECTION_TIMEOUT, message: "Connection timed out", isRetryable: true, underlyingError: urlError)
            case .cannotFindHost:
                return .networkError(.E1002_UNKNOWN_HOST, message: "Cannot find host", isRetryable: true, underlyingError: urlError)
            default:
                return .networkError(.E1003_NETWORK_IO, message: "Network error", isRetryable: true, underlyingError: urlError)
            }
        }

        return .unknownError(.E9000_UNKNOWN, message: error.localizedDescription, underlyingError: error)
    }
}
```

#### 4. Analytics with Error Codes

```swift
protocol AnalyticsTracker {
    func trackEvent(_ event: AnalyticsEvent, params: [String: Any])
    func trackScreen(_ screen: String, params: [String: Any])
    func trackError(_ error: Error, context: [String: Any])
    func trackAppError(_ appError: AppError, context: [String: Any])
}

final class PersistentAnalyticsTracker: AnalyticsTracker {
    private let coreDataStack: CoreDataStack
    private let sessionId = UUID().uuidString

    func trackAppError(_ appError: AppError, context: [String: Any]) {
        var params = context

        // Add error code information
        params["error_code"] = appError.errorCode.code
        params["error_code_description"] = appError.errorCode.description
        params["error_code_category"] = appError.errorCode.category

        // Add error-specific info
        switch appError {
        case .networkError(_, let message, let isRetryable, let underlying):
            params["error_type"] = "NetworkError"
            params["error_message"] = message
            params["is_retryable"] = isRetryable
            if let error = underlying {
                params["underlying_error"] = String(describing: error)
            }

        case .validationError(_, let field, let message):
            params["error_type"] = "ValidationError"
            params["field"] = field
            params["error_message"] = message

        case .serverError(_, let statusCode, let message):
            params["error_type"] = "ServerError"
            params["http_status_code"] = statusCode
            params["error_message"] = message
            params["is_retryable"] = (500...599).contains(statusCode)

        case .authError(_, let message):
            params["error_type"] = "AuthError"
            params["error_message"] = message
            params["is_retryable"] = false

        case .conflictError(_, let message):
            params["error_type"] = "ConflictError"
            params["error_message"] = message
            params["is_retryable"] = true

        case .unknownError(_, let message, let underlying):
            params["error_type"] = "UnknownError"
            params["error_message"] = message
            params["underlying_error"] = String(describing: underlying)
        }

        // Store event in Core Data
        let event = AnalyticsEventEntity(context: coreDataStack.context)
        event.eventId = UUID().uuidString
        event.eventType = "ERROR"
        event.eventName = "error_occurred"
        event.timestamp = Date()
        event.sessionId = sessionId
        event.params = try? JSONSerialization.data(withJSONObject: params)

        try? coreDataStack.context.save()

        print("❌ AppError tracked: [\(appError.errorCode.code)] \(appError.errorCode.description)")
    }
}
```

---

## 📦 Technology Stack Mapping

### Android → iOS Equivalents

| Android | iOS | Purpose |
|---------|-----|---------|
| **Kotlin** | **Swift** | Programming language |
| **Jetpack Compose** | **SwiftUI** | Declarative UI framework |
| **Coroutines + Flow** | **async/await + Combine** | Asynchronous programming |
| **Hilt** | **Dependency Injection Protocol** | Dependency injection |
| **Room** | **Core Data / SwiftData** | Local database |
| **Ktorfit + Ktor** | **URLSession + Codable** | Networking |
| **WorkManager** | **BackgroundTasks** | Background tasks |
| **Navigation Compose** | **NavigationStack** | Type-safe navigation |
| **ViewModel** | **ObservableObject** | View state management |
| **StateFlow** | **@Published** | Reactive state |
| **Channel** | **PassthroughSubject** | One-time events |
| **JUnit** | **XCTest** | Unit testing |
| **Mockito** | **Protocol Mocks** | Test doubles |
| **Turbine** | **Combine TestScheduler** | Flow testing |
| **Timber** | **OSLog / SwiftLog** | Logging |
| **Gradle** | **Swift Package Manager / CocoaPods** | Build system |
| **Dokka** | **DocC** | Documentation generation |

---

## 📁 Project Structure

```
ArcanaIOS/
├── ArcanaIOS/
│   ├── Core/                           # Cross-cutting concerns
│   │   ├── Analytics/                  # Analytics system
│   │   │   ├── AnalyticsTracker.swift
│   │   │   ├── PersistentAnalyticsTracker.swift
│   │   │   ├── AnalyticsEvent.swift
│   │   │   └── AnalyticsViewModel.swift
│   │   ├── Common/                     # Utilities, Extensions
│   │   │   ├── ErrorCode.swift
│   │   │   ├── AppError.swift
│   │   │   ├── LRUCache.swift
│   │   │   └── Extensions/
│   │   └── DI/                         # Dependency Injection
│   │       └── Container.swift
│   │
│   ├── Data/                           # Data layer
│   │   ├── Local/                      # Core Data
│   │   │   ├── CoreDataStack.swift
│   │   │   ├── Entities/
│   │   │   │   ├── UserEntity.swift
│   │   │   │   └── AnalyticsEventEntity.swift
│   │   │   └── DAOs/
│   │   │       ├── UserDAO.swift
│   │   │       └── AnalyticsEventDAO.swift
│   │   ├── Remote/                     # Network data sources
│   │   │   ├── UserNetworkDataSource.swift
│   │   │   ├── APIService.swift
│   │   │   └── DTOs/
│   │   │       └── UserDTO.swift
│   │   ├── Repository/                 # Repository implementations
│   │   │   ├── OfflineFirstUserRepository.swift
│   │   │   └── CachingDataRepository.swift
│   │   ├── Model/                      # Data models
│   │   │   └── User.swift
│   │   └── Worker/                     # Background tasks
│   │       └── AnalyticsUploadWorker.swift
│   │
│   ├── Domain/                         # Business logic layer
│   │   ├── Model/                      # Value objects
│   │   │   └── EmailAddress.swift
│   │   ├── Service/                    # Domain services
│   │   │   ├── UserService.swift
│   │   │   └── UserServiceImpl.swift
│   │   ├── Validation/                 # Input validators
│   │   │   └── UserValidator.swift
│   │   └── UseCase/                    # Use cases
│   │       ├── GetUsersUseCase.swift
│   │       ├── CreateUserUseCase.swift
│   │       └── DeleteUserUseCase.swift
│   │
│   ├── Presentation/                   # Presentation layer
│   │   ├── Screens/                    # SwiftUI Views + ViewModels
│   │   │   ├── Home/
│   │   │   │   ├── HomeView.swift
│   │   │   │   └── HomeViewModel.swift
│   │   │   └── User/
│   │   │       ├── UserListView.swift
│   │   │       ├── UserListViewModel.swift
│   │   │       ├── UserDetailView.swift
│   │   │       └── UserDetailViewModel.swift
│   │   ├── Components/                 # Reusable UI components
│   │   │   ├── UserCard.swift
│   │   │   └── LoadingView.swift
│   │   ├── Theme/                      # UI theming
│   │   │   ├── Colors.swift
│   │   │   ├── Typography.swift
│   │   │   └── ArcanaTheme.swift
│   │   └── Navigation/
│   │       ├── Router.swift
│   │       └── Route.swift
│   │
│   ├── Sync/                           # Sync management
│   │   ├── SyncManager.swift
│   │   └── BackgroundSyncTask.swift
│   │
│   └── Resources/                      # Assets, Localization
│       ├── Assets.xcassets
│       └── Localizable.strings
│
├── ArcanaIOSTests/                     # Unit tests
│   ├── Core/
│   │   └── AppErrorTests.swift
│   ├── Domain/
│   │   ├── Service/
│   │   │   └── UserServiceTests.swift
│   │   └── Validation/
│   │       └── UserValidatorTests.swift
│   ├── Data/
│   │   └── Repository/
│   │       └── OfflineFirstRepositoryTests.swift
│   └── Presentation/
│       └── UserViewModelTests.swift
│
├── ArcanaIOSUITests/                   # UI tests
│   └── UserFlowTests.swift
│
├── Package.swift                       # Swift Package Manager
└── README.md                           # This file
```

---

## 🎨 UI Theme - Arcana iOS

```swift
// ArcanaTheme.swift
struct ArcanaTheme {
    // MARK: - Colors
    struct Colors {
        // Primary gradient
        static let primaryPurple = Color(hex: "667eea")
        static let primaryViolet = Color(hex: "764ba2")

        // Accent colors
        static let accentGold = Color(hex: "FFD700")
        static let accentViolet = Color(hex: "9333EA")

        // Backgrounds
        static let backgroundDark = Color(hex: "1a1a2e")
        static let backgroundLight = Color(hex: "f8f9fa")

        // Gradients
        static let primaryGradient = LinearGradient(
            colors: [primaryPurple, primaryViolet],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )

        static let cardGradient = LinearGradient(
            colors: [Color(hex: "f5f7fa"), Color(hex: "c3cfe2")],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
    }

    // MARK: - Typography
    struct Typography {
        static let title = Font.system(size: 28, weight: .bold, design: .rounded)
        static let headline = Font.system(size: 20, weight: .semibold, design: .rounded)
        static let body = Font.system(size: 16, weight: .regular, design: .rounded)
        static let caption = Font.system(size: 14, weight: .regular, design: .rounded)
    }

    // MARK: - Spacing
    struct Spacing {
        static let xs: CGFloat = 4
        static let sm: CGFloat = 8
        static let md: CGFloat = 16
        static let lg: CGFloat = 24
        static let xl: CGFloat = 32
    }
}

// Usage in Views
struct HomeView: View {
    var body: some View {
        ZStack {
            ArcanaTheme.Colors.primaryGradient
                .ignoresSafeArea()

            VStack(spacing: ArcanaTheme.Spacing.lg) {
                Text("Arcana")
                    .font(ArcanaTheme.Typography.title)
                    .foregroundColor(.white)
            }
        }
    }
}
```

---

## ✅ Input Validation

```swift
// UserValidator.swift
struct UserValidator {
    enum ValidationError: Error {
        case invalidEmail(String)
        case invalidName(String)
        case fieldTooLong(String, maxLength: Int)
        case requiredField(String)

        var appError: AppError {
            switch self {
            case .invalidEmail(let field):
                return .validationError(.E2001_INVALID_EMAIL, field: field, message: "Invalid email format")
            case .invalidName(let field):
                return .validationError(.E2002_INVALID_NAME, field: field, message: "Invalid name")
            case .fieldTooLong(let field, let max):
                return .validationError(.E2004_FIELD_TOO_LONG, field: field, message: "Maximum \(max) characters")
            case .requiredField(let field):
                return .validationError(.E2003_REQUIRED_FIELD, field: field, message: "Field is required")
            }
        }
    }

    static func validateEmail(_ email: String) -> Result<Void, ValidationError> {
        guard !email.isEmpty else {
            return .failure(.requiredField("email"))
        }

        let emailRegex = #"^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$"#
        let emailPredicate = NSPredicate(format: "SELF MATCHES[c] %@", emailRegex)

        guard emailPredicate.evaluate(with: email) else {
            return .failure(.invalidEmail("email"))
        }

        return .success(())
    }

    static func validateName(_ name: String, field: String) -> Result<Void, ValidationError> {
        guard !name.isEmpty else {
            return .failure(.requiredField(field))
        }

        guard name.count <= 100 else {
            return .failure(.fieldTooLong(field, maxLength: 100))
        }

        return .success(())
    }
}

// Usage in ViewModel
final class UserFormViewModel: ObservableObject {
    @Published var firstName = ""
    @Published var lastName = ""
    @Published var email = ""

    @Published var firstNameError: String?
    @Published var lastNameError: String?
    @Published var emailError: String?

    var isFormValid: Bool {
        firstNameError == nil && lastNameError == nil && emailError == nil &&
        !firstName.isEmpty && !lastName.isEmpty && !email.isEmpty
    }

    func validateFirstName() {
        switch UserValidator.validateName(firstName, field: "firstName") {
        case .success:
            firstNameError = nil
        case .failure(let error):
            firstNameError = error.appError.localizedDescription
        }
    }

    func validateEmail() {
        switch UserValidator.validateEmail(email) {
        case .success:
            emailError = nil
        case .failure(let error):
            emailError = error.appError.localizedDescription
        }
    }
}

// SwiftUI View
struct UserFormView: View {
    @StateObject private var viewModel = UserFormViewModel()

    var body: some View {
        Form {
            Section {
                TextField("First Name", text: $viewModel.firstName)
                    .onChange(of: viewModel.firstName) { _ in
                        viewModel.validateFirstName()
                    }

                if let error = viewModel.firstNameError {
                    Text(error)
                        .font(.caption)
                        .foregroundColor(.red)
                }
            }

            Section {
                TextField("Email", text: $viewModel.email)
                    .keyboardType(.emailAddress)
                    .autocapitalization(.none)
                    .onChange(of: viewModel.email) { _ in
                        viewModel.validateEmail()
                    }

                if let error = viewModel.emailError {
                    Text(error)
                        .font(.caption)
                        .foregroundColor(.red)
                }
            }

            Button("Create User") {
                viewModel.send(.createUser)
            }
            .disabled(!viewModel.isFormValid)
        }
    }
}
```

---

## 🧪 Testing Requirements

### Unit Tests (100% Coverage for Business Logic)

```swift
// UserServiceTests.swift
import XCTest
@testable import ArcanaIOS

final class UserServiceTests: XCTestCase {
    var sut: UserServiceImpl!
    var mockRepository: MockUserRepository!
    var mockValidator: MockUserValidator!

    override func setUp() {
        super.setUp()
        mockRepository = MockUserRepository()
        mockValidator = MockUserValidator()
        sut = UserServiceImpl(repository: mockRepository, validator: mockValidator)
    }

    func testGetUsers_success() async throws {
        // Given
        let expectedUsers = [User.mock(), User.mock()]
        mockRepository.getUsersResult = .success(expectedUsers)

        // When
        let users = try await sut.getUsers()

        // Then
        XCTAssertEqual(users.count, 2)
        XCTAssertEqual(mockRepository.getUsersCallCount, 1)
    }

    func testCreateUser_validationError() async {
        // Given
        let invalidUser = User(firstName: "", lastName: "", email: "invalid")
        mockValidator.validateResult = .failure(.invalidEmail("email"))

        // When/Then
        do {
            _ = try await sut.createUser(invalidUser)
            XCTFail("Should throw validation error")
        } catch let error as AppError {
            XCTAssertEqual(error.errorCode, .E2001_INVALID_EMAIL)
        }
    }
}

// ViewModelTests.swift
final class UserViewModelTests: XCTestCase {
    var sut: UserViewModel!
    var mockService: MockUserService!
    var mockAnalytics: MockAnalyticsTracker!

    func testLoadUsers_success() async {
        // Given
        let users = [User.mock()]
        mockService.getUsersResult = .success(users)

        // When
        await sut.send(.loadInitial)

        // Then
        XCTAssertEqual(sut.state.users.count, 1)
        XCTAssertFalse(sut.state.isLoading)
        XCTAssertEqual(mockAnalytics.trackEventCallCount, 1)
    }
}
```

---

## 📊 Analytics Events

```swift
enum AnalyticsEvent: String {
    // Screen Views
    case screenHomeViewed = "screen_home_viewed"
    case screenUserListViewed = "screen_user_list_viewed"

    // User Actions
    case userCreateClicked = "user_create_clicked"
    case userCreateSuccess = "user_create_success"
    case userCreateFailed = "user_create_failed"

    // Network Events
    case networkRequestStarted = "network_request_started"
    case networkRequestSuccess = "network_request_success"
    case networkRequestFailed = "network_request_failed"

    // Error Events
    case errorOccurred = "error_occurred"
    case validationError = "validation_error"

    // Performance
    case pageLoaded = "page_loaded"
}
```

---

## 🔄 Background Sync

```swift
import BackgroundTasks

final class BackgroundSyncManager {
    static let taskIdentifier = "com.example.arcana.sync"

    func registerBackgroundTasks() {
        BGTaskScheduler.shared.register(
            forTaskWithIdentifier: Self.taskIdentifier,
            using: nil
        ) { task in
            self.handleBackgroundSync(task: task as! BGAppRefreshTask)
        }
    }

    func scheduleBackgroundSync() {
        let request = BGAppRefreshTaskRequest(identifier: Self.taskIdentifier)
        request.earliestBeginDate = Date(timeIntervalSinceNow: 6 * 60 * 60) // 6 hours

        do {
            try BGTaskScheduler.shared.submit(request)
        } catch {
            print("Failed to schedule background sync: \(error)")
        }
    }

    private func handleBackgroundSync(task: BGAppRefreshTask) {
        scheduleBackgroundSync() // Schedule next run

        Task {
            do {
                try await syncManager.sync()
                task.setTaskCompleted(success: true)
            } catch {
                task.setTaskCompleted(success: false)
            }
        }
    }
}
```

---

## 📝 Implementation Checklist

### Phase 1: Project Setup
- [ ] Create Xcode project with SwiftUI
- [ ] Set up Swift Package Manager dependencies
- [ ] Configure project structure with Clean Architecture folders
- [ ] Set up Core Data model
- [ ] Implement dependency injection container

### Phase 2: Core Layer
- [ ] Implement ErrorCode enum with all codes
- [ ] Implement AppError enum
- [ ] Create LRU Cache with TTL
- [ ] Set up analytics tracking system
- [ ] Implement logging infrastructure

### Phase 3: Domain Layer
- [ ] Define domain models (User, etc.)
- [ ] Create value objects (EmailAddress)
- [ ] Implement validators (UserValidator)
- [ ] Define service protocols
- [ ] Implement domain services

### Phase 4: Data Layer
- [ ] Set up Core Data stack
- [ ] Create entities and DAOs
- [ ] Implement network data sources
- [ ] Create offline-first repositories
- [ ] Implement sync manager

### Phase 5: Presentation Layer
- [ ] Create ViewModels with Input/Output pattern
- [ ] Build SwiftUI views
- [ ] Implement navigation
- [ ] Apply Arcana theme
- [ ] Add input validation

### Phase 6: Advanced Features
- [ ] Implement background sync
- [ ] Add analytics tracking
- [ ] Create batch upload worker
- [ ] Implement caching strategy
- [ ] Add pagination

### Phase 7: Testing
- [ ] Write unit tests (100% coverage for business logic)
- [ ] Create mock implementations
- [ ] Write ViewModel tests
- [ ] Add UI tests
- [ ] Integration tests

### Phase 8: Documentation
- [ ] Generate DocC documentation
- [ ] Create README
- [ ] Document architecture
- [ ] Create error code reference
- [ ] Write API documentation

---

## 🚀 Getting Started

### Prerequisites
- macOS 14.0+
- Xcode 15.0+
- iOS 16.0+ deployment target
- Swift 5.9+

### Quick Start

1. **Create new Xcode project**:
   ```
   File → New → Project → iOS App
   Name: Arcana iOS
   Interface: SwiftUI
   Language: Swift
   ```

2. **Set up Swift Package Manager dependencies**:
   ```swift
   // Package.swift
   dependencies: [
       .package(url: "https://github.com/apple/swift-log.git", from: "1.0.0"),
   ]
   ```

3. **Implement base architecture**:
   - Create folder structure as outlined above
   - Implement ErrorCode and AppError
   - Set up dependency injection
   - Create Core Data model

4. **Build features**:
   - Follow Input/Output ViewModel pattern
   - Implement offline-first repositories
   - Add analytics tracking
   - Create SwiftUI views

---

## 📖 Additional Resources

- [Clean Architecture Guide](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [SwiftUI Best Practices](https://developer.apple.com/documentation/swiftui)
- [Core Data Guide](https://developer.apple.com/documentation/coredata)
- [Combine Framework](https://developer.apple.com/documentation/combine)
- [Background Tasks](https://developer.apple.com/documentation/backgroundtasks)

---

## 📄 License

MIT License - Same as Android project

---

**This prompt provides a complete blueprint for generating an iOS app with the exact same architecture, patterns, and features as the Arcana Android project.**

Use this as your guide to create a production-ready iOS application with Clean Architecture, offline-first design, comprehensive analytics, and error code tracking.
