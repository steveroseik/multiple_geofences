import Foundation
import Flutter
import UIKit
import CoreLocation

public class MultipleGeofencesPlugin: NSObject, FlutterPlugin, CLLocationManagerDelegate {
  private var locationManager: CLLocationManager?
  private var channel: FlutterMethodChannel?
  private var pendingLocationResult: FlutterResult?

  public static func register(with registrar: FlutterPluginRegistrar) {
    let channel = FlutterMethodChannel(name: "com.roseik.multiple_geofences/geofencing", binaryMessenger: registrar.messenger())
    let instance = MultipleGeofencesPlugin()
    instance.channel = channel
    registrar.addMethodCallDelegate(instance, channel: channel)
  }

  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
    switch call.method {

    case "openLocationSettings":
        openAppSettings()
    case "isLocationPermissionAllowed":
        checkLocationPermissionStatus(result: result)
    case "requestLocationPermission":
      requestLocationPermission(result: result)
    case "startGeofencing":
      if let args = call.arguments as? [String: Any],
         let latitude = args["latitude"] as? Double,
         let longitude = args["longitude"] as? Double,
         let radius = args["radius"] as? Double,
         let geofenceId = args["geofenceId"] as? String{
          startGeofencing(id: geofenceId, latitude: latitude, longitude: longitude, radius: radius, result: result)
      } else {
        result(FlutterError(code: "INVALID_ARGUMENTS", message: "Invalid arguments for startGeofencing", details: nil))
      }
   case "isServiceRunning":
         if let args = call.arguments as? [String: Any],
            let geofenceId = args["geofenceId"] as? String {
           result(isServiceRunning(geofenceId: geofenceId))
         } else {
           result(FlutterError(code: "INVALID_ARGUMENTS", message: "Invalid arguments for isServiceRunning", details: nil))
         }
   case "restartService":
    if let args = call.arguments as? [String: Any],
     let latitude = args["latitude"] as? Double,
     let longitude = args["longitude"] as? Double,
     let radius = args["radius"] as? Double,
     let geofenceId = args["geofenceId"] as? String{
       restartService(geofenceId: geofenceId, latitude: latitude, longitude: longitude, radius: radius, result: result)
     } else {
       result(FlutterError(code: "INVALID_ARGUMENTS", message: "Invalid arguments for restartService", details: nil))
     }
    default:
      result(FlutterMethodNotImplemented)
    }
  }

  public func openAppSettings() {
      guard let settingsUrl = URL(string: UIApplication.openSettingsURLString) else {
          return
      }

      if UIApplication.shared.canOpenURL(settingsUrl) {
          UIApplication.shared.open(settingsUrl, options: [:], completionHandler: nil)
      }
  }
    
 private func isServiceRunning(geofenceId: String) -> Bool {
    guard let monitoredRegions = locationManager?.monitoredRegions else {
      return false
    }

    for region in monitoredRegions {
      if let circularRegion = region as? CLCircularRegion,
         circularRegion.identifier == geofenceId {
        return true
      }
    }
    return false
  }

  private func restartService(geofenceId: String, latitude: Double, longitude: Double, radius: Double, result: @escaping FlutterResult) {
    // Stop monitoring the existing geofence
    guard let monitoredRegions = locationManager?.monitoredRegions else {
      // Restart the geofencing service
      startGeofencing(id: geofenceId, latitude: latitude, longitude: longitude, radius: radius, result: result)
      return
    }

    for region in monitoredRegions {
      if let circularRegion = region as? CLCircularRegion, circularRegion.identifier == geofenceId {
        locationManager?.stopMonitoring(for: circularRegion)
        break
      }
    }

    // Restart the geofencing service
    startGeofencing(id: geofenceId, latitude: latitude, longitude: longitude, radius: radius, result: result)
  }

  private func requestLocationPermission(result: @escaping FlutterResult) {
    if locationManager == nil {
            locationManager = CLLocationManager()
            locationManager?.delegate = self
            locationManager?.allowsBackgroundLocationUpdates = true // Enable background updates
            locationManager?.pausesLocationUpdatesAutomatically = false // Prevent pausing updates
        }
        let currentStatus = CLLocationManager.authorizationStatus()

        // Handle permanently denied scenario
        if currentStatus == .denied || currentStatus == .restricted {
            result(false) // Return immediately with false
            return
        }

        // If permission was already granted
        if currentStatus == .authorizedAlways {
            result(true)
            return
        }

        // Save the result to return later from the delegate
        pendingLocationResult = result
        locationManager?.requestAlwaysAuthorization()
  }

  private func checkLocationPermissionStatus(result: @escaping FlutterResult) {
      if locationManager == nil {
          locationManager = CLLocationManager()
          locationManager?.delegate = self
          locationManager?.allowsBackgroundLocationUpdates = true // Enable background updates
          locationManager?.pausesLocationUpdatesAutomatically = false // Prevent pausing updates
      }

      // Get the current authorization status
      let currentStatus = CLLocationManager.authorizationStatus()

      switch currentStatus {
      case .authorizedAlways:
          result(true) // Permission is granted for Always
      case .authorizedWhenInUse, .notDetermined, .denied, .restricted:
          result(false) // Permission is not Always
      @unknown default:
          result(false)
      }
  }


    private func startGeofencing(id: String, latitude: Double, longitude: Double, radius: Double, result: @escaping FlutterResult) {
    guard CLLocationManager.isMonitoringAvailable(for: CLCircularRegion.self) else {
      result(FlutterError(code: "GEOFENCING_NOT_AVAILABLE", message: "Geofencing is not supported on this device", details: nil))
      return
    }

    guard CLLocationManager.authorizationStatus() == .authorizedAlways else {
      result(FlutterError(code: "AUTHORIZATION_DENIED", message: "App does not have correct location authorization", details: nil))
      return
    }

    let region = CLCircularRegion(center: CLLocationCoordinate2D(latitude: latitude, longitude: longitude), radius: radius, identifier: id ?? "default_geofence_id")
    region.notifyOnEntry = true
    region.notifyOnExit = true

    locationManager?.startMonitoring(for: region)
    result("Geofencing started for region: \(region.identifier)")
  }

  // CLLocationManagerDelegate methods
  public func locationManager(_ manager: CLLocationManager, didEnterRegion region: CLRegion) {
//    // Send a local notification
//      let content = UNMutableNotificationContent()
//      content.title = "Geofence Entered"
//      content.body = "Region ID: \(region.identifier)"
//      content.sound = .default
//
//      let request = UNNotificationRequest(identifier: UUID().uuidString, content: content, trigger: nil)
//      UNUserNotificationCenter.current().add(request, withCompletionHandler: nil)

    print("User entereddd region: \(region.identifier)")
    // Notify Flutter about the event
    channel?.invokeMethod("onEnterRegion", arguments: ["regionId": region.identifier])
  }

  public func locationManager(_ manager: CLLocationManager, didExitRegion region: CLRegion) {

//  // Send a local notification
//    let content = UNMutableNotificationContent()
//    content.title = "Geofence Entered"
//    content.body = "Region ID: \(region.identifier)"
//    content.sound = .default
//
//    let request = UNNotificationRequest(identifier: UUID().uuidString, content: content, trigger: nil)
//    UNUserNotificationCenter.current().add(request, withCompletionHandler: nil)

    print("User exiteddd region: \(region.identifier)")
    // Notify Flutter about the event
    channel?.invokeMethod("onExitRegion", arguments: ["regionId": region.identifier])
  }

  // Handle authorization changes
  public func locationManager(_ manager: CLLocationManager, didChangeAuthorization status: CLAuthorizationStatus) {
      switch status {
      case .authorizedAlways:
          print("Authorization changed to: Always")
          pendingLocationResult?(true) // Return result to Flutter
      case .authorizedWhenInUse, .denied, .restricted:
          print("Authorization changed to: Limited")
          pendingLocationResult?(false)
      case .notDetermined:
          print("Authorization status not determined")
          // Don't return result yet; user hasn't responded
          return
      @unknown default:
          print("Unknown authorization status")
          pendingLocationResult?(false)
      }

      // Clear the pending result to avoid multiple calls
      pendingLocationResult = nil

      // Notify Flutter about the updated status
      channel?.invokeMethod("onAuthorizationChanged", arguments: ["status": status.rawValue])
  }

  // Handle errors
  public func locationManager(_ manager: CLLocationManager, monitoringDidFailFor region: CLRegion?, withError error: Error) {
    print("Monitoring failed for region with identifier: \(region?.identifier ?? "unknown")")
    print("Error: \(error.localizedDescription)")
    // Notify Flutter about the error
    channel?.invokeMethod("onMonitoringFailed", arguments: ["regionId": region?.identifier ?? "unknown", "error": error.localizedDescription])
  }
}
