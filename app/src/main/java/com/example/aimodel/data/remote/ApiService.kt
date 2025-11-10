package com.example.aimodel.data.remote

import com.example.aimodel.data.model.User
import de.jensklingenberg.ktorfit.http.Body
import de.jensklingenberg.ktorfit.http.DELETE
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.POST
import de.jensklingenberg.ktorfit.http.PUT
import de.jensklingenberg.ktorfit.http.Path
import kotlinx.serialization.Serializable

@Serializable
data class CreateUserRequest(
    val name: String,
    val job: String
)

@Serializable
data class CreateUserResponse(
    val name: String,
    val job: String,
    val id: String,
    val createdAt: String
)

interface ApiService {

    @GET("users")
    suspend fun getUsers(): List<User>

    @POST("users")
    suspend fun createUser(@Body request: CreateUserRequest): CreateUserResponse

    @PUT("users/{id}")
    suspend fun updateUser(@Path("id") id: Int, @Body request: CreateUserRequest): CreateUserResponse

    @DELETE("users/{id}")
    suspend fun deleteUser(@Path("id") id: Int)
}
