# BTCFace Wearable - ProGuard Rules

# Keep Watch Face Service
-keep class * extends androidx.wear.watchface.WatchFaceService { *; }

# Keep Watch Face Renderer
-keep class * extends androidx.wear.watchface.Renderer { *; }

# Keep User Style classes
-keep class androidx.wear.watchface.style.** { *; }

# Keep Complication classes
-keep class androidx.wear.watchface.complications.** { *; }

# Keep Google Play Services Wearable
-keep class com.google.android.gms.wearable.** { *; }
-dontwarn com.google.android.gms.wearable.**

# Keep Kotlin coroutines
-keepnames class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Keep SharedAssets inner class
-keep class com.roklab.btcface.BTCWatchFaceRenderer$Assets { *; }
