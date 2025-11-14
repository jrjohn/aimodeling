package com.example.arcana.sync

interface Synchronizer {
    suspend fun sync(): Boolean
}
