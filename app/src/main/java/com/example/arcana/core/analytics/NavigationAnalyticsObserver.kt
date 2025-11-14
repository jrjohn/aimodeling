package com.example.arcana.core.analytics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavController
import timber.log.Timber

/**
 * Observes navigation changes and automatically tracks screen views
 *
 * Usage in NavGraph:
 * ```
 * @Composable
 * fun NavGraph(analyticsTracker: AnalyticsTracker) {
 *     val navController = rememberNavController()
 *
 *     // Auto-track screen navigation
 *     NavigationAnalyticsObserver(
 *         navController = navController,
 *         analyticsTracker = analyticsTracker,
 *         routeToScreenNameMapper = { route ->
 *             when (route) {
 *                 "home" -> AnalyticsScreens.HOME
 *                 "user_crud" -> AnalyticsScreens.USER_CRUD
 *                 else -> route
 *             }
 *         }
 *     )
 *
 *     NavHost(navController = navController, startDestination = "home") {
 *         // ... composables
 *     }
 * }
 * ```
 */
@Composable
fun NavigationAnalyticsObserver(
    navController: NavController,
    analyticsTracker: AnalyticsTracker,
    routeToScreenNameMapper: (String) -> String = { it }
) {
    DisposableEffect(navController) {
        val listener = NavController.OnDestinationChangedListener { _, destination, arguments ->
            val route = destination.route ?: "unknown"
            val screenName = routeToScreenNameMapper(route)

            try {
                val params = mutableMapOf<String, String>()

                // Add route arguments if any
                arguments?.keySet()?.forEach { key ->
                    @Suppress("DEPRECATION")
                    val value = arguments.get(key)
                    if (value != null) {
                        params[key] = value.toString()
                    }
                }

                // Add timestamp
                params[Params.TIMESTAMP] = System.currentTimeMillis().toString()

                // Track screen view
                analyticsTracker.trackScreen(screenName, params)

                Timber.d("📊 Auto-tracked navigation: $screenName (route: $route)")
            } catch (e: Exception) {
                Timber.e(e, "Failed to track navigation to $screenName")
            }
        }

        navController.addOnDestinationChangedListener(listener)

        onDispose {
            navController.removeOnDestinationChangedListener(listener)
        }
    }
}

/**
 * Track screen view when composable enters composition
 *
 * Usage in individual screens:
 * ```
 * @Composable
 * fun HomeScreen(analyticsTracker: AnalyticsTracker) {
 *     TrackScreenView(
 *         screenName = AnalyticsScreens.HOME,
 *         analyticsTracker = analyticsTracker
 *     )
 *
 *     // ... screen content
 * }
 * ```
 */
@Composable
fun TrackScreenView(
    screenName: String,
    analyticsTracker: AnalyticsTracker,
    params: Map<String, String> = emptyMap()
) {
    LaunchedEffect(screenName) {
        try {
            analyticsTracker.trackScreen(
                screenName,
                params + mapOf(Params.TIMESTAMP to System.currentTimeMillis().toString())
            )
            Timber.d("📊 Tracked screen view: $screenName")
        } catch (e: Exception) {
            Timber.e(e, "Failed to track screen view: $screenName")
        }
    }
}

/**
 * Track screen exit when composable leaves composition
 *
 * Usage:
 * ```
 * @Composable
 * fun HomeScreen(analyticsTracker: AnalyticsTracker) {
 *     TrackScreenLifecycle(
 *         screenName = AnalyticsScreens.HOME,
 *         analyticsTracker = analyticsTracker
 *     )
 *
 *     // ... screen content
 * }
 * ```
 */
@Composable
fun TrackScreenLifecycle(
    screenName: String,
    analyticsTracker: AnalyticsTracker,
    trackEntry: Boolean = true,
    trackExit: Boolean = false
) {
    DisposableEffect(screenName) {
        val entryTime = System.currentTimeMillis()

        if (trackEntry) {
            try {
                analyticsTracker.trackEvent(
                    Events.SCREEN_ENTERED,
                    mapOf(
                        Params.SCREEN_NAME to screenName,
                        Params.TIMESTAMP to entryTime.toString()
                    )
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to track screen entry: $screenName")
            }
        }

        onDispose {
            if (trackExit) {
                val exitTime = System.currentTimeMillis()
                val duration = exitTime - entryTime

                try {
                    analyticsTracker.trackEvent(
                        Events.SCREEN_EXITED,
                        mapOf(
                            Params.SCREEN_NAME to screenName,
                            Params.TIMESTAMP to exitTime.toString(),
                            Params.DURATION_MS to duration.toString()
                        )
                    )
                } catch (e: Exception) {
                    Timber.e(e, "Failed to track screen exit: $screenName")
                }
            }
        }
    }
}
