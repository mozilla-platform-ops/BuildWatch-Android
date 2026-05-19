# BuildWatch Android

A native Android app for Mozilla engineers to monitor Firefox CI build status. Android port of [BuildWatch (iOS)](https://github.com/mozilla-platform-ops/BuildWatch).

![Platform](https://img.shields.io/badge/platform-Android%208.0%2B-brightgreen)
![Kotlin](https://img.shields.io/badge/kotlin-2.x-purple)
![License](https://img.shields.io/badge/license-MPL--2.0-green)

---

## Install

Testers: install via [Firebase App Distribution](https://appdistribution.firebase.dev/i/e1124d6939a77afa). Currently requires a `mozilla.com` email address to authenticate (tracked in `bwa-o8c` — we'd like to lift this).

---

## Features

### Try Pushes
- Live push list for your try pushes
- Per-push platform status dots — color coded by worst result (green / red / orange / blue)
- Failure count and in-progress job counts per push
- Pull-to-refresh with last-updated timestamp
- Swipe right on any push to toggle build-failure notifications for it

### Push Detail
- Full commit messages with clickable bug number links
- Jobs grouped by platform (linux64, win64, macos, android-arm64, …)
- Filter by All / Failures / Running
- Retrigger any individual job with a tap
- **Retrigger All Failed** with a single tap
- **Failure Summary** — groups failed jobs by test name, shows affected job counts and raw error lines from TreeHerder log data
- All external links (TreeHerder, Taskcluster, Bugzilla) open in an in-app Custom Tab — tap back to return instantly
- Duration shown for every completed job

### Settings
- LDAP username for filtering your try pushes
- Build notification opt-in per push (via swipe gesture on push list)

---

## Data Sources

All APIs are public — no VPN or Mozilla network access required.

| Source | Used for |
|--------|----------|
| [TreeHerder](https://treeherder.mozilla.org) | Push list, job results, retrigger |
| [Taskcluster](https://firefox-ci-tc.services.mozilla.com) | Task deep links |
| [Bugzilla](https://bugzilla.mozilla.org) | Bug links from commit messages |

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

---

## Building from the command line

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"

./gradlew assembleDebug          # build only
./gradlew installDebug           # build + install on connected device/emulator
./gradlew test                   # run unit tests
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

---

## Optional Setup

**Try Pushes tab** — add your LDAP username (e.g. `jsmith`) in Settings. The app will filter the push list to your pushes only.

**Build notifications** — swipe right on any push in the list to watch it. You'll receive a notification when its builds finish.

---

## Architecture

```
app/src/main/java/com/mozilla/buildwatch/
├── model/           — Push, Job, FailureLine data classes
├── service/         — TreeHerderService (Retrofit + OkHttp)
├── repository/      — SettingsRepository (DataStore)
├── viewmodel/       — DashboardViewModel, SettingsViewModel
└── ui/
    ├── screen/      — TryPushesScreen, PushDetailScreen, SettingsScreen, FailureSummarySheet
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

## Contributing

PRs welcome. File issues on [GitHub](https://github.com/aerickson/BuildWatch-Android/issues).

---

*Android port maintained at [mozilla-platform-ops/BuildWatch-Android](https://github.com/mozilla-platform-ops/BuildWatch-Android) · Powered by [TreeHerder](https://treeherder.mozilla.org)*
