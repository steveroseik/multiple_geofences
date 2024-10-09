package com.roseik.multiple_geofences

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Start GeofenceService to handle the geofence event as a foreground service
        // Enqueue work to GeofenceService to handle the geofence event properly
        GeofenceService.enqueueWork(context, intent)
//        ContextCompat.startForegroundService(
//            context,
//            Intent(context, GeofenceService::class.java).apply {
//                putExtras(intent)
//            }
//        )
    }
}
