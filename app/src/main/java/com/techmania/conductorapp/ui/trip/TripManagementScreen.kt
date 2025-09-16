package com.techmania.conductorapp.ui.trip

import android.Manifest
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.techmania.conductorapp.ui.navigation.ConductorScreen
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TripManagementScreen(
    navController: NavController,
    viewModel: TripManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)

    LaunchedEffect(key1 = true) {
        viewModel.navigationEvent.collectLatest { event ->
            when (event) {
                is TripManagementViewModel.NavigationEvent.NavigateToLogin -> {
                    navController.navigate(ConductorScreen.Login.route) {
                        popUpTo(ConductorScreen.TripManagement.route) { inclusive = true }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trip Management") },
                actions = {
                    TextButton(onClick = viewModel::onSignOutClicked) {
                        Text("Sign Out")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (uiState.step) {
                TripStep.SCAN_BUS -> {
                    ScanBusContent(
                        uiState = uiState, // <-- Pass the uiState here
                        hasCameraPermission = cameraPermissionState.status.isGranted,
                        onRequestPermission = { cameraPermissionState.launchPermissionRequest() },
                        onQrCodeScanned = viewModel::onQrCodeScanned
                    )
                }
                TripStep.CONFIRM_TRIP -> {
                    ConfirmTripContent(
                        busId = uiState.scannedBusId ?: "Unknown",
                        onConfirm = viewModel::onStartTripConfirmed,
                        onCancel = viewModel::onScanCancelled
                    )
                }
                TripStep.TRIP_ACTIVE -> {
                    TripActiveContent(
                        busId = uiState.scannedBusId ?: "Unknown",
                        onEndTrip = viewModel::onEndTripClicked
                    )
                }
            }
        }
    }
}

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
@OptIn(ExperimentalGetImage::class)
@Composable
private fun ScanBusContent(
    uiState: TripManagementUiState, // <-- Add this parameter
    hasCameraPermission: Boolean,
    onRequestPermission: () -> Unit,
    onQrCodeScanned: (String) -> Unit
) {
    if (hasCameraPermission) {
        if (uiState.isLoading) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Verifying Bus ID...")
        } else {
            Text(
                "Scan the QR code inside the bus to begin.",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))

            // ADD THIS to show validation errors
            if (uiState.error != null) {
                Text(
                    text = uiState.error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            CameraView(onQrCodeScanned = onQrCodeScanned)
        }
    } else {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text("Camera permission is required to scan the bus QR code.")
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRequestPermission) {
                Text("Grant Permission")
            }
        }
    }
}

@Composable
private fun ConfirmTripContent(busId: String, onConfirm: () -> Unit, onCancel: () -> Unit) {
    Text("Bus Scanned", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = busId,
        style = MaterialTheme.typography.displayMedium,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(32.dp))
    Text("Confirm to start the trip for this bus.", textAlign = TextAlign.Center)
    Spacer(modifier = Modifier.height(16.dp))
    Button(
        onClick = onConfirm,
        modifier = Modifier.fillMaxWidth().height(50.dp)
    ) {
        Text("Start Trip", fontSize = 18.sp)
    }
    TextButton(onClick = onCancel) {
        Text("Scan Again")
    }
}

@Composable
private fun ColumnScope.TripActiveContent(busId: String, onEndTrip: () -> Unit) {
    Text("Trip in Progress", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
    Spacer(modifier = Modifier.height(16.dp))
    Text("Broadcasting location for bus:", style = MaterialTheme.typography.bodyLarge)
    Text(
        text = busId,
        style = MaterialTheme.typography.displaySmall,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.weight(1f))
    Button(
        onClick = onEndTrip,
        modifier = Modifier.fillMaxWidth().height(60.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
    ) {
        Text("End Trip", fontSize = 20.sp)
    }
}

@androidx.camera.core.ExperimentalGetImage
@Composable
fun CameraView(
    modifier: Modifier = Modifier,
    onQrCodeScanned: (String) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var hasScanned by remember { mutableStateOf(false) }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val executor = ContextCompat.getMainExecutor(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build()

                val options = BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                    .build()
                val scanner = BarcodeScanning.getClient(options)

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .apply {
                        setAnalyzer(executor) { imageProxy ->
                            imageProxy.image?.let { image ->
                                val inputImage = InputImage.fromMediaImage(image, imageProxy.imageInfo.rotationDegrees)
                                scanner.process(inputImage)
                                    .addOnSuccessListener { barcodes ->
                                        if (barcodes.isNotEmpty() && !hasScanned) {
                                            barcodes.first().rawValue?.let {
                                                hasScanned = true // Prevent multiple scans
                                                onQrCodeScanned(it)
                                            }
                                        }
                                    }
                                    .addOnFailureListener {
                                        // Handle failure
                                    }
                                    .addOnCompleteListener {
                                        imageProxy.close()
                                    }
                            }
                        }
                    }

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis)
            }, executor)
            previewView
        },
        modifier = modifier.fillMaxWidth().aspectRatio(1f)
    )
}
