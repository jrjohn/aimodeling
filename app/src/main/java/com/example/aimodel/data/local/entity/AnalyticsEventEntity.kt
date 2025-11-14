package com.example.aimodel.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for storing analytics events locally before uploading
 */
@Entity(tableName = "analytics_events")
data class AnalyticsEventEntity(
    @PrimaryKey
    val eventId: String,
    val eventType: String,
    val eventName: String,
    val timestamp: Long,
    val sessionId: String,
    val userId: String? = null,
    val screenName: String? = null,
    val params: String, // JSON string
    val deviceInfo: String, // JSON string
    val appInfo: String, // JSON string
    val uploaded: Boolean = false,
    val uploadAttempts: Int = 0,
    val lastUploadAttempt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
