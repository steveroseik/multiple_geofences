import Foundation
import Flutter
import UIKit
import CoreLocation

public class MultipleGeofencesPlugin: NSObject, FlutterPlugin, CLLocationManagerDelegate {
  private var locationManager: CLLocationManager?
  private var channel: FlutterMethodChannel?

  public static func register(with registrar: FlutterPluginRegistrar) {
    let channel = FlutterMethodChannel(name: "geofencing", binaryMessenger: registrar.messenger())
    let instance = MultipleGeofencesPlugin()
    instance.channel = channel
    registrar.addMethodCallDelegate(instance, channel: channel)
  }

  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
    switch call.method {
    case "requestLocationPermission":
      requestLocationPermission(result: result)
    case "startGeofencing":
      if let args = call.arguments as? [String: Any],
         let latitude = args["latitude"] as? Double,
         let longitude = args["longitude"] as? Double,
         let radius = args["radius"] as? Double {
        startGeofencing(latitude: latitude, longitude: longitude, radius: radius, result: result)
      } else {
        result(FlutterError(code: "INVALID_ARGUMENTS", message: "Invalid arguments for startGeofencing", details: nil))
      }
    default:
      result(FlutterMethodNotImplemented)
    }
  }

  private func requestLocationPermission(result: @escaping FlutterResult) {
    if locationManager == nil {
      locationManager = CLLocationManager()
      locationManager?.delegate = self
      locationManager?.allowsBackgroundLocationUpdates = true // Enable background updates
      locationManager?.pausesLocationUpdatesAutomatically = false // Prevent pausing updates
    }
    locationManager?.requestAlwaysAuthorization()
    result("Location permission requested")
  }

  private func startGeofencing(latitude: Double, longitude: Double, radius: Double, result: @escaping FlutterResult) {
    guard CLLocationManager.isMonitoringAvailable(for: CLCircularRegion.self) else {
      result(FlutterError(code: "GEOFENCING_NOT_AVAILABLE", message: "Geofencing is not supported on this device", details: nil))
      return
    }

    guard CLLocationManager.authorizationStatus() == .authorizedAlways else {
      result(FlutterError(code: "AUTHORIZATION_DENIED", message: "App does not have correct location authorization", details: nil))
      return
    }

    let region = CLCircularRegion(center: CLLocationCoordinate2D(latitude: latitude, longitude: longitude), radius: radius, identifier: "geofence")
    region.notifyOnEntry = true
    region.notifyOnExit = true

    locationManager?.startMonitoring(for: region)
    result("Geofencing started for region: \(region.identifier)")
  }

  // CLLocationManagerDelegate methods
  public func locationManager(_ manager: CLLocationManager, didEnterRegion region: CLRegion) {
    print("User entereddd region: \(region.identifier)")
    // Notify Flutter about the event
    channel?.invokeMethod("onEnterRegion", arguments: ["regionId": region.identifier])
  }

  public func locationManager(_ manager: CLLocationManager, didExitRegion region: CLRegion) {
    print("User exiteddd region: \(region.identifier)")
    // Notify Flutter about the event
    channel?.invokeMethod("onExitRegion", arguments: ["regionId": region.identifier])
  }

  // Handle authorization changes
  public func locationManager(_ manager: CLLocationManager, didChangeAuthorization status: CLAuthorizationStatus) {
    switch status {
    case .authorizedAlways:
      print("Authorization changed to: Always")
    case .authorizedWhenInUse, .denied, .restricted:
      print("Authorization changed to: Limited")
    case .notDetermined:
      print("Authorization status not determined")
    @unknown default:
      print("Unknown authorization status")
    }
    // Notify Flutter about authorization status change
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
