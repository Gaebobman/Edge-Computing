package hcilab.standardlee.vibrationDetector.ui.navigation
import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import hcilab.standardlee.vibrationDetector.ui.viewmodel.BLEClientViewModel
import hcilab.standardlee.vibrationDetector.ui.screens.DeviceScreen
import hcilab.standardlee.vibrationDetector.ui.screens.PermissionsRequiredScreen
import hcilab.standardlee.vibrationDetector.ui.screens.ScanningScreen
import hcilab.standardlee.vibrationDetector.ui.screens.haveAllPermissions

@SuppressLint("MissingPermission")
@Composable
fun MainNavigation(viewModel: BLEClientViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var allPermissionsGranted by remember {
        mutableStateOf (haveAllPermissions(context))
    }

    if (!allPermissionsGranted) {
        PermissionsRequiredScreen { allPermissionsGranted = true }
    }
    else if (uiState.activeDevice == null) {
        ScanningScreen(
            isScanning = uiState.isScanning,
            foundDevices = uiState.foundDevices,
            startScanning = viewModel::startScanning,
            stopScanning = viewModel::stopScanning,
            selectDevice = { device ->
                viewModel.stopScanning()
                viewModel.setActiveDevice(device)
            }
        )
    }
    else {
        DeviceScreen(
            unselectDevice = {
                viewModel.disconnectActiveDevice()
                viewModel.setActiveDevice(null)
            },
            isDeviceConnected = uiState.isDeviceConnected,
            discoveredCharacteristics = uiState.discoveredCharacteristics,
            inferenceResult = uiState.inferenceResult,
            connect = viewModel::connectActiveDevice,
            discoverServices = viewModel::discoverActiveDeviceServices,
            resultRead = viewModel::readInferenceResultFromActiveDevice     // Result of the Communication
        )
    }
}
