package com.techmania.conductorapp.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.techmania.conductorapp.util.Resource
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

// This repository will handle all trip-related logic,
// like starting/ending a trip and broadcasting location.
// For now, it's a placeholder that we will build out later.
class TripRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    // We will add a FusedLocationProviderClient later for GPS
) {

    suspend fun isValidBus(busId: String): Resource<Boolean> {
        return try {
            val document = firestore.collection("vehicles").document(busId).get().await()
            if (document.exists()) {
                Resource.Success(true)
            } else {
                Resource.Error("Bus ID '$busId' not found in the system.")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to query database.")
        }
    }
    // Functions to start/stop trips and update location will be added here.
}
