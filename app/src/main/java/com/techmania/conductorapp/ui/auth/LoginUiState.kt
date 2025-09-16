package com.techmania.conductorapp.ui.auth


// Represents the different stages of the login process
enum class LoginStep {
    PhoneNumber,
    OtpVerification
}

// Holds all the state information for the login screen
data class LoginUiState(
    val step: LoginStep = LoginStep.PhoneNumber,
    val phoneNumber: String = "",
    val otp: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isOtpSent: Boolean = false
)
