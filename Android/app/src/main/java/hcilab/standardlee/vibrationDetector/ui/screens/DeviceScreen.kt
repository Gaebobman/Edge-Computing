package hcilab.standardlee.vibrationDetector.ui.screens

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import hcilab.standardlee.vibrationDetector.ble.INFERENCE_SERVICE_UUID

@Composable
fun DeviceScreen(
    unselectDevice: () -> Unit,
    isDeviceConnected: Boolean,
    discoveredCharacteristics: Map<String, List<String>>,
    inferenceResult: String?,
    connect: () -> Unit,
    discoverServices: () -> Unit,
    resultRead: () -> Unit
) {

    val foundTargetService = discoveredCharacteristics.contains(INFERENCE_SERVICE_UUID.toString())

    Column(
        Modifier.scrollable(rememberScrollState(), Orientation.Vertical)
    ) {
        Button(onClick = connect) {
            Text("1. Connect")
        }
        Text("Device connected: $isDeviceConnected")
        Button(onClick = discoverServices, enabled = isDeviceConnected) {
            Text("2. Discover Services")
        }
        LazyColumn {
            items(discoveredCharacteristics.keys.sorted()) { serviceUuid ->
                Text(text = serviceUuid, fontWeight = FontWeight.Black)
                Column(modifier = Modifier.padding(start = 10.dp)) {
                    discoveredCharacteristics[serviceUuid]?.forEach {
                        Text(it)
                    }
                }
            }
        }
        Button(onClick = resultRead, enabled = isDeviceConnected && foundTargetService) {
            Text("3. Read Inference Result")
        }
        if (inferenceResult != null) {
            Text("Read Inference Result: $inferenceResult")
        }

        OutlinedButton(modifier = Modifier.padding(top = 40.dp),  onClick = unselectDevice) {
            Text("Disconnect")
        }
    }
}