# BuildWatch Android

A native Android app for Mozilla engineers to monitor Firefox CI build status. Android port of [BuildWatch (iOS)](https://github.com/mozilla-platform-ops/BuildWatch).

---

## Requirements

- **Android Studio** Meerkat (2024.3) or newer — [download](https://developer.android.com/studio)
- **Android SDK** platform 35 (installed automatically by Android Studio on first sync)
- **Java 17+** — bundled with Android Studio, no separate install needed
- **Android Emulator** or a physical device running Android 8.0+ (API 26+)

---

## Building and Running

### 1. Clone the repo

```bash
git clone https://github.com/mozilla-platform-ops/BuildWatch-Android.git
cd BuildWatch-Android
```

### 2. Open in Android Studio

File → Open → select the `BuildWatch-Android` folder.

Android Studio will detect the Gradle project and prompt you to sync. Click **Sync Now**. This downloads dependencies (~1–2 min on first run).

### 3. Create an emulator

If you don't have an emulator set up:

1. **Device Manager** (toolbar or View → Tool Windows → Device Manager)
2. Click **+** → **Create Virtual Device**
3. Pick any phone (e.g. **Pixel 8**) → Next
4. Select a system image — **API 35** (download if needed) → Next → Finish

### 4. Run the app

Select your emulator from the device dropdown in the toolbar, then click **Run ▶** (or `Ctrl+R` / `Cmd+R`).

The app will build and launch on the emulator.

---

## Building from the command line

Set `JAVA_HOME` to Android Studio's bundled JDK and `ANDROID_HOME` to your SDK location, then use the Gradle wrapper:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"

./gradlew assembleDebug          # build only
./gradlew installDebug           # build + install on connected device/emulator
./gradlew test                   # run unit tests
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

---

## First-time setup in the app

1. Open **Settings** (bottom tab)
2. Enter your LDAP username (e.g. `rcurran` — just the handle, not the full email)
3. Switch to the **Try Pushes** tab to see your CI pushes

---

## Architecture

```
app/src/main/java/com/mozilla/buildwatch/
├── model/           — Push, Job, FailureLine data classes
├── service/         — TreeHerderService (Retrofit + OkHttp)
├── repository/      — SettingsRepository (DataStore)
├── viewmodel/       — DashboardViewModel, SettingsViewModel
└── ui/
    ├── screen/      — TryPushesScreen, PushDetailScreen, SettingsScreen
    ├── component/   — Reusable Compose components
    └── theme/       — Material 3 theme, JobColors
```

Single-module Kotlin + Jetpack Compose app. State via `StateFlow`, persistence via DataStore Preferences, networking via Retrofit + OkHttp. See [`docs/BuildWatch-Android-Design-Doc-v1.md`](docs/BuildWatch-Android-Design-Doc-v1.md) for the full design spec and [`docs/API.md`](docs/API.md) for the TreeHerder API contract.

---

## Development workflow

Issues are tracked in `.beads/` using [`br`](https://github.com/Dicklesworthstone/beads_rust). To find the next actionable task:

```bash
br ready
```

This shows open, unblocked issues sorted by priority. See [AGENTS.md](AGENTS.md) for the full workflow (claiming work, closing issues, syncing).

---

## Data sources

All APIs are public — no VPN or Mozilla network access required.

| Source | Used for |
|--------|----------|
| [TreeHerder](https://treeherder.mozilla.org) | Push list, job results, retrigger |
| [Taskcluster](https://firefox-ci-tc.services.mozilla.com) | Task deep links |
| [Bugzilla](https://bugzilla.mozilla.org) | Bug links from commit messages |
