package com.techmania.conductorapp.data.model

// Represents a conductor's profile document in Firestore
data class Conductor(
    val uid: String = "",
    val conductorId: String = "",
    val fullName: String = "",
    val phone: String = "",
    val isActive: Boolean = false
)
