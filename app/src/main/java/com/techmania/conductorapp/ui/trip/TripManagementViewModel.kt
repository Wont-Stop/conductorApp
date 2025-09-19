package com.techmania.conductorapp.ui.trip

import android.util.Log
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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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

    fun onQrCodeScanned(busId: String) {
        if (busId.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = tripRepository.isValidBus(busId)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(isLoading = false, scannedBusId = busId, step = TripStep.CONFIRM_TRIP)
                    }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                else -> {}
            }
        }
    }

    fun onStartTripConfirmed() {
        val busId = _uiState.value.scannedBusId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (tripRepository.startTrip(busId)) {
                is Resource.Success -> {
                    loadInitialData(busId) // Fetch seats and next stop
                    _uiState.update { it.copy(isLoading = false, step = TripStep.TRIP_ACTIVE) }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = "Failed to start trip.") }
                }
                else -> {}
            }
        }
    }

    private fun loadInitialData(busId: String) {
        tripRepository.getInitialTripData(busId).onEach { result ->
            when (result) {
                is Resource.Success -> {
                    // This 'let' block safely unwraps the data
                    result.data?.let { dataPair ->
                        _uiState.update {
                            it.copy(
                                vacantSeats = dataPair.first,
                                nextStop = dataPair.second
                            )
                        }
                    }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(error = "Could not load trip data.") }
                }
                is Resource.Loading -> {
                    // You can add a loading state update here if needed
                }
            }
        }.launchIn(viewModelScope)
    }


    fun onIncrementSeats() {
        val busId = _uiState.value.scannedBusId ?: return
        _uiState.update { it.copy(vacantSeats = it.vacantSeats + 1) }
        viewModelScope.launch { tripRepository.updateSeats(busId, 1) }
    }

    fun onDecrementSeats() {
        val busId = _uiState.value.scannedBusId ?: return
        if (_uiState.value.vacantSeats > 0) {
            _uiState.update { it.copy(vacantSeats = it.vacantSeats - 1) }
            viewModelScope.launch { tripRepository.updateSeats(busId, -1) }
        }
    }

    fun onSendQuickMessage(message: String) {
        val busId = _uiState.value.scannedBusId ?: return
        viewModelScope.launch { tripRepository.updateStatusMessage(busId, message) }
    }

    fun onAtStopClicked() {
        val busId = _uiState.value.scannedBusId ?: return
        val stopName = _uiState.value.nextStop ?: "the current stop"
        viewModelScope.launch { tripRepository.updateStatusMessage(busId, "Bus is at $stopName") }
    }


    fun onEndTripClicked() {
        val busId = _uiState.value.scannedBusId ?: return
        viewModelScope.launch {
            tripRepository.endTrip(busId)
            _uiState.update { TripManagementUiState() } // Reset state
        }
    }

    fun onScanCancelled() {
        _uiState.update { it.copy(step = TripStep.SCAN_BUS, scannedBusId = null, error = null) }
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