package com.techmania.conductorapp.ui.auth

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.techmania.conductorapp.ui.navigation.ConductorScreen
import kotlinx.coroutines.flow.collectLatest

@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalContext.current as Activity

    LaunchedEffect(key1 = true) {
        viewModel.navigationEvent.collectLatest { event ->
            when (event) {
                LoginViewModel.NavigationEvent.NavigateToTripManagement -> {
                    navController.navigate(ConductorScreen.TripManagement.route) {
                        // Clear the login screen from the back stack
                        popUpTo(ConductorScreen.Login.route) { inclusive = true }
                    }
                }
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            Text(
                text = "Conductor Login",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Conditionally show Phone Number or OTP view
            when (uiState.step) {
                LoginStep.PhoneNumber -> PhoneNumberStep(
                    uiState = uiState,
                    onPhoneNumberChanged = viewModel::onPhoneNumberChanged,
                    onSendOtpClicked = { viewModel.onSendOtpClicked(activity) }
                )
                LoginStep.OtpVerification -> OtpStep(
                    uiState = uiState,
                    onOtpChanged = viewModel::onOtpChanged,
                    onVerifyOtpClicked = viewModel::onVerifyOtpClicked
                )
            }
        }
    }
}

@Composable
private fun PhoneNumberStep(
    uiState: LoginUiState,
    onPhoneNumberChanged: (String) -> Unit,
    onSendOtpClicked: () -> Unit
) {
    Text(
        text = "Enter your registered 10-digit mobile number to receive an OTP.",
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(32.dp))

    OutlinedTextField(
        value = uiState.phoneNumber,
        onValueChange = onPhoneNumberChanged,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Phone Number") },
        leadingIcon = { Text("+91 |") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true
    )
    Spacer(modifier = Modifier.height(24.dp))

    if (uiState.error != null) {
        Text(
            text = uiState.error,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }

    Button(
        onClick = onSendOtpClicked,
        enabled = !uiState.isLoading,
        modifier = Modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
        } else {
            Text("Send OTP", fontSize = 16.sp)
        }
    }
}

@Composable
private fun OtpStep(
    uiState: LoginUiState,
    onOtpChanged: (String) -> Unit,
    onVerifyOtpClicked: () -> Unit
) {
    Text(
        text = "Enter the 6-digit OTP sent to your mobile number.",
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(32.dp))

    OutlinedTextField(
        value = uiState.otp,
        onValueChange = onOtpChanged,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("OTP") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true
    )
    Spacer(modifier = Modifier.height(24.dp))

    if (uiState.error != null) {
        Text(
            text = uiState.error,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }

    Button(
        onClick = onVerifyOtpClicked,
        enabled = !uiState.isLoading,
        modifier = Modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
        } else {
            Text("Verify & Login", fontSize = 16.sp)
        }
    }
}
