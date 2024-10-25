import 'package:permission_handler/permission_handler.dart';

Future<bool> requestAndroidPermissions() async {
  PermissionStatus status = await Permission.location.request();
  print('first android: $status');
  if (status.isGranted) {
    // Check if background location is needed
    // Request background location for Android 10 and above
    print('requesting always android');
    PermissionStatus backgroundStatus =
        await Permission.locationAlways.request();
    if (!backgroundStatus.isGranted) {
      return false;
    } else {
      return true;
    }
  } else {
    return false;
  }
}
