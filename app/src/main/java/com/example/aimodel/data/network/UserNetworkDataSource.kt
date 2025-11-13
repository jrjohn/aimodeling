package com.example.aimodel.data.network

import com.example.aimodel.data.model.User
import com.example.aimodel.data.remote.ApiService
import com.example.aimodel.data.remote.CreateUserRequest
import com.example.aimodel.data.remote.UserDto
import javax.inject.Inject

class UserNetworkDataSource @Inject constructor(
    private val apiService: ApiService,
) {
    suspend fun getUsers(): List<User> {
        val response = apiService.getUsers()
        return response.data.map { it.toUser() }
    }

    suspend fun createUser(request: CreateUserRequest) = apiService.createUser(request)
    suspend fun updateUser(id: Int, request: CreateUserRequest) = apiService.updateUser(id, request)
    suspend fun deleteUser(id: Int) = apiService.deleteUser(id)
}

private fun UserDto.toUser(): User {
    return User(
        id = id,
        name = "$firstName $lastName"
    )
}
