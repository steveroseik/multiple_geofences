package com.roseik.multiple_geofences

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.Geofence

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("GeofenceBroadcast", "Geofence event received")

        // Enqueue the work for the JobIntentService
        GeofenceService.enqueueWork(context, intent)

        // Handle the geofencing event
        val geofencingEvent = GeofencingEvent.fromIntent(intent)

        if (geofencingEvent == null) {
            Log.e("GeofenceBroadcast", "Geofencing event is null")
            MultipleGeofencesPlugin.methodChannel?.invokeMethod("onMonitoringFailed", mapOf(
                "regionId" to "unknown",
                "error" to "Geofencing event is null"
            ))
            return
        }

        if (geofencingEvent.hasError()) {
            val errorCode = geofencingEvent.errorCode
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(errorCode)
            Log.e("GeofenceBroadcast", "Error code: $errorCode - $errorMessage")
            MultipleGeofencesPlugin.methodChannel?.invokeMethod("onMonitoringFailed", mapOf(
                "regionId" to "unknown",
                "error" to errorMessage
            ))
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition
        val triggeringGeofences = geofencingEvent.triggeringGeofences

        if (triggeringGeofences != null) {
            for (geofence in triggeringGeofences) {
                val geofenceId = geofence.requestId
                when (geofenceTransition) {
                    Geofence.GEOFENCE_TRANSITION_ENTER -> {
                        Log.d("GeofenceBroadcast", "Entered geofence with ID: $geofenceId")
                        MultipleGeofencesPlugin.methodChannel?.invokeMethod("onEnterRegion", mapOf("regionId" to geofenceId))
                    }
                    Geofence.GEOFENCE_TRANSITION_EXIT -> {
                        Log.d("GeofenceBroadcast", "Exited geofence with ID: $geofenceId")
                        MultipleGeofencesPlugin.methodChannel?.invokeMethod("onExitRegion", mapOf("regionId" to geofenceId))
                    }
                    else -> {
                        Log.e("GeofenceBroadcast", "Unknown geofence transition: $geofenceTransition")
                        MultipleGeofencesPlugin.methodChannel?.invokeMethod("onUnexpectedAction", geofenceTransition.toString())
                    }
                }
            }
        } else {
            Log.e("GeofenceBroadcast", "No triggering geofences")
        }
    }
}
