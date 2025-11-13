package com.example.aimodel.data.repository

import com.example.aimodel.data.model.User
import kotlinx.coroutines.flow.Flow

interface DataRepository {
    fun getUsers(): Flow<List<User>>
    suspend fun getUsersPage(page: Int): Result<Pair<List<User>, Int>>
    suspend fun getTotalUserCount(): Int
    suspend fun createUser(user: User): Boolean
    suspend fun updateUser(user: User): Boolean
    suspend fun deleteUser(id: Int): Boolean
}
