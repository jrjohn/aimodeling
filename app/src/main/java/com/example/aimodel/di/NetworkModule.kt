package com.example.aimodel.di

import com.example.aimodel.data.remote.ApiService
import com.example.aimodel.data.remote.createApiService
import de.jensklingenberg.ktorfit.Ktorfit
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient {
        return HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    prettyPrint = true
                })
            }

            defaultRequest {
                header("x-api-key", "reqres-free-v1")
            }

            expectSuccess = true
        }
    }

    @Provides
    @Singleton
    fun provideKtorfit(httpClient: HttpClient): Ktorfit {
        return Ktorfit.Builder()
            .baseUrl("https://reqres.in/api/")
            .httpClient(httpClient)
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(ktorfit: Ktorfit): ApiService {
        return ktorfit.createApiService()
    }
}
