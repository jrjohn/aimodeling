# Project Configuration Snapshot

This file contains a complete snapshot of the project's configuration, including the AI prompt, library versions, and Gradle build settings from the last successful build.

---

## 1. AI Prompt (`APP_GENERATION_PROMPT.md`)

# AI Prompt for App Generation

This document outlines the specifications for the generated Android application, reflecting the final successful build state.

## Core Requirements:

-   **Kotlin:** Use version 1.9.22.
-   **Coroutines:** Integrate Kotlin Coroutines for asynchronous operations.
-   **Jetpack Compose:** Use for the entire UI layer.
-   **Hilt:** Implement for dependency injection.
-   **Navigation Compose:** Use for screen navigation.
-   **Theme:** Create a complete theme with a `ColorScheme` and support for edge-to-edge display.
-   **Room:** Use for the local database.
-   **WorkManager:** Use for background data synchronization.
-   **DataStore:** Use for managing sync versions.
-   **Unit Testing:** Include a unit testing framework (JUnit, Mockito, Turbine).
-   **Logging:** Implement a logging framework (Timber).

## Architecture:

-   **MVVM:** Follow the Model-View-ViewModel architecture.
-   **ViewModel IO:** ViewModels should have clear inputs (events) and outputs (state) for event-driven UI.
-   **Offline-First:** Implement a robust offline-first architecture inspired by the "nowinandroid" sample.
    -   A `NetworkMonitor` observes device connectivity.
    -   A `UserChange` Room entity queues offline CRUD operations.
    -   The `OfflineFirstDataRepository` immediately modifies the local database for UI responsiveness and queues changes for later sync when offline.
    -   A `SyncWorker` (using `WorkManager`) processes the offline change queue first, then fetches the latest data.
-   **Repository Pattern:** Use a repository to abstract data sources, with a dedicated `UserNetworkDataSource` to handle network operations, separating them from the repository's core logic.

## Data Layer & Networking:

-   **API:** Use `https://reqres.in/api/` for all network requests.
-   **Ktorfit:** Use for REST API communication.
-   **CRUD Operations:** The application must support full offline-first Create, Read, Update, and Delete (CRUD) functionality for Users.

## UI:

-   A `HomeScreen` to display the list of users and navigate to the CRUD screen.
-   A `UserScreen` that provides a user interface for creating, updating, and deleting users.

## Build Configuration:

-   **Android Gradle Plugin:** Use version 8.2.0.
-   **KSP:** Use KSP (version 1.9.22-1.0.17) instead of Kapt for annotation processing.
-   **compileSdk:** 34
-   **targetSdk:** 34
-   **minSdk:** 28

---

## 2. Library Versions (`gradle/libs.versions.toml`)

```toml
[versions]
android-gradle-plugin = "8.13.0"
kotlin = "2.2.10"
core-ktx = "1.17.0"
junit = "4.13.2"
androidx-test-ext-junit = "1.3.0"
espresso-core = "3.7.0"
appcompat = "1.7.1"
material = "1.13.0"
compose-bom = "2025.11.00"
hilt = "2.57.2"
navigation-compose = "2.9.6"
lifecycle-viewmodel-compose = "2.9.4"
coroutines = "1.10.2"
room = "2.8.3"
ktorfit = "2.6.4"
ktor = "3.1.2"
serialization = "2.2.21"
mockito-core = "5.20.0"
mockito-kotlin = "5.4.0"
turbine = "1.2.1"
ksp = "2.3.2"
hilt-navigation-compose = "1.3.0"
timber = "5.0.1"
work = "2.11.0"
hilt-work = "1.3.0"


[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "core-ktx" }
junit = { group = "junit", name = "junit", version.ref = "junit" }
androidx-test-ext-junit = { group = "androidx.test.ext", name = "junit", version.ref = "androidx-test-ext-junit" }
espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espresso-core" }
androidx-appcompat = { group = "androidx.appcompat", name = "appcompat", version.ref = "appcompat" }
material = { group = "com.google.android.material", name = "material", version.ref = "material" }
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
androidx-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
androidx-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }
androidx-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }
androidx-material3 = { group = "androidx.compose.material3", name = "material3" }
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigation-compose" }
androidx-hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hilt-navigation-compose" }
androidx-hilt-work = { group = "androidx.hilt", name = "hilt-work", version.ref = "hilt-work" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle-viewmodel-compose" }
androidx-lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycle-viewmodel-compose" }
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
androidx-room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
androidx-work-runtime-ktx = { group = "androidx.work", name = "work-runtime-ktx", version.ref = "work" }
ktorfit-lib = { group = "de.jensklingenberg.ktorfit", name = "ktorfit-lib", version.ref = "ktorfit" }
ktor-client-core = { group = "io.ktor", name = "ktor-client-core", version.ref = "ktor" }
ktor-client-cio = { group = "io.ktor", name = "ktor-client-cio", version.ref = "ktor" }
ktor-client-content-negotiation = { group = "io.ktor", name = "ktor-client-content-negotiation", version.ref = "ktor" }
ktor-serialization-kotlinx-json = { group = "io.ktor", name = "ktor-serialization-kotlinx-json", version.ref = "ktor" }
mockito-core = { group = "org.mockito", name = "mockito-core", version.ref = "mockito-core" }
mockito-kotlin = { group = "org.mockito.kotlin", name = "mockito-kotlin", version.ref = "mockito-kotlin" }
turbine = { group = "app.cash.turbine", name = "turbine", version.ref = "turbine" }
timber = { group = "com.jakewharton.timber", name = "timber", version.ref = "timber" }


[plugins]
android-application = { id = "com.android.application", version.ref = "android-gradle-plugin" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
hilt-android = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "serialization" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
ktorfit = { id = "de.jensklingenberg.ktorfit", version.ref = "ktorfit" }
```

---

## 3. Root Gradle Settings (`build.gradle.kts`)

```kotlin
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.ksp) apply false
}
```

---

## 4. App-Level Gradle Settings (`app/build.gradle.kts`)

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ktorfit)
}

android {
    namespace = "com.example.arcana"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.arcana"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            excludes += "**/libdatastore_shared_counter.so"
            excludes += "**/libandroidx.graphics.path.so"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.timber)
    implementation(libs.androidx.work.runtime.ktx)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)


    // Jetpack Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Navigation Compose
    implementation(libs.androidx.navigation.compose)

    // ViewModel Compose
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Ktorfit
    implementation(libs.ktorfit.lib)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)


    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Unit Test
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}
```
