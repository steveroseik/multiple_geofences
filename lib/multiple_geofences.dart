library multiple_geofences;

import 'dart:async';
import 'dart:io';

import 'package:flutter/cupertino.dart';
import 'package:flutter/services.dart';
import 'package:multiple_geofences/request_permission_android.dart';

import 'multiple_geofences_platform_interface.dart';

export './multiple_geofences.dart';

enum GeofencePermissionStatus {
  /// The user has denied the use of location services for the app or has not yet responded to the permission request.
  idle,

  /// The user has allowed the use of location services for the app.
  allowed,

  /// The user has denied the use of location services for the app.
  denied,

  /// In Android, user has declined the request to ignore battery optimizations.
  limited,
}

class MultipleGeofences {
  Future<String?> getPlatformVersion() {
    return MultipleGeofencesPlatform.instance.getPlatformVersion();
  }

  static const MethodChannel _channel =
      MethodChannel('com.roseik.multiple_geofences/geofencing');
  final Function(String regionId)? onEnterRegion;
  final Function(String regionId)? onLeaveRegion;
  final Function(int status)? onAuthorizationChanged;
  final Function(String regionId, String error)? onMonitoringFailed;
  final Function()? onServiceStarted;
  final ValueChanged<String>? onUnexpectedAction;

  ValueNotifier<int> iosPermissionStatus = ValueNotifier<int>(-1);

  MultipleGeofences(
      {this.onEnterRegion,
      this.onLeaveRegion,
      this.onAuthorizationChanged,
      this.onMonitoringFailed,
      this.onUnexpectedAction,
      this.onServiceStarted});

  dispose() {
    iosPermissionStatus.dispose();
  }

  void initialize() {
    _channel.setMethodCallHandler(_handleMethodCall);
  }

  Future<void> requestIgnoreBatteryOptimization() async {
    if (Platform.isIOS) return;
    try {
      await _channel.invokeMethod('requestIgnoreBatteryOptimization');
    } catch (e, s) {
      print('$e, $s');
    }
  }

  Future<bool> isBatteryOptimizationIgnored() async {
    if (Platform.isIOS) return true;
    try {
      return await _channel.invokeMethod('isBatteryOptimizationIgnored');
    } catch (e, s) {
      print('$e, $s');
      return false;
    }
  }

  Future<bool> isLocationPermissionAllowed() async {
    if (Platform.isAndroid) {
      return await _channel.invokeMethod('requestLocationPermission');
    } else if (Platform.isIOS) {
      return await _channel.invokeMethod('isLocationPermissionAllowed');
    }

    return false;
  }

  Future<GeofencePermissionStatus> requestLocationPermission() async {
    try {
      bool response = await _channel.invokeMethod('requestLocationPermission');

      if (response == false) {
        if (Platform.isAndroid) {
          return await requestAndroidPermission();
        } else {
          return GeofencePermissionStatus.denied;
        }
      } else {
        final batteryOptimization = await isBatteryOptimizationIgnored();
        if (!batteryOptimization) return GeofencePermissionStatus.limited;
      }

      return GeofencePermissionStatus.allowed;
    } on PlatformException catch (e) {
      if (Platform.isAndroid && e.code == 'PERMISSION_DENIED') {
        return await requestAndroidPermission();
      }
      return GeofencePermissionStatus.denied;
    }
  }

  Future<GeofencePermissionStatus> requestIOSPermission() async {
    Completer<GeofencePermissionStatus> permissionChanged =
        Completer<GeofencePermissionStatus>();

    whenChanged() async {
      if (iosPermissionStatus.value == 3) {
        permissionChanged.complete(GeofencePermissionStatus.allowed);
      } else {
        permissionChanged.complete(GeofencePermissionStatus.denied);
      }
      iosPermissionStatus.removeListener(whenChanged);
    }

    iosPermissionStatus.addListener(whenChanged);

    return permissionChanged.future;
  }

  // Check for location permissions
  Future<GeofencePermissionStatus> requestAndroidPermission() async {
    try {
      await requestIgnoreBatteryOptimization();
      final granted = await requestAndroidPermissions();
      if (!granted) return GeofencePermissionStatus.denied;
      final ignoredBattery = await isBatteryOptimizationIgnored();
      if (!ignoredBattery) return GeofencePermissionStatus.limited;
      return GeofencePermissionStatus.allowed;
    } on PlatformException catch (e) {
      print('Error requesting location permission: $e');
    }
    return GeofencePermissionStatus.denied;
  }

  Future<void> startGeofencing(
      String fenceId, double latitude, double longitude, double radius) async {
    try {
      final result = await _channel.invokeMethod('startGeofencing', {
        'geofenceId': fenceId,
        'latitude': latitude,
        'longitude': longitude,
        'radius': radius,
      });
      print('Registered fence: $result');
    } on PlatformException catch (e) {
      print('Error starting geofencing: $e');
    }
  }

  Future<bool> isServiceRunning(String fenceId) async {
    return await _channel
        .invokeMethod('isServiceRunning', {'geofenceId': fenceId});
  }

  Future<bool> restartService(
      String fenceId, double latitude, double longitude, double radius) async {
    return await _channel.invokeMethod('restartService', {
      'geofenceId': fenceId,
      'latitude': latitude,
      'longitude': longitude,
      'radius': radius,
    });
  }

  Future openLocationSettings() async {
    return await _channel.invokeMethod('openLocationSettings');
  }

  Future<void> _handleMethodCall(MethodCall call) async {
    print('Method called: ${call.method}');
    switch (call.method) {
      case 'onEnterRegion':
        print('Entered region: ${call.arguments['regionId']}');
        if (onEnterRegion != null) {
          onEnterRegion!(call.arguments['regionId']);
        }
        // Handle region entry
        break;
      case 'onExitRegion':
        print('Exited region: ${call.arguments['regionId']}');
        if (onLeaveRegion != null) {
          onLeaveRegion!(call.arguments['regionId']);
        }
        break;
      case 'onAuthorizationChanged':
        print('only data: "${call.arguments['status']}"');
        iosPermissionStatus.value =
            int.parse(call.arguments['status'].toString());
        print('Authorization status changed: ${call.arguments['status']}');
        // Handle authorization status change
        onAuthorizationChanged?.call(call.arguments['status']);

        break;
      case 'onMonitoringFailed':
        print(
            'Monitoring failed: ${call.arguments['regionId']} with error: ${call.arguments['error']}');
        // Handle monitoring failure
        onMonitoringFailed?.call(
            call.arguments['regionId'], call.arguments['error']);
        break;
      case 'onServiceStarted':
        print('Service started!!');
        onServiceStarted?.call();
        break;
      default:
        print('Unknown method called: ${call.method}');
        onUnexpectedAction?.call(call.method);
    }
  }
}
