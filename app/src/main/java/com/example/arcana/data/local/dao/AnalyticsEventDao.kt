package com.example.arcana.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.arcana.data.local.entity.AnalyticsEventEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for accessing analytics events in the local database
 */
@Dao
interface AnalyticsEventDao {

    /**
     * Insert a new analytics event
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: AnalyticsEventEntity)

    /**
     * Insert multiple analytics events
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<AnalyticsEventEntity>)

    /**
     * Get all pending events that haven't been uploaded
     *
     * @param limit Maximum number of events to fetch
     * @return List of pending events
     */
    @Query("""
        SELECT * FROM analytics_events
        WHERE uploaded = 0
        ORDER BY timestamp ASC
        LIMIT :limit
    """)
    suspend fun getPendingEvents(limit: Int = 100): List<AnalyticsEventEntity>

    /**
     * Get count of pending events
     */
    @Query("SELECT COUNT(*) FROM analytics_events WHERE uploaded = 0")
    suspend fun getPendingEventCount(): Int

    /**
     * Observe pending event count
     */
    @Query("SELECT COUNT(*) FROM analytics_events WHERE uploaded = 0")
    fun observePendingEventCount(): Flow<Int>

    /**
     * Mark events as uploaded
     *
     * @param eventIds List of event IDs to mark as uploaded
     */
    @Query("""
        UPDATE analytics_events
        SET uploaded = 1
        WHERE eventId IN (:eventIds)
    """)
    suspend fun markAsUploaded(eventIds: List<String>)

    /**
     * Increment upload attempt count
     *
     * @param eventIds List of event IDs
     * @param timestamp Current timestamp
     */
    @Query("""
        UPDATE analytics_events
        SET uploadAttempts = uploadAttempts + 1,
            lastUploadAttempt = :timestamp
        WHERE eventId IN (:eventIds)
    """)
    suspend fun incrementUploadAttempts(eventIds: List<String>, timestamp: Long)

    /**
     * Delete successfully uploaded events older than the specified time
     *
     * @param olderThan Timestamp threshold
     * @return Number of deleted events
     */
    @Query("""
        DELETE FROM analytics_events
        WHERE uploaded = 1 AND timestamp < :olderThan
    """)
    suspend fun deleteOldUploadedEvents(olderThan: Long): Int

    /**
     * Delete failed events that have exceeded max retry attempts
     *
     * @param maxAttempts Maximum number of upload attempts allowed
     * @return Number of deleted events
     */
    @Query("""
        DELETE FROM analytics_events
        WHERE uploaded = 0 AND uploadAttempts >= :maxAttempts
    """)
    suspend fun deleteFailedEvents(maxAttempts: Int = 5): Int

    /**
     * Delete all events (for testing or privacy purposes)
     */
    @Query("DELETE FROM analytics_events")
    suspend fun deleteAll()

    /**
     * Get total event count
     */
    @Query("SELECT COUNT(*) FROM analytics_events")
    suspend fun getTotalEventCount(): Int
}
