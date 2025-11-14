package com.example.arcana.sync

/**
 * Represents the current synchronization status
 */
data class SyncStatus(
    val isSyncing: Boolean = false,
    val pendingChanges: Int = 0,
    val lastSyncTime: Long? = null,
    val lastSyncSuccess: Boolean = true,
    val error: String? = null
) {
    /**
     * True if there are changes waiting to be synced
     */
    val hasPendingChanges: Boolean
        get() = pendingChanges > 0

    /**
     * Gets a user-friendly status message
     */
    fun getStatusMessage(): String {
        return when {
            isSyncing -> "Syncing..."
            error != null -> "Sync failed: $error"
            pendingChanges > 0 -> "$pendingChanges change${if (pendingChanges > 1) "s" else ""} waiting to sync"
            lastSyncTime != null -> "Last synced ${getRelativeTime(lastSyncTime)}"
            else -> "Not synced yet"
        }
    }

    private fun getRelativeTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            seconds < 60 -> "just now"
            minutes < 60 -> "$minutes minute${if (minutes > 1) "s" else ""} ago"
            hours < 24 -> "$hours hour${if (hours > 1) "s" else ""} ago"
            days < 7 -> "$days day${if (days > 1) "s" else ""} ago"
            else -> "over a week ago"
        }
    }

    companion object {
        /**
         * Creates an idle sync status
         */
        fun idle(): SyncStatus {
            return SyncStatus()
        }

        /**
         * Creates a syncing status
         */
        fun syncing(pendingChanges: Int): SyncStatus {
            return SyncStatus(isSyncing = true, pendingChanges = pendingChanges)
        }

        /**
         * Creates a success status
         */
        fun success(timestamp: Long): SyncStatus {
            return SyncStatus(
                isSyncing = false,
                pendingChanges = 0,
                lastSyncTime = timestamp,
                lastSyncSuccess = true
            )
        }

        /**
         * Creates an error status
         */
        fun error(error: String, pendingChanges: Int): SyncStatus {
            return SyncStatus(
                isSyncing = false,
                pendingChanges = pendingChanges,
                lastSyncSuccess = false,
                error = error
            )
        }
    }
}
