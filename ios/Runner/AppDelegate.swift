import Flutter
import UIKit

@main
@objc class AppDelegate: FlutterAppDelegate, FlutterImplicitEngineDelegate {
  override func application(
    _ application: UIApplication,
    didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
  ) -> Bool {
    return super.application(application, didFinishLaunchingWithOptions: launchOptions)
  }

  func didInitializeImplicitFlutterEngine(_ engineBridge: FlutterImplicitEngineBridge) {
    GeneratedPluginRegistrant.register(with: engineBridge.pluginRegistry)
    let settingsChannel = FlutterMethodChannel(
      name: "cropdoc/app_settings",
      binaryMessenger: engineBridge.engine.binaryMessenger
    )
    settingsChannel.setMethodCallHandler { call, result in
      guard call.method == "openAppSettings" else {
        result(FlutterMethodNotImplemented)
        return
      }
      guard let settingsURL = URL(string: UIApplication.openSettingsURLString) else {
        result(FlutterError(
          code: "settings_unavailable",
          message: "Could not create the device settings URL.",
          details: nil
        ))
        return
      }
      UIApplication.shared.open(settingsURL, options: [:]) { opened in
        result(opened)
      }
    }
  }
}
