package com.techmania.conductorapp.ui.auth


import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.techmania.conductorapp.data.repository.AuthRepository
import com.techmania.conductorapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<NavigationEvent>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    private var verificationId: String? = null

    // --- Event Handlers ---

    fun onPhoneNumberChanged(number: String) {
        if (number.length <= 10) {
            _uiState.update { it.copy(phoneNumber = number, error = null) }
        }
    }

    fun onOtpChanged(otp: String) {
        if (otp.length <= 6) {
            _uiState.update { it.copy(otp = otp, error = null) }
        }
    }

    /**
     * Initiates the phone number verification process by sending an OTP.
     */
    fun onSendOtpClicked(activity: Activity) {
        val phone = _uiState.value.phoneNumber

        Log.d("LoginViewModel", "Attempting to send OTP for phone number: $phone")

        if (phone.length != 10) {
            _uiState.update { it.copy(error = "Please enter a valid 10-digit phone number.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = authRepository.sendOtp(phone, activity, phoneAuthCallbacks)
            if (result is Resource.Error) {
                _uiState.update { it.copy(isLoading = false, error = result.message) }
            }
            // Success is handled by the callback
        }
    }

    /**
     * Verifies the entered OTP and signs the user in.
     */
    fun onVerifyOtpClicked() {
        val otp = _uiState.value.otp
        val currentVerificationId = verificationId

        if (otp.length != 6) {
            _uiState.update { it.copy(error = "Please enter a valid 6-digit OTP.") }
            return
        }
        if (currentVerificationId == null) {
            _uiState.update { it.copy(error = "Verification failed. Please try again.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.verifyOtpAndSignIn(currentVerificationId, otp)) {
                is Resource.Success -> {
                    // Navigate to the main app screen on successful login
                    _navigationEvent.emit(NavigationEvent.NavigateToTripManagement)
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                else -> { /* No-op */ }
            }
        }
    }

    // --- Private Helper ---

    /**
     * Callbacks to handle the result of the phone number verification from Firebase.
     */
    private val phoneAuthCallbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            // This is for auto-retrieval, which we aren't focusing on for simplicity.
            // In some cases, login can be instant.
            Log.d("LoginViewModel", "onVerificationCompleted")
        }

        override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {
            _uiState.update { it.copy(isLoading = false, error = e.localizedMessage ?: "Verification failed.") }
        }

        override fun onCodeSent(
            newVerificationId: String,
            token: PhoneAuthProvider.ForceResendingToken
        ) {
            // This is the success case for sending OTP.
            // Save the verificationId and move to the OTP entry screen.
            verificationId = newVerificationId
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isOtpSent = true,
                    step = LoginStep.OtpVerification // Change the screen step
                )
            }
        }
    }

    sealed class NavigationEvent {
        object NavigateToTripManagement : NavigationEvent()
    }
}
