package com.roseik.multiple_geofences

import android.app.Service
import android.content.Intent
import android.os.IBinder
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.dart.DartExecutor
import io.flutter.plugin.common.MethodChannel
import android.util.Log

class IsolateHolderService : Service() {
    companion object {
        private lateinit var flutterEngine: FlutterEngine
        private lateinit var methodChannel: MethodChannel

        fun invokeCallback(method: String, geofenceId: String) {
            if (::methodChannel.isInitialized) {
                Log.d("IsolateHolderService", "Invoking Dart callback: $method for Geofence: $geofenceId")
                methodChannel.invokeMethod(method, mapOf("geofenceId" to geofenceId))
            } else {
                Log.e("IsolateHolderService", "MethodChannel is not initialized!")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        flutterEngine = FlutterEngine(this)
        flutterEngine.dartExecutor.executeDartEntrypoint(
            DartExecutor.DartEntrypoint.createDefault()
        )
        methodChannel = MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            "com.roseik.multiple_geofences/geofencing"
        )
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
