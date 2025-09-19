package com.techmania.conductorapp.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.google.android.gms.location.*
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.techmania.conductorapp.util.Resource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose // <-- ADD THIS IMPORT
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow // <-- ADD THIS IMPORT
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TripRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context
) {
    private val locationProvider: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private var locationCallback: LocationCallback? = null

    suspend fun isValidBus(busId: String): Resource<Boolean> {
        return try {
            val document = firestore.collection("vehicles").document(busId).get().await()
            if (document.exists()) Resource.Success(true)
            else Resource.Error("Bus ID '$busId' not found in the system.")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to query database.")
        }
    }

    suspend fun startTrip(busId: String): Resource<Unit> {
        return try {
            val initialData = mapOf(
                "status" to "online",
                "currentStatusMessage" to "Trip started",
                "lastUpdated" to FieldValue.serverTimestamp()
            )
            firestore.collection("vehicles").document(busId).update(initialData).await()
            startLocationUpdates(busId)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to start trip.")
        }
    }

    suspend fun endTrip(busId: String): Resource<Unit> {
        stopLocationUpdates()
        return try {
            val finalData = mapOf(
                "status" to "offline",
                "speed" to 0,
                "currentStatusMessage" to "Trip ended"
            )
            firestore.collection("vehicles").document(busId).update(finalData).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to end trip.")
        }
    }

    suspend fun updateSeats(busId: String, change: Int): Resource<Unit> {
        return try {
            firestore.collection("vehicles").document(busId)
                .update("vacantSeats", FieldValue.increment(change.toLong())).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update seats.")
        }
    }

    suspend fun updateStatusMessage(busId: String, message: String): Resource<Unit> {
        return try {
            firestore.collection("vehicles").document(busId)
                .update("currentStatusMessage", message).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to send message.")
        }
    }

    fun getInitialTripData(busId: String): Flow<Resource<Pair<Int, String?>>> = callbackFlow {
        trySend(Resource.Loading)
        val snapshot = firestore.collection("vehicles").document(busId).get().await()
        val seats = snapshot.getLong("vacantSeats")?.toInt() ?: 0
        val routeId = snapshot.getString("routeId")

        var nextStopName: String? = null
        if (routeId != null) {
            val routeDoc = firestore.collection("routes").document(routeId).get().await()
            val stopIds = routeDoc.get("stops") as? List<String>
            if (!stopIds.isNullOrEmpty()) {
                val nextStopId = stopIds.getOrNull(0) // Default to first stop
                if (nextStopId != null) {
                    val stopDoc = firestore.collection("stops").document(nextStopId).get().await()
                    nextStopName = stopDoc.getString("name")
                }
            }
        }
        trySend(Resource.Success(Pair(seats, nextStopName)))
        awaitClose()
    }


    @SuppressLint("MissingPermission")
    private fun startLocationUpdates(busId: String) {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .setMinUpdateIntervalMillis(5000)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    val vehicleUpdate = mapOf(
                        "location" to GeoPoint(location.latitude, location.longitude),
                        "speed" to (location.speed * 3.6).toLong(), // m/s to km/h
                        "lastUpdated" to FieldValue.serverTimestamp()
                    )
                    firestore.collection("vehicles").document(busId).update(vehicleUpdate)
                }
            }
        }
        locationProvider.requestLocationUpdates(locationRequest, locationCallback!!, Looper.getMainLooper())
    }

    private fun stopLocationUpdates() {
        locationCallback?.let {
            locationProvider.removeLocationUpdates(it)
            locationCallback = null
        }
    }
}