# Analytics Error Tracking with Error Codes

## Overview

The analytics system now includes comprehensive error code tracking. Every `AppError` is automatically tracked with its error code, description, category, and additional context when uploaded to the cloud in batches.

## Features

### 1. Error Code Information
Every tracked error includes:
- **Error Code** (e.g., `E1000`, `E2001`, `W1000`)
- **Error Code Description** (e.g., "No internet connection available")
- **Error Code Category** (e.g., "Network", "Validation", "Server")
- **Error Message** (User-friendly message)
- **Error Type** (NetworkError, ValidationError, ServerError, etc.)

### 2. Additional Context
Depending on the error type, additional information is tracked:
- **Network Errors**: `is_retryable` flag
- **Validation Errors**: `field` name
- **Server Errors**: `http_status_code`, `is_retryable`
- **All Errors**: Stack trace (first 500 chars), throwable class, throwable message

### 3. Batch Upload
All error events are:
- Stored locally in Room database
- Uploaded in batches via `AnalyticsUploadWorker`
- Retried automatically on failure
- Include full device and app context

## Usage Examples

### Track AppError from ViewModel

```kotlin
@HiltViewModel
@TrackScreen(AnalyticsScreens.USER_CRUD)
class UserViewModel @Inject constructor(
    private val userService: UserService,
    analyticsTracker: AnalyticsTracker
) : AnalyticsViewModel(analyticsTracker) {

    fun createUser(user: User) {
        viewModelScope.launch {
            userService.createUser(user)
                .onFailure { error ->
                    if (error is AppError) {
                        // Automatically tracks with error code
                        trackAppError(error)
                    }
                }
        }
    }
}
```

### Track with Additional Context

```kotlin
fun syncData() {
    viewModelScope.launch {
        try {
            syncManager.sync()
        } catch (e: Exception) {
            val appError = AppError.fromException(e)
            trackAppError(
                appError = appError,
                params = mapOf(
                    "sync_type" to "manual",
                    "data_size" to "1024",
                    "attempt_number" to "3"
                )
            )
        }
    }
}
```

### Direct Tracking

```kotlin
// From any component with AnalyticsTracker
analyticsTracker.trackAppError(
    appError = AppError.NetworkError(
        errorCode = ErrorCode.E1000_NO_CONNECTION,
        message = "Unable to sync data",
        isRetryable = true
    ),
    context = mapOf(
        "screen" to "home",
        "action" to "refresh"
    )
)
```

## Analytics Event Structure

When an AppError is tracked, the following event is created:

```json
{
  "eventId": "uuid-v4",
  "eventType": "ERROR",
  "eventName": "error_occurred",
  "timestamp": 1699999999999,
  "sessionId": "session-uuid",
  "userId": "user-123",
  "screenName": "user_crud",
  "params": {
    "error_code": "E1000",
    "error_code_description": "No internet connection available",
    "error_code_category": "Network",
    "error_message": "Unable to sync data",
    "error_type": "NetworkError",
    "is_retryable": "true",
    "throwable_class": "java.net.UnknownHostException",
    "throwable_message": "Unable to resolve host",
    "stack_trace_top": "java.net.UnknownHostException: Unable to resolve host...",
    "screen": "home",
    "action": "refresh"
  },
  "deviceInfo": {
    "deviceId": "device-uuid",
    "manufacturer": "Google",
    "model": "Pixel 7",
    "osVersion": "Android 14",
    "appVersion": "1.0.0",
    "locale": "en_US",
    "timezone": "America/New_York"
  },
  "appInfo": {
    "appVersion": "1.0.0",
    "buildNumber": "1",
    "isDebug": true
  }
}
```

## Error Code Categories

### Network Errors (E1000-E1999)
- `E1000`: No internet connection available
- `E1001`: Connection attempt timed out
- `E1002`: Unable to resolve host address
- `E1003`: Network I/O error occurred
- `E1004`: SSL/TLS connection error

### Validation Errors (E2000-E2999)
- `E2000`: Input validation failed
- `E2001`: Invalid email address format
- `E2002`: Invalid name format or length
- `E2003`: Required field is missing
- `E2004`: Field value exceeds maximum length
- `E2005`: Field value below minimum length

### Server Errors (E3000-E3999)
- `E3000`: Internal server error
- `E3001`: Bad request - invalid parameters
- `E3002`: Resource not found
- `E3003`: Service temporarily unavailable
- `E3004`: Rate limit exceeded

### Authentication Errors (E4000-E4999)
- `E4000`: Authentication required
- `E4001`: Invalid credentials
- `E4002`: Access forbidden - insufficient permissions
- `E4003`: Session has expired
- `E4004`: Invalid or malformed token

### Data Errors (E5000-E5999)
- `E5000`: Data conflict detected
- `E5001`: Data has been modified by another user
- `E5002`: Duplicate entry detected
- `E5003`: Database constraint violation

### Database Errors (E6000-E6999)
- `E6000`: Database operation failed
- `E6001`: Database query execution failed
- `E6002`: Database transaction failed
- `E6003`: Database migration failed

### System Errors (E9000-E9999)
- `E9000`: Unknown error occurred
- `E9001`: Unexpected application state
- `E9002`: Null pointer exception
- `E9003`: Data serialization/deserialization failed

### Warnings (W1000-W3999)
- `W1000`: Network connection is slow
- `W1001`: Operating in offline mode
- `W1002`: Data synchronization pending
- `W2000`: Data is incomplete but acceptable
- `W2001`: Using deprecated data format
- `W3000`: Cache data may be stale
- `W3001`: Partial data synchronization completed
- `W3002`: Data was truncated to fit limits

## Database Storage

Errors are stored in the `analytics_events` table with the following structure:

```sql
CREATE TABLE analytics_events (
    eventId TEXT PRIMARY KEY,
    eventType TEXT,           -- "ERROR" for error events
    eventName TEXT,           -- "error_occurred"
    timestamp INTEGER,
    sessionId TEXT,
    userId TEXT,
    screenName TEXT,
    params TEXT,              -- JSON string with error code info
    deviceInfo TEXT,          -- JSON string
    appInfo TEXT,             -- JSON string
    uploaded INTEGER,         -- 0 or 1
    uploadAttempts INTEGER,
    lastUploadAttempt INTEGER,
    createdAt INTEGER
);
```

## Batch Upload Configuration

The `AnalyticsUploadWorker` handles batch uploads:

- **Frequency**: Every 6 hours
- **Batch Size**: 100 events per upload
- **Retry Policy**: Exponential backoff
- **Network Requirement**: Unmetered connection preferred
- **Battery Optimization**: Runs only when not low on battery

## Benefits

1. **Standardized Error Tracking**: All errors use the same format with unique codes
2. **Easy Debugging**: Error codes make it easy to identify and track specific issues
3. **Rich Context**: Full stack traces and device information included
4. **Offline Support**: Errors tracked offline are synced when connection restored
5. **Analytics Integration**: Errors can be analyzed in your analytics dashboard
6. **Performance Monitoring**: Track error rates by code, category, or screen
7. **Automatic Retry Logic**: Failed uploads are retried automatically

## Cloud Analytics Dashboard Queries

### Example Queries for Error Analysis

#### Errors by Category
```sql
SELECT
    params->>'error_code_category' as category,
    COUNT(*) as count
FROM analytics_events
WHERE eventType = 'ERROR'
GROUP BY category
ORDER BY count DESC;
```

#### Top Error Codes
```sql
SELECT
    params->>'error_code' as code,
    params->>'error_code_description' as description,
    COUNT(*) as count
FROM analytics_events
WHERE eventType = 'ERROR'
GROUP BY code, description
ORDER BY count DESC
LIMIT 10;
```

#### Errors by Screen
```sql
SELECT
    screenName,
    params->>'error_code' as code,
    COUNT(*) as count
FROM analytics_events
WHERE eventType = 'ERROR'
GROUP BY screenName, code
ORDER BY count DESC;
```

#### Network Error Trends
```sql
SELECT
    DATE(timestamp) as date,
    params->>'error_code' as code,
    COUNT(*) as count
FROM analytics_events
WHERE eventType = 'ERROR'
  AND params->>'error_code_category' = 'Network'
GROUP BY date, code
ORDER BY date DESC;
```

## Testing

To test error tracking in development:

1. **Enable Debug Logging**:
```kotlin
if (BuildConfig.DEBUG) {
    Timber.plant(Timber.DebugTree())
}
```

2. **Trigger Errors**:
```kotlin
// Network error
val error = AppError.NetworkError(
    errorCode = ErrorCode.E1000_NO_CONNECTION,
    message = "Test error",
    isRetryable = true
)
analyticsTracker.trackAppError(error)
```

3. **Check Database**:
```bash
adb shell
cd /data/data/com.example.arcana/databases/
sqlite3 app_database
SELECT * FROM analytics_events WHERE eventType = 'ERROR';
```

4. **View Logs**:
```bash
adb logcat | grep "Analytics"
```

## API Reference

### AnalyticsTracker Interface

```kotlin
interface AnalyticsTracker {
    fun trackAppError(appError: AppError, context: Map<String, Any> = emptyMap())
}
```

### AnalyticsViewModel Methods

```kotlin
abstract class AnalyticsViewModel(
    protected val analyticsTracker: AnalyticsTracker
) : ViewModel() {

    protected fun trackAppError(
        appError: AppError,
        params: Map<String, String> = emptyMap()
    )
}
```

### Error Code Constants

```kotlin
// Import
import com.example.arcana.core.common.ErrorCode

// Usage
val errorCode = ErrorCode.E1000_NO_CONNECTION
println(errorCode.code)        // "E1000"
println(errorCode.description) // "No internet connection available"
println(errorCode.category)    // "Network"
```

## Migration Guide

### Before (Old Way)
```kotlin
try {
    performOperation()
} catch (e: Exception) {
    Timber.e(e, "Operation failed")
    analyticsTracker.trackError(e)
}
```

### After (New Way with Error Codes)
```kotlin
try {
    performOperation()
} catch (e: Exception) {
    val appError = AppError.fromException(e)
    // Now includes error code, description, and category
    analyticsTracker.trackAppError(appError)
}
```

## Documentation

- **Error Code Reference**: `docs/ERROR_CODES.html`
- **Architecture Verification**: `docs/ARCHITECTURE_VERIFICATION.html`
- **API Documentation**: `docs/api/index.html`

---

**Generated**: 2025-11-15
**Version**: 1.0.0
**Project**: Arcana Android
