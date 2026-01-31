# ₿ BTCFace — Bitcoin Analog Watch Face for Wear OS

A sleek analog watch face for Wear OS with a Bitcoin soul. Features a subtle tilted Bitcoin logo watermark, live BTC price display, customizable color themes, and complication support.

## Features

- **Analog dial** with smooth sweeping seconds hand
- **Live BTC price** window at 6 o'clock (updates every ~15 min via [Coinlore API](https://api.coinlore.net/api/ticker/?id=90))
- **Bitcoin logo watermark** — tilted 14° like the real deal, subtle in the background
- **4 color themes** — Bitcoin Gold, Silver, Satoshi Green, Ice Blue (all gradient-based)
- **2 complication slots** — left (9 o'clock) and right (3 o'clock), configurable
- **BTC Price complication provider** — other watch faces can use this too
- **Ambient mode** — OLED-friendly outline hands, dimmed display
- **Customizable toggles** — seconds hand, price display, hour markers

## Color Themes

| Theme | Primary | Vibe |
|-------|---------|------|
| Bitcoin Gold | 🟠 `#F7931A` | The classic — warm, amber, iconic |
| Silver | ⚪ `#C0C0C0` | Clean, minimal, metallic |
| Satoshi Green | 🟢 `#00E676` | Matrix-core — line goes up |
| Ice Blue | 🔵 `#40C4FF` | Cool, futuristic, calm |

## Architecture

```
com.roklab.btcface/
├── BTCWatchFaceService.kt          # Main service — style schema, complication slots
├── BTCCanvasRenderer.kt            # Canvas renderer — draws everything
├── BTCPriceFetcher.kt              # API calls + SharedPreferences cache
├── BTCPriceWorker.kt               # WorkManager periodic fetch (every 15 min)
├── BTCPriceComplicationService.kt  # Complication data source for other faces
└── theme/
    └── WatchFaceColors.kt          # Color scheme definitions
```

## Building

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- Wear OS emulator or physical watch (API 30+)
- JDK 17

### Build
```bash
./gradlew assembleDebug
```

### Install on watch
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

Then select **BTCFace** from the watch face picker on your watch.

## API

BTC price is fetched from [Coinlore](https://www.coinlore.com/cryptocurrency-data-api):
```
GET https://api.coinlore.net/api/ticker/?id=90
→ [{ "price_usd": "104250.12", ... }]
```

No API key required. Updates every 15 minutes via WorkManager with network constraints.

## Requirements

- **Wear OS 3+** (API 30)
- **Internet permission** for price fetching
- Targets **Wear OS 5** (API 34)

## License

MIT
