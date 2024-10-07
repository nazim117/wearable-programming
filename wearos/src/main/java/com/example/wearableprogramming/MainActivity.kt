package com.example.wearableprogramming

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
import androidx.wear.tooling.preview.devices.WearDevices
import com.example.wearableprogramming.theme.WearableProgrammingTheme
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults

class MainActivity : ComponentActivity() {
    private var heartRate: Float? by mutableStateOf(null)
    private var isMonitoring: Boolean by mutableStateOf(false)
    private lateinit var handler: Handler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handler = Handler(Looper.getMainLooper())

        setContent {
            WearApp(
                heartRate = heartRate,
                isMonitoring = isMonitoring,
                onToggleMonitoring = { toggleHeartRateMonitoring() }
            )
        }

        checkConnection()
        simulateHeartRateData()
    }

    private fun toggleHeartRateMonitoring() {
        if (isMonitoring) {
            handler.removeCallbacksAndMessages(null)
        } else {
            simulateHeartRateData()
        }
        isMonitoring = !isMonitoring
    }

    private fun simulateHeartRateData() {
        val runnable = object : Runnable {
            override fun run() {
                val simulatedHeartRate = (60..120).random().toFloat()
                heartRate = simulatedHeartRate

                sendHeartRateToPhone(simulatedHeartRate)
                if (isMonitoring) {
                    handler.postDelayed(this, 3000)
                }
            }
        }
        handler.post(runnable)
    }

    private fun checkConnection() {
        Wearable.getNodeClient(this).connectedNodes
            .addOnSuccessListener { nodes ->
                if (nodes.isNotEmpty()) {
                    Log.d("Connection", "Connected to ${nodes.size} nodes")
                    nodes.forEach { node ->
                        Log.d("Connection", "Node ${node.id}: ${node.displayName}")
                    }
                } else {
                    Log.e("Connection", "No nodes connected")
                }
            }
            .addOnFailureListener { e ->
                Log.e("Connection", "Failed to get connected nodes", e)
            }
    }

    private fun sendHeartRateToPhone(heartRate: Float) {
        val dataMap = PutDataMapRequest.create("/heart_rate").apply {
            dataMap.putFloat("heart_rate", heartRate)
        }
        val request = dataMap.asPutDataRequest().setUrgent()

        Wearable.getDataClient(this).putDataItem(request)
            .addOnSuccessListener {
                Log.d("WearOS", "Heart rate sent successfully: $heartRate")
            }
            .addOnFailureListener { e ->
                Log.e("WearOS", "Failed to send heart rate", e)
            }
    }
}

@Composable
fun WearApp(
    heartRate: Float?,
    isMonitoring: Boolean,
    onToggleMonitoring: () -> Unit,
) {
    WearableProgrammingTheme {
        ScalingLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Card(
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colors.surface,
                        contentColor = MaterialTheme.colors.onSurface
                    )
                ) {
                    Text(
                        text = if (heartRate != null) "Heart Rate: ${heartRate.toInt()} bpm" else "Heart Rate: -- bpm",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.title1,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            item {
                Chip(
                    onClick = onToggleMonitoring,
                    label = {
                        Text(
                            if (isMonitoring) "Stop Monitoring" else "Start Monitoring",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colors.onSurface
                        )
                    },
                    colors = ChipDefaults.secondaryChipColors()
                )
            }
        }
    }
}



@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    WearApp(
        heartRate = 72f,
        isMonitoring = false,
        onToggleMonitoring = {}
    )
}
