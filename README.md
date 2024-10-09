Here's an updated `README.md` file for your `multiple_geofences` Flutter plugin, highlighting its capabilities, functions, and setup instructions for both iOS and Android:

---

# multiple_geofences

A Flutter plugin for managing multiple geofences on both Android and iOS, allowing apps to track when users enter or exit specific geographical regions.

## Features

- **Geofencing Support**: Easily create and manage geofences with defined latitude, longitude, and radius.
- **Cross-Platform**: Supports both Android and iOS geofencing APIs.
- **Event Handling**: Get notified when a user enters or exits a geofenced region.
- **Background Location**: Works in the background to monitor geofences.

## Capabilities

1. **Start Geofencing**: Define a geofence with latitude, longitude, and radius to monitor a specific region.
2. **Stop Geofencing**: Stop monitoring a specific geofence by its ID.
3. **Event Callbacks**: Receive notifications when a user enters or exits a geofenced region.
4. **Authorization Status**: Track and respond to changes in location permission status.
5. **Error Handling**: Handle geofencing setup errors and failures.

## Installation

### Add the Dependency

Add `multiple_geofences` to your `pubspec.yaml`:

```yaml
dependencies:
  multiple_geofences:
    path: path/to/multiple_geofences # Replace with actual path or version
```

Run the following command to install the plugin:

```bash
flutter pub get
```

## Usage

### Import the Plugin

```dart
import 'package:multiple_geofences/multiple_geofences.dart';
```

### Initialize the Plugin

```dart
MultipleGeofences geofencing = MultipleGeofences(
  onEnterRegion: (regionId) {
    print("Entered region: $regionId");
    // Handle region entry
  },
  onLeaveRegion: (regionId) {
    print("Exited region: $regionId");
    // Handle region exit
  },
  onAuthorizationChanged: (status) {
    print("Authorization status changed: $status");
    // Handle authorization status change
  },
  onMonitoringFailed: (regionId, error) {
    print("Monitoring failed for $regionId: $error");
    // Handle monitoring failure
  },
  onUnexpectedAction: (action) {
    print("Unexpected action: $action");
    // Handle unexpected actions
  },
);
```

### Request Location Permission

```dart
await geofencing.requestLocationPermission();
```

### Start Geofencing

```dart
String? geofenceId = await geofencing.startGeofencing(37.3349, -122.00902, 100.0);
print("Started geofencing with ID: $geofenceId");
```

### Stop Geofencing

```dart
await geofencing.stopGeofencing(geofenceId!);
print("Stopped geofencing with ID: $geofenceId");
```

## iOS Setup

1. **Add Location Permissions to `Info.plist`**:
   Add the following keys to your `ios/Runner/Info.plist` file:

   ```xml
   <key>NSLocationWhenInUseUsageDescription</key>
   <string>This app requires location services to monitor geofenced regions.</string>
   <key>NSLocationAlwaysUsageDescription</key>
   <string>This app needs access to your location in the background to monitor geofences.</string>
   <key>NSLocationAlwaysAndWhenInUseUsageDescription</key>
   <string>This app needs access to your location in both foreground and background to provide geofencing features.</string>
   <key>UIBackgroundModes</key>
   <array>
     <string>location</string>
   </array>
   ```

2. **Request Location Permission**:
   The plugin automatically handles location permission requests, but you can customize this using Flutter's `permission_handler` plugin if needed.

3. **Background Location**:
   Ensure the app is allowed to access location in the background by setting the required keys in the `Info.plist` as shown above.

## Android Setup

1. **Add Permissions to `AndroidManifest.xml`**:
   Add the following permissions to your `android/app/src/main/AndroidManifest.xml` file:

   ```xml
   <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
   <uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
   ```

2. **Register `BroadcastReceiver` for Geofence Events**:
   In the `AndroidManifest.xml`, register the `GeofenceBroadcastReceiver` to listen for geofence transition events:

   ```xml
   <application>
       <receiver
           android:name="com.roseik.multiple_geofences.GeofenceBroadcastReceiver"
           android:exported="true"
           android:permission="android.permission.ACCESS_FINE_LOCATION">
           <intent-filter>
               <action android:name="com.google.android.gms.location.Geofence" />
           </intent-filter>
       </receiver>
   </application>
   ```

3. **Request Permissions in Flutter**:
   Handle runtime permission requests for location services (both foreground and background) in your Flutter app. You can use Flutter's `permission_handler` plugin to handle permissions.

   Example:
   ```dart
   await Permission.location.request();
   await Permission.locationAlways.request();
   ```

## Example

```dart
import 'package:flutter/material.dart';
import 'package:multiple_geofences/multiple_geofences.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatelessWidget {
  final MultipleGeofences geofencing = MultipleGeofences(
    onEnterRegion: (regionId) {
      print("Entered region: $regionId");
    },
    onLeaveRegion: (regionId) {
      print("Exited region: $regionId");
    },
    onMonitoringFailed: (regionId, error) {
      print("Monitoring failed for $regionId: $error");
    },
  );

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: GeofencingDemo(geofencing: geofencing),
    );
  }
}

class GeofencingDemo extends StatefulWidget {
  final MultipleGeofences geofencing;

  GeofencingDemo({required this.geofencing});

  @override
  _GeofencingDemoState createState() => _GeofencingDemoState();
}

class _GeofencingDemoState extends State<GeofencingDemo> {
  String? currentGeofenceId;

  void _startGeofencing() async {
    await widget.geofencing.requestLocationPermission();
    String? geofenceId = await widget.geofencing.startGeofencing(37.3349, -122.00902, 100);
    setState(() {
      currentGeofenceId = geofenceId;
    });
  }

  void _stopGeofencing() async {
    if (currentGeofenceId != null) {
      await widget.geofencing.stopGeofencing(currentGeofenceId!);
      setState(() {
        currentGeofenceId = null;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text('Geofencing Plugin Demo')),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            ElevatedButton(
              onPressed: _startGeofencing,
              child: Text('Start Geofencing'),
            ),
            ElevatedButton(
              onPressed: _stopGeofencing,
              child: Text('Stop Geofencing'),
            ),
            if (currentGeofenceId != null)
              Text("Active Geofence ID: $currentGeofenceId")
            else
              Text("No active geofence"),
          ],
        ),
      ),
    );
  }
}
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

This `README.md` covers all necessary aspects of the plugin, including setup for iOS and Android, the basic usage of the plugin, permissions, and handling geofencing events. This should provide a clear and detailed guide for anyone using your plugin.