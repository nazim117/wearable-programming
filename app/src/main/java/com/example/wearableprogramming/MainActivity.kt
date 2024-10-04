package com.example.wearableprogramming

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable

class MainActivity : AppCompatActivity(), SensorEventListener, DataClient.OnDataChangedListener {
    private val BODY_SENSORS_PERMISSION_CODE = 100
    private lateinit var sensorManager: SensorManager
    private var heartRateSensor: Sensor? = null
    private val CHANNEL_ID = "heart_rate_alerts"
    private var connectedNodeId: String? = null

    private fun checkPermission(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED){
            Log.d("HeartRateMonitor", "Requesting BODY_SENSORS permissions")
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BODY_SENSORS),
                BODY_SENSORS_PERMISSION_CODE
            )
        }else{
            Log.d("HeartRateMonitor", "BODY_SENSORS permissions granted")
            initializeHeartRateSensor()
        }
    }

//    private fun simulateHeartRateData() {
//        val handler = android.os.Handler(Looper.getMainLooper())
//        val runnable = object : Runnable {
//            override fun run() {
//                val simulatedHeartRate = (60..120).random().toFloat()
//                updateHeartRateUI(simulatedHeartRate)
//                checkHeartRateThreshold(simulatedHeartRate)
//                handler.postDelayed(this, 2000) // Update every 5 seconds
//            }
//        }
//        handler.post(runnable)
//    }

    private fun checkConnectedWearable(retryCount: Int = 3) {
        Wearable.getNodeClient(this).connectedNodes
            .addOnSuccessListener { nodes ->
                if (nodes.isNotEmpty()) {
                    connectedNodeId = nodes.first().id
                    nodes.forEach { node ->
                        Log.d("PhoneApp", "Connected to node: ${node.displayName}")
                    }
                    Log.d("PhoneApp", "Connected to node ID: $connectedNodeId")
                    startDataSync()
                } else {
                    Log.d("PhoneApp", "No connected wearables found")
                    if (retryCount > 0) {
                        Log.d("PhoneApp", "Retrying to find nodes, attempts left: $retryCount")
                        Handler(Looper.getMainLooper()).postDelayed({
                            checkConnectedWearable(retryCount - 1)
                        }, 2000)
                    }
                }
            }
            .addOnFailureListener {
                Log.e("PhoneApp", "Failed to retrieve connected nodes", it)
            }
    }


    private fun startDataSync() {
        Log.d("WearOS", "Starting data sync")
        Wearable.getDataClient(this).dataItems
            .addOnSuccessListener { dataItems ->
                for(dataItem in dataItems){
                    Log.d("PhoneApp", "Existing DataItem URI: ${dataItem.uri}")
                }
            }
            .addOnFailureListener {
                Log.e("PhoneApp", "Failed to get data items", it)
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkPermission()
        createNotificationChannel()
        checkConnectedWearable()
        checkExistingDataItems()
    }

    private fun checkExistingDataItems() {
        Wearable.getDataClient(this).dataItems
            .addOnSuccessListener { dataItems ->
                for (dataItem in dataItems) {
                    Log.d("PhoneApp", "Existing DataItem: ${dataItem.uri}")
                    if (dataItem.uri.path == "/heart_rate") {
                        val dataMap = DataMapItem.fromDataItem(dataItem).dataMap
                        val heartRate = dataMap.getFloat("heart_rate")
                        Log.d("PhoneApp", "Found heart rate: $heartRate")
                        updateHeartRateUI(heartRate)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("PhoneApp", "Failed to get data items", e)
            }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(requestCode == BODY_SENSORS_PERMISSION_CODE){
            if((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Log.d("HearRateMonitor", "BODY_SENSORS permissions granted by user")
            } else {
                Log.d("HeartRateMonitor", "BODY_SENSORS permissions denied by user")
                Toast.makeText(this, "Permission denied to read heart rate data", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initializeHeartRateSensor(){
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)

        if(heartRateSensor != null){
            Log.d("HeartRateMonitor", "Heart Rate sensor found: ${heartRateSensor?.name}")
        }else{
            Toast.makeText(this, "Heart Rate sensor not available, using simulated data", Toast.LENGTH_SHORT).show()
            Log.d("HeartRateMonitor", "Heart Rate sensor is not available, using simulated data")
        }
    }

    private fun startHeartRateMonitoring() {
        if(heartRateSensor == null){
            initializeHeartRateSensor()
        }
        heartRateSensor?.also { sensor ->
            Log.d("HeartRateMonitor", "Heart rate sensor registered")
            sensorManager.registerListener(
                this,
                sensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        Log.d("HeartRateMonitor", "Sensor changed event triggered")
        event?.let{
            val heartRate = it.values[0]
            Log.d("HeartRateMonitor", "Heart rate: $heartRate"  )
            updateHeartRateUI(heartRate)
            checkHeartRateThreshold(heartRate)
        }
    }

    private fun sendHeartRateToWearable(heartRate: Float){
        val dataMap = PutDataMapRequest.create("/heart_rate")
        dataMap.dataMap.putFloat("heart_rate", heartRate)

        val putDataRequest = dataMap.asPutDataRequest().setUrgent()
        Wearable.getDataClient(this).putDataItem(putDataRequest)
            .addOnSuccessListener {
                Log.d("WearOS", "heart rate sent successfully: $heartRate bpm")
            }
            .addOnFailureListener {
                Log.e("WearOS", "Failed to sned heart rate", it)
            }
    }

    private fun checkHeartRateThreshold(heartRate: Float) {
        if(heartRate > HEART_RATE_THRESHOLD){
            sendHeartRateAlert(heartRate)
        }
    }

    override fun onStart(){
        super.onStart()

        Wearable.getDataClient(this).addListener(this)
        Log.d("PhoneApp", "Data listener registered")
    }

    override fun onResume(){
        super.onResume()
        Wearable.getDataClient(this).addListener(this)
    }

    override fun onPause(){
        super.onPause()
        Wearable.getDataClient(this).removeListener(this)
    }

    override fun onStop(){
        super.onStop()

        Wearable.getDataClient(this).removeListener(this)
        Log.d("PhoneApp", "Data listener unregistered")
    }

    private fun sendHeartRateAlert(heartRate: Float) {
        val builder = NotificationCompat.Builder(this,CHANNEL_ID)
            .setSmallIcon(R.drawable.heart)
            .setContentTitle("High Heart Rate Alert")
            .setContentText("Your heart rate is $heartRate bpm, which exceeds the threshold.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVibrate(longArrayOf(0, 500, 1000))
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(this)){
            notify(HEART_RATE_NOTIFICATION_ID, builder.build())
        }
    }

    private fun updateHeartRateUI(heartRate: Float) {
        val heartRateTextView = findViewById<TextView>(R.id.heart_rate_text_view)
        Log.d("HeartRateMonitor", "Updating UI: Heart Rate: $heartRate")
        heartRateTextView.text = "Heart rate: $heartRate bpm"
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.d("HeartRateMonitor", "Sensor accuracy changed: $accuracy")
    }

    private fun createNotificationChannel(){
        val name = "Heart Rate Alerts"
        val descriptionText = "Notifications for heart rate thresholds"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }

        val notificationManager : NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        private const val HEART_RATE_NOTIFICATION_ID = 1
        private const val HEART_RATE_THRESHOLD = 100f
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        Log.d("PhoneApp", "onDataChanged triggered")
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED) {
                val dataItem = event.dataItem
                Log.d("PhoneApp", "Data item received with URI: ${dataItem.uri.path}")
                if (dataItem.uri.path == "/heart_rate") {
                    val dataMap = DataMapItem.fromDataItem(dataItem).dataMap
                    val heartRate = dataMap.getFloat("heart_rate")
                    Log.d("PhoneApp", "Received heart rate: $heartRate bpm")
                    runOnUiThread {
                        updateHeartRateUI(heartRate)
                    }
                }
            }
        }
    }
}