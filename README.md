# ₿ BTCFace — Bitcoin Analog Watch Face for Wear OS 5 (WFF Format)

A sleek analog watch face for Wear OS with a Bitcoin soul. Features a subtle tilted Bitcoin logo watermark, live BTC price display, customizable color themes, and complication support. **New: Two-app architecture with phone companion app for data sync.**

## 🏗️ Architecture

This project consists of two separate Android modules:

### `/app` — Companion Phone App
- Fetches BTC price from Coinlore API every 15 minutes (configurable)
- Syncs price to watch via Wear Data Layer
- Provides UI for viewing price and managing settings
- Handles all network requests and WorkManager scheduling
- No watch-specific dependencies

### `/wearable` — Watch Face App
- WFF (Watch Face Format) compatible
- Receives BTC price updates from phone via Data Layer
- Displays live price on analog dial
- Supports 4 color themes
- Supports complications on watch
- No direct API calls (phone handles all fetching)

## ✨ Features

### Watch Face
- **Analog dial** with smooth sweeping seconds hand
- **Live BTC price** display at 6 o'clock (synced from phone every ~15 min)
- **Bitcoin logo watermark** — tilted 14° like the real deal, subtle in the background
- **4 color themes** — Bitcoin Gold, Silver, Satoshi Green, Ice Blue (all gradient-based)
- **2 complication slots** — left (9 o'clock) and right (3 o'clock), configurable
- **Ambient mode** — OLED-friendly outline hands, dimmed display
- **Customizable toggles** — seconds hand, price display

### Phone App
- **Manual sync button** for immediate price updates
- **Settings UI** to configure:
  - Sync interval (5, 10, 15, 30 min, 1 hour)
  - Watch face theme (sent to watch)
- **Background sync** via WorkManager (default 15 min)
- **Network-aware** — only syncs on connected WiFi/mobile

## 🎨 Color Themes

| Theme | Primary | Vibe |
|-------|---------|------|
| Bitcoin Gold | 🟠 `#F7931A` | The classic — warm, amber, iconic |
| Silver | ⚪ `#C0C0C0` | Clean, minimal, metallic |
| Satoshi Green | 🟢 `#00E676` | Matrix-core — line goes up |
| Ice Blue | 🔵 `#40C4FF` | Cool, futuristic, calm |

## 📁 Project Structure

```
BTCFace/
├── app/                               # Companion Phone App
│   ├── src/main/
│   │   ├── java/com/roklab/btcface/
│   │   │   ├── MainApplication.kt          # App initialization
│   │   │   ├── model/
│   │   │   │   └── BTCPrice.kt             # Price data model
│   │   │   ├── sync/
│   │   │   │   ├── BTCPriceFetcher.kt      # Coinlore API client
│   │   │   │   └── DataLayerSender.kt      # Wear Data Layer sync
│   │   │   ├── worker/
│   │   │   │   └── BTCPriceSyncWorker.kt   # WorkManager periodic sync
│   │   │   └── ui/
│   │   │       ├── MainActivity.kt         # Price display + sync button
│   │   │       └── SettingsActivity.kt     # Settings preferences
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   │   ├── activity_main.xml
│   │   │   │   └── activity_settings.xml
│   │   │   ├── xml/settings_preferences.xml
│   │   │   └── values/
│   │   │       ├── strings.xml
│   │   │       └── arrays.xml
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
│
├── wearable/                          # Watch Face App (WFF Format)
│   ├── src/main/
│   │   ├── java/com/roklab/btcface/
│   │   │   ├── BTCWatchFaceService.kt      # Watch face service
│   │   │   ├── BTCWatchFaceRenderer.kt     # Analog dial rendering (minimal Canvas)
│   │   │   └── BTCDataLayerListener.kt     # Receives synced data from phone
│   │   ├── res/
│   │   │   ├── drawable/
│   │   │   │   ├── ic_bitcoin_logo.xml
│   │   │   │   └── preview_btcface.xml
│   │   │   ├── xml/
│   │   │   │   ├── watch_face_btc_gold.xml (WFF format reference)
│   │   │   │   └── watch_face_info.xml
│   │   │   └── values/
│   │   │       ├── colors.xml
│   │   │       └── strings.xml
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
│
├── settings.gradle.kts                # Includes both modules
└── build.gradle.kts
```

## 🔄 Data Flow

```
Phone App                          Watch App
─────────────────────────────────────────────
WorkManager (15 min)
  ↓
BTCPriceSyncWorker
  ↓
BTCPriceFetcher (Coinlore API)
  ↓
DataLayerSender (Wear Data Layer)
  ────────────→ Wearable Data Client
                  ↓
                BTCDataLayerListener
                  ↓
                BTCWatchFaceRenderer (displays on dial)
```

## 🔧 Building

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- Wear OS emulator or physical watch (API 30+, Galaxy Watch 7 recommended)
- JDK 17

### Build Both Apps
```bash
./gradlew assembleDebug
```

This builds:
- `app/build/outputs/apk/debug/app-debug.apk` — Phone companion app
- `wearable/build/outputs/apk/debug/wearable-debug.apk` — Watch face app

### Install on Devices

**Phone (companion app):**
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

**Watch (watch face):**
```bash
adb install wearable/build/outputs/apk/debug/wearable-debug.apk
```

Then select **BTCFace** from the watch face picker on your watch.

## 🔌 API & Data Sync

### Coinlore API (Phone App)
```
GET https://api.coinlore.net/api/ticker/?id=90
→ [{ "price_usd": "104250.12", ... }]
```
- No API key required
- Updates every 15 minutes via WorkManager
- Network constraints: Connected WiFi/mobile

### Wear Data Layer (Phone ↔ Watch)
- **Path:** `/btc_price`
- **Keys:** `price` (Double), `price_formatted` (String), `timestamp` (Long)
- **Type:** Urgent high-priority update
- Uses Google Play Services Wearable

## ⚙️ Configuration

### Sync Interval (Phone App)
Edit `app/src/main/res/values/arrays.xml` to change available intervals:
- Default: 15 minutes
- Also available: 5, 10, 30 min, 1 hour

### Watch Face Themes
Available in phone app settings → will sync to watch:
- Bitcoin Gold (default)
- Silver
- Satoshi Green
- Ice Blue

## 🌙 Ambient Mode
- Low-power outline rendering for OLED watches
- Shows time only (no price display in ambient)
- 1-bit monochrome for Galaxy Watch 7

## 🎯 Target Device
- **Device:** Samsung Galaxy Watch 7
- **OS:** Wear OS 5 (API 34)
- **Arch:** ARM64 + ARM32
- **Screen:** 1.3" - 1.6" AMOLED (typically 432×432 or 480×480)

## 📝 Requirements

### Phone App
- **Min SDK:** 26 (Android 8.0)
- **Target SDK:** 34 (Android 14)
- **Internet permission** for API fetching
- **Network access** for Wear Data Layer sync

### Watch Face App
- **Min SDK:** 30 (Wear OS 3)
- **Target SDK:** 34 (Wear OS 5)
- Works without internet on watch (data pushed from phone)

## 🚀 Deployment

### Release Build
```bash
./gradlew assembleRelease
```

### ProGuard/R8 Obfuscation
Enabled in release builds. Rules in `proguard-rules.pro` prevent stripping of:
- WorkManager
- Google Play Services
- Wear framework

## 📊 Permissions

### Phone App (`app/AndroidManifest.xml`)
- `android.permission.INTERNET` — API calls
- `android.permission.ACCESS_NETWORK_STATE` — Connection check
- `com.google.android.permission.PROVIDE_BACKGROUND` — WorkManager background

### Watch App (`wearable/AndroidManifest.xml`)
- `com.google.android.permission.PROVIDE_BACKGROUND` — Data Layer listening

## 🔐 Security Notes

- No authentication required for Coinlore API (public endpoint)
- Price data is cached locally on both phone and watch
- Data Layer uses Google Play Services encryption
- No sensitive user data collected or stored

## 📚 Dependencies

See `app/build.gradle.kts` and `wearable/build.gradle.kts` for full list.

Key libraries:
- **Wear OS:** `androidx.wear.watchface:watchface:1.2.1`
- **Data Layer:** `com.google.android.gms:play-services-wearable:18.2.0`
- **WorkManager:** `androidx.work:work-runtime-ktx:2.9.1`
- **Coroutines:** `org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0`

## 📄 License

MIT

## 🙌 Credits

- Bitcoin logo design adapted from official Bitcoin logo
- Color schemes inspired by Bitcoin aesthetics and popular crypto dashboards
- Built with Android Wear OS and Kotlin

## 📞 Support

For issues, feature requests, or questions:
1. Check existing GitHub issues
2. Open a new issue with details and device info
3. Include logs: `adb logcat | grep BTCFace`

---

**Made with ₿ for Bitcoin enthusiasts on Wear OS**
