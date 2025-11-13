package com.example.aimodel.data.repository

import android.util.LruCache
import com.example.aimodel.data.model.User
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import javax.inject.Inject

/**
 * Decorator that adds caching capabilities to a DataRepository
 * Uses LRU cache to store frequently accessed data
 */
class CachingDataRepository @Inject constructor(
    private val delegate: DataRepository
) : DataRepository {

    companion object {
        private const val CACHE_SIZE_PAGES = 20 // Cache up to 20 pages
        private const val CACHE_SIZE_USERS = 100 // Cache up to 100 individual users
        private const val CACHE_TTL_MS = 5 * 60 * 1000L // 5 minutes
    }

    /**
     * Cache entry with timestamp for TTL checks
     */
    private data class CacheEntry<T>(
        val data: T,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        fun isExpired(): Boolean {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MS
        }
    }

    /**
     * Cache for paginated user lists
     * Key: page number
     * Value: (List<User>, totalPages)
     */
    private val pageCache = object : LruCache<Int, CacheEntry<Pair<List<User>, Int>>>(CACHE_SIZE_PAGES) {
        override fun sizeOf(key: Int, value: CacheEntry<Pair<List<User>, Int>>): Int {
            // Size is based on number of users in the page
            return value.data.first.size
        }

        override fun entryRemoved(
            evicted: Boolean,
            key: Int,
            oldValue: CacheEntry<Pair<List<User>, Int>>,
            newValue: CacheEntry<Pair<List<User>, Int>>?
        ) {
            if (evicted) {
                Timber.d("CachingDataRepository: Evicted page $key from cache")
            }
        }
    }

    /**
     * Cache for total user count
     */
    private var totalCountCache: CacheEntry<Int>? = null

    /**
     * Cache for individual users by ID (for quick lookups)
     */
    private val userCache = object : LruCache<Int, CacheEntry<User>>(CACHE_SIZE_USERS) {
        override fun entryRemoved(
            evicted: Boolean,
            key: Int,
            oldValue: CacheEntry<User>,
            newValue: CacheEntry<User>?
        ) {
            if (evicted) {
                Timber.d("CachingDataRepository: Evicted user $key from cache")
            }
        }
    }

    override suspend fun getUsersPage(page: Int): Result<Pair<List<User>, Int>> {
        // Check cache first
        pageCache[page]?.let { entry ->
            if (!entry.isExpired()) {
                Timber.d("CachingDataRepository: Cache HIT for page $page")
                return Result.success(entry.data)
            } else {
                Timber.d("CachingDataRepository: Cache EXPIRED for page $page")
                pageCache.remove(page)
            }
        }

        Timber.d("CachingDataRepository: Cache MISS for page $page, fetching from delegate")

        // Fetch from delegate
        return delegate.getUsersPage(page).onSuccess { (users, totalPages) ->
            // Store in cache
            pageCache.put(page, CacheEntry(Pair(users, totalPages)))

            // Also cache individual users for quick lookup
            users.forEach { user ->
                userCache.put(user.id, CacheEntry(user))
            }

            Timber.d("CachingDataRepository: Cached page $page with ${users.size} users")
        }
    }

    override suspend fun getTotalUserCount(): Int {
        // Check cache first
        totalCountCache?.let { entry ->
            if (!entry.isExpired()) {
                Timber.d("CachingDataRepository: Cache HIT for total count")
                return entry.data
            } else {
                Timber.d("CachingDataRepository: Cache EXPIRED for total count")
                totalCountCache = null
            }
        }

        Timber.d("CachingDataRepository: Cache MISS for total count, fetching from delegate")

        val count = delegate.getTotalUserCount()
        totalCountCache = CacheEntry(count)
        Timber.d("CachingDataRepository: Cached total count: $count")
        return count
    }

    override fun getUsers(): Flow<List<User>> {
        // Don't cache flows, they're already reactive
        return delegate.getUsers()
    }

    override suspend fun createUser(user: User): Boolean {
        val result = delegate.createUser(user)
        if (result) {
            // Invalidate caches on successful create
            invalidateAllCaches()
            Timber.d("CachingDataRepository: Invalidated caches after user creation")
        }
        return result
    }

    override suspend fun updateUser(user: User): Boolean {
        val result = delegate.updateUser(user)
        if (result) {
            // Update user cache
            userCache.put(user.id, CacheEntry(user))
            // Invalidate page caches as user data changed
            invalidatePageCaches()
            Timber.d("CachingDataRepository: Invalidated page caches after user update")
        }
        return result
    }

    override suspend fun deleteUser(id: Int): Boolean {
        val result = delegate.deleteUser(id)
        if (result) {
            // Remove from cache
            userCache.remove(id)
            // Invalidate all caches as list changed
            invalidateAllCaches()
            Timber.d("CachingDataRepository: Invalidated caches after user deletion")
        }
        return result
    }

    /**
     * Invalidates all page caches
     */
    private fun invalidatePageCaches() {
        pageCache.evictAll()
        totalCountCache = null
    }

    /**
     * Invalidates all caches
     */
    private fun invalidateAllCaches() {
        pageCache.evictAll()
        userCache.evictAll()
        totalCountCache = null
    }

    /**
     * Gets a user from cache if available
     */
    fun getCachedUser(userId: Int): User? {
        return userCache[userId]?.let { entry ->
            if (!entry.isExpired()) {
                Timber.d("CachingDataRepository: Cache HIT for user $userId")
                entry.data
            } else {
                Timber.d("CachingDataRepository: Cache EXPIRED for user $userId")
                userCache.remove(userId)
                null
            }
        }
    }

    /**
     * Gets cache statistics for monitoring
     */
    fun getCacheStats(): CacheStats {
        return CacheStats(
            pageCacheSize = pageCache.size(),
            pageCacheMaxSize = pageCache.maxSize(),
            userCacheSize = userCache.size(),
            userCacheMaxSize = userCache.maxSize(),
            hasCountCache = totalCountCache != null && !totalCountCache!!.isExpired()
        )
    }

    data class CacheStats(
        val pageCacheSize: Int,
        val pageCacheMaxSize: Int,
        val userCacheSize: Int,
        val userCacheMaxSize: Int,
        val hasCountCache: Boolean
    )
}
