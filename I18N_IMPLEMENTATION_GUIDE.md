# Internationalization (i18n) Implementation Guide

This guide explains how to use the new i18n system in the AI Model Android app.

## ✅ What's Been Implemented

### 1. String Resources Created
- **`app/src/main/res/values/strings.xml`** - English (default)
- **`app/src/main/res/values-es/strings.xml`** - Spanish
- **`app/src/main/res/values/plurals.xml`** - English plurals
- **`app/src/main/res/values-es/plurals.xml`** - Spanish plurals

### 2. StringProvider Interface
**Location:** `app/src/main/java/com/example/aimodel/core/common/StringProvider.kt`

```kotlin
interface StringProvider {
    fun getString(@StringRes resId: Int): String
    fun getString(@StringRes resId: Int, vararg formatArgs: Any): String
    fun getQuantityString(@PluralsRes resId: Int, quantity: Int): String
    fun getQuantityString(@PluralsRes resId: Int, quantity: Int, vararg formatArgs: Any): String
}
```

**Why?** ViewModels and domain layer classes cannot access `Context` directly. `StringProvider` gives them a clean way to access localized strings.

### 3. Dependency Injection
`StringProvider` is automatically injected via Hilt in `DomainModule.kt`.

---

## 📋 How to Use i18n in Different Layers

### In Jetpack Compose UI

Use `stringResource()` directly:

```kotlin
import androidx.compose.ui.res.stringResource
import com.example.aimodel.R

@Composable
fun UserScreen() {
    Text(text = stringResource(R.string.screen_title_users))

    // With format arguments
    Text(text = stringResource(R.string.pagination_page_info, currentPage, totalPages))

    // Plurals
    val count = 5
    Text(text = pluralStringResource(R.plurals.sync_pending_changes, count, count))
}
```

### In ViewModels

Inject `StringProvider` and use it:

```kotlin
@HiltViewModel
class UserViewModel @Inject constructor(
    private val userService: UserService,
    private val stringProvider: StringProvider  // ← Inject this
) : ViewModel() {

    private fun createUser(user: User) {
        viewModelScope.launch {
            val success = userService.createUser(user)
            if (success) {
                _effect.send(
                    UserEffect.ShowSuccess(
                        stringProvider.getString(R.string.user_created_success)  // ← Use it
                    )
                )
            } else {
                _effect.send(
                    UserEffect.ShowError(
                        stringProvider.getString(R.string.user_create_failed)
                    )
                )
            }
        }
    }

    // With format arguments
    private fun loadPage(page: Int) {
        // ...
        _effect.send(
            UserEffect.ShowError(
                stringProvider.getString(R.string.error_failed_load_page, page)
            )
        )
    }
}
```

### In Domain Layer (Use Cases, Validators)

Same as ViewModels - inject `StringProvider`:

```kotlin
class UserValidator @Inject constructor(
    private val stringProvider: StringProvider
) {
    fun validate(user: User): Result<Unit> {
        val errors = mutableListOf<AppError.ValidationError>()

        if (user.firstName.isBlank() && user.lastName.isBlank()) {
            errors.add(
                AppError.validation(
                    "name",
                    stringProvider.getString(R.string.error_name_required)
                )
            )
        }

        if (user.firstName.length > 100) {
            errors.add(
                AppError.validation(
                    "firstName",
                    stringProvider.getString(R.string.error_first_name_too_long)
                )
            )
        }

        return if (errors.isEmpty()) Result.success(Unit)
        else Result.failure(Exception(errors.first().message))
    }
}
```

### In Data/Repository Layer

Repository layer typically doesn't need user-facing strings. If needed, inject `StringProvider` the same way.

---

## 🔄 Migration Checklist

### Files to Update

#### 1. ✅ ViewModels
- [x] `UserViewModel.kt`
- [ ] `HomeViewModel.kt`

**Pattern:**
```kotlin
// BEFORE
_effect.send(UserEffect.ShowError("Failed to create user"))

// AFTER
_effect.send(UserEffect.ShowError(
    stringProvider.getString(R.string.user_create_failed)
))
```

#### 2. ✅ Domain Layer
- [ ] `UserValidator.kt`
- [ ] `CreateUserUseCase.kt`
- [ ] `UpdateUserUseCase.kt`

**Pattern:**
```kotlin
// BEFORE
return Result.failure(Exception("Email is required"))

// AFTER
return Result.failure(Exception(
    stringProvider.getString(R.string.error_email_required)
))
```

#### 3. ✅ AppError
- [ ] `AppError.kt` - Refactor to use resource IDs

**New Pattern:**
```kotlin
sealed class AppError {
    abstract val messageResId: Int
    abstract val formatArgs: Array<out Any>

    data class NetworkError(
        @StringRes override val messageResId: Int,
        override val formatArgs: Array<out Any> = emptyArray(),
        val isRetryable: Boolean = true
    ) : AppError()

    // ...
}

// Extension to get message
fun AppError.getUserMessage(stringProvider: StringProvider): String {
    return stringProvider.getString(messageResId, *formatArgs)
}
```

#### 4. ✅ SyncStatus
- [ ] `SyncStatus.kt` - Use StringProvider for messages

**Pattern:**
```kotlin
// BEFORE
fun getStatusMessage(): String {
    return when {
        isSyncing -> "Syncing..."
        pendingChanges > 0 -> "$pendingChanges changes waiting to sync"
        // ...
    }
}

// AFTER
fun getStatusMessage(stringProvider: StringProvider): String {
    return when {
        isSyncing -> stringProvider.getString(R.string.sync_status_syncing)
        pendingChanges > 0 -> stringProvider.getQuantityString(
            R.plurals.sync_pending_changes,
            pendingChanges,
            pendingChanges
        )
        // ...
    }
}
```

---

## 🌍 Adding New Languages

### 1. Create values-XX directory

```bash
mkdir app/src/main/res/values-fr  # French
mkdir app/src/main/res/values-de  # German
mkdir app/src/main/res/values-zh  # Chinese
```

### 2. Copy and translate strings.xml

```bash
cp app/src/main/res/values/strings.xml app/src/main/res/values-fr/strings.xml
# Then translate all strings in the new file
```

### 3. Copy and translate plurals.xml

```bash
cp app/src/main/res/values/plurals.xml app/src/main/res/values-fr/plurals.xml
# Then translate plurals
```

### 4. Test

Change device language to the new language and verify strings appear correctly.

---

## 📊 String Resource Naming Conventions

### Prefixes

| Prefix | Usage | Example |
|--------|-------|---------|
| `action_` | Button/action labels | `action_save`, `action_delete` |
| `error_` | Error messages | `error_no_connection`, `error_invalid_email` |
| `screen_title_` | Screen titles | `screen_title_home`, `screen_title_users` |
| `dialog_title_` | Dialog titles | `dialog_title_create_user` |
| `dialog_message_` | Dialog messages | `dialog_message_delete_confirm` |
| `field_` | Form field labels | `field_first_name`, `field_email` |
| `empty_` | Empty state messages | `empty_no_users`, `empty_no_results` |
| `sync_status_` | Sync status messages | `sync_status_syncing` |
| `time_` | Time-related strings | `time_just_now`, `time_minutes_ago` |
| `content_description_` | Accessibility | `content_description_user_avatar` |

### Plurals Naming

Plurals use the base name without suffix:

```xml
<plurals name="sync_pending_changes">
    <item quantity="one">%1$d change waiting to sync</item>
    <item quantity="other">%1$d changes waiting to sync</item>
</plurals>
```

---

## 🧪 Testing Language Support

### 1. Change Device Language

**Settings → System → Languages → Add Language → Select Language**

### 2. Test in Emulator

```bash
# Set language to Spanish
adb shell "setprop persist.sys.locale es-ES; stop; start"

# Set language to English
adb shell "setprop persist.sys.locale en-US; stop; start"
```

### 3. Test Plurals

Ensure you test with quantities of 0, 1, 2, and many to verify plural rules work correctly for each language.

### 4. Test Format Arguments

Verify that strings with placeholders (e.g., `%1$d`, `%1$s`) display correctly.

---

## 📝 Complete Example: UserViewModel Migration

### Before
```kotlin
@HiltViewModel
class UserViewModel @Inject constructor(
    private val userService: UserService
) : ViewModel() {

    private fun createUser(user: User) {
        viewModelScope.launch {
            val success = userService.createUser(user)
            if (success) {
                _effect.send(UserEffect.ShowSuccess("User created successfully"))
            } else {
                _effect.send(UserEffect.ShowError("Failed to create user"))
            }
        }
    }

    private fun loadUsers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            userService.getUsersPage(1).fold(
                onSuccess = { (users, totalPages) -> /* ... */ },
                onFailure = { error ->
                    _effect.send(UserEffect.ShowError(
                        error.message ?: "Failed to load users"
                    ))
                }
            )
        }
    }
}
```

### After
```kotlin
@HiltViewModel
class UserViewModel @Inject constructor(
    private val userService: UserService,
    private val stringProvider: StringProvider  // ← Added
) : ViewModel() {

    private fun createUser(user: User) {
        viewModelScope.launch {
            val success = userService.createUser(user)
            if (success) {
                _effect.send(UserEffect.ShowSuccess(
                    stringProvider.getString(R.string.user_created_success)  // ← Changed
                ))
            } else {
                _effect.send(UserEffect.ShowError(
                    stringProvider.getString(R.string.user_create_failed)  // ← Changed
                ))
            }
        }
    }

    private fun loadUsers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            userService.getUsersPage(1).fold(
                onSuccess = { (users, totalPages) -> /* ... */ },
                onFailure = { error ->
                    _effect.send(UserEffect.ShowError(
                        error.message ?: stringProvider.getString(R.string.error_failed_load_users)  // ← Changed
                    ))
                }
            )
        }
    }
}
```

---

## 🚀 Benefits

### For Users
- ✅ App automatically shows in their device language
- ✅ Better user experience with native language support
- ✅ Proper plural forms for their language
- ✅ Culturally appropriate time/date formatting

### For Developers
- ✅ Centralized string management
- ✅ Easy to add new languages
- ✅ Type-safe string access (compile-time checking)
- ✅ No hardcoded strings scattered throughout code
- ✅ Automatic RTL support for Arabic, Hebrew, etc.

### For Business
- ✅ Global market reach
- ✅ Higher user satisfaction
- ✅ Professional app appearance
- ✅ Compliance with localization requirements

---

## 📦 Currently Supported Languages

| Language | Code | Status |
|----------|------|--------|
| English | `en` | ✅ Complete |
| Spanish | `es` | ✅ Complete |
| French | `fr` | ⬜ Template ready, needs translation |
| German | `de` | ⬜ Template ready, needs translation |

---

## 🔧 Troubleshooting

### Strings not updating after language change?
- Force close and reopen the app
- Clear app data and restart

### Missing translation warning?
- Android falls back to default (`values/`) if translation is missing
- Add the missing string to the specific language's `strings.xml`

### Format arguments not working?
- Ensure you're using the right format specifiers:
  - `%1$s` for strings
  - `%1$d` for integers
  - `%1$f` for floats

### Plurals not working correctly?
- Different languages have different plural rules
- Check Android documentation for plural rules per language
- Test with quantities: 0, 1, 2, 5, 11, 100

---

## 📚 Resources

- [Android Localization Guide](https://developer.android.com/guide/topics/resources/localization)
- [String Resources](https://developer.android.com/guide/topics/resources/string-resource)
- [Plural Resources](https://developer.android.com/guide/topics/resources/string-resource#Plurals)
- [Language and Locale Resolution](https://developer.android.com/guide/topics/resources/multilingual-support)

---

**Status**: Core i18n infrastructure complete. Migration of existing hardcoded strings in progress.

**Next Steps**:
1. Update remaining ViewModels
2. Update domain layer classes
3. Refactor AppError to use resource IDs
4. Add more language translations
5. Test with all supported languages
