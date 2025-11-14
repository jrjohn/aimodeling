package com.example.aimodel.core.analytics.annotations

/**
 * Annotation to automatically track screen views
 *
 * Usage:
 * ```
 * @TrackScreen(AnalyticsScreens.HOME)
 * class HomeViewModel : AnalyticsViewModel()
 * ```
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class TrackScreen(
    val screenName: String,
    val autoTrack: Boolean = true
)
