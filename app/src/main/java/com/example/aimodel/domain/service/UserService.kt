package com.example.aimodel.domain.service

import com.example.aimodel.data.model.User
import kotlinx.coroutines.flow.Flow

interface UserService {
    fun getUsers(): Flow<List<User>>
    suspend fun createUser(name: String, job: String): Boolean
    suspend fun updateUser(id: Int, name: String, job: String): Boolean
    suspend fun deleteUser(id: Int): Boolean
    suspend fun syncUsers(): Boolean
}