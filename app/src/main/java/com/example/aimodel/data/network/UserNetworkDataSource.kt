package com.example.aimodel.data.network

import com.example.aimodel.data.remote.ApiService
import com.example.aimodel.data.remote.CreateUserRequest
import javax.inject.Inject

class UserNetworkDataSource @Inject constructor(
    private val apiService: ApiService,
) {
    suspend fun getUsers() = apiService.getUsers()
    suspend fun createUser(request: CreateUserRequest) = apiService.createUser(request)
    suspend fun updateUser(id: Int, request: CreateUserRequest) = apiService.updateUser(id, request)
    suspend fun deleteUser(id: Int) = apiService.deleteUser(id)
}
