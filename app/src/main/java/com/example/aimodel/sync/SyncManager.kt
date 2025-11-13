package com.example.aimodel.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class SyncManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) : Synchronizer {

    private val workManager = WorkManager.getInstance(context)

    companion object {
        private const val SYNC_WORK_NAME = "sync_work"
        private const val PERIODIC_SYNC_WORK_NAME = "periodic_sync_work"
        private const val SYNC_INTERVAL_MINUTES = 15L
    }

    override suspend fun sync(): Boolean {
        Timber.d("SyncManager: Triggering one-time sync")
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(createSyncConstraints())
            .addTag(SYNC_WORK_NAME)
            .build()

        workManager.enqueueUniqueWork(
            SYNC_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request
        )
        return true
    }

    /**
     * Schedules periodic background sync
     *
     * @param intervalMinutes Interval between syncs in minutes (default: 15)
     */
    fun schedulePeriodicSync(intervalMinutes: Long = SYNC_INTERVAL_MINUTES) {
        Timber.d("SyncManager: Scheduling periodic sync every $intervalMinutes minutes")

        val workRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            intervalMinutes,
            TimeUnit.MINUTES
        )
            .setConstraints(createSyncConstraints())
            .addTag(PERIODIC_SYNC_WORK_NAME)
            .build()

        workManager.enqueueUniquePeriodicWork(
            PERIODIC_SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )

        Timber.d("SyncManager: Periodic sync scheduled")
    }

    /**
     * Cancels periodic sync
     */
    fun cancelPeriodicSync() {
        Timber.d("SyncManager: Cancelling periodic sync")
        workManager.cancelUniqueWork(PERIODIC_SYNC_WORK_NAME)
    }

    /**
     * Observes the sync work status
     *
     * @return Flow of WorkInfo for monitoring sync status
     */
    fun observeSyncStatus(): Flow<List<WorkInfo>> {
        return workManager.getWorkInfosForUniqueWorkFlow(SYNC_WORK_NAME)
    }

    /**
     * Checks if sync is currently running
     *
     * @return Flow emitting true when sync is in progress
     */
    fun isSyncing(): Flow<Boolean> {
        return workManager.getWorkInfosForUniqueWorkFlow(SYNC_WORK_NAME)
            .map { workInfos ->
                workInfos.any { it.state == WorkInfo.State.RUNNING }
            }
    }

    private fun createSyncConstraints(): Constraints {
        return Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true) // Don't sync when battery is low
            .build()
    }
}
