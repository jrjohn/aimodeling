package com.example.aimodel.data.repository

import com.example.aimodel.core.common.NetworkMonitor
import com.example.aimodel.data.local.UserChangeDao
import com.example.aimodel.data.local.UserDao
import com.example.aimodel.data.model.ChangeType
import com.example.aimodel.data.model.User
import com.example.aimodel.data.model.UserChange
import com.example.aimodel.data.network.UserNetworkDataSource
import com.example.aimodel.data.remote.CreateUserRequest
import com.example.aimodel.sync.Syncable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

class OfflineFirstDataRepository @Inject constructor(
    private val userDao: UserDao,
    private val userChangeDao: UserChangeDao,
    private val networkDataSource: UserNetworkDataSource,
    private val networkMonitor: NetworkMonitor
) : DataRepository, Syncable {

    override fun getUsers(): Flow<List<User>> {
        return userDao.getUsers()
    }

    override suspend fun getUsersPage(page: Int): Result<Pair<List<User>, Int>> {
        return try {
            if (!networkMonitor.isOnline.first()) {
                Result.failure(Exception("Network unavailable"))
            } else {
                val (users, totalPages) = networkDataSource.getUsersPage(page)
                Result.success(Pair(users, totalPages))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get users page $page")
            Result.failure(e)
        }
    }

    override suspend fun sync(): Boolean {
        if (!networkMonitor.isOnline.first()) {
            Timber.d("Sync skipped: Device is offline")
            return false
        }

        try {
            Timber.d("Starting sync process")
            processOfflineChanges()
            Timber.d("Fetching users from network")
            val (networkUsers, totalCount) = networkDataSource.getUsersWithTotal()
            Timber.d("Received ${networkUsers.size} users from network, total count: $totalCount")

            // Get local users for conflict resolution
            val localUsers = userDao.getUsers().first()
            Timber.d("Found ${localUsers.size} local users for conflict resolution")

            // Resolve conflicts and merge data
            val resolvedUsers = resolveConflicts(localUsers, networkUsers)
            Timber.d("Resolved conflicts, inserting ${resolvedUsers.size} users")

            userDao.insertUsers(resolvedUsers)
            Timber.d("Sync completed successfully")
            return true
        } catch (e: Exception) {
            Timber.e(e, "Sync failed for DataRepository")
            return false
        }
    }

    /**
     * Resolves conflicts between local and network user data
     * Strategy: Last-write-wins based on updatedAt timestamp
     *
     * @param localUsers Users from local database
     * @param networkUsers Users from network
     * @return Merged list of users with conflicts resolved
     */
    private suspend fun resolveConflicts(
        localUsers: List<User>,
        networkUsers: List<User>
    ): List<User> {
        val localMap = localUsers.associateBy { it.id }
        val networkMap = networkUsers.associateBy { it.id }
        val resolvedUsers = mutableListOf<User>()
        var conflictsDetected = 0
        var conflictsResolved = 0

        // Process all network users
        networkMap.forEach { (id, networkUser) ->
            val localUser = localMap[id]

            if (localUser == null) {
                // User only exists on network, add it
                Timber.d("ConflictResolution: User $id only on network, adding")
                resolvedUsers.add(networkUser)
            } else {
                // User exists both locally and on network, resolve conflict
                val resolved = resolveUserConflict(localUser, networkUser)
                if (resolved != networkUser) {
                    conflictsDetected++
                    if (resolved == localUser) {
                        Timber.d("ConflictResolution: User $id - local version newer, keeping local")
                        // Local version is newer, push to network
                        try {
                            networkDataSource.updateUser(
                                localUser.id,
                                CreateUserRequest(localUser.name, "Developer")
                            )
                            conflictsResolved++
                            Timber.d("ConflictResolution: Successfully pushed local user $id to network")
                        } catch (e: Exception) {
                            Timber.e(e, "ConflictResolution: Failed to push local user $id to network")
                        }
                    } else {
                        Timber.d("ConflictResolution: User $id - network version newer, using network")
                    }
                }
                resolvedUsers.add(resolved)
            }
        }

        // Add any users that only exist locally (not on network)
        localMap.forEach { (id, localUser) ->
            if (!networkMap.containsKey(id)) {
                Timber.d("ConflictResolution: User $id only exists locally, keeping")
                resolvedUsers.add(localUser)
            }
        }

        if (conflictsDetected > 0) {
            Timber.d("ConflictResolution: Detected $conflictsDetected conflicts, resolved $conflictsResolved")
        }

        return resolvedUsers
    }

    /**
     * Resolves conflict for a single user using last-write-wins strategy
     * Compares timestamps and versions to determine which version to keep
     *
     * @param localUser User from local database
     * @param networkUser User from network
     * @return The user version to keep
     */
    private fun resolveUserConflict(localUser: User, networkUser: User): User {
        // Compare timestamps first
        return when {
            localUser.updatedAt > networkUser.updatedAt -> {
                // Local is newer
                Timber.v("ConflictResolution: User ${localUser.id} - local timestamp ${localUser.updatedAt} > network ${networkUser.updatedAt}")
                localUser
            }
            localUser.updatedAt < networkUser.updatedAt -> {
                // Network is newer
                Timber.v("ConflictResolution: User ${localUser.id} - network timestamp ${networkUser.updatedAt} > local ${localUser.updatedAt}")
                networkUser
            }
            else -> {
                // Same timestamp, compare versions
                if (localUser.version > networkUser.version) {
                    Timber.v("ConflictResolution: User ${localUser.id} - same timestamp, local version ${localUser.version} > network ${networkUser.version}")
                    localUser
                } else {
                    Timber.v("ConflictResolution: User ${localUser.id} - same timestamp, network version ${networkUser.version} >= local ${localUser.version}")
                    networkUser
                }
            }
        }
    }

    override suspend fun getTotalUserCount(): Int {
        return try {
            if (networkMonitor.isOnline.first()) {
                val (_, total) = networkDataSource.getUsersWithTotal()
                total
            } else {
                userDao.getUsers().first().size
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get total user count")
            userDao.getUsers().first().size
        }
    }

    override suspend fun createUser(user: User): Boolean {
        if (networkMonitor.isOnline.first()) {
            return try {
                networkDataSource.createUser(CreateUserRequest(user.name, "Developer"))
                sync()
                true
            } catch (e: Exception) {
                Timber.e(e, "Failed to create user online, will queue for offline")
                queueCreateUser(user)
                false
            }
        } else {
            queueCreateUser(user)
            return true
        }
    }

    override suspend fun updateUser(user: User): Boolean {
        if (networkMonitor.isOnline.first()) {
            return try {
                networkDataSource.updateUser(user.id, CreateUserRequest(user.name, "Developer"))
                sync()
                true
            } catch (e: Exception) {
                Timber.e(e, "Failed to update user online, will queue for offline")
                queueUpdateUser(user)
                false
            }
        } else {
            queueUpdateUser(user)
            return true
        }
    }

    override suspend fun deleteUser(id: Int): Boolean {
        if (networkMonitor.isOnline.first()) {
            return try {
                networkDataSource.deleteUser(id)
                sync()
                true
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete user online, will queue for offline")
                queueDeleteUser(id)
                false
            }
        } else {
            queueDeleteUser(id)
            return true
        }
    }

    private suspend fun queueCreateUser(user: User) {
        val tempId = UUID.randomUUID().hashCode()
        userDao.upsertUser(
            user.copy(id = tempId)
        )
        userChangeDao.insert(
            UserChange(
                userId = tempId,
                type = ChangeType.CREATE,
                name = user.name,
                job = "Developer"
            )
        )
    }

    private suspend fun queueUpdateUser(user: User) {
        userDao.upsertUser(user)
        userChangeDao.insert(
            UserChange(
                userId = user.id,
                type = ChangeType.UPDATE,
                name = user.name,
                job = "Developer"
            )
        )
    }

    private suspend fun queueDeleteUser(id: Int) {
        userDao.deleteUser(User(id = id))
        userChangeDao.insert(UserChange(userId = id, type = ChangeType.DELETE))
    }

    private suspend fun processOfflineChanges() {
        val pendingChanges = userChangeDao.getAll()
        Timber.d("Processing ${pendingChanges.size} offline changes")
        val processedIds = mutableListOf<Long>()

        pendingChanges.forEach { change ->
            try {
                Timber.d("Processing offline change: ${change.type} for user ${change.userId}")
                when (change.type) {
                    ChangeType.CREATE -> {
                        networkDataSource.createUser(
                            CreateUserRequest(
                                change.name!!,
                                change.job!!
                            )
                        )
                        Timber.d("Successfully processed CREATE for user ${change.userId}")
                    }
                    ChangeType.UPDATE -> {
                        networkDataSource.updateUser(
                            change.userId,
                            CreateUserRequest(change.name!!, change.job!!)
                        )
                        Timber.d("Successfully processed UPDATE for user ${change.userId}")
                    }
                    ChangeType.DELETE -> {
                        networkDataSource.deleteUser(change.userId)
                        Timber.d("Successfully processed DELETE for user ${change.userId}")
                    }
                }
                processedIds.add(change.id)
            } catch (e: Exception) {
                Timber.e(e, "Failed to process offline change: $change")
            }
        }
        if (processedIds.isNotEmpty()) {
            userChangeDao.delete(processedIds)
            Timber.d("Deleted ${processedIds.size} processed offline changes from queue")
        }
    }
}
