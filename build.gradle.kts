plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
}

tasks.register("buildAndInstall") {
    dependsOn(":app:assembleDebug", ":wearable:assembleDebug")
    
    doLast {
        val adb = if (System.getProperty("os.name").lowercase().contains("win")) {
            "adb.exe"
        } else {
            "adb"
        }
        
        val phoneApk = "app/build/outputs/apk/debug/app-debug.apk"
        val watchApk = "wearable/build/outputs/apk/debug/wearable-debug.apk"
        
        println("📱 Installing phone app...")
        exec {
            commandLine(adb, "install", "-r", phoneApk)
        }
        
        println("⌚ Installing watch face...")
        exec {
            commandLine(adb, "install", "-r", watchApk)
        }
        
        println("✅ Done! Both apps installed.")
    }
}
