package com.example.arcana.sync

interface Syncable {
    suspend fun sync(): Boolean
}
