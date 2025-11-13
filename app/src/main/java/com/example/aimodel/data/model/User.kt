package com.example.aimodel.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity
data class User(
    @PrimaryKey val id: Int,
    val email: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val avatar: String = ""
) {
    val name: String
        get() = "$firstName $lastName".trim().ifEmpty { email }
}