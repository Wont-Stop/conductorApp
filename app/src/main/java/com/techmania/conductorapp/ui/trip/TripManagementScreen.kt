package com.techmania.conductorapp.ui.trip

import android.Manifest
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.google.accompanist.permissions.rememberMultiplePermissionsState
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
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION)
    )

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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (uiState.step) {
                TripStep.SCAN_BUS -> {
                    ScanBusContent(
                        uiState = uiState,
                        hasCameraPermission = permissionsState.permissions[0].status.isGranted,
                        onRequestPermission = { permissionsState.launchMultiplePermissionRequest() },
                        onQrCodeScanned = viewModel::onQrCodeScanned
                    )
                }
                TripStep.CONFIRM_TRIP -> {
                    val hasLocationPermission = permissionsState.permissions[1].status.isGranted
                    ConfirmTripContent(
                        busId = uiState.scannedBusId ?: "Unknown",
                        hasLocationPermission = hasLocationPermission,
                        onRequestPermission = { permissionsState.launchMultiplePermissionRequest() },
                        onConfirm = viewModel::onStartTripConfirmed,
                        onCancel = viewModel::onScanCancelled
                    )
                }
                TripStep.TRIP_ACTIVE -> {
                    TripActiveContent(
                        uiState = uiState,
                        onIncrementSeats = viewModel::onIncrementSeats,
                        onDecrementSeats = viewModel::onDecrementSeats,
                        onSendQuickMessage = viewModel::onSendQuickMessage,
                        onAtStop = viewModel::onAtStopClicked,
                        onEndTrip = viewModel::onEndTripClicked
                    )
                }
            }
        }
    }
}

@Composable
private fun ConfirmTripContent(
    busId: String,
    hasLocationPermission: Boolean,
    onRequestPermission: () -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxHeight()) {
        Text("Bus Scanned", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = busId,
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(32.dp))

        if(hasLocationPermission) {
            Text("Ready to start the trip for this bus.", textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Start Trip", fontSize = 18.sp)
            }
        } else {
            Text("Location permission is required to start the trip.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRequestPermission) {
                Text("Grant Location Permission")
            }
        }
        TextButton(onClick = onCancel) {
            Text("Scan Again")
        }
    }
}


@Composable
fun TripActiveContent(
    uiState: TripManagementUiState,
    onIncrementSeats: () -> Unit,
    onDecrementSeats: () -> Unit,
    onSendQuickMessage: (String) -> Unit,
    onAtStop: () -> Unit,
    onEndTrip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status and Bus ID
        Text("Trip in Progress", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
        Text("Broadcasting live data for bus:", style = MaterialTheme.typography.bodyLarge)
        Text(
            text = uiState.scannedBusId ?: "N/A",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )
        Divider(modifier = Modifier.padding(vertical = 16.dp))

        // Next Stop Info
        InfoCard(title = "Upcoming Stop", value = uiState.nextStop ?: "N/A")

        // Vacant Seats Control
        ControlSection(title = "Vacant Seats") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDecrementSeats, modifier = Modifier.background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)) {
                    Icon(Icons.Default.Remove, "Decrement")
                }
                Text(
                    text = "${uiState.vacantSeats}",
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                IconButton(onClick = onIncrementSeats, modifier = Modifier.background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)) {
                    Icon(Icons.Default.Add, "Increment")
                }
            }
        }

        // At Stop Button
        Button(
            onClick = onAtStop,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            Text("Bus has reached: ${uiState.nextStop ?: "Stop"}")
        }


        // Quick Updates
        ControlSection(title = "Send Quick Update") {
            QuickMessageDropdown(messages = uiState.quickMessages, onMessageSelected = onSendQuickMessage)
        }

        Spacer(modifier = Modifier.weight(1f))

        // End Trip Button
        Button(
            onClick = onEndTrip,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("End Trip", fontSize = 20.sp)
        }
    }
}

@Composable
fun InfoCard(title: String, value: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(text = value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ControlSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickMessageDropdown(messages: List<String>, onMessageSelected: (String) -> Unit) {
    var isExpanded by remember { mutableStateOf(false) }
    var selectedMessage by remember { mutableStateOf("Select a message...") }

    ExposedDropdownMenuBox(expanded = isExpanded, onExpandedChange = { isExpanded = !isExpanded }) {
        OutlinedTextField(
            value = selectedMessage,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) }
        )
        ExposedDropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }) {
            messages.forEach { message ->
                DropdownMenuItem(
                    text = { Text(message) },
                    onClick = {
                        selectedMessage = message
                        isExpanded = false
                        onMessageSelected(message)
                    }
                )
            }
        }
    }
}


// ScanBusContent and CameraView can remain the same as your previous version.
// Make sure to import everything correctly.
@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
@Composable
private fun ScanBusContent(
    uiState: TripManagementUiState,
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
            Text("Camera and Location permissions are required for this app.")
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRequestPermission) {
                Text("Grant Permissions")
            }
        }
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
                                                hasScanned = true
                                                onQrCodeScanned(it)
                                            }
                                        }
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
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
    )
}