plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ktorfit)
    alias(libs.plugins.dokka)
}

android {
    namespace = "com.example.aimodel"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.aimodel"
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
        debug {
            buildConfigField("String", "API_BASE_URL", "\"https://reqres.in/api/\"")
            buildConfigField("String", "API_KEY", "\"reqres-free-v1\"")
            buildConfigField("Boolean", "ENABLE_LOGGING", "true")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "API_BASE_URL", "\"https://reqres.in/api/\"")
            buildConfigField("String", "API_KEY", "\"reqres-free-v1\"")
            buildConfigField("Boolean", "ENABLE_LOGGING", "false")
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
    ksp(libs.androidx.hilt.compiler)


    // Jetpack Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.coil.compose)
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
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.serialization.kotlinx.json)


    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Unit Test
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
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

// Configure test task
tasks.withType<Test> {
    // Suppress OpenJDK warning about bootstrap classpath
    jvmArgs("-XX:+IgnoreUnrecognizedVMOptions", "-Xshare:off")

    // Show test results
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = false
        showExceptions = true
        showCauses = true
        showStackTraces = true

        // Show summary after test execution
        afterSuite(KotlinClosure2<TestDescriptor, TestResult, Unit>({ desc, result ->
            if (desc.parent == null) { // Only execute for the whole test suite
                println("\n📊 Test Results:")
                println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                println("  Total:   ${result.testCount}")
                println("  ✅ Passed:  ${result.successfulTestCount}")
                println("  ❌ Failed:  ${result.failedTestCount}")
                println("  ⏭️  Skipped: ${result.skippedTestCount}")
                println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                println("  Result: ${result.resultType}")
                println("  Duration: ${result.endTime - result.startTime}ms")
                println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
            }
        }))
    }

    // Continue running tests even if some fail (to see all failures)
    ignoreFailures = false

    // Run tests in parallel for faster execution
    maxParallelForks = Runtime.getRuntime().availableProcessors().div(2).coerceAtLeast(1)
}

// Make build task depend on test
tasks.named("build") {
    dependsOn("test")
}

// ============================================
// API Documentation Configuration (Dokka V2)
// ============================================

// Configure Dokka
dokka {
    moduleName.set("AI Model Android App")

    dokkaPublications.html {
        outputDirectory.set(layout.buildDirectory.dir("docs/api"))

        // Suppress obvious functions
        suppressObviousFunctions.set(false)
    }

    dokkaSourceSets.configureEach {
        // Include all source sets
        suppressGeneratedFiles.set(false)

        // Skip test sources
        suppressedFiles.from(
            fileTree(projectDir.resolve("src/test")),
            fileTree(projectDir.resolve("src/androidTest"))
        )

        // Custom documentation sections (optional - uncomment if you have a Module.md file)
        // includes.from("Module.md")
    }
}

// Generate docs task
tasks.register("generateApiDocs") {
    group = "documentation"
    description = "Generates API documentation in HTML format"

    dependsOn("dokkaGeneratePublicationHtml")

    doLast {
        val docsDir = layout.buildDirectory.dir("docs/api").get().asFile
        println("")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("📚 API Documentation Generated Successfully!")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("  Location: ${docsDir.absolutePath}")
        println("  Format:   HTML")
        println("")
        println("  To view the documentation:")
        println("  open ${docsDir.resolve("index.html").absolutePath}")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("")
    }
}

// Make assemble and assembleDebug/assembleRelease tasks generate documentation
tasks.matching { it.name.matches(Regex("assemble(Debug|Release)?")) }.configureEach {
    finalizedBy("generateApiDocs")
}

// Also create a convenient task to build with docs
tasks.register("assembleWithDocs") {
    group = "build"
    description = "Assembles the app and generates API documentation"
    dependsOn("assemble", "generateApiDocs")
}
