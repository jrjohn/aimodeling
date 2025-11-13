package com.example.aimodel.domain.service

import com.example.aimodel.data.model.User
import kotlinx.coroutines.flow.Flow

interface UserService {
    fun getUsers(): Flow<List<User>>
    suspend fun getUsersPage(page: Int): Result<Pair<List<User>, Int>>
    suspend fun getTotalUserCount(): Int
    suspend fun createUser(user: User): Boolean
    suspend fun updateUser(user: User): Boolean
    suspend fun deleteUser(id: Int): Boolean
    suspend fun syncUsers(): Boolean
    fun invalidateCache()
}