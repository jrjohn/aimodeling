package com.example.arcana.core.analytics.annotations

/**
 * Annotation to automatically track errors in methods
 *
 * Usage:
 * ```
 * @TrackError(source = "syncData")
 * suspend fun syncData() {
 *     // Any exception automatically tracked
 * }
 * ```
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class TrackError(
    val source: String = "",
    val trackSuccess: Boolean = false
)
