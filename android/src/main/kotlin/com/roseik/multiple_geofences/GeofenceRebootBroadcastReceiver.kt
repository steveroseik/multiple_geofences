package com.roseik.multiple_geofences

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class GeofencingRebootBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("GeofencingRebootReceiver", "Re-registering geofences after reboot")
            MultipleGeofencesPlugin.reRegisterGeofences(context)
        }
    }
}
