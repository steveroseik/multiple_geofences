import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'multiple_geofences_platform_interface.dart';

/// An implementation of [MultipleGeofencesPlatform] that uses method channels.
class MethodChannelMultipleGeofences extends MultipleGeofencesPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('multiple_geofences');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }
}
