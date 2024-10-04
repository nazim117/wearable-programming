package com.example.wearos.presentation

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.tooling.preview.devices.WearDevices
import com.example.wearos.presentation.theme.WearableProgrammingTheme
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable

class MainActivity : ComponentActivity() {
    private var heartRate: Float? by mutableStateOf(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set content for Compose UI
        setContent {
            WearApp(heartRate)
        }

        // Start simulating heart rate data
        simulateHeartRateData()

        // Check connection with phone app
        checkWearableConnection()
    }

    private fun checkWearableConnection() {
        Wearable.getNodeClient(this).connectedNodes
            .addOnSuccessListener { nodes ->
                if (nodes.isNotEmpty()) {
                    for (node in nodes) {
                        Log.d("WearOS", "Connected to node: ${node.displayName}")
                    }
                } else {
                    Log.d("WearOS", "No connected nodes found")
                }
            }
            .addOnFailureListener {
                Log.e("WearOS", "Failed to retrieve connected nodes", it)
            }
    }

    private fun simulateHeartRateData() {
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                // Generate a random heart rate between 60 and 120 bpm
                val simulatedHeartRate = (60..120).random().toFloat()
                heartRate = simulatedHeartRate

                sendHeartRateToPhone(simulatedHeartRate)
                handler.postDelayed(this, 2000) // Update every 2 seconds
            }
        }
        handler.post(runnable)
    }

    private fun sendHeartRateToPhone(heartRate: Float) {
        Log.d("WearOS", "Preparing to send heart rate: $heartRate bpm")
        val dataMapRequest = PutDataMapRequest.create("/heart_rate")
        dataMapRequest.dataMap.putFloat("heart_rate", heartRate)

        val putDataRequest = dataMapRequest.asPutDataRequest().setUrgent()
        Wearable.getDataClient(this).putDataItem(putDataRequest)
            .addOnSuccessListener { dataItem ->
                Log.d("WearOS", "Heart rate sent successfully: $heartRate bpm")
                Log.d("WearOS", "DataItem: ${dataItem.uri}")
            }
            .addOnFailureListener {e ->
                Log.e("WearOS", "Failed to send heart rate", e)
            }
    }
}

@Composable
fun WearApp(heartRate: Float?) {
    WearableProgrammingTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            contentAlignment = Alignment.Center
        ) {
            TimeText()

            if (heartRate != null) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = "Heart Rate: $heartRate bpm",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.primary
                )
            } else {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = "Heart Rate: -- bpm",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.primary
                )
            }
        }
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    WearApp(null)
}
