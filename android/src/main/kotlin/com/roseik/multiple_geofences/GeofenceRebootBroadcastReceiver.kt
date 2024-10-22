package com.roseik.multiple_geofences

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat

class GeofenceRebootBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.i("GeofenceRebootBroadcastReceiver", "RBF:: Received broadcast: ${intent.action}")

        try {
            // Start the GeofenceService as a foreground service
            val serviceIntent = Intent(context, GeofenceService::class.java).apply {
                putExtras(intent)
            }
            ContextCompat.startForegroundService(context, serviceIntent)

            MultipleGeofencesPlugin.reRegisterGeofences(context)
        } catch (e: Exception) {
            Log.e("GeofenceBroadcastReceiver", "RBF:: Failed to start GeofenceService", e)
        }

        // Re-register geofences

    }
}
