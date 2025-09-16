package com.techmania.conductorapp.data.repository

import android.app.Activity
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.techmania.conductorapp.util.Resource
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    /**
     * Checks if a phone number is registered in the 'conductors' collection
     * and then initiates the Firebase phone authentication process to send an OTP.
     */
    suspend fun sendOtp(
        phoneNumber: String,
        activity: Activity,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    ): Resource<Unit> {
        // 1. Check if the conductor's phone number is whitelisted in Firestore
        val fullPhoneNumber = "+91$phoneNumber"

        Log.d("AuthRepository", "Checking whitelist in Firestore for: $fullPhoneNumber")

        return try {
            val conductorQuery = firestore.collection("conductors")
                .whereEqualTo("phone", fullPhoneNumber)
                .get()
                .await()

            if (conductorQuery.isEmpty) {
                Log.w("AuthRepository", "Whitelist check FAILED. No conductor found for this number.")

                return Resource.Error("This phone number is not registered.")
            }

            // 2. If registered, proceed to send the OTP
            Log.d("AuthRepository", "Whitelist check PASSED. Conductor found. Proceeding to send OTP.")
            val options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(fullPhoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)
                .build()
            PhoneAuthProvider.verifyPhoneNumber(options)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unexpected error occurred.")
        }
    }

    /**
     * Signs the user in using the verification ID and OTP code provided by Firebase.
     */
    suspend fun verifyOtpAndSignIn(verificationId: String, otp: String): Resource<Unit> {
        return try {
            val credential = PhoneAuthProvider.getCredential(verificationId, otp)
            auth.signInWithCredential(credential).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Invalid OTP or an error occurred.")
        }
    }

    /**
     * Checks if there is a currently signed-in user.
     */
    fun getCurrentUser() = auth.currentUser

    /**
     * Signs out the current user.
     */
    fun signOut() {
        auth.signOut()
    }
}
