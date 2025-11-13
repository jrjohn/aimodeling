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

    override suspend fun sync(): Boolean {
        if (!networkMonitor.isOnline.first()) {
            Timber.d("Sync skipped: Device is offline")
            return false
        }

        try {
            Timber.d("Starting sync process")
            processOfflineChanges()
            Timber.d("Fetching users from network")
            val users = networkDataSource.getUsers()
            Timber.d("Received ${users.size} users from network")
            userDao.insertUsers(users)
            Timber.d("Sync completed successfully")
            return true
        } catch (e: Exception) {
            Timber.e(e, "Sync failed for DataRepository")
            return false
        }
    }

    override suspend fun createUser(name: String, job: String): Boolean {
        if (networkMonitor.isOnline.first()) {
            return try {
                networkDataSource.createUser(CreateUserRequest(name, job))
                sync()
                true
            } catch (e: Exception) {
                Timber.e(e, "Failed to create user online, will queue for offline")
                queueCreateUser(name, job)
                false
            }
        } else {
            queueCreateUser(name, job)
            return true
        }
    }

    override suspend fun updateUser(id: Int, name: String, job: String): Boolean {
        if (networkMonitor.isOnline.first()) {
            return try {
                networkDataSource.updateUser(id, CreateUserRequest(name, job))
                sync()
                true
            } catch (e: Exception) {
                Timber.e(e, "Failed to update user online, will queue for offline")
                queueUpdateUser(id, name, job)
                false
            }
        } else {
            queueUpdateUser(id, name, job)
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

    private suspend fun queueCreateUser(name: String, job: String) {
        val tempId = UUID.randomUUID().hashCode()
        userDao.upsertUser(User(id = tempId, name = "$name (pending)"))
        userChangeDao.insert(
            UserChange(
                userId = tempId,
                type = ChangeType.CREATE,
                name = name,
                job = job
            )
        )
    }

    private suspend fun queueUpdateUser(id: Int, name: String, job: String) {
        userDao.upsertUser(User(id = id, name = "$name (pending)"))
        userChangeDao.insert(
            UserChange(
                userId = id,
                type = ChangeType.UPDATE,
                name = name,
                job = job
            )
        )
    }

    private suspend fun queueDeleteUser(id: Int) {
        userDao.deleteUser(User(id = id, name = "")) // Name is not used for deletion
        userChangeDao.insert(UserChange(userId = id, type = ChangeType.DELETE))
    }

    private suspend fun processOfflineChanges() {
        val pendingChanges = userChangeDao.getAll()
        val processedIds = mutableListOf<Long>()

        pendingChanges.forEach { change ->
            try {
                when (change.type) {
                    ChangeType.CREATE -> networkDataSource.createUser(
                        CreateUserRequest(
                            change.name!!,
                            change.job!!
                        )
                    )
                    ChangeType.UPDATE -> networkDataSource.updateUser(
                        change.userId,
                        CreateUserRequest(change.name!!, change.job!!)
                    )
                    ChangeType.DELETE -> networkDataSource.deleteUser(change.userId)
                }
                processedIds.add(change.id)
            } catch (e: Exception) {
                Timber.e(e, "Failed to process offline change: $change")
            }
        }
        userChangeDao.delete(processedIds)
    }
}
