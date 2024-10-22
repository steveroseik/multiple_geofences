library multiple_geofences;

import 'dart:async';
import 'dart:io';

import 'package:flutter/cupertino.dart';
import 'package:flutter/services.dart';
import 'package:multiple_geofences/request_permission.dart';

import 'multiple_geofences_platform_interface.dart';

export './multiple_geofences.dart';

class MultipleGeofences {
  Future<String?> getPlatformVersion() {
    return MultipleGeofencesPlatform.instance.getPlatformVersion();
  }

  static const MethodChannel _channel =
      MethodChannel('com.roseik.multiple_geofences/geofencing');
  final Function(String regionId)? onEnterRegion;
  final Function(String regionId)? onLeaveRegion;
  final Function(String status)? onAuthorizationChanged;
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

  Future<bool> requestLocationPermission() async {
    try {
      if (Platform.isAndroid)
        await _channel.invokeMethod('checkAndRequestBatteryOptimization');
      bool response = await _channel.invokeMethod('requestLocationPermission');
      print('Plugin perm response: $response');
      print('Response type: ${response is bool}');
      if (response == false) {
        print('What platform: ${Platform.operatingSystem}');
        if (Platform.isAndroid) {
          print('Access android request permission');
          return await requestAndroidPermission();
        } else if (Platform.isIOS) {
          return await requestIOSPermission();
        }
      }
      return response;
    } on PlatformException catch (e) {
      print('Error requesting location permission: ${e.code}');
      if (Platform.isAndroid && e.code == 'PERMISSION_DENIED') {
        return await requestAndroidPermission();
      }
      return false;
    }
  }

  Future<bool> requestIOSPermission() async {
    print('accessed ios permission: ${iosPermissionStatus.value}');
    Completer<bool> permissionChanged = Completer<bool>();

    whenChanged() async {
      print('ios permission status: ${iosPermissionStatus.value}');
      if (iosPermissionStatus.value == 3) {
        permissionChanged.complete(true);
      } else {
        permissionChanged.complete(false);
      }
      iosPermissionStatus.removeListener(whenChanged);
    }

    iosPermissionStatus.addListener(whenChanged);

    return permissionChanged.future;
  }

  // Check for location permissions
  Future<bool> requestAndroidPermission() async {
    print('Accessed hena yaba');
    try {
      print('bahawel ahoo');
      return await requestPermissions();
    } on PlatformException catch (e) {
      print('Error requesting location permission: $e');
      return false;
    }
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

  Future<void> _handleMethodCall(MethodCall call) async {
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
