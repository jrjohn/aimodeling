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
            val (users, totalCount) = networkDataSource.getUsersWithTotal()
            Timber.d("Received ${users.size} users from network, total count: $totalCount")
            userDao.insertUsers(users)
            Timber.d("Sync completed successfully")
            return true
        } catch (e: Exception) {
            Timber.e(e, "Sync failed for DataRepository")
            return false
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
