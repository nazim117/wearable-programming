package com.example.wearableprogramming

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.Wearable

class MainActivity : AppCompatActivity() {
    private lateinit var dataClient: DataClient
    private val heartRateReadings = mutableListOf<Float>()

    private fun calculateAverageHeartRate(): Float {
        return if (heartRateReadings.isNotEmpty()) {
            heartRateReadings.sum() / heartRateReadings.size
        } else {
            0f
        }
    }

    private fun addHeartRateReading(heartRate: Float){
        heartRateReadings.add(heartRate)
        if(heartRateReadings.size > 10){
            heartRateReadings.removeAt(0)
        }
    }

    private val heartRateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val heartRate = intent?.getFloatExtra("heart_rate", -1f) ?: -1f
            if (heartRate != -1f) {
                addHeartRateReading(heartRate)
                val averageHeartRate = calculateAverageHeartRate()
                Log.d("HeartRateReceiver", "Average Heart Rate: $averageHeartRate")

                if(averageHeartRate > 100){
                    sendHeartRateNotificationToWatch(heartRate)
                    Log.d("HeartRateReceiver", "Sending notification for heart rate: $heartRate")
                }
                updateHeartRateUI(heartRate)
            }
        }

    }

    private fun sendHeartRateNotificationToWatch(heartRate: Float) {
        Log.d("Notification", "Creating notification for heart rate: $heartRate")
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(this, "HEART_RATE_CHANNEL")
            .setSmallIcon(R.drawable.heart)
            .setContentTitle("High Heart Rate Alert")
            .setContentText("Your heart rate is $heartRate bpm")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        dataClient = Wearable.getDataClient(this)

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            if(checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!= PackageManager.PERMISSION_GRANTED){
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }
        checkConnection()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == 1001){
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Log.d("Permissions", "POST_NOTIFICATIONS permission granted.")
            }else{
                Log.d("Permissions", "POST_NOTIFICATIONS permission denied.")
            }
        }
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