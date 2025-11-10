package com.example.aimodel.nav

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.aimodel.ui.screens.HomeScreen
import com.example.aimodel.ui.screens.UserScreen

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(onNavigateToUserCrud = { navController.navigate("user_crud") })
        }
        composable("user_crud") {
            UserScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
