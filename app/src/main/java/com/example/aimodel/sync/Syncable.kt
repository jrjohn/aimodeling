package com.example.aimodel.sync

interface Syncable {
    suspend fun sync(): Boolean
}
