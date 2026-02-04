# BTCFace Quick Start Guide

Get BTCFace running in 5 minutes!

## 📋 Prerequisites

1. **Android Studio** (Hedgehog 2023.1.1+)
2. **Android Phone** (API 26+, Android 8.0+)
3. **Galaxy Watch 7** or Wear OS emulator (API 30+)
4. **USB Cable** or WiFi adb connection

## ⚡ Setup

### 1. Clone Repository
```bash
git clone https://github.com/pir4t94/BTCFace.git
cd BTCFace
```

### 2. Open in Android Studio
```bash
# Open project
Android Studio → Open → /path/to/BTCFace
```

### 3. Gradle Sync
- Android Studio will automatically sync gradle files
- Wait for "Gradle build finished"

## 🔨 Build

### Build Both Apps
```bash
./gradlew assembleDebug
```

Output:
- Phone app: `app/build/outputs/apk/debug/app-debug.apk`
- Watch face: `wearable/build/outputs/apk/debug/wearable-debug.apk`

Or build in Android Studio:
- Select "app" module → Build → Build Bundle(s)/APK(s) → Build APK(s)
- Select "wearable" module → Build → Build Bundle(s)/APK(s) → Build APK(s)

## 📱 Install

### 1. Install Phone App
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 2. Install Watch Face
```bash
adb install wearable/build/outputs/apk/debug/wearable-debug.apk
```

Or use Android Studio's "Run" button:
1. Select "app" → Run (Shift+F10)
2. Select "wearable" → Run (Shift+F10)

## 🎯 First Run

### Phone App
1. Open app drawer
2. Launch **BTCFace**
3. You should see "Loading..." (first sync happens)
4. After a few seconds, BTC price appears

### Watch Face
1. Swipe to watch face picker (right from clock)
2. Search for **BTCFace**
3. Tap to apply

### Manual Sync
1. Open phone app
2. Tap **"Sync Now"** button
3. Watch should update price within 1 second

## ⚙️ Configuration

### Change Sync Interval (Phone App)
1. Open **BTCFace** on phone
2. Tap **"Settings"** button
3. Select **"Sync Interval"**
4. Choose: 5, 10, 15, 30 min, or 1 hour
5. Back to apply

### Change Color Theme (Phone App)
> **Coming in v1.1** - Currently hardcoded to Bitcoin Gold

Manual workaround:
1. Open watch face settings (hold on clock)
2. Swipe to "Color Theme"
3. Select: Bitcoin Gold, Silver, Satoshi Green, or Ice Blue

## 🐛 Troubleshooting

### Phone App Won't Install
```bash
# Check if device connected
adb devices

# Clear previous installation
adb uninstall com.roklab.btcface.companion

# Try again
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Watch Face Won't Install
```bash
# Make sure watch is connected via Bluetooth
adb devices  # Should show watch device ID

# Clear previous installation
adb uninstall com.roklab.btcface.wearable

# Try again
adb install wearable/build/outputs/apk/debug/wearable-debug.apk
```

### Price Not Updating
1. Check phone app is still running (background)
2. Verify WiFi/mobile on phone (required for API calls)
3. Check Bluetooth between phone and watch
4. Manually tap "Sync Now" on phone app
5. Check logcat:
   ```bash
   adb logcat | grep BTCFace
   ```

### WorkManager Not Running
1. Open Settings → Apps → BTCFace → Battery → Background restriction
2. Change to **"Unrestricted"**
3. Force stop WorkManager:
   ```bash
   adb shell am force-stop com.android.systemui
   ```

## 📊 Verify It's Working

### Logcat Check
```bash
adb logcat | grep BTCFace

# Expected output:
# BTCFace: BTC Price fetched: $104250.12
# BTCFace: Synced to watch
# BTCFace: Price displayed on watch
```

### Manual Verification
1. **Phone:** Price shows USD value (e.g., "$104,250")
2. **Watch:** Price appears at 6 o'clock position
3. **Timestamp:** "Last update: Feb 4, 14:19" shown on phone
4. **Hands:** Hour/minute hands move correctly

## 🚀 Next Steps

### Explore Settings
- Sync interval: Change to 5 min to see faster updates
- Theme: Try each color theme
- Complications: Long-press watch face → Customize complications

### Customize
- Modify colors in `wearable/src/main/res/values/colors.xml`
- Change sync interval defaults in `app/src/main/res/values/arrays.xml`
- Edit price format in `app/src/main/java/com/roklab/btcface/sync/BTCPriceFetcher.kt`

### Submit Changes
```bash
git checkout -b my-feature
git add .
git commit -m "Add awesome feature"
git push origin my-feature
# Open Pull Request on GitHub
```

## 📚 Documentation

- **README.md** - Overview & features
- **ARCHITECTURE.md** - Technical deep dive
- **MIGRATION.md** - Changes from original Canvas version

## ⚠️ Common Mistakes

❌ **Don't:** Only install watch face (phone app required)
✅ **Do:** Install both phone and watch apps

❌ **Don't:** Expect price to update without internet on phone
✅ **Do:** Keep phone on WiFi/mobile for sync

❌ **Don't:** Clear app data without uninstalling
✅ **Do:** Uninstall and reinstall if cache corrupted

## 🆘 Still Stuck?

1. Check logcat for errors:
   ```bash
   adb logcat -s BTCFace:V,WorkManager:V,Wearable:V
   ```

2. Verify module structure:
   ```bash
   ls -la BTCFace/
   # Should see: app/, wearable/, settings.gradle.kts, README.md
   ```

3. Open GitHub issue with:
   - Android version + watch model
   - Last 50 lines of logcat
   - Steps to reproduce

## 🎉 Success!

If you see:
- ✅ Phone app displaying BTC price
- ✅ Watch face showing time
- ✅ Price synced from phone to watch within 1 second
- ✅ Prices match between phone and watch

**You're all set!** Enjoy your Bitcoin watch face! ₿

---

**Need help?** See ARCHITECTURE.md or open an issue on GitHub.
