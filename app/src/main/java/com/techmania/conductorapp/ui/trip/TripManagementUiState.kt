package com.techmania.conductorapp.ui.trip

// Defines the different states the Trip Management screen can be in
enum class TripStep {
    SCAN_BUS,       // Waiting for the conductor to scan a bus QR code
    CONFIRM_TRIP,   // A bus has been scanned, waiting for confirmation
    TRIP_ACTIVE     // The trip is active and location is being broadcast
}

data class TripManagementUiState(
    val step: TripStep = TripStep.SCAN_BUS,
    val scannedBusId: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
