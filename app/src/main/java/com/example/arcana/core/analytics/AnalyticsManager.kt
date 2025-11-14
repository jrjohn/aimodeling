package com.example.arcana.core.analytics

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.arcana.data.worker.AnalyticsUploadWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages analytics operations including scheduling periodic uploads
 */
@Singleton
class AnalyticsManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val analyticsTracker: AnalyticsTracker
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val workManager = WorkManager.getInstance(context)

    /**
     * Initialize analytics manager and schedule periodic uploads
     */
    fun initialize() {
        Timber.d("📊 Initializing Analytics Manager")
        schedulePeriodicUpload()
        trackAppOpened()
    }

    /**
     * Schedule periodic upload of analytics events
     */
    private fun schedulePeriodicUpload() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED) // Require network
            .setRequiresBatteryNotLow(true) // Don't drain battery
            .build()

        val uploadWorkRequest = PeriodicWorkRequestBuilder<AnalyticsUploadWorker>(
            repeatInterval = UPLOAD_INTERVAL_HOURS,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .addTag(AnalyticsUploadWorker.WORK_NAME)
            .build()

        workManager.enqueueUniquePeriodicWork(
            AnalyticsUploadWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP, // Keep existing if already scheduled
            uploadWorkRequest
        )

        Timber.d("📤 Scheduled periodic analytics upload every $UPLOAD_INTERVAL_HOURS hours")
    }

    /**
     * Trigger immediate upload (for testing or user-initiated sync)
     */
    fun triggerImmediateUpload() {
        scope.launch {
            Timber.d("📤 Triggering immediate analytics upload")
            // WorkManager will handle the upload via AnalyticsUploadWorker
            val uploadWorkRequest = androidx.work.OneTimeWorkRequestBuilder<AnalyticsUploadWorker>()
                .addTag(AnalyticsUploadWorker.WORK_NAME)
                .build()

            workManager.enqueue(uploadWorkRequest)
        }
    }

    /**
     * Track app opened event
     */
    private fun trackAppOpened() {
        if (analyticsTracker is PersistentAnalyticsTracker) {
            analyticsTracker.trackLifecycleEvent(
                Events.APP_OPENED,
                mapOf(
                    Params.TIMESTAMP to System.currentTimeMillis().toString()
                )
            )
        }
    }

    /**
     * Track app closed event
     */
    fun trackAppClosed() {
        if (analyticsTracker is PersistentAnalyticsTracker) {
            analyticsTracker.trackLifecycleEvent(
                Events.APP_CLOSED,
                mapOf(
                    Params.TIMESTAMP to System.currentTimeMillis().toString()
                )
            )
        }
    }

    companion object {
        private const val UPLOAD_INTERVAL_HOURS = 6L // Upload every 6 hours
    }
}
