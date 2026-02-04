# BTCFace Architecture Documentation

## Overview

BTCFace has been refactored from a single Wear OS watch face app to a **two-app architecture** optimized for Galaxy Watch 7 (Wear OS 5) with WFF (Watch Face Format) compatibility.

### Key Design Principles

1. **Separation of Concerns:** Phone handles all network operations; watch face is data-driven
2. **WFF Compatibility:** Watch face uses minimal custom rendering (only for analog dial)
3. **Data-Driven:** Watch face receives price updates via Wear Data Layer
4. **Battery Efficient:** Phone manages background sync with WorkManager; watch is purely reactive
5. **No Direct Watch API Calls:** All fetching/syncing happens on phone

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    PHONE (Android 8.0+)                     │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              Companion App                           │  │
│  │  ┌────────────────────────────────────────────────┐ │  │
│  │  │ WorkManager (BTCPriceSyncWorker)              │ │  │
│  │  │ Trigger: Every 15 min (configurable)          │ │  │
│  │  └─────────────┬──────────────────────────────────┘ │  │
│  │               │                                      │  │
│  │  ┌────────────▼──────────────────────────────────┐ │  │
│  │  │ BTCPriceFetcher                              │ │  │
│  │  │ - HTTP GET to Coinlore API                   │ │  │
│  │  │ - Parse JSON response                        │ │  │
│  │  │ - Cache to SharedPreferences                 │ │  │
│  │  └─────────────┬──────────────────────────────────┘ │  │
│  │               │                                      │  │
│  │  ┌────────────▼──────────────────────────────────┐ │  │
│  │  │ DataLayerSender                              │ │  │
│  │  │ - Create PutDataMapRequest                   │ │  │
│  │  │ - Send via Wearable.getDataClient()          │ │  │
│  │  │ - Set as urgent (immediate delivery)         │ │  │
│  │  └─────────────┬──────────────────────────────────┘ │  │
│  │               │ (Wear Data Layer)                    │  │
│  └───────────────┼────────────────────────────────────┘  │
│                  │                                        │
└──────────────────┼────────────────────────────────────────┘
                   │
        ┌──────────▼────────────┐
        │  Wear Data Layer      │
        │  Path: /btc_price     │
        │  Keys:                │
        │  - price: Double      │
        │  - price_formatted    │
        │  - timestamp          │
        └──────────┬────────────┘
                   │
┌──────────────────▼────────────────────────────────────────┐
│                   WATCH (Wear OS 5)                        │
│  ┌──────────────────────────────────────────────────────┐ │
│  │         Watch Face App (WFF-Compatible)             │ │
│  │  ┌────────────────────────────────────────────────┐ │ │
│  │  │ BTCWatchFaceService                           │ │ │
│  │  │ - Extends WatchFaceService                    │ │ │
│  │  │ - Implements DataClient.OnDataChangedListener │ │ │
│  │  └─────────────┬──────────────────────────────────┘ │ │
│  │               │                                      │ │
│  │  ┌────────────▼──────────────────────────────────┐ │ │
│  │  │ BTCDataLayerListener                         │ │ │
│  │  │ - onDataChanged() callback                   │ │ │
│  │  │ - Parse received DataMap                     │ │ │
│  │  │ - Cache to SharedPreferences                 │ │ │
│  │  └─────────────┬──────────────────────────────────┘ │ │
│  │               │                                      │ │
│  │  ┌────────────▼──────────────────────────────────┐ │ │
│  │  │ BTCWatchFaceRenderer                         │ │ │
│  │  │ - Draw analog dial                           │ │ │
│  │  │ - Hour/minute/second hands                   │ │ │
│  │  │ - Display price from cache                   │ │ │
│  │  │ - Theme support (4 colors)                   │ │ │
│  │  │ - Ambient mode (OLED-friendly)               │ │ │
│  │  └────────────────────────────────────────────────┘ │ │
│  └──────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────┘
```

## Module Details

### `/app` — Companion Phone App

**Purpose:** Single source of truth for BTC price data, manages sync to watch

**Key Components:**

1. **MainApplication**
   - Called on app startup
   - Initializes WorkManager for background sync
   - Schedules default 15-minute sync interval

2. **BTCPriceSyncWorker (WorkManager)**
   - Runs every 15 minutes (configurable)
   - Fetches latest BTC price from API
   - Syncs to watch via Data Layer
   - Retries on failure with exponential backoff
   - Network-aware: only runs when connected

3. **BTCPriceFetcher**
   - Async HTTP client for Coinlore API
   - Parses JSON response
   - Formats price as USD currency
   - Caches to SharedPreferences (key: `btc_price_cache`)
   - Fallback: returns cached price on network error

4. **DataLayerSender**
   - Uses Google Play Services Wearable API
   - Creates urgent PutDataMapRequest
   - Path: `/btc_price`
   - Payload: `{price, price_formatted, timestamp}`
   - Handles sync errors gracefully

5. **MainActivity**
   - Displays current BTC price
   - Shows last sync time
   - Manual sync button
   - Navigation to settings

6. **SettingsActivity**
   - Configuration screen
   - Sync interval selector (5, 10, 15, 30 min, 1 hour)
   - Uses PreferenceFragmentCompat
   - Reschedules WorkManager on interval change
   - Theme info display

**Data Flow:**
```
App Start → MainApplication.onCreate()
  ↓
BTCPriceSyncWorker.schedule()
  ↓
WorkManager triggers every 15 min
  ↓
BTCPriceSyncWorker.doWork()
  ↓
1. Fetch BTC price (BTCPriceFetcher.fetchPrice())
2. Cache locally (BTCPriceFetcher.cachePrice())
3. Send to watch (DataLayerSender.sendPriceToWatch())
  ↓
Success → Result.success()
Failure → Result.retry()
```

### `/wearable` — Watch Face App (WFF)

**Purpose:** Display BTC price on analog dial, receive data from phone

**Key Components:**

1. **BTCWatchFaceService**
   - Extends WatchFaceService
   - Implements DataClient.OnDataChangedListener
   - Registers style settings (color theme, show seconds, etc.)
   - Creates complication slots (left and right)
   - Instantiates BTCWatchFaceRenderer
   - Handles Data Layer callbacks: `onDataChanged()`
   - Registers/unregisters listener on resume/pause

2. **BTCWatchFaceRenderer**
   - Extends Renderer.CanvasRenderer2
   - Draws analog dial (minimal Canvas usage, WFF-compatible approach)
   - Renders hour/minute/second hands
   - Draws 12 hour markers (4 larger at cardinal positions)
   - Price display window at 6 o'clock
   - 4 color themes with dynamic paint switching
   - Ambient mode: outline-only, dimmed
   - Complication support via ComplicationSlotsManager

3. **BTCDataLayerListener**
   - Utility to parse received Data Layer items
   - Cache methods for price storage
   - Get cached price when rendering

**Supported Styles:**
- `color_theme`: Bitcoin Gold | Silver | Satoshi Green | Ice Blue
- `show_seconds`: Boolean toggle
- `show_price`: Boolean toggle

**Complications:**
- Left (9 o'clock): SHORT_TEXT, SMALL_IMAGE, RANGED_VALUE, MONOCHROMATIC_IMAGE
- Right (3 o'clock): Same types
- Defaults: Date (left), Step Count (right)

**Data Flow:**
```
Watch Face Start → BTCWatchFaceService
  ↓
onDataChanged() callback (Data Layer)
  ↓
Parse DataMap
  ↓
Store in SharedPreferences via BTCDataLayerListener
  ↓
BTCWatchFaceRenderer accesses cached price
  ↓
Render on next tick
```

## Communication Protocol

### Wear Data Layer (Phone → Watch)

**Data Item Structure:**
```
URI Path: /btc_price

DataMap {
  "price": Double (numeric price in USD)
  "price_formatted": String (e.g., "$104,250")
  "timestamp": Long (milliseconds since epoch)
}

Priority: URGENT (immediate delivery, even if watch is in ambient mode)
```

**Example:**
```kotlin
PutDataMapRequest.create("/btc_price")
  .dataMap.apply {
    putDouble("price", 104250.12)
    putString("price_formatted", "$104,250")
    putLong("timestamp", 1707038340000L)
  }
  .setUrgent()  // Force immediate sync
```

**Callbacks:**
- Watch: `onDataChanged(DataEventBuffer)` triggered when data arrives
- Data is automatically persisted by system
- Watch can query latest data anytime via `getDataItems()`

## Storage Strategy

### Phone App
**SharedPreferences:** `btc_price_cache`
```
Key: "price_usd" → Double (string serialized)
Key: "price_formatted" → String
Key: "last_update" → Long (timestamp)
```

### Watch App
**SharedPreferences:** `btc_price_watch`
```
Key: "price" → String (double serialized)
Key: "price_formatted" → String
Key: "timestamp" → Long
```

**Note:** Both use serialization as strings to avoid precision loss with SharedPreferences

## Background Sync Strategy

### WorkManager Configuration
```kotlin
PeriodicWorkRequestBuilder<BTCPriceSyncWorker>(
  15, TimeUnit.MINUTES  // Interval (configurable)
)
.setConstraints(Constraints.Builder()
  .setRequiredNetworkType(NetworkType.CONNECTED)
  .build()
)
.setBackoffCriteria(
  BackoffPolicy.EXPONENTIAL,
  5, TimeUnit.MINUTES  // Initial backoff
)
```

**Retry Logic:**
- Failure → `Result.retry()` → exponential backoff (5, 10, 20, 40 min)
- Max retries: Managed by WorkManager (default ~7 days)
- Success → `Result.success()` → next scheduled run

**Network Awareness:**
- Only runs if phone has active internet connection
- Respects device battery saver mode (if constraints set)
- Works over WiFi or mobile data

## Color Theme System

**Theme Data Structure:**
```kotlin
data class ColorScheme(
  val primaryColor: Int,     // Main hand/border color
  val secondaryColor: Int,   // Secondary hand/text
  val accentColor: Int,      // Seconds hand/highlights
  val bgCenterColor: Int,    // Center background (radial start)
  val bgEdgeColor: Int,      // Edge background (radial end)
  val logoTintColor: Int,    // Bitcoin logo tint
  val priceTextColor: Int    // Price text color
)
```

**Themes:**
1. **Bitcoin Gold** (default) — Classic orange (#F7931A)
2. **Silver** — Clean metallic (#C0C0C0)
3. **Satoshi Green** — Matrix green (#00E676)
4. **Ice Blue** — Cool cyan (#40C4FF)

**Implementation:**
- User selects theme in phone app settings
- Theme applied immediately to watch face
- Renderer switches paint colors on theme change
- Persisted via UserStyleRepository (watch OS framework)

## Ambient Mode (OLED-Friendly)

**When Activated:**
- Screen off / low-power state
- Typically every other second (battery saver)
- Necessary for OLED burn-in prevention

**What Changes:**
- Background: Pure black (`Color.BLACK`)
- Hands: Outline-only (1-2pt white stroke)
- Price display: Hidden (too much power)
- Update frequency: Reduced refresh rate
- Complications: Still rendered but simplified

**Implementation:**
```kotlin
val isAmbientMode = renderParameters.watchFaceLayer == WatchFaceLayer.BASE &&
                    renderParameters.drawMode == DrawMode.AMBIENT

if (isAmbientMode) {
  // Render outline hands only
  canvas.drawLine(..., ambientOutlinePaint)
} else {
  // Render filled hands with colors
  canvas.drawLine(..., hourPaint)
}
```

## Compatibility Notes

### Wear OS 5 & Galaxy Watch 7
- **Screen Sizes:** 1.3" - 1.6" AMOLED (typically 432×432 or 480×480)
- **Refresh Rate:** 60 FPS in interactive, reduced in ambient
- **Complications:** Full support (SHORT_TEXT, RANGED_VALUE, etc.)
- **Data Layer:** Full support via Google Play Services 18.2.0+
- **WorkManager:** Full support on phone side

### Canvas vs. WFF
- **WFF Format:** Purely declarative XML (not used in current implementation, but structure supports it)
- **Current Implementation:** Minimal Canvas rendering (watch_face_btc_gold.xml is reference)
- **Reason:** Analog hand rotation requires dynamic calculation, not pure XML
- **Future:** Could be converted to pure WFF if needed (would require pre-rotated assets)

## Performance Considerations

### Phone App
- **CPU:** Minimal - mostly I/O bound (network + prefs writes)
- **Memory:** ~50 MB (app + frameworks)
- **Battery:** Negligible (WorkManager is optimized for batch operations)
- **Network:** ~10 KB per sync request, ~1 KB response

### Watch Face
- **CPU:** Minimal - rendering ~60 FPS in interactive mode
- **Memory:** ~20 MB
- **Battery:** Most efficient when receiving pushed data (not polling)
- **GPU:** Accelerated rendering via CanvasRenderer2

### Data Layer Sync
- **Latency:** Typically <100ms if watch is active, delayed if in deep sleep
- **Bandwidth:** Negligible (~1 KB per update)
- **Fallback:** Watch can query cached data even if phone not reachable

## Error Handling

### Phone App
```
Network Error
  ↓
BTCPriceFetcher catches and returns null
  ↓
BTCPriceSyncWorker.doWork() returns Result.retry()
  ↓
WorkManager retries with exponential backoff
  ↓
Eventually succeeds or gives up (after ~7 days)
  ↓
Watch continues showing cached (stale) price
```

### Watch App
```
Data Layer not available
  ↓
BTCWatchFaceRenderer uses cached price
  ↓
Display shows last synced value + timestamp
  ↓
User knows data may be stale
```

## Security

### API
- Coinlore: Public, no authentication
- Data: Bitcoin price is not sensitive

### Transport
- HTTPS enforced for Coinlore API
- Wear Data Layer: Uses Play Services encryption

### Storage
- SharedPreferences: App-private scope (MODE_PRIVATE)
- No user credentials stored

### Permissions
- **Phone:** INTERNET, ACCESS_NETWORK_STATE (minimal)
- **Watch:** PROVIDE_BACKGROUND only (no internet needed)

## Testing

### Unit Tests
```
BTCPriceFetcher
  - Valid JSON parsing
  - Invalid response handling
  - Network error fallback

DataLayerSender
  - Correct DataMap structure
  - Urgent flag setting

BTCWatchFaceRenderer
  - Angle calculations (hands)
  - Color theme switching
  - Ambient mode rendering
```

### Integration Tests
- Phone → Watch data sync (MockDataClient)
- WorkManager execution (WorkManager test library)
- UI: Manual testing on real device

### Manual Testing
1. Install both apps
2. Launch phone app
3. Tap "Sync Now" → watch should update within 1 sec
4. Change sync interval in settings → watch should reflect new schedule
5. Change color theme in phone → watch should update colors
6. Verify ambient mode transitions properly
7. Uninstall phone app → watch continues showing cached price

## Debugging

### Logs
```bash
# Watch face logs
adb logcat | grep BTCFace

# WorkManager
adb logcat | grep WorkManager

# Data Layer
adb logcat | grep Wearable
```

### Common Issues

1. **Watch doesn't update price**
   - Check phone app is running
   - Verify WiFi/Bluetooth connected between phone and watch
   - Restart watch face from picker
   - Check logcat for Data Layer errors

2. **WorkManager not triggering**
   - Check "Battery Saver" or "Doze" mode on phone
   - Verify app has background permission
   - Check logcat for WorkManager logs

3. **Colors not changing**
   - Theme change must come through phone app settings
   - Clear watch face app cache and retry
   - Restart watch face service

---

**Document Version:** 1.0  
**Last Updated:** 2026-02-04  
**Target:** Galaxy Watch 7 (Wear OS 5, API 34)
