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

    override suspend fun getUsersPage(page: Int): Result<Pair<List<User>, Int>> {
        return dataRepository.getUsersPage(page)
    }

    override suspend fun getTotalUserCount(): Int {
        return dataRepository.getTotalUserCount()
    }

    override suspend fun createUser(user: User): Boolean {
        return dataRepository.createUser(user)
    }

    override suspend fun updateUser(user: User): Boolean {
        return dataRepository.updateUser(user)
    }

    override suspend fun deleteUser(id: Int): Boolean {
        return dataRepository.deleteUser(id)
    }

    override suspend fun syncUsers(): Boolean {
        return synchronizer.sync()
    }
}