# Keep native JNI classes and callbacks used by UVCAndroid / libuvc
-keep class com.serenegiant.** { *; }
-keep class com.herohan.** { *; }
-dontwarn com.serenegiant.**
-dontwarn com.herohan.**
