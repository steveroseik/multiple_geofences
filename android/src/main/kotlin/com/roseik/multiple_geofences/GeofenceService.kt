package com.roseik.multiple_geofences

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.location.GeofenceStatusCodes
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.dart.DartExecutor
import io.flutter.plugin.common.MethodChannel

class GeofenceService : Service() {

    private val WAKELOCK_TAG = "GeofenceService::WAKE_LOCK"
    private var wakeLock: PowerManager.WakeLock? = null
    private var flutterEngine: FlutterEngine? = null
    companion object {
        var isServiceRunning: Boolean = false

    }

    override fun onCreate() {
        super.onCreate()
        Log.d("GeofenceService", "RBF:: GeofenceService started.")

        try{
            Log.d("GeofenceService", "RBF:: Starting GeofenceService...")
            flutterEngine = FlutterEngine(this)
            Log.d("GeofenceService", "RBF:: FlutterEngine created: ${flutterEngine}")
            flutterEngine?.dartExecutor?.executeDartEntrypoint(
                DartExecutor.DartEntrypoint.createDefault()
            )

            flutterEngine?.let {
                Log.d("GeofenceService", "RBF:: FlutterEngine set: ${it}")
                MethodChannel(it.dartExecutor.binaryMessenger, "com.roseik.multiple_geofences/geofencing")
                    .setMethodCallHandler { call, result ->
                        // Handle method calls if necessary
                    }
            }

            // Start foreground service as early as possible
            startForegroundServiceWithNotification()

            acquireWakeLock()
            isServiceRunning = true

            // Notify Flutter that the service has successfully started
            flutterEngine?.let {
                MethodChannel(it.dartExecutor.binaryMessenger, "com.roseik.multiple_geofences/geofencing")
                    .invokeMethod("onServiceStarted", true)
            }
        }catch (e: Exception){
            Log.e("GeofenceService", "RBF:: Failed to start GeofenceService: ${e.message}")
        }
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
        Log.i("GeofenceService", "RBF:: Started foreground service with notification.")
        invokeFlutterMethod("onServiceStarted", mapOf("isServiceRunning" to true))
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
        if (!isServiceRunning) {
            Log.e("GeofenceService", "RBF:: Service failed to start properly in onCreate.")
        } else {
            intent?.let {
                handleGeofenceEvent(it)
            }
        }
        return START_STICKY
    }

    private fun handleGeofenceEvent(intent: Intent) {
        Log.d("GeofenceService", "RBF:: Handling geofence event: ${intent}")
        val geofencingEvent = GeofencingEvent.fromIntent(intent)

        if (geofencingEvent?.hasError() == true) {
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
            Log.e("GeofenceService", "RBF:: Error: $errorMessage")
            return
        }

        Log.d("GeofenceService", "RBF:: Geofence event: $geofencingEvent")
        Log.d("GeofenceService", "RBF:: Geofence Transiition: ${geofencingEvent?.geofenceTransition}")

        geofencingEvent?.let { event ->
            val geofenceTransition = event.geofenceTransition
            val triggeringGeofences = event.triggeringGeofences ?: emptyList()

            for (geofence in triggeringGeofences) {
                val geofenceId = geofence.requestId
                when (geofenceTransition) {
                    Geofence.GEOFENCE_TRANSITION_ENTER -> {
                        Log.d("GeofenceService", "Entered geofence with ID: $geofenceId")

                        invokeFlutterMethod("onEnterRegion", mapOf("regionId" to geofenceId))
                    }
                    Geofence.GEOFENCE_TRANSITION_EXIT -> {
                        Log.d("GeofenceService", "Exited geofence with ID: $geofenceId")

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
        try {
            if (flutterEngine != null) {
                Log.d("GeofenceService", "RBF:: Invoking Flutter method: $method -> $arguments")
                MethodChannel(flutterEngine!!.dartExecutor.binaryMessenger, "com.roseik.multiple_geofences/geofencing")
                    .invokeMethod(method, arguments)
            } else {
                Log.e("GeofenceService", "RBF:: FlutterEngine is null. Unable to invoke method: $method")
                // You can log or handle the situation here where FlutterEngine is not available.
            }
        } catch (e: Exception) {
            Log.e("GeofenceService", "RBF:: Failed to invoke Flutter method: ${e.message}")
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseWakeLock()
        flutterEngine?.destroy() // Clean up the FlutterEngine when the service is destroyed
        isServiceRunning = false
    }
}
