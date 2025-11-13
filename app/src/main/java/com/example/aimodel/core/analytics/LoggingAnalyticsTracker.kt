package com.example.aimodel.core.analytics

import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Simple analytics tracker implementation that logs events to Timber
 * In production, this would be replaced with Firebase Analytics, Mixpanel, etc.
 */
@Singleton
class LoggingAnalyticsTracker @Inject constructor() : AnalyticsTracker {

    override fun trackEvent(event: String, params: Map<String, Any>) {
        val paramsString = if (params.isEmpty()) {
            ""
        } else {
            " | ${params.entries.joinToString(", ") { "${it.key}=${it.value}" }}"
        }
        Timber.d("📊 Analytics Event: $event$paramsString")
    }

    override fun trackError(error: Throwable, context: Map<String, Any>) {
        val contextString = if (context.isEmpty()) {
            ""
        } else {
            " | Context: ${context.entries.joinToString(", ") { "${it.key}=${it.value}" }}"
        }
        Timber.e(error, "❌ Analytics Error: ${error.message}$contextString")
    }

    override fun trackScreen(screenName: String, params: Map<String, Any>) {
        val paramsString = if (params.isEmpty()) {
            ""
        } else {
            " | ${params.entries.joinToString(", ") { "${it.key}=${it.value}" }}"
        }
        Timber.d("📱 Analytics Screen: $screenName$paramsString")
    }

    override fun setUserProperty(key: String, value: String) {
        Timber.d("👤 Analytics User Property: $key=$value")
    }
}
