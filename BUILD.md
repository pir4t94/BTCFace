# BTCFace Build & Release Guide

Complete guide to building, testing, and releasing BTCFace apps.

## Table of Contents

1. [Build Environment Setup](#build-environment-setup)
2. [Debug Build](#debug-build)
3. [Release Build](#release-build)
4. [Testing](#testing)
5. [Publishing](#publishing)
6. [Troubleshooting](#troubleshooting)

## Build Environment Setup

### Requirements

- **Android Studio:** Hedgehog (2023.1.1) or later
- **JDK:** 17 or later
- **Gradle:** 8.0+
- **Android SDK:**
  - Compile SDK: 34
  - Min SDK (phone): 26
  - Min SDK (watch): 30
- **Emulators (optional):**
  - Android phone emulator (API 26+)
  - Wear OS emulator (API 30+)

### Setup Steps

1. **Install Android Studio**
   - Download from developer.android.com
   - Install JDK 17+ during setup

2. **Configure SDK**
   ```
   Android Studio → Settings → Appearance & Behavior → System Settings → Android SDK
   ```
   - SDK Platforms: API 34 (Android 14)
   - SDK Tools:
     - Android SDK Build Tools 34.x.x
     - Android SDK Platform-Tools
     - Android Emulator
     - Google Play Services

3. **Configure Gradle**
   ```
   Android Studio → Settings → Build, Execution, Deployment → Gradle
   ```
   - Gradle JDK: 17 or later
   - Build tools version: 34.x.x

4. **Check JDK**
   ```bash
   java -version
   # Should output: openjdk version "17.x.x" or higher
   ```

## Debug Build

### From Command Line

#### Build Both Modules
```bash
cd BTCFace
./gradlew clean assembleDebug

# Output locations:
# - Phone: app/build/outputs/apk/debug/app-debug.apk
# - Watch: wearable/build/outputs/apk/debug/wearable-debug.apk
```

#### Build Individual Modules
```bash
# Phone app only
./gradlew clean :app:assembleDebug

# Watch face only
./gradlew clean :wearable:assembleDebug
```

### From Android Studio

1. **Select Module**
   - Dropdown in toolbar: Select "app" or "wearable"

2. **Build**
   - Menu: Build → Build Bundle(s)/APK(s) → Build APK(s)
   - Or: Shift+F10 (default shortcut)

3. **Output**
   - Android Studio shows location in popup
   - Usually in `module/build/outputs/apk/debug/`

### Install Debug APK

#### On Physical Device
```bash
# Phone
adb install app/build/outputs/apk/debug/app-debug.apk

# Watch (must be connected via Bluetooth)
adb install wearable/build/outputs/apk/debug/wearable-debug.apk
```

#### Using Android Studio Run
1. Connect device (or open emulator)
2. Select module in dropdown
3. Click Run (Ctrl+R or ▶️ button)
4. Select device/emulator
5. APK installs automatically

### Debug APK Info

```bash
# Check installed app version
adb shell dumpsys package com.roklab.btcface.companion | grep version

# Check watch app version
adb shell dumpsys package com.roklab.btcface.wearable | grep version

# View app info
adb shell pm list packages | grep btcface
```

## Release Build

### Prerequisites

1. **Keystore File**
   - Create if not exists:
   ```bash
   keytool -genkey -v -keystore BTCFace.keystore \
     -keyalg RSA -keysize 2048 -validity 10000 \
     -alias BTCFace_Key
   ```
   - Save keystore password securely
   - Keep `BTCFace.keystore` in project root

2. **Build Properties**
   Create `local.properties` in project root:
   ```properties
   # Not in git (add to .gitignore if not there)
   storeFile=./BTCFace.keystore
   storePassword=YOUR_STORE_PASSWORD
   keyAlias=BTCFace_Key
   keyPassword=YOUR_KEY_PASSWORD
   ```

   Or export as environment variables:
   ```bash
   export BTC_STORE_PASSWORD="your_password"
   export BTC_KEY_PASSWORD="your_password"
   ```

3. **Update Version**
   In `app/build.gradle.kts` and `wearable/build.gradle.kts`:
   ```kotlin
   defaultConfig {
       versionCode = 2  // Increment by 1
       versionName = "1.1.0"  // Update version
   }
   ```

### Build Release APK

#### Command Line
```bash
./gradlew clean bundleRelease

# Output:
# - Phone: app/build/outputs/bundle/release/app-release.aab
# - Watch: wearable/build/outputs/bundle/release/wearable-release.aab

# Alternative: Build APK directly (older, not recommended for Store)
./gradlew clean assembleRelease
```

#### Android Studio
1. Build → Generate Signed Bundle / APK
2. Select "Android App Bundle" (recommended for Play Store)
3. Select keystore file and enter passwords
4. Select "release" build variant
5. Finish

### Release APK Info

```bash
# Extract APK from Bundle (for testing)
bundletool build-apks \
  --bundle=app/build/outputs/bundle/release/app-release.aab \
  --output=app-release.apks \
  --ks=BTCFace.keystore \
  --ks-pass=pass:PASSWORD

# Install from APKS
bundletool install-apks --apks=app-release.apks
```

### ProGuard/R8 Obfuscation

Debug builds skip obfuscation (faster builds).
Release builds use ProGuard rules in:
- `app/proguard-rules.pro`
- `wearable/proguard-rules.pro`

**Check ProGuard output:**
```bash
# Mapping file (obfuscation rules)
app/build/outputs/mapping/release/mapping.txt

# Keep this for crash stack traces!
# Store in version control (separately encrypted if public)
```

## Testing

### Unit Tests

Not included in current version. To add:
```bash
# Create test file
mkdir -p app/src/test/java/com/roklab/btcface
cat > app/src/test/java/com/roklab/btcface/BTCPriceFetcherTest.kt << 'EOF'
// Test code here
EOF

# Run tests
./gradlew test
```

### Manual Testing

#### Setup
1. **Phone:** API 26+ emulator or real device
2. **Watch:** API 30+ emulator or Galaxy Watch 7
3. **Network:** Both connected to same network

#### Test Cases

**1. Installation**
- [ ] Phone app installs without errors
- [ ] Watch face installs without errors
- [ ] Both apps appear in respective app drawers

**2. Basic Functionality**
- [ ] Phone app displays BTC price
- [ ] Phone app shows "Loading..." initially
- [ ] Watch face appears in face picker
- [ ] Watch face shows time (hours + minutes)

**3. Data Sync (Manual)**
- [ ] Phone: Tap "Sync Now"
- [ ] Watch: Price updates within 1 second
- [ ] Prices match between phone and watch

**4. Data Sync (Automatic)**
- [ ] Wait 15 minutes
- [ ] Watch price auto-updates
- [ ] Phone shows "Last update" timestamp

**5. Settings**
- [ ] Change sync interval to 5 min
- [ ] Change color theme
- [ ] Theme updates appear on watch
- [ ] Seconds hand toggle works

**6. Offline Behavior**
- [ ] Disable phone WiFi
- [ ] Watch shows cached (old) price
- [ ] Re-enable WiFi → sync resumes
- [ ] Price updates correctly

**7. Ambient Mode (Watch)**
- [ ] Let watch go to sleep
- [ ] Wake with tap
- [ ] Ambient mode displays outline only
- [ ] No price display in ambient
- [ ] Returns to normal mode

**8. Complications**
- [ ] Left complication (9 o'clock) shows data
- [ ] Right complication (3 o'clock) shows data
- [ ] Complications update

**9. Edge Cases**
- [ ] Force stop phone app → watch shows cached price
- [ ] Restart watch → price syncs on next interval
- [ ] Network error (WiFi off) → retry works
- [ ] Battery saver on phone → sync still works

**10. Performance**
- [ ] Watch face: ~60 FPS interactive, smooth scrolling
- [ ] Phone app: <2 sec startup
- [ ] No memory leaks (check Android Profiler)
- [ ] No ANRs (Application Not Responding)

### Performance Testing

#### Memory Profiler
```
Android Studio → Profiler → Memory
- Phone app idle: ~50 MB
- Watch face idle: ~20 MB
- No memory leaks on 1-hour run
```

#### Battery Profiler
```
Android Studio → Profiler → Energy
- Phone app background: ~1-2% per hour
- Watch face: Minimal CPU impact
```

#### Frame Rate
```
Android Studio → Profiler → CPU / Frame Rendering
- Interactive (watch): 60 FPS
- Ambient (watch): <1 FPS (expected)
```

### Automated Testing (CI/CD)

GitHub Actions example (`.github/workflows/build.yml`):
```yaml
name: Build
on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Set up JDK 17
        uses: actions/setup-java@v2
        with:
          java-version: 17
      - name: Build debug
        run: ./gradlew assembleDebug
      - name: Upload APKs
        uses: actions/upload-artifact@v2
        with:
          name: apks
          path: '*/build/outputs/apk/debug/*.apk'
```

## Publishing

### Google Play Store

#### Prerequisites
1. Google Play Developer account ($25 one-time fee)
2. Signed App Bundle (.aab files)
3. Icon and screenshots
4. Privacy policy
5. Store listing text

#### Preparation

**App Icons**
- Launcher icon: 512x512 PNG
- Place in `app/src/main/res/drawable/ic_launcher_foreground.xml`

**Screenshots (for Play Store)**
- Phone: 1242x2208 PNG
- Watch: 480x480 PNG
- 2-5 screenshots each

**Store Listing**
- Title: "BTCFace - Bitcoin Wear OS Watch Face"
- Short description: "Live BTC price on your wrist"
- Full description: See QUICKSTART.md for content
- Category: Lifestyle / Customization (watch face)
- Content rating: Everyone

#### Release Steps

1. **Build Release Bundle**
   ```bash
   ./gradlew bundleRelease
   ```

2. **Create Google Play Console App**
   - Go to play.google.com/console
   - Create new app
   - Fill in app details
   - Create "Phone App" and "Watch App" separately

3. **Upload Bundle**
   - Releases → Create Release → Internal Testing
   - Upload .aab file
   - Add release notes
   - Review app details

4. **Test Track**
   - Move to Testing → Beta release
   - Add testers (email list)
   - Share beta link with testers
   - Gather feedback

5. **Staged Rollout**
   - Move to Production
   - Start with 25% rollout
   - Monitor crash reports + ratings
   - Increase to 50%, 75%, 100%

6. **Release Notes**
   ```
   Version 1.1.0 - Data Layer Sync
   - Companion phone app handles BTC price fetching
   - Configurable sync interval (5, 10, 15, 30 min, 1 hour)
   - Manual sync button in phone app
   - Improved battery efficiency
   
   Version 1.0.0 - Initial Release
   - Analog Bitcoin watch face
   - 4 color themes
   - Live BTC price display
   - Complication support
   ```

### GitHub Release

```bash
# Create git tag
git tag -a v1.1.0 -m "Release version 1.1.0"
git push origin v1.1.0

# Create release on GitHub
# - Go to Releases → Draft New Release
# - Select tag: v1.1.0
# - Add release notes
# - Upload APK/AAB files
# - Publish
```

## Troubleshooting

### Build Errors

#### "Could not resolve dependency"
```bash
# Update gradle
./gradlew wrapper --gradle-version=8.5

# Refresh dependencies
./gradlew clean build --refresh-dependencies
```

#### "Gradle build failed"
```
Error: Could not determine the dependencies of...

Solution:
1. Clean: ./gradlew clean
2. Invalidate Android Studio cache: File → Invalidate Caches
3. Retry build
```

#### "java.lang.UnsupportedClassVersionError"
```
Error: Unsupported class-file format

Solution: Update JDK to 17+
Android Studio → Settings → Build, Execution, Deployment → Gradle
Change "Gradle JDK" to 17
```

### Installation Errors

#### "INSTALL_FAILED_INVALID_APK"
```bash
# APK may be corrupted
./gradlew clean assembleDebug

# Re-sign
adb uninstall com.roklab.btcface.companion
adb install app/build/outputs/apk/debug/app-debug.apk
```

#### "INSTALL_FAILED_INSUFFICIENT_STORAGE"
```bash
# Device storage full
adb shell pm clear com.android.vending  # Clear Play Store cache
adb shell pm trim-caches 1G  # Free up space
```

#### "INSTALL_FAILED_PERMISSION_ERROR"
```bash
# Keystore signature mismatch
adb uninstall com.roklab.btcface.companion  # Uninstall old version
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Release Build Errors

#### "Keystore not found"
```
Error: Keystore file not found

Solution:
1. Ensure BTCFace.keystore exists in project root
2. Check local.properties has correct path
3. Verify file permissions: chmod 600 BTCFace.keystore
```

#### "Invalid keystore password"
```
Error: wrong password for the key

Solution:
1. Verify password in local.properties
2. Check no special chars are escaped incorrectly
3. Recreate keystore if password forgotten
```

#### "APK signature verification failed"
```bash
# Check signature
jarsigner -verify -verbose -certs app-release.apk

# Solution: Re-build with correct keystore
./gradlew clean bundleRelease
```

### Runtime Errors

#### "No matching client found for package"
```
Error: BroadcastReceiver not registered

Solution:
1. Check AndroidManifest.xml has all intent-filters
2. Ensure exported="true" for all services
3. Verify package name matches (com.roklab.btcface)
```

#### "Data Layer not connecting"
```
Error: Wearable Data Client connection failed

Solution:
1. Verify Google Play Services installed on both devices
2. Check Bluetooth between phone and watch
3. Clear Google Play Services cache:
   adb shell pm clear com.google.android.gms
```

## Version Management

### Version Scheme

```
vMAJOR.MINOR.PATCH

Example: v1.2.3
- 1 = major version (breaking changes)
- 2 = minor version (new features)
- 3 = patch version (bug fixes)
```

### Updating Version

**In `app/build.gradle.kts`:**
```kotlin
android {
    defaultConfig {
        versionCode = 3  // Always increment
        versionName = "1.0.3"  // Follow semver
    }
}
```

**In `wearable/build.gradle.kts`:**
```kotlin
android {
    defaultConfig {
        versionCode = 3  // Match phone app
        versionName = "1.0.3"  // Match phone app
    }
}
```

### Changelog

Maintain `CHANGELOG.md`:
```markdown
## [1.1.0] - 2026-02-04

### Added
- Companion phone app
- Configurable sync interval
- Manual sync button

### Changed
- Two-app architecture
- Wear Data Layer sync

### Fixed
- Battery life on watch
```

## Continuous Integration

### GitHub Actions Setup

1. Create `.github/workflows/build.yml`
2. Configure build job (see example above)
3. Push to GitHub
4. Check Actions tab for build status

### Pre-commit Hooks

Optional: Prevent commits with build errors
```bash
cat > .git/hooks/pre-commit << 'EOF'
#!/bin/bash
./gradlew lint || exit 1
EOF
chmod +x .git/hooks/pre-commit
```

---

**Build Summary:**
- Debug: `./gradlew assembleDebug` (fast, for testing)
- Release: `./gradlew bundleRelease` (optimized, for publishing)
- Test: Manual + logcat (no automated tests yet)
- Publish: Google Play Console + GitHub Releases

**Keep keystore safe!** Store `BTCFace.keystore` securely (not in git).
