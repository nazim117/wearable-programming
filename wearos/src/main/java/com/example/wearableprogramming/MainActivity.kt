package com.example.wearableprogramming

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
import com.example.wearableprogramming.theme.WearableProgrammingTheme
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable

class MainActivity : ComponentActivity() {
    private var heartRate: Float? by mutableStateOf(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            WearApp(heartRate)
        }

        checkConnection()
        simulateHeartRateData()
    }

    private fun simulateHeartRateData() {
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                val simulatedHeartRate = (60..120).random().toFloat()
                heartRate = simulatedHeartRate

                sendHeartRateToPhone(simulatedHeartRate)
                handler.postDelayed(this, 3000)
            }
        }
        handler.post(runnable)
    }

    private fun checkConnection(){
        Wearable.getNodeClient(this).connectedNodes
            .addOnSuccessListener { nodes ->
                if(nodes.isNotEmpty()){
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
