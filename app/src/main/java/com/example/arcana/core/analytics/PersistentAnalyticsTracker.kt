package com.example.arcana.core.analytics

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.example.arcana.BuildConfig
import com.example.arcana.data.local.dao.AnalyticsEventDao
import com.example.arcana.data.local.entity.AnalyticsEventEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Analytics tracker that persists events to local database before uploading
 */
@Singleton
class PersistentAnalyticsTracker @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val analyticsEventDao: AnalyticsEventDao,
    private val json: Json
) : AnalyticsTracker {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sessionId = UUID.randomUUID().toString()
    private var currentUserId: String? = null
    private var currentScreen: String? = null

    private val deviceInfo by lazy {
        DeviceInfo(
            deviceId = getDeviceId(),
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            osVersion = "Android ${Build.VERSION.RELEASE}",
            appVersion = BuildConfig.VERSION_NAME,
            locale = Locale.getDefault().toString(),
            timezone = TimeZone.getDefault().id
        )
    }

    private val appInfo by lazy {
        AppInfo(
            appVersion = BuildConfig.VERSION_NAME,
            buildNumber = BuildConfig.VERSION_CODE.toString(),
            isDebug = BuildConfig.DEBUG
        )
    }

    override fun trackEvent(event: String, params: Map<String, Any>) {
        val analyticsEvent = createAnalyticsEvent(
            eventType = EventType.USER_ACTION,
            eventName = event,
            params = params.mapValues { it.value.toString() }
        )
        persistEvent(analyticsEvent)
        Timber.d("📊 Event tracked: $event | ${params.entries.joinToString { "${it.key}=${it.value}" }}")
    }

    override fun trackError(error: Throwable, context: Map<String, Any>) {
        val params = context.mapValues { it.value.toString() }.toMutableMap()
        params[Params.ERROR_MESSAGE] = error.message ?: "Unknown error"
        params[Params.ERROR_TYPE] = error::class.simpleName ?: "Unknown"

        val analyticsEvent = createAnalyticsEvent(
            eventType = EventType.ERROR,
            eventName = Events.ERROR_OCCURRED,
            params = params
        )
        persistEvent(analyticsEvent)
        Timber.e(error, "❌ Error tracked: ${error.message}")
    }

    override fun trackScreen(screenName: String, params: Map<String, Any>) {
        currentScreen = screenName
        val screenParams = params.mapValues { it.value.toString() }.toMutableMap()
        screenParams[Params.SCREEN_NAME] = screenName

        val analyticsEvent = createAnalyticsEvent(
            eventType = EventType.SCREEN_VIEW,
            eventName = when (screenName) {
                AnalyticsScreens.HOME -> Events.SCREEN_HOME_VIEWED
                AnalyticsScreens.USER_LIST -> Events.SCREEN_USER_LIST_VIEWED
                AnalyticsScreens.USER_DIALOG -> Events.SCREEN_USER_DIALOG_OPENED
                else -> "screen_${screenName}_viewed"
            },
            params = screenParams,
            screenName = screenName
        )
        persistEvent(analyticsEvent)
        Timber.d("📱 Screen tracked: $screenName")
    }

    override fun setUserProperty(key: String, value: String) {
        if (key == "user_id") {
            currentUserId = value
        }
        Timber.d("👤 User property set: $key=$value")
    }

    /**
     * Track a lifecycle event
     */
    fun trackLifecycleEvent(event: String, params: Map<String, String> = emptyMap()) {
        val analyticsEvent = createAnalyticsEvent(
            eventType = EventType.LIFECYCLE,
            eventName = event,
            params = params
        )
        persistEvent(analyticsEvent)
        Timber.d("🔄 Lifecycle event: $event")
    }

    /**
     * Track a network event
     */
    fun trackNetworkEvent(event: String, params: Map<String, String> = emptyMap()) {
        val analyticsEvent = createAnalyticsEvent(
            eventType = EventType.NETWORK,
            eventName = event,
            params = params
        )
        persistEvent(analyticsEvent)
        Timber.d("🌐 Network event: $event")
    }

    /**
     * Track a performance metric
     */
    fun trackPerformance(event: String, durationMs: Long, params: Map<String, String> = emptyMap()) {
        val perfParams = params.toMutableMap()
        perfParams[Params.DURATION_MS] = durationMs.toString()

        val analyticsEvent = createAnalyticsEvent(
            eventType = EventType.PERFORMANCE,
            eventName = event,
            params = perfParams
        )
        persistEvent(analyticsEvent)
        Timber.d("⚡ Performance: $event | ${durationMs}ms")
    }

    private fun createAnalyticsEvent(
        eventType: EventType,
        eventName: String,
        params: Map<String, String> = emptyMap(),
        screenName: String? = null
    ): AnalyticsEvent {
        return AnalyticsEvent(
            eventId = UUID.randomUUID().toString(),
            eventType = eventType,
            eventName = eventName,
            timestamp = System.currentTimeMillis(),
            sessionId = sessionId,
            userId = currentUserId,
            screenName = screenName ?: currentScreen,
            params = params,
            deviceInfo = deviceInfo,
            appInfo = appInfo
        )
    }

    private fun persistEvent(event: AnalyticsEvent) {
        scope.launch {
            try {
                val entity = AnalyticsEventEntity(
                    eventId = event.eventId,
                    eventType = event.eventType.name,
                    eventName = event.eventName,
                    timestamp = event.timestamp,
                    sessionId = event.sessionId,
                    userId = event.userId,
                    screenName = event.screenName,
                    params = json.encodeToString(event.params),
                    deviceInfo = json.encodeToString(event.deviceInfo),
                    appInfo = json.encodeToString(event.appInfo)
                )
                analyticsEventDao.insert(entity)
            } catch (e: Exception) {
                Timber.e(e, "Failed to persist analytics event: ${event.eventName}")
            }
        }
    }

    private fun getDeviceId(): String {
        return try {
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            ) ?: UUID.randomUUID().toString()
        } catch (e: Exception) {
            UUID.randomUUID().toString()
        }
    }
}
