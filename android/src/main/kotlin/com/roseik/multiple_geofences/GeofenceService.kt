package com.roseik.multiple_geofences

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.location.GeofenceStatusCodes
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class GeofenceService : Service() {

    private val WAKELOCK_TAG = "GeofenceService::WAKE_LOCK"
    private var wakeLock: PowerManager.WakeLock? = null
    private lateinit var flutterEngine: FlutterEngine

    override fun onCreate() {
        super.onCreate()
        startForegroundServiceWithNotification()
        acquireWakeLock()

        // Initialize a new FlutterEngine for handling background method calls
        flutterEngine = FlutterEngine(this)
        flutterEngine.dartExecutor.executeDartEntrypoint(
            io.flutter.embedding.engine.dart.DartExecutor.DartEntrypoint.createDefault()
        )
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, "com.roseik.multiple_geofences/geofencing")
    }

    private fun startForegroundServiceWithNotification() {
        val CHANNEL_ID = "geofencing_notification_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Geofencing Service",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Channel for geofencing service notifications"
            val notificationManager = getSystemService(NotificationManager::class.java)
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
        wakeLock?.acquire(10 * 60 * 1000L) // 10 minutes wake lock
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            handleGeofenceEvent(intent)
        }
        return START_STICKY // Ensures the service is restarted if killed
    }

    private fun handleGeofenceEvent(intent: Intent) {
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

    private fun invokeFlutterMethod(method: String, arguments: Map<String, Any>) {
        Handler(Looper.getMainLooper()).post {
            MethodChannel(flutterEngine.dartExecutor.binaryMessenger, "com.roseik.multiple_geofences/geofencing")
                .invokeMethod(method, arguments)
        }
    }

    private fun sendEnterNotification() {
        val CHANNEL_ID = "geofencing_notification_channel"
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Entered Region")
            .setContentText("You have entered a monitored geofence area.")
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(2, notification)
    }

    private fun sendLeaveNotification() {
        val CHANNEL_ID = "geofencing_notification_channel"
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Left Region")
            .setContentText("You have left a monitored geofence area.")
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(3, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseWakeLock()
        flutterEngine.destroy() // Clean up the FlutterEngine when the service is destroyed
    }

    override fun onBind(intent: Intent?) = null
}
