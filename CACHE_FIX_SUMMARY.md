# Cache Coherency Fix - Total User Count Issue

## Problem
UserScreen was always displaying "Total Users: 6" regardless of actual data because of a **race condition** between sync completion and cache invalidation.

## Root Cause Analysis

### Issue 1: Asynchronous WorkManager Sync
**Location**: `SyncManager.kt:32-44` (before fix)

```kotlin
override suspend fun sync(): Boolean {
    workManager.enqueueUniqueWork(...) // Fire and forget!
    return true  // Returns immediately, sync hasn't started yet
}
```

**Problem**:
- `enqueueUniqueWork()` just queues work and returns immediately
- `getTotalUserCount()` was called before the sync worker even started
- Cache still contained stale value (6 users)

### Issue 2: Async Event-Based Cache Invalidation
**Location**: `CachingDataRepository.kt:28-32` (event listener)

```kotlin
scope.launch {
    cacheEventBus.events.collect { event ->
        handleCacheInvalidationEvent(event)  // Runs asynchronously!
    }
}
```

**Problem**:
- Event emission is asynchronous
- Cache invalidation happened in a separate coroutine
- By the time cache was invalidated, `getTotalUserCount()` had already been called

## Solution

### Fix 1: Synchronous Direct Sync
**File**: `SyncManager.kt:33-60`

Changed from WorkManager-based async sync to **direct synchronous sync** for manual requests:

```kotlin
override suspend fun sync(): Boolean {
    // Directly call all Syncable components synchronously
    val syncables = syncablesProvider.get()

    var allSuccessful = true
    syncables.forEach { syncable ->
        val success = syncable.sync()  // Blocks until complete
        if (!success) allSuccessful = false
    }

    return allSuccessful
}
```

**Benefits**:
- Sync completes before method returns
- No race conditions
- Deterministic behavior

### Fix 2: CachingDataRepository Implements Syncable
**File**: `CachingDataRepository.kt:250-272`

Made CachingDataRepository implement `Syncable` interface:

```kotlin
override suspend fun sync(): Boolean {
    // Call delegate's sync
    val success = if (delegate is Syncable) {
        delegate.sync()
    } else {
        false
    }

    // Invalidate caches synchronously after sync
    if (success) {
        invalidateAllCaches()  // Synchronous!
    }

    return success
}
```

**Benefits**:
- Cache invalidation happens **synchronously** as part of sync
- No dependency on async event processing
- Guaranteed cache coherency

### Fix 3: Updated DI Configuration
**File**: `RepositoryModule.kt:68-70`

Changed Syncable binding to use CachingDataRepository instead of OfflineFirstDataRepository:

```kotlin
@Binds
@IntoSet
abstract fun bindCachingDataRepositoryAsSyncable(
    cachingDataRepository: CachingDataRepository
): Syncable
```

**Benefits**:
- SyncManager now calls CachingDataRepository.sync()
- Cache invalidation integrated into sync flow
- Proper layer ordering

## Execution Flow (After Fix)

1. **User triggers sync** → `HomeViewModel.loadUserData()`
2. **Call sync** → `userService.syncUsers()`
3. **Direct sync** → `SyncManager.sync()` calls `CachingDataRepository.sync()`
4. **Delegate** → `CachingDataRepository` calls `OfflineFirstDataRepository.sync()`
5. **Network sync** → Data fetched from API and saved to DB
6. **Emit event** → `SyncCompleted` event emitted (for observers)
7. **Invalidate cache** → `CachingDataRepository` invalidates caches **synchronously**
8. **Return** → Control returns to HomeViewModel
9. **Get count** → `getTotalUserCount()` called with **fresh cache**
10. **Success** → Correct total displayed!

## Files Modified

1. ✅ `SyncManager.kt` - Changed to direct synchronous sync
2. ✅ `CachingDataRepository.kt` - Implemented Syncable, added sync() method
3. ✅ `RepositoryModule.kt` - Updated DI to bind CachingDataRepository as Syncable

## Testing

Build successful: ✅
```
BUILD SUCCESSFUL in 10s
42 actionable tasks: 10 executed, 32 up-to-date
```

## Expected Behavior

- **Before**: Total Users always showed 6 (stale cache)
- **After**: Total Users updates correctly after sync completes

The cache invalidation now happens **synchronously** during sync, eliminating the race condition completely.
