import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'multiple_geofences_method_channel.dart';

abstract class MultipleGeofencesPlatform extends PlatformInterface {
  /// Constructs a MultipleGeofencesPlatform.
  MultipleGeofencesPlatform() : super(token: _token);

  static final Object _token = Object();

  static MultipleGeofencesPlatform _instance = MethodChannelMultipleGeofences();

  /// The default instance of [MultipleGeofencesPlatform] to use.
  ///
  /// Defaults to [MethodChannelMultipleGeofences].
  static MultipleGeofencesPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [MultipleGeofencesPlatform] when
  /// they register themselves.
  static set instance(MultipleGeofencesPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }
}
