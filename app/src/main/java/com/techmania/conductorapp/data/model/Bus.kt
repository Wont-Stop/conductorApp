package com.techmania.conductorapp.data.model

// Represents a vehicle document in Firestore
data class Bus(
    val id: String = "", // e.g., "PB-11-A1234"
    val routeId: String = ""
    // We will add more fields like location, status later
)
