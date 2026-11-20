# USB QR Scanner (Android / Kotlin)

A simple Android app that previews an external USB (UVC) webcam over USB OTG and scans QR codes in real time on-device.

## Libraries Used
- **[UVCAndroid (`com.herohan:UVCAndroid:1.0.13`)](https://github.com/shiyinghan/UVCAndroid)** — accesses external UVC webcams on non-rooted Android devices via Android's USB Host API (even on phones that do not expose USB cameras to Camera2/CameraX).
- **[Google ML Kit Barcode Scanning (`com.google.mlkit:barcode-scanning:17.3.0`)](https://developers.google.com/ml-kit/vision/barcode-scanning/android)** — performs fast, offline QR code recognition directly on NV21 preview frames.

## Requirements
- Android Studio (Ladybug / 2024.2 or newer) with JDK 17
- An Android device running **Android 7.0 (API 24) or higher** with **USB Host (OTG)** support
- A USB-C / OTG adapter and a standard UVC-compliant USB webcam

## How to Build & Run
1. Unzip `UsbQrScanner-Android.zip` and open the `UsbQrScanner` folder in **Android Studio**.
2. Wait for Gradle Sync to finish downloading dependencies from Google Maven and Maven Central.
3. Connect your Android device (with USB Debugging enabled) and click **Run**.
4. Grant **Camera** permission when prompted (required by Android 10+ to access USB video devices).
5. Plug in your external USB camera via an OTG adapter and approve the Android USB device permission dialog.
6. Point the external USB camera at any QR code:
   - The decoded result appears in the bottom panel with its type (`URL`, `WI-FI`, `EMAIL`, `PHONE`, `SMS`, `LOCATION`, or `QR TEXT`).
   - Use **Rotate** (`0° / 90° / 180° / 270°`) if your external camera is mounted sideways or upside-down.
   - Use **USB Camera** to switch between multiple connected USB cameras, **Pause / Resume** to freeze scanning, and **Copy** or **Open Link** on detected QR codes.

## Troubleshooting
- **Camera not detected**: Confirm your phone supports USB OTG (some devices require enabling *OTG connection* in Android Settings) and use a data-capable OTG cable rather than a charge-only adapter.
- **No preview on Android 10+**: Android requires the runtime `CAMERA` permission before `UsbManager` will grant access to USB Video Class (UVC) devices; verify Camera permission is enabled in App Info.
