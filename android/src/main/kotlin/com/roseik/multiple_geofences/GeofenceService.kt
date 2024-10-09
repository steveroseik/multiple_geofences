package com.roseik.multiple_geofences

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.JobIntentService
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.Geofence
import androidx.core.app.NotificationCompat
import android.os.PowerManager
import android.os.Build
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class GeofenceService : JobIntentService() {

    private val WAKELOCK_TAG = "GeofenceService::WAKE_LOCK"
    private var wakeLock: PowerManager.WakeLock? = null
    private var flutterEngine: FlutterEngine? = null

    companion object {
        const val JOB_ID = 12378123

        fun enqueueWork(context: Context, intent: Intent) {
            Log.d("GeofenceService", "Enqueuing work to GeofenceService")
            enqueueWork(context, GeofenceService::class.java, JOB_ID, intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForegroundService()
        acquireWakeLock()

        // Initialize FlutterEngine to communicate with Dart
        flutterEngine = FlutterEngine(this)
        flutterEngine?.dartExecutor?.let {
            MethodChannel(it.binaryMessenger, "com.roseik.multiple_geofences/geofencing")
        }
    }

    private fun startForegroundService() {
        val CHANNEL_ID = "high_importance_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Flutter Geofencing Plugin",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Channel for geofence notifications"
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }

        val imageId = resources.getIdentifier("ic_launcher", "mipmap", packageName)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Geofencing Active")
            .setContentText("Monitoring your geofence location")
            .setSmallIcon(imageId)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        startForeground(1, notification)
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG)
        wakeLock?.acquire(10 * 60 * 1000L /*10 minutes*/)
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
    }

    override fun onHandleWork(intent: Intent) {
        Log.d("GeofenceService", "Handling geofence event")
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
            flutterEngine?.dartExecutor?.let {
                MethodChannel(it.binaryMessenger, "com.roseik.multiple_geofences/geofencing")
                    .invokeMethod(method, arguments)
            }
        }
    }

    private fun sendEnterNotification() {
        val CHANNEL_ID = "high_importance_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Flutter Geofencing Plugin",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Channel for geofence notifications"
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }

        val imageId = resources.getIdentifier("ic_launcher", "mipmap", packageName)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Entered Region")
            .setContentText("Entered current fence")
            .setSmallIcon(imageId)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        startForeground(1, notification)
    }

    private fun sendLeaveNotification() {
        val CHANNEL_ID = "high_importance_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Flutter Geofencing Plugin",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Channel for geofence notifications"
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }

        val imageId = resources.getIdentifier("ic_launcher", "mipmap", packageName)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Left Region")
            .setContentText("Left current fence")
            .setSmallIcon(imageId)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        startForeground(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseWakeLock()
        flutterEngine?.destroy() // Destroy the Flutter engine when the service is destroyed
    }
}
