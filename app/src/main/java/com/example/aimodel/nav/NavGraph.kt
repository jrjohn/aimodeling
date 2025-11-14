package com.example.aimodel.nav

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.aimodel.core.analytics.AnalyticsScreens
import com.example.aimodel.core.analytics.AnalyticsTracker
import com.example.aimodel.core.analytics.NavigationAnalyticsObserver
import com.example.aimodel.ui.screens.HomeScreen
import com.example.aimodel.ui.screens.UserScreen

/**
 * Main navigation graph with automatic analytics tracking
 *
 * Note: Screen views are automatically tracked via:
 * 1. NavigationAnalyticsObserver (tracks navigation changes)
 * 2. @TrackScreen annotation on ViewModels (tracks when ViewModel is created)
 *
 * Both approaches work together to ensure comprehensive tracking.
 */
@Composable
fun NavGraph(
    analyticsTracker: AnalyticsTracker
) {
    val navController = rememberNavController()

    // Automatically track all navigation changes
    NavigationAnalyticsObserver(
        navController = navController,
        analyticsTracker = analyticsTracker,
        routeToScreenNameMapper = { route ->
            when (route) {
                "home" -> AnalyticsScreens.HOME
                "user_crud" -> AnalyticsScreens.USER_CRUD
                else -> route
            }
        }
    )

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(onNavigateToUserCrud = { navController.navigate("user_crud") })
        }
        composable("user_crud") {
            UserScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
