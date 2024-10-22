import 'package:permission_handler/permission_handler.dart';

Future<bool> requestPermissions() async {
  print('Ahoo trying');
  PermissionStatus status = await Permission.location.request();

  print('Plugin Android status: $status');
  if (status.isGranted) {
    // Check if background location is needed
    if (await Permission.locationAlways.isDenied) {
      // Request background location for Android 10 and above
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
  } else {
    return false;
  }
}
