//package com.roseik.multiple_geofences
//
//import android.content.BroadcastReceiver
//import android.content.Context
//import android.content.Intent
//import android.content.pm.PackageManager
//import androidx.core.app.ActivityCompat
//import android.util.Log
//
//class LocationPermissionChangeReceiver(private val service: GeofenceService) : BroadcastReceiver() {
//
//    override fun onReceive(context: Context, intent: Intent) {
//        if (intent.action == Intent.ACTION_PACKAGE_CHANGED) {
////            val permissionStatus = getLocationPermissionStatus(context)
////
////            if (permissionStatus != 3) {
////                // Stop geofence service if permissions are revoked
////                service.stopSelf()
////                service.invokeFlutterMethod("onAuthorizationChanged", mapOf("status" to permissionStatus))
////            }
//        }
//    }
//
////    private fun getLocationPermissionStatus(context: Context): Int {
////        val fineLocationStatus = ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION)
////        val backgroundLocationStatus = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
////            ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)
////        } else {
////            PackageManager.PERMISSION_GRANTED
////        }
////
////        return when {
////            fineLocationStatus == PackageManager.PERMISSION_GRANTED && backgroundLocationStatus == PackageManager.PERMISSION_GRANTED -> 3 // Authorized Always
////            fineLocationStatus == PackageManager.PERMISSION_GRANTED -> 1 // Authorized When In Use
////            fineLocationStatus == PackageManager.PERMISSION_DENIED -> 2 // Denied
////            else -> 0 // Not Determined
////        }
////    }
//}
