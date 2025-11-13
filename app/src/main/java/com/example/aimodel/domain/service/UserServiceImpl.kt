package com.example.aimodel.domain.service

import com.example.aimodel.data.model.User
import com.example.aimodel.data.repository.DataRepository
import com.example.aimodel.sync.Synchronizer
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class UserServiceImpl @Inject constructor(
    private val dataRepository: DataRepository,
    private val synchronizer: Synchronizer
) : UserService {

    override fun getUsers(): Flow<List<User>> {
        return dataRepository.getUsers()
    }

    override suspend fun createUser(name: String, job: String): Boolean {
        return dataRepository.createUser(name, job)
    }

    override suspend fun updateUser(id: Int, name: String, job: String): Boolean {
        return dataRepository.updateUser(id, name, job)
    }

    override suspend fun deleteUser(id: Int): Boolean {
        return dataRepository.deleteUser(id)
    }

    override suspend fun syncUsers(): Boolean {
        return synchronizer.sync()
    }
}