# BoltShot

A lightweight Android app for **Nothing-style screenshot workflows**. When you take a screenshot, BoltShot shows a floating prompt so you can copy to clipboard, delete, save, or share — without digging through your gallery.

**Free and open source** — sideload the APK or build from source. Not on Google Play. Built for Nothing phones and anyone who wants a fast post-screenshot chooser.

> **Unofficial project.** BoltShot is not affiliated with, endorsed by, or sponsored by Nothing Technology Ltd.

---

## What it does

1. Runs a small foreground monitor while enabled.
2. Detects new screenshots via MediaStore.
3. Shows a floating overlay prompt (or activity/notification fallback).
4. You pick an action — dismiss leaves the screenshot in your gallery.

### Actions

| Action | Behavior |
|--------|----------|
| **Copy & Delete** | Copies the image to clipboard, then deletes the screenshot file |
| **Copy & Save** | Copies to clipboard and keeps the file in your gallery |
| **Share & Delete** | Opens the share sheet, then deletes the screenshot after you pick a target |
| **Share & Save** | Opens the share sheet and keeps the file in your gallery |

The prompt uses a **2×2 grid**: copy actions on one row, share actions on the other. Left = delete variant, right = save variant.

Dismiss (tap outside, if enabled) closes the prompt only.

---

## Install

1. Download the latest **`BoltShot-*.apk`** from [Releases](https://github.com/Red-Bolt-Design/BoltShot/releases).
2. Install the APK (enable “Install unknown apps” for your browser/files app if prompted).
3. Open **BoltShot** and grant the permissions below.

### Requirements

- Android **8.0+** (API 26)
- Tested on Nothing phones; should work on most modern Android devices

---

## First-time setup

Open BoltShot and turn on what you need:

| Setting | Why |
|---------|-----|
| **Monitor screenshots** | Photo/media access so the app can detect new screenshots |
| **Pop-up prompt** | Show the chooser immediately after capture |
| **Floating prompt** *(recommended)* | Display over other apps — works with Dynamic Island / system UI |
| **Instant delete** *(optional)* | “All files” access lets delete actions skip Android’s delete confirmation |
| **Dismiss system preview** *(optional)* | Uses accessibility to auto-hide Android’s corner screenshot preview |

**Notifications** may be requested so the monitor can run in the background and as a fallback if the overlay cannot show.

After a reboot, the monitor restarts automatically if it was enabled.

---

## Customization

In settings you can adjust:

- **Prompt layout** — copy row on top or share row on top (2×2 preview in settings)
- **Theme** — six Nothing-inspired accent palettes (Bolt Red, Glyph, Mono, Ash, Ember, Frost)
- **Prompt position** — top, center, or bottom
- **Vibrate on prompt**
- **Tap outside to dismiss**
- **Detection delay** — increase if the prompt appears before the screenshot finishes saving
- **Test prompt** — preview placement without taking a screenshot

Typography: **Doto Rounded SemiBold** for headlines and the floating prompt; **Roboto Mono** for settings body text.

---

## Privacy

BoltShot is **local-only**:

- No accounts, analytics SDKs, or cloud uploads
- Screenshots are read from your device’s MediaStore only when detected
- Clipboard and share actions use standard Android APIs
- Preferences are stored on-device

The foreground service shows a persistent notification while monitoring is on so Android keeps the process alive.

---

## Build from source

### Prerequisites

- Android Studio (or JDK 17+ and the Android SDK)
- Android SDK with compile SDK 36

### Commands

```bash
git clone https://github.com/Red-Bolt-Design/BoltShot.git
cd BoltShot
./gradlew assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

Install via ADB:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Fonts

Both fonts are downloaded automatically at build time:

- **[Doto](https://fonts.google.com/specimen/Doto)** (Rounded SemiBold) — dot-matrix display type for headlines and the screenshot prompt ([SIL OFL 1.1](https://openfontlicense.org))
- **[Roboto Mono](https://fonts.google.com/specimen/Roboto+Mono)** — settings body copy ([SIL OFL 1.1](https://openfontlicense.org))

---

## Permissions reference

| Permission | Purpose |
|------------|---------|
| `READ_MEDIA_IMAGES` | Detect new screenshots (Android 13+) |
| `READ_EXTERNAL_STORAGE` | Detect screenshots on older Android versions |
| `SYSTEM_ALERT_WINDOW` | Floating overlay prompt |
| `POST_NOTIFICATIONS` | Monitor notification and prompt fallback |
| `FOREGROUND_SERVICE` / `SPECIAL_USE` | Background screenshot monitoring |
| `MANAGE_EXTERNAL_STORAGE` | Optional silent delete without system confirm |
| `VIBRATE` | Haptic on prompt |
| `RECEIVE_BOOT_COMPLETED` | Restart monitor after reboot |
| `USE_FULL_SCREEN_INTENT` | Prompt fallback when overlay is unavailable |
| Accessibility service | Optional — dismisses the system screenshot corner preview when enabled in settings |

---

## Contributing

Issues and pull requests are welcome. Please keep changes focused and match the existing Kotlin/Compose style.

---

## License

BoltShot is **free and open source software**.

- **App source code:** [MIT License](LICENSE) — use, modify, and redistribute with attribution.
- **Fonts:** [Doto](https://fonts.google.com/specimen/Doto) and [Roboto Mono](https://fonts.google.com/specimen/Roboto+Mono) are under the [SIL Open Font License 1.1](https://openfontlicense.org).
- **Android libraries** (AndroidX, Compose, etc.) are used under their respective licenses.
