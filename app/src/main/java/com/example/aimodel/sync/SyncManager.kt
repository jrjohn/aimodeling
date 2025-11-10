package com.example.aimodel.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class SyncManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) : Synchronizer {
    override suspend fun sync(): Boolean {
        val workManager = WorkManager.getInstance(context)
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        workManager.enqueueUniqueWork(
            "sync",
            ExistingWorkPolicy.KEEP,
            request
        )
        return true
    }
}
