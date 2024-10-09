package com.roseik.multiple_geofences

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.JobIntentService
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.Geofence

class GeofenceService : JobIntentService() {

    companion object {
        const val JOB_ID = 12378123

        fun enqueueWork(context: Context, intent: Intent) {
            enqueueWork(context, GeofenceService::class.java, JOB_ID, intent)
        }
    }

    override fun onHandleWork(intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)

        if (geofencingEvent?.hasError() == true) {
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
            Log.e("GeofenceService", "Error: $errorMessage")
            return
        }

        val geofenceTransition = geofencingEvent?.geofenceTransition
        val triggeringGeofences = geofencingEvent?.triggeringGeofences ?: emptyList()

        for (geofence in triggeringGeofences) {
            val geofenceId = geofence.requestId
            when (geofenceTransition) {
                Geofence.GEOFENCE_TRANSITION_ENTER -> {
                    Log.d("GeofenceService", "Entered geofence with ID: $geofenceId")
                    Handler(Looper.getMainLooper()).post {
                        MultipleGeofencesPlugin.methodChannel?.invokeMethod(
                            "onEnterRegion", mapOf("regionId" to geofenceId)
                        )
                    }
                }
                Geofence.GEOFENCE_TRANSITION_EXIT -> {
                    Log.d("GeofenceService", "Exited geofence with ID: $geofenceId")
                    Handler(Looper.getMainLooper()).post {
                        MultipleGeofencesPlugin.methodChannel?.invokeMethod(
                            "onExitRegion", mapOf("regionId" to geofenceId)
                        )
                    }
                }
                else -> {
                    Log.e("GeofenceService", "Unknown geofence transition: $geofenceTransition")
                }
            }
        }
    }
}
