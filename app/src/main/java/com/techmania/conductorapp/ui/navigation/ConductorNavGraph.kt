package com.techmania.conductorapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.techmania.conductorapp.ui.auth.LoginScreen
import com.techmania.conductorapp.ui.trip.TripManagementScreen

@Composable
fun ConductorNavGraph() {
    val navController = rememberNavController()

    // Determine the starting screen based on whether the user is already logged in.
    val startDestination = if (FirebaseAuth.getInstance().currentUser != null) {
        ConductorScreen.TripManagement.route
    } else {
        ConductorScreen.Login.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(route = ConductorScreen.Login.route) {
            LoginScreen(navController = navController)
        }

        composable(route = ConductorScreen.TripManagement.route) {
            // Uncomment the line below to show the actual screen
            TripManagementScreen(navController = navController)
        }
    }
}
