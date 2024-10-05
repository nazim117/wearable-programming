package com.example.wearableprogramming

import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService

class MyWearableListenerService : WearableListenerService() {
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
                }
            }
        }
    }
}
