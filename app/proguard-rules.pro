# BTCFace Phone Companion - ProGuard Rules

# Keep WorkManager worker classes
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Keep Google Play Services Wearable
-keep class com.google.android.gms.wearable.** { *; }
-dontwarn com.google.android.gms.wearable.**

# Keep data classes
-keep class com.roklab.btcface.model.** { *; }

# Keep Kotlin coroutines
-keepnames class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Keep Preferences
-keep class androidx.preference.** { *; }

# Keep JSON parsing
-keep class org.json.** { *; }
