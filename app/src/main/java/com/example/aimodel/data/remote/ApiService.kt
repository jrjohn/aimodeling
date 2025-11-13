package com.example.aimodel.data.remote

import de.jensklingenberg.ktorfit.http.Body
import de.jensklingenberg.ktorfit.http.DELETE
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.POST
import de.jensklingenberg.ktorfit.http.PUT
import de.jensklingenberg.ktorfit.http.Path
import de.jensklingenberg.ktorfit.http.Query
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: Int,
    val email: String,
    @SerialName("first_name")
    val firstName: String,
    @SerialName("last_name")
    val lastName: String,
    val avatar: String
)

@Serializable
data class UsersResponse(
    val page: Int,
    @SerialName("per_page")
    val perPage: Int,
    val total: Int,
    @SerialName("total_pages")
    val totalPages: Int,
    val data: List<UserDto>
)

@Serializable
data class CreateUserRequest(
    val name: String,
    val job: String
)

@Serializable
data class CreateUserResponse(
    val name: String,
    val job: String,
    val id: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

interface ApiService {

    @GET("users")
    suspend fun getUsers(): UsersResponse

    @GET("users")
    suspend fun getUsersPage(
        @Query("page") page: Int,
        @Query("per_page") perPage: Int = 6
    ): UsersResponse

    @POST("users")
    suspend fun createUser(@Body request: CreateUserRequest): CreateUserResponse

    @PUT("users/{id}")
    suspend fun updateUser(@Path("id") id: Int, @Body request: CreateUserRequest): CreateUserResponse

    @DELETE("users/{id}")
    suspend fun deleteUser(@Path("id") id: Int)
}
