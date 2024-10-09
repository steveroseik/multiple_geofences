package com.roseik.multiple_geofences

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class GeofenceRebootBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Re-register geofences after reboot by starting GeofenceService
            ContextCompat.startForegroundService(
                context,
                Intent(context, GeofenceService::class.java)
            )
        }
    }
}
