package com.roseik.multiple_geofences

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.roseik.multiple_geofences.GeofenceService

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.i("GeofenceBroadcastReceiver", "RBF:: Geofence event received.")

        // Acquire a temporary wake lock to ensure the device does not go to sleep before starting the service
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GeofenceReceiver::WakeLock")
        wakeLock.acquire(10 * 60 * 1000L /*10 minutes*/) // Acquire for up to 10 minutes

        try {
            Log.i("GeofenceBroadcastReceiver", "RBF:: intiializing service intent")
            // Start the GeofenceService as a foreground service
            val serviceIntent = Intent(context, GeofenceService::class.java).apply {
                putExtras(intent)
            }
            ContextCompat.startForegroundService(context, serviceIntent)
            Log.i("GeofenceBroadcastReceiver", "RBF:: Service started")
        } catch (e: Exception) {
            Log.e("GeofenceBroadcastReceiver", "RBF:: Failed to start GeofenceService from receiver", e)
        } finally {
            // Release the wake lock after starting the service
            wakeLock.release()
        }
    }
}
