package com.example.aimodel.sync

interface Synchronizer {
    suspend fun sync(): Boolean
}
