package com.example.aimodel.core.analytics.annotations

/**
 * Annotation to automatically track method performance
 *
 * Usage:
 * ```
 * @TrackPerformance(Events.DATA_LOAD)
 * suspend fun loadData() {
 *     // Execution time automatically tracked
 * }
 * ```
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class TrackPerformance(
    val eventName: String,
    val threshold: Long = 0L // Only track if duration exceeds threshold (ms)
)
