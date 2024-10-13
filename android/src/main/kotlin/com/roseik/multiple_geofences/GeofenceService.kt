package com.roseik.multiple_geofences

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.location.GeofenceStatusCodes
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.dart.DartExecutor
import io.flutter.plugin.common.MethodChannel

class GeofenceService : Service() {

    private val WAKELOCK_TAG = "GeofenceService::WAKE_LOCK"
    private var wakeLock: PowerManager.WakeLock? = null
    private lateinit var flutterEngine: FlutterEngine

    override fun onCreate() {
        super.onCreate()
        Log.d("GeofenceService", "GeofenceService started.")

        // Create and start a FlutterEngine
        flutterEngine = FlutterEngine(this)
        flutterEngine.dartExecutor.executeDartEntrypoint(
            DartExecutor.DartEntrypoint.createDefault()
        )

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, "com.roseik.multiple_geofences/geofencing")
            .setMethodCallHandler { call, result ->
                // Handle method calls if necessary
            }

        acquireWakeLock()
        startForegroundServiceWithNotification()
    }

    private fun startForegroundServiceWithNotification() {
        val CHANNEL_ID = "geofencing_notification_channel"
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                CHANNEL_ID,
                "Geofencing Service",
                android.app.NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Channel for geofencing service notifications"
            val notificationManager = getSystemService(android.app.NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Geofencing Active")
            .setContentText("Monitoring your geofence location")
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        startForeground(1, notification)
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG)
        wakeLock?.acquire(10 * 60 * 1000L) // Acquire for up to 10 minutes
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            handleGeofenceEvent(it)
        }
        return START_STICKY
    }

    private fun handleGeofenceEvent(intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)

        if (geofencingEvent?.hasError() == true) {
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
            Log.e("GeofenceService", "Error: $errorMessage")
            return
        }

        geofencingEvent?.let { event ->
            val geofenceTransition = event.geofenceTransition
            val triggeringGeofences = event.triggeringGeofences ?: emptyList()

            for (geofence in triggeringGeofences) {
                val geofenceId = geofence.requestId
                when (geofenceTransition) {
                    Geofence.GEOFENCE_TRANSITION_ENTER -> {
                        Log.d("GeofenceService", "Entered geofence with ID: $geofenceId")
                        sendEnterNotification()
                        invokeFlutterMethod("onEnterRegion", mapOf("regionId" to geofenceId))
                    }
                    Geofence.GEOFENCE_TRANSITION_EXIT -> {
                        Log.d("GeofenceService", "Exited geofence with ID: $geofenceId")
                        sendLeaveNotification()
                        invokeFlutterMethod("onExitRegion", mapOf("regionId" to geofenceId))
                    }
                    else -> {
                        Log.e("GeofenceService", "Unknown geofence transition: $geofenceTransition")
                    }
                }
            }
        }
    }

    private fun invokeFlutterMethod(method: String, arguments: Map<String, Any>) {
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, "com.roseik.multiple_geofences/geofencing")
            .invokeMethod(method, arguments)
    }

    private fun sendEnterNotification() {
        val CHANNEL_ID = "geofencing_notification_channel"
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Entered Region")
            .setContentText("You have entered a monitored geofence area.")
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(this).notify(2, notification)
    }

    private fun sendLeaveNotification() {
        val CHANNEL_ID = "geofencing_notification_channel"
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Left Region")
            .setContentText("You have left a monitored geofence area.")
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(this).notify(3, notification)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseWakeLock()
        flutterEngine.destroy() // Clean up the FlutterEngine when the service is destroyed
    }
}