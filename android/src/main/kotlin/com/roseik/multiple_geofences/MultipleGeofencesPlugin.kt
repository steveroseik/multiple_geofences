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
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import android.util.Log
import android.Manifest
import android.app.AlertDialog
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.app.Activity

/** MultipleGeofencesPlugin */
class MultipleGeofencesPlugin : FlutterPlugin, MethodCallHandler, ActivityAware {

  private lateinit var channel: MethodChannel
  private lateinit var context: Context
  private var activity: Activity? = null
  private lateinit var geofencingClient: GeofencingClient
  private var geofencePendingIntent: PendingIntent? = null

  companion object {
    var methodChannel: MethodChannel? = null

    fun reRegisterGeofences(context: Context) {
      try{
        Log.i("MultipleGeofencesPlugin", "RBF:: reRegisterGeofences ACTION")

        // Initialize geofencing client using the provided context to ensure it is available after a reboot
        val geofencingClient = LocationServices.getGeofencingClient(context)

        val prefs = context.getSharedPreferences("GeofencingPrefs", Context.MODE_PRIVATE)
        val geofences = prefs.getStringSet("geofences", mutableSetOf()) ?: mutableSetOf()

        for (geofenceEntry in geofences) {
          val parts = geofenceEntry.split(":")
          if (parts.size == 2) {
            val geofenceId = parts[0]
            val geofenceParams = parts[1].split(",")
            if (geofenceParams.size == 3) {
              val latitude = geofenceParams[0].toDoubleOrNull() ?: continue
              val longitude = geofenceParams[1].toDoubleOrNull() ?: continue
              val radius = geofenceParams[2].toFloatOrNull() ?: continue
              Log.i("MultipleGeofencesPlugin", "RBF:: Re-registering geofence with ID: $geofenceId")

              // Re-register the geofence using the newly created client
              startGeofencing(context, geofencingClient, latitude, longitude, radius, geofenceId)
            }
          }
        }
        Log.i("MultipleGeofencesPlugin", "RBF:: reRegisterGeofences END")
      }catch(e: Exception){
        Log.e("MultipleGeofencesPlugin", "RBF:: Failed to re-register geofences: ${e.message}")
        throw e
      }
    }

    private fun startGeofencing(
      context: Context,
      geofencingClient: GeofencingClient,
      latitude: Double,
      longitude: Double,
      radius: Float,
      geofenceId: String
    ) {
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

      val geofencePendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, GeofenceBroadcastReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
      )

      geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)
        .addOnSuccessListener {
          Log.i("MultipleGeofencesPlugin", "Geofence added successfully with ID: $geofenceId")
        }
        .addOnFailureListener { e ->
          val errorMessage = if (e is com.google.android.gms.common.api.ApiException) {
            GeofenceStatusCodes.getStatusCodeString(e.statusCode)
          } else {
            e.localizedMessage ?: "Unknown error"
          }
          Log.e("MultipleGeofencesPlugin", "Failed to add geofence: $errorMessage")
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

  override fun onAttachedToActivity(binding: ActivityPluginBinding) {
    activity = binding.activity
  }

  override fun onDetachedFromActivity() {
    activity = null
  }

  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
    activity = binding.activity
  }

  override fun onDetachedFromActivityForConfigChanges() {
    activity = null
  }

  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: MethodChannel.Result) {
    when (call.method) {

      "openLocationSettings" -> {
        activity?.let { openLocationSettings(it) }
        result.success(null)
      }
      "isBatteryOptimizationIgnored" -> {
        val isIgnoringBatteryOptimizations = isBatteryOptimizationIgnored(context)
        result.success(isIgnoringBatteryOptimizations)
      }
      "requestIgnoreBatteryOptimization" -> {
        activity?.let { requestIgnoreBatteryOptimization(it) }
        result.success(null)
      }
      "requestLocationPermission" -> {
        hasLocationPermissions(result)
      }
      "startGeofencing" -> {
        Log.d("MultipleGeofencesPlugin", "startGeofencing ACTION")
        val latitude = call.argument<Double>("latitude") ?: 0.0
        val longitude = call.argument<Double>("longitude") ?: 0.0
        val radius = call.argument<Double>("radius")?.toFloat() ?: 100f
        val geofenceId = call.argument<String>("geofenceId") ?: "default_geofence_id"
        startGeofencing(latitude, longitude, radius, geofenceId)
        result.success(geofenceId)
      }
      "stopGeofencing" -> {
        val geofenceId = call.argument<String>("geofenceId") ?: "default_geofence_id"
        stopGeofencing(geofenceId)
        result.success("Geofencing stopped for ID: $geofenceId")
      }
      "isServiceRunning" -> {
        val geofenceId = call.argument<String>("geofenceId") ?: "default_geofence_id"
        val isRunning = GeofenceService.isServiceRunning && isGeofenceRegistered(geofenceId)
        result.success(isRunning)
      }
      "restartService" -> {
        val latitude = call.argument<Double>("latitude") ?: 0.0
        val longitude = call.argument<Double>("longitude") ?: 0.0
        val radius = call.argument<Double>("radius")?.toFloat() ?: 100f
        val geofenceId = call.argument<String>("geofenceId") ?: "default_geofence_id"
        if (!GeofenceService.isServiceRunning) {
          Log.d("MultipleGeofencesPlugin", "Attempting to restart GeofenceService...")
          // Stop the service if it might be lingering
          val stopIntent = Intent(context, GeofenceService::class.java)
          context.stopService(stopIntent)

          // Add a delay to allow the service to stop cleanly before restarting
          Handler(Looper.getMainLooper()).postDelayed({
            val restartIntent = Intent(context, GeofenceService::class.java)
            ContextCompat.startForegroundService(context, restartIntent)
            // Re-register the geofence
            startGeofencing(latitude, longitude, radius, geofenceId)
            result.success(true)
          }, 2000) // 2 seconds delay to allow for cleanup
        } else {
          result.success(false)
        }
      }
      "checkLocationPermissionStatus" -> {
        val status = getLocationPermissionStatus()
        result.success(status)
      }
      "updateGeofences" -> {
        val geofences = call.argument<List<Map<String, Any>>>("geofences")
        if (geofences != null) {
          updateGeofences(geofences)
          result.success(true)
        } else {
          result.error("INVALID_ARGUMENT", "Geofences list is null", null)
        }
      }
      "clearAllGeofences" -> {
        clearAllGeofences()
        result.success(true)
      }
      else -> {
        result.notImplemented()
      }
    }
  }

  private fun hasLocationPermissions(result: MethodChannel.Result) {
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

    if (fineLocationGranted && backgroundLocationGranted){
        result.success(true)
    } else {
        result.success(false)
    }
  }

  private fun getLocationPermissionStatus(): Int {
    val fineLocationStatus = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
    val backgroundLocationStatus = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    } else {
      PackageManager.PERMISSION_GRANTED
    }

    return when {
      fineLocationStatus == PackageManager.PERMISSION_GRANTED && backgroundLocationStatus == PackageManager.PERMISSION_GRANTED -> 3 // Authorized Always
      fineLocationStatus == PackageManager.PERMISSION_GRANTED -> 1 // Authorized When In Use
      fineLocationStatus == PackageManager.PERMISSION_DENIED -> 2 // Denied
      else -> 0 // Not Determined
    }
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


  private fun updateGeofences(geofences: List<Map<String, Any>>) {
    // Step 1: Remove all old geofences
    removeAllGeofences {
      // Step 2: Clear shared preferences
      clearGeofencesFromSharedPreferences()

      // Step 3: Add new geofences
      for (geofence in geofences) {
        val latitude = geofence["latitude"] as? Double ?: continue
        val longitude = geofence["longitude"] as? Double ?: continue
        val radius = (geofence["radius"] as? Double)?.toFloat() ?: continue
        val geofenceId = geofence["geofenceId"] as? String ?: continue
        startGeofencing(latitude, longitude, radius, geofenceId)
      }
    }
  }

  private fun removeAllGeofences(onSuccess: () -> Unit) {
    geofencePendingIntent?.let { pendingIntent ->
      geofencingClient.removeGeofences(pendingIntent)
        .addOnSuccessListener {
          Log.i("MultipleGeofencesPlugin", "All geofences removed successfully.")
          onSuccess()
        }
        .addOnFailureListener { e ->
          Log.e("MultipleGeofencesPlugin", "Failed to remove geofences: ${e.message}")
        }
    } ?: run {
      Log.e("MultipleGeofencesPlugin", "Failed to remove geofences: PendingIntent is null.")
    }
  }




  private fun clearGeofencesFromSharedPreferences() {
    val prefs = context.getSharedPreferences("GeofencingPrefs", Context.MODE_PRIVATE)
    val editor = prefs.edit()
    editor.clear()
    editor.apply()
    Log.i("MultipleGeofencesPlugin", "Geofencing shared preferences cleared.")
  }

  private fun saveGeofenceToSharedPreferences(geofenceId: String, latitude: Double, longitude: Double, radius: Float) {
    val prefs = context.getSharedPreferences("GeofencingPrefs", Context.MODE_PRIVATE)
    val editor = prefs.edit()
    val geofenceData = "$latitude,$longitude,$radius"
    val geofences = prefs.getStringSet("geofences", mutableSetOf()) ?: mutableSetOf()
    geofences.add("$geofenceId:$geofenceData")
    editor.putStringSet("geofences", geofences)
    editor.apply()
  }

  private fun isGeofenceRegistered(geofenceId: String): Boolean {
    val prefs = context.getSharedPreferences("GeofencingPrefs", Context.MODE_PRIVATE)
    return prefs.contains(geofenceId)
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

  private fun clearAllGeofences() {
    removeAllGeofences {
      clearGeofencesFromSharedPreferences()
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

  fun isBatteryOptimizationIgnored(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
      val packageName = context.packageName
      return powerManager.isIgnoringBatteryOptimizations(packageName)
    }
    // Assume true for devices below API level 23 as battery optimizations are not relevant
    return true
  }

  fun requestIgnoreBatteryOptimization(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !isBatteryOptimizationIgnored(context)) {
      val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
        data = Uri.parse("package:${context.packageName}")
      }
      context.startActivity(intent)
    }
  }

  fun openLocationSettings(context: Context) {
    val intent = Intent().apply {
      action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
      data = Uri.fromParts("package", context.packageName, null)
    }
    context.startActivity(intent)
  }


  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
    methodChannel = null
  }
}
