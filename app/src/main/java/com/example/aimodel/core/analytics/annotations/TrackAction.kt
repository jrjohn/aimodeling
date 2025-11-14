package com.example.aimodel.core.analytics.annotations

/**
 * Annotation to automatically track user actions
 *
 * Usage:
 * ```
 * @TrackAction(Events.USER_CREATED)
 * fun createUser(name: String) {
 *     // Method automatically tracked when called
 * }
 * ```
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class TrackAction(
    val eventName: String,
    val includeParams: Boolean = true
)
