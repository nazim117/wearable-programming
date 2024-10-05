package com.example.wearableprogramming

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.Wearable

class MainActivity : AppCompatActivity() {
    private lateinit var dataClient: DataClient

    private val heartRateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val heartRate = intent?.getFloatExtra("heart_rate", -1f) ?: -1f
            if (heartRate != -1f) {
                updateHeartRateUI(heartRate)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        dataClient = Wearable.getDataClient(this)
        checkConnection()
    }

    override fun onStart(){
        super.onStart()
        LocalBroadcastManager.getInstance(this).registerReceiver(
            heartRateReceiver, IntentFilter("HeartRateUpdate")
        )
    }

    override fun onStop(){
        super.onStop()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(heartRateReceiver)
    }

    private fun updateHeartRateUI(heartRate: Float) {
        val heartRateTextView = findViewById<TextView>(R.id.heart_rate_text_view)
        Log.d("HeartRateMonitor", "Updating UI: Heart Rate: $heartRate")
        heartRateTextView.text = "Heart rate: $heartRate bpm"
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
}