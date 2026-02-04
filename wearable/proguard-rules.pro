# ProGuard rules for BTCFace Watch Face App

# Keep watch face classes
-keep class com.roklab.btcface.** { *; }

# Wear OS WatchFace
-keep class androidx.wear.watchface.** { *; }
-keep class androidx.wear.complications.** { *; }
-keepclasseswithmembers class * {
    @androidx.wear.watchface.** <methods>;
}

# Google Play Services (Wearable)
-keep class com.google.android.gms.wearable.** { *; }
-dontwarn com.google.android.gms.**

# Coroutines
-keepclassmembers class kotlinx.coroutines.internal.MainDispatcherFactory {
    *;
}

-keepclassmembers class kotlinx.coroutines.scheduling.DefaultScheduler {
    *;
}

# Kotlin
-keepclassmembers class kotlin.Metadata {
    *;
}

# Keep runtime visible annotations
-keepattributes RuntimeVisibleAnnotations
-keepattributes *Annotation*

# Keep source file names and line numbers for crashes
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep lifecycle callbacks
-keep class androidx.lifecycle.** { *; }
