package com.techmania.conductorapp.ui.navigation


/**
 * A sealed class representing the distinct screens in the Conductor App.
 * Using a sealed class provides compile-time safety for navigation routes.
 */
sealed class ConductorScreen(val route: String) {
    // Represents the Phone Number + OTP authentication screen
    object Login : ConductorScreen("login")

    // The main screen after login for managing a trip (QR scan, start/end trip)
    object TripManagement : ConductorScreen("trip_management")
}
