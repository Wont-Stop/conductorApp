package com.techmania.conductorapp.ui.trip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techmania.conductorapp.data.repository.AuthRepository
import com.techmania.conductorapp.data.repository.TripRepository
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
class TripManagementViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val tripRepository: TripRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TripManagementUiState())
    val uiState = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<NavigationEvent>()
    val navigationEvent = _navigationEvent.asSharedFlow()


    // --- Event Handlers ---

    /**
     * Called when the QR code scanner successfully detects a bus ID.
     */
    fun onQrCodeScanned(busId: String) {
        if (busId.isBlank()) return

        // We now need to launch a coroutine to call the repository
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = tripRepository.isValidBus(busId)) {
                is Resource.Success -> {
                    // If valid, proceed to the confirmation step
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            scannedBusId = busId,
                            step = TripStep.CONFIRM_TRIP
                        )
                    }
                }
                is Resource.Error -> {
                    // If invalid, show an error message and stay on the scan step
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
                else -> { /* No-op for Loading */ }
            }
        }
    }

    /**
     * Called when the conductor confirms the scanned bus to start the trip.
     */
    fun onStartTripConfirmed() {
        // TODO: Implement the logic in TripRepository to update the bus status in Firestore
        // and start broadcasting the GPS location.

        _uiState.update { it.copy(isLoading = true) }
        // For now, we'll just move to the active state for UI demonstration.
        _uiState.update { it.copy(isLoading = false, step = TripStep.TRIP_ACTIVE) }
    }

    /**
     * Called when the conductor ends the active trip.
     */
    fun onEndTripClicked() {
        // TODO: Implement the logic in TripRepository to update bus status to "offline"
        // and stop broadcasting GPS.

        // Reset the screen to the initial state.
        _uiState.update { TripManagementUiState() }
    }

    /**
     * Resets the screen back to the QR scanning step from the confirmation step.
     */
    fun onScanCancelled() {
        _uiState.update { it.copy(step = TripStep.SCAN_BUS, scannedBusId = null) }
    }

    fun onSignOutClicked() {
        authRepository.signOut()
        viewModelScope.launch {
            _navigationEvent.emit(NavigationEvent.NavigateToLogin)
        }
    }


    sealed class NavigationEvent {
        object NavigateToLogin : NavigationEvent()
    }
}
