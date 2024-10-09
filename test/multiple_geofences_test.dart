import 'package:flutter_test/flutter_test.dart';
import 'package:multiple_geofences/multiple_geofences.dart';
import 'package:multiple_geofences/multiple_geofences_platform_interface.dart';
import 'package:multiple_geofences/multiple_geofences_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockMultipleGeofencesPlatform
    with MockPlatformInterfaceMixin
    implements MultipleGeofencesPlatform {

  @override
  Future<String?> getPlatformVersion() => Future.value('42');
}

void main() {
  final MultipleGeofencesPlatform initialPlatform = MultipleGeofencesPlatform.instance;

  test('$MethodChannelMultipleGeofences is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelMultipleGeofences>());
  });

  test('getPlatformVersion', () async {
    MultipleGeofences multipleGeofencesPlugin = MultipleGeofences();
    MockMultipleGeofencesPlatform fakePlatform = MockMultipleGeofencesPlatform();
    MultipleGeofencesPlatform.instance = fakePlatform;

    expect(await multipleGeofencesPlugin.getPlatformVersion(), '42');
  });
}
