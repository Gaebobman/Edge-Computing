// Original Code: https://github.com/tdcolvin/BLEClient
// Modified by Gaebobman

package hcilab.standardlee.vibrationDetector.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.Context
import android.util.Log
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.UUID

import hcilab.standardlee.vibrationDetector.BuildConfig

val INFERENCE_SERVICE_UUID: UUID = UUID.fromString(BuildConfig.INFERENCE_SERVICE_UUID)
val RESULT_CHARACTERISTIC_UUID = UUID.fromString(BuildConfig.RESULT_CHARACTERISTIC_UUID)

class ConnectBLE  @RequiresPermission("PERMISSION_BLUETOOTH_CONNECT") constructor(
    private val context: Context,
    private val bluetoothDevice: BluetoothDevice
) {
    val isConnected = MutableStateFlow(false)
    val resultRead = MutableStateFlow<String?>(null)
    val successfulNameWrites = MutableStateFlow(0)
    val services = MutableStateFlow<List<BluetoothGattService>>(emptyList())


    // GATT (Generic Attribute Profile): Describes Server-Client data exchange rules
    private val callback = object: BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            val connected = newState == BluetoothGatt.STATE_CONNECTED
            if (connected) {
                //read the list of services
                services.value = gatt.services
            }
            isConnected.value = connected
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            services.value = gatt.services
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, status)
            if (characteristic.uuid ==  RESULT_CHARACTERISTIC_UUID) {
                resultRead.value = String(characteristic.value)
            }
        }

//        override fun onCharacteristicWrite(
//            gatt: BluetoothGatt,
//            characteristic: BluetoothGattCharacteristic,
//            status: Int
//        ) {
//            super.onCharacteristicWrite(gatt, characteristic, status)
//            if (characteristic.uuid == NAME_CHARACTERISTIC_UUID) {
//                successfulNameWrites.update { it + 1 }
//            }
//        }
    }

    private var gatt: BluetoothGatt? = null

    @RequiresPermission(PERMISSION_BLUETOOTH_CONNECT)
    fun disconnect() {
        gatt?.disconnect()
        gatt?.close()
        gatt = null
    }

    @RequiresPermission(PERMISSION_BLUETOOTH_CONNECT)
    fun connect() {
        gatt = bluetoothDevice.connectGatt(context, false, callback)
    }

    @RequiresPermission(PERMISSION_BLUETOOTH_CONNECT)
    fun discoverServices() {
        gatt?.discoverServices()
    }

    @RequiresPermission(PERMISSION_BLUETOOTH_CONNECT)
    fun readResult() {
        val service = gatt?.getService(INFERENCE_SERVICE_UUID)
        val characteristic = service?.getCharacteristic(RESULT_CHARACTERISTIC_UUID)
        if (characteristic != null) {
            val success = gatt?.readCharacteristic(characteristic)
            Log.v("bluetooth", "Read status: $success")
        }
    }

//    @RequiresPermission(PERMISSION_BLUETOOTH_CONNECT)
//    fun writeName() {
//        val service = gatt?.getService(INFERENCE_SERVICE_UUID)
//        val characteristic = service?.getCharacteristic(NAME_CHARACTERISTIC_UUID)
//        if (characteristic != null) {
//            characteristic.value = "Tom".toByteArray()
//            val success = gatt?.writeCharacteristic(characteristic)
//            Log.v("bluetooth", "Write status: $success")
//        }
//    }
}