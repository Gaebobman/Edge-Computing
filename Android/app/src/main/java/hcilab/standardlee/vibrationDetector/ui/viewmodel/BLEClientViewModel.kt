package hcilab.standardlee.vibrationDetector.ui.viewmodel

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import hcilab.standardlee.vibrationDetector.ble.BLEScanner
import hcilab.standardlee.vibrationDetector.ble.ConnectBLE
import hcilab.standardlee.vibrationDetector.ble.PERMISSION_BLUETOOTH_CONNECT
import hcilab.standardlee.vibrationDetector.ble.PERMISSION_BLUETOOTH_SCAN
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class BLEClientViewModel(private val application: Application): AndroidViewModel(application) {
    private val bleScanner = BLEScanner(application)
    private var activeConnection = MutableStateFlow<ConnectBLE?>(null)

    private val isDeviceConnected = activeConnection.flatMapLatest { it?.isConnected ?: flowOf(false) }
    private val activeDeviceServices = activeConnection.flatMapLatest {
        it?.services ?: flowOf(emptyList())
    }
    private val activeInferenceResult = activeConnection.flatMapLatest {
        it?.resultRead ?: flowOf(null)
    }
    private val activeDeviceNameWrittenTimes = activeConnection.flatMapLatest {
        it?.successfulNameWrites ?: flowOf(0)
    }

    private val _uiState = MutableStateFlow(BLEClientUIState())
    val uiState = combine(
        _uiState,
        isDeviceConnected,
        activeDeviceServices,
        activeInferenceResult
    ) { state, isDeviceConnected, services, inferenceResult ->
        state.copy(
            isDeviceConnected = isDeviceConnected,
            discoveredCharacteristics = services.associate { service -> Pair(service.uuid.toString(), service.characteristics.map { it.uuid.toString() }) },
            inferenceResult = inferenceResult,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BLEClientUIState())

    init {
        viewModelScope.launch {
            bleScanner.foundDevices.collect { devices ->
                _uiState.update { it.copy(foundDevices = devices) }
            }
        }
        viewModelScope.launch {
            bleScanner.isScanning.collect { isScanning ->
                _uiState.update { it.copy(isScanning = isScanning) }
            }
        }
    }

    @RequiresPermission(PERMISSION_BLUETOOTH_SCAN)
    fun startScanning() {
        bleScanner.startScanning()
    }

    @RequiresPermission(PERMISSION_BLUETOOTH_SCAN)
    fun stopScanning() {
        bleScanner.stopScanning()
    }

    @SuppressLint("MissingPermission")
    @RequiresPermission(allOf = [PERMISSION_BLUETOOTH_CONNECT, PERMISSION_BLUETOOTH_SCAN])
    fun setActiveDevice(device: BluetoothDevice?) {
        activeConnection.value = device?.run { ConnectBLE(application, device) }
        _uiState.update { it.copy(activeDevice = device) }
    }

    @RequiresPermission(PERMISSION_BLUETOOTH_CONNECT)
    fun connectActiveDevice() {
        activeConnection.value?.connect()
    }

    @RequiresPermission(PERMISSION_BLUETOOTH_CONNECT)
    fun disconnectActiveDevice() {
        activeConnection.value?.disconnect()
    }

    @RequiresPermission(PERMISSION_BLUETOOTH_CONNECT)
    fun discoverActiveDeviceServices() {
        activeConnection.value?.discoverServices()
    }

    @RequiresPermission(PERMISSION_BLUETOOTH_CONNECT)
    fun readInferenceResultFromActiveDevice() {
        activeConnection.value?.readResult()
    }

//    @RequiresPermission(PERMISSION_BLUETOOTH_CONNECT)
//    fun writeNameToActiveDevice() {
//        activeConnection.value?.writeName()
//    }

    override fun onCleared() {
        super.onCleared()

        //when the ViewModel dies, shut down the BLE client with it
        if (bleScanner.isScanning.value) {
            if (ActivityCompat.checkSelfPermission(
                    getApplication(),
                    Manifest.permission.BLUETOOTH_SCAN
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                bleScanner.stopScanning()
            }
        }
    }
}

data class BLEClientUIState(
    val isScanning: Boolean = false,
    val foundDevices: List<BluetoothDevice> = emptyList(),
    val activeDevice: BluetoothDevice? = null,
    val isDeviceConnected: Boolean = false,
    val discoveredCharacteristics: Map<String, List<String>> = emptyMap(),
    val inferenceResult: String? = null,
)