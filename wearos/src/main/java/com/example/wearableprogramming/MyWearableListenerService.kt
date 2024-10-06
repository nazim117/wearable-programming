package com.example.wearableprogramming

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService

class MyWearableListenerService : WearableListenerService() {
    private val heartRateReadings = mutableListOf<Float>()

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            Log.d("WearableListener", "Data change event received")
            if (event.type == DataEvent.TYPE_CHANGED) {
                val uriPath = event.dataItem.uri.path
                Log.d("WearableListener", "Data URI path: $uriPath")

                if (uriPath == "/heart_rate") {
                    val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                    val heartRate = dataMap.getFloat("heart_rate")
                    Log.d("WearableListener", "Received heart rate: $heartRate")

                    val intent = Intent("HeartRateUpdate")
                    intent.putExtra("heart_rate", heartRate)
                    LocalBroadcastManager.getInstance(this).sendBroadcast(intent)

                    if (heartRate != -1f) {
                        addHeartRateReading(heartRate)
                        val averageHeartRate = calculateAverageHeartRate()
                        Log.d("HeartRateReceiver", "Average Heart Rate: $averageHeartRate")

                        if(averageHeartRate > 100){
                            sendHeartRateNotificationToWatch(heartRate)
                            Log.d("HeartRateReceiver", "Sending notification for heart rate: $heartRate")
                        }
                    }
                }
            }
        }
    }

    private fun addHeartRateReading(heartRate: Float){
        heartRateReadings.add(heartRate)
        if(heartRateReadings.size > 10){
            heartRateReadings.removeAt(0)
        }
    }

    private fun calculateAverageHeartRate(): Float {
        return if (heartRateReadings.isNotEmpty()) {
            heartRateReadings.sum() / heartRateReadings.size
        } else {
            0f
        }
    }

    private fun createNotificationChannel(){
        val name = "Heart Rate Alerts"
        val descText = "Notifications for heart rate data"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel("HEART_RATE_CHANNEL", name, importance).apply {
            description = descText
        }

        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun sendHeartRateNotificationToWatch(heartRate: Float) {
        Log.d("Notification", "Creating notification for heart rate: $heartRate")

        createNotificationChannel()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(this, "HEART_RATE_CHANNEL")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("High Heart Rate Alert")
            .setContentText("Your heart rate is $heartRate bpm")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)
    }
}
