package com.example.aimodel.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.aimodel.data.model.UserChange

@Dao
interface UserChangeDao {
    @Query("SELECT * FROM UserChange ORDER BY id ASC")
    suspend fun getAll(): List<UserChange>

    @Insert
    suspend fun insert(userChange: UserChange)

    @Query("DELETE FROM UserChange WHERE id IN (:ids)")
    suspend fun delete(ids: List<Long>)
}
