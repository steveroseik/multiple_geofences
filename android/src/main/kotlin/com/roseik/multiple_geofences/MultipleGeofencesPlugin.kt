package com.roseik.multiple_geofences

import android.app.PendingIntent
import android.content.Intent
import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.NonNull
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.LocationServices
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.GeofenceStatusCodes
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import android.util.Log
import android.Manifest
import android.os.Build

/** MultipleGeofencesPlugin */
class MultipleGeofencesPlugin : FlutterPlugin, MethodChannel.MethodCallHandler {
  private lateinit var channel: MethodChannel
  private lateinit var context: Context
  private lateinit var geofencingClient: GeofencingClient
  private var geofencePendingIntent: PendingIntent? = null

  companion object {
    var methodChannel: MethodChannel? = null

    fun reRegisterGeofences(context: Context) {
      val prefs = context.getSharedPreferences("GeofencingPrefs", Context.MODE_PRIVATE)
      val allGeofences = prefs.all

      for ((geofenceId, geofenceData) in allGeofences) {
        if (geofenceData is String) {
          val geofenceParams = geofenceData.split(",")
          if (geofenceParams.size == 3) {
            val latitude = geofenceParams[0].toDoubleOrNull() ?: continue
            val longitude = geofenceParams[1].toDoubleOrNull() ?: continue
            val radius = geofenceParams[2].toFloatOrNull() ?: continue

            // Re-register the geofence
            MultipleGeofencesPlugin().startGeofencing(latitude, longitude, radius, geofenceId)
          }
        }
      }
    }
  }

  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "com.roseik.multiple_geofences/geofencing")
    channel.setMethodCallHandler(this)
    context = flutterPluginBinding.applicationContext
    geofencingClient = LocationServices.getGeofencingClient(context)
    methodChannel = channel
  }

  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: MethodChannel.Result) {
    when (call.method) {
      "requestLocationPermission" -> {
        if (hasLocationPermissions(result)) {
          result.success(true)
        } else {
          result.success(false)
        }
      }
      "startGeofencing" -> {
        Log.d("MultipleGeofencesPlugin", "startGeofencing ACTION")
        val latitude = call.argument<Double>("latitude") ?: 0.0
        val longitude = call.argument<Double>("longitude") ?: 0.0
        val radius = call.argument<Double>("radius")?.toFloat() ?: 100f
        val geofenceId = "geofence_${latitude}_${longitude}"
        startGeofencing(latitude, longitude, radius, geofenceId)
        result.success(geofenceId)
      }
      "stopGeofencing" -> {
        val geofenceId = call.argument<String>("geofenceId") ?: "default_geofence_id"
        stopGeofencing(geofenceId)
        result.success("Geofencing stopped for ID: $geofenceId")
      }
      else -> {
        result.notImplemented()
      }
    }
  }

  private fun hasLocationPermissions(result: MethodChannel.Result): Boolean {
    val fineLocationGranted = ActivityCompat.checkSelfPermission(
      context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    val backgroundLocationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      ActivityCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
      ) == PackageManager.PERMISSION_GRANTED
    } else {
      true
    }

    return fineLocationGranted && backgroundLocationGranted
  }

  private fun startGeofencing(latitude: Double, longitude: Double, radius: Float, geofenceId: String) {
    val geofence = Geofence.Builder()
      .setRequestId(geofenceId)
      .setCircularRegion(latitude, longitude, radius)
      .setExpirationDuration(Geofence.NEVER_EXPIRE)
      .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
      .build()

    val geofencingRequest = GeofencingRequest.Builder()
      .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
      .addGeofence(geofence)
      .build()

    if (geofencePendingIntent == null) {
      val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
      geofencePendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
      )
    }

    geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent!!)
      .addOnSuccessListener {
        saveGeofenceToSharedPreferences(geofenceId, latitude, longitude, radius)
      }
      .addOnFailureListener { e ->
        val errorMessage = if (e is com.google.android.gms.common.api.ApiException) {
          GeofenceStatusCodes.getStatusCodeString(e.statusCode)
        } else {
          e.localizedMessage ?: "Unknown error"
        }
        methodChannel?.invokeMethod("onMonitoringFailed", mapOf(
          "regionId" to geofenceId,
          "error" to errorMessage
        ))
      }
  }

  private fun saveGeofenceToSharedPreferences(geofenceId: String, latitude: Double, longitude: Double, radius: Float) {
    val prefs = context.getSharedPreferences("GeofencingPrefs", Context.MODE_PRIVATE)
    val editor = prefs.edit()
    val geofenceData = "$latitude,$longitude,$radius"
    editor.putString(geofenceId, geofenceData)
    editor.apply()
  }

  fun reRegisterGeofences(context: Context) {
    val prefs = context.getSharedPreferences("GeofencingPrefs", Context.MODE_PRIVATE)
    val allGeofences = prefs.all

    for ((geofenceId, geofenceData) in allGeofences) {
      if (geofenceData is String) {
        val geofenceParams = geofenceData.split(",")
        if (geofenceParams.size == 3) {
          val latitude = geofenceParams[0].toDoubleOrNull() ?: continue
          val longitude = geofenceParams[1].toDoubleOrNull() ?: continue
          val radius = geofenceParams[2].toFloatOrNull() ?: continue
          startGeofencing(latitude, longitude, radius, geofenceId)
        }
      }
    }
  }


  private fun stopGeofencing(geofenceId: String) {
    geofencingClient.removeGeofences(listOf(geofenceId))
      .addOnSuccessListener {
        methodChannel?.invokeMethod("onMonitoringStopped", mapOf("regionId" to geofenceId))
      }
      .addOnFailureListener { e ->
        methodChannel?.invokeMethod("onMonitoringFailed", mapOf(
          "regionId" to geofenceId,
          "error" to e.message
        ))
      }
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
    methodChannel = null
  }
}
