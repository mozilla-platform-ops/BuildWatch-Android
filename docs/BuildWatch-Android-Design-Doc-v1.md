# BuildWatch Android — Design Document v1

**Author**: aerickson  
**Date**: 2026-04-29  
**Status**: Draft  
**iOS reference**: [BuildWatch (iOS)](https://github.com/mozilla-platform-ops/BuildWatch) — authored by a teammate

---

## Goals

Ship a native Android version of BuildWatch that Mozilla teammates can install and use as a real alternative to the iOS app. Feature parity with the current iOS build. Single-developer project, built in Kotlin + Jetpack Compose. No shared code with iOS — the `docs/API.md` file in the iOS repo is the shared source of truth for API contracts and domain rules.

**Out of scope for v1**: Wear OS, widgets, multi-repo (only `try` is implemented in iOS), tree status banner, Bugzilla filing, Tier 2 job toggle, multiple accounts.

---

## Approach: Standalone Fork

The iOS codebase is ~550 LOC of a thin REST client. The decision was made to fork rather than use Kotlin Multiplatform:

- Imposing a Gradle/JVM/Android SDK toolchain on the teammate's Xcode-only iOS project isn't worth it at this size.
- Duplication is low: the shared logic is mostly JSON mapping and a few heuristics, documented in `docs/API.md`.
- Reversible: KMP can be adopted later if drift becomes a real problem.

---

## Repository

New repo: `BuildWatch-Android` (mozilla-platform-ops org or personal fork to start).

---

## Library Choices

Boring and mainstream. No experimental APIs.

| Concern          | Library                                              | Notes                                                  |
|------------------|------------------------------------------------------|--------------------------------------------------------|
| Networking       | Retrofit 2 + OkHttp                                  | REST + JSON, no streaming needed                       |
| JSON             | `kotlinx.serialization`                              | Mirrors Swift's `Codable` ergonomics                   |
| Concurrency      | Kotlin coroutines + `StateFlow`                      | Maps 1:1 to the iOS `async`/`await` + `withTaskGroup`  |
| State / UI       | Jetpack Compose + Material 3                         | Modern Android UI toolkit                              |
| Persistence      | Jetpack DataStore (Preferences)                      | Replaces `UserDefaults`; two keys only                 |
| Notifications    | `NotificationManagerCompat`                          | One channel: `build_complete`                          |
| In-app browser   | Chrome Custom Tabs                                   | Matches iOS `SFSafariViewController` UX                |
| Pull-to-refresh  | Compose `PullToRefreshBox` (Material 3)              | Stable API 26+                                         |
| Navigation       | `androidx.navigation.compose`                        |                                                        |
| DI               | None — manual construction                           | App is too small; matches iOS singleton style          |

`minSdk = 26` (Android 8.0). `targetSdk = 35`.

---

## Module Layout

Single-module app (`:app`). No multi-module, no feature modules.

```
app/src/main/java/com/mozilla/buildwatch/
├── model/
│   ├── Push.kt                  — Push, PushRevision, PushesResponse
│   ├── Job.kt                   — JobResult, JobState, Job, PlatformGroup
│   └── FailureLine.kt           — TextLogError, FailureGroup
├── service/
│   └── TreeHerderService.kt     — Retrofit interface + suspend implementations
├── viewmodel/
│   └── DashboardViewModel.kt    — Single shared ViewModel; StateFlow state
├── repository/
│   └── SettingsRepository.kt    — DataStore wrapper for username + watchedPushIds
└── ui/
    ├── MainActivity.kt
    ├── theme/
    │   ├── Theme.kt             — Material 3 color scheme, typography
    │   └── JobColors.kt         — JobResult → Color + icon mappings (Compose)
    ├── screen/
    │   ├── TryPushesScreen.kt   — Push list (tab 1)
    │   ├── PushDetailScreen.kt  — Job list, filters, retrigger
    │   ├── FailureSummarySheet.kt — Bottom sheet: grouped failures
    │   └── SettingsScreen.kt    — LDAP username, notification toggle (tab 2)
    └── component/
        ├── PushRowItem.kt       — Push card with status dots + badge
        ├── PlatformStatusDots.kt — Row of 8px colored circles
        ├── PlatformGroupHeader.kt — Section header with status icon + counts
        ├── JobRowItem.kt        — Job list row with icon, name, duration, retrigger swipe
        └── PushSummaryBar.kt    — Job count chips at top of push detail
```

---

## Data Flow

```
DataStore (SettingsRepository)
        │ username, watchedPushIds
        ▼
DashboardViewModel
  ├── StateFlow<List<Push>>            pushes
  ├── StateFlow<Map<Int,List<Job>>>    jobsByPush
  ├── StateFlow<Map<Int,List<TextLogError>>> failureLinesByPush
  ├── StateFlow<Boolean>               isRefreshing
  ├── StateFlow<String?>               errorMessage
  ├── StateFlow<Instant?>              lastRefresh
  └── StateFlow<Set<Int>>              watchedPushIds
        │
        ▼
TreeHerderService (Retrofit)
  ├── fetchPushes(count, author?)
  ├── fetchJobs(pushId)
  ├── fetchTextLogErrors(jobId)
  └── retriggerJob(jobId)
```

The ViewModel is created once in `MainActivity` and accessed via `viewModel()` in each Composable. No `@Singleton` needed — Activity scope is fine for this app.

---

## Screen Designs

### Tab 1 — Try Pushes

Mirrors `TryPushesView.swift`. Shows the authenticated user's try pushes.

**States**:
- No username set → prompt to go to Settings (full-screen empty state)
- Loading (first load) → centered `CircularProgressIndicator`
- Error (no data) → error message + "Try Again" button
- Empty (no pushes) → "No try pushes" empty state
- Data → scrollable `LazyColumn` of `PushRowItem`s

**Push row** (`PushRowItem`):
- Title: `push.displayTitle` (2-line max, medium weight)
- Second line: author handle | bell icon if watched | status dots | failure badge or running spinner | time ago

**Status dots**: up to 8 dots (8dp circles), one per `PlatformGroup`, colored by `overallStatus`. Placeholder gray dots while loading.

**Failure badge**: red pill with failure count (shown when `failureCount > 0`). If running, show a rotating `CircularProgressIndicator` (small). If complete and passing, show a green checkmark icon.

**Swipe to watch**: swipe right on a push row to toggle watch. Show bell icon: orange to watch, gray to unwatch.

**App bar**: title = LDAP handle (from settings). Trailing: last-refresh timestamp + refresh button (or `CircularProgressIndicator` while refreshing).

**Pull to refresh**: `PullToRefreshBox` wrapper.

---

### Tab 2 — Settings

Mirrors `SettingsView.swift`.

**Profile section**: text field for LDAP handle (`rcurran`, not full email). Store as `rcurran@mozilla.com` internally. Caption: "Filters try pushes to your commits."

**Notifications section**: toggle switch. On first enable, request `POST_NOTIFICATIONS` permission (Android 13+). Show status beneath: Enabled / Disabled in Settings / Not Set Up.

**About section**: app version, links to TreeHerder and Taskcluster (open in Chrome Custom Tabs).

---

### Push Detail

Mirrors `PushDetailView.swift`. Full screen (not a tab).

**Top section** (inside lazy column):
- Author + time ago
- Each revision: short message + 12-char revision hash + optional "Bug NNNNNN" chip (opens Bugzilla in Custom Tab)
- `PushSummaryBar`: ✓N ✗N ⚙N ⏱N | total jobs

**Quick Actions section**:
- "Retrigger All Failed" — disabled if no failures
- "Failure Summary" — opens `FailureSummarySheet`; disabled if no failures
- "Open in TreeHerder" — opens Custom Tab
- "Open Bug" — shown if any revision has a bug number; opens Custom Tab

**Jobs sections**: grouped by `PlatformGroup`, sorted alphabetically. Section header: `PlatformGroupHeader` (status icon + name + failure/running/success counts). Each row: `JobRowItem`.

**Job row**:
- Leading icon: result icon (colored) or running spinner or clock (pending)
- Job type name (1 line)
- Duration if complete
- Trailing: result label if failure, Taskcluster link button if `taskId` present
- Swipe left: "Retrigger" action (blue) — shows confirmation dialog

**Filter picker** (app bar trailing): All / Failures / Running. Filters both groups shown and jobs within each group (same logic as iOS).

---

### Failure Summary Sheet

`ModalBottomSheet`. Mirrors `FailureSummaryView.swift`.

Loads `TextLogError` for up to 15 failed jobs in parallel (`coroutineScope { … awaitAll() }`). Groups by `groupKey` (see `docs/API.md`). Sorted by `affectedJobCount` descending.

Each item: pattern string (the group key), "N jobs" badge, collapsed example error line (expandable or truncated to 2 lines).

---

## ViewModel Design

`DashboardViewModel` extends `androidx.lifecycle.ViewModel`. State is `MutableStateFlow<T>` exposed as `StateFlow<T>`.

Key behaviors mirroring the iOS ViewModel:

**`refresh()`**:
1. Guard: `isRefreshing == true` → return.
2. Set `isRefreshing = true`.
3. Fetch pushes (filtered by `username` if set).
4. On success: update `pushes`, set `lastRefresh`. Clear cached jobs for watched pushes.
5. Immediately launch job fetches for the first 5 pushes (unconditionally).
6. Also launch job fetches for any watched push beyond the first 5.
7. On error: set `errorMessage`.
8. Set `isRefreshing = false` (in `finally`).

**`fetchJobs(push)`**: guard if `jobsByPush[push.id] != null`. Fetch, store, then call `checkCompletion(push)`.

**`fetchFailureLines(push)`**: guard if already loaded. Collect all failed+completed jobs (cap at 15). Fetch text log errors in parallel via `coroutineScope { jobs.map { async { … } }.awaitAll() }`. Store result.

**`toggleWatch(push)`**: add or remove from `watchedPushIds`, persist via `SettingsRepository`. On add, request notification permission if needed, then call `checkCompletion(push)`.

**`checkCompletion(push)`** (see full predicate in `docs/API.md` → "Watch / completion notification predicate"):
1. Guard: push is watched, not yet notified, jobs loaded, no running/pending jobs.
2. Remove from `watchedPushIds`, add to `notifiedPushIds`, persist.
3. Compute failure count. Fire local notification.

**Notification flow**: permission request on first watch. Android 13+: request `POST_NOTIFICATIONS` at runtime before scheduling. Notification channel `build_complete` created at app startup. Show notification in foreground too (set `heads-up` priority).

---

## Notification Deep Link

On notification tap, open the app and navigate to the relevant push detail. Pass `pushId` in the notification's `extras` bundle. In `MainActivity.onCreate` and `onNewIntent`, check for `pushId` and navigate to `PushDetailScreen`.

This mirrors the iOS `NotificationDelegate` + `Notification.Name.openPush` pattern.

---

## Chrome Custom Tabs

Create a helper:

```
fun openUrl(context: Context, url: String) {
    CustomTabsIntent.Builder()
        .setShowTitle(true)
        .build()
        .launchUrl(context, url.toUri())
}
```

Used by all external links: TreeHerder, Taskcluster, Bugzilla. Back button returns to the app instantly — same as iOS `SFSafariViewController` + "Done" button.

---

## Distribution (v1)

Firebase App Distribution for initial teammate testing. Set up Play Store internal track once the app is stable. Signing keystore kept outside the repo.

---

## Build & Test

**Unit tests**: JUnit + Kotlin test. Cover:
- `groupKey` parser against fixture log lines
- `cleanTryMessage` against all prefix variants
- `PlatformGroup.overallStatus` reduction logic
- `displayTitle` selection across mixed revision lists

**Service tests**: OkHttp `MockWebServer`. Verify request URLs and query params match `docs/API.md` exactly. Use captured Treeherder JSON fixtures.

**Manual device QA** (against the iOS app as the baseline, same push):
- Push list loads, status dots update
- Watch + completion notification fires (use a live running push)
- Android 13+: notification permission denial handled gracefully
- Retrigger single job (confirm dialog → call fires)
- Retrigger all failed (re-fetches jobs after)
- Failure summary groups correctly
- All three Custom Tab link types open and Back returns to app
- Settings persist across cold restarts

---

## Porting Reference

The five iOS source files are the definitive spec for logic. Read them before porting each piece.

| Android file | iOS reference | Key notes |
|---|---|---|
| `model/Push.kt` | `Models/Push.swift` | `@SerialName("push_timestamp")` for the snake_case field. Port `displayTitle` + `cleanTryMessage` exactly per API.md. |
| `model/Job.kt` | `Models/Job.swift` | Strip `Color` and SF Symbol strings — those go in `ui/theme/JobColors.kt`. Port `durationString` format: `"Nm Ss"` or `"Ss"`. |
| `model/FailureLine.kt` | `Models/FailureLine.swift` | Port `groupKey` verbatim. Add unit tests with fixture log lines. |
| `service/TreeHerderService.kt` | `Services/TreeHerderService.swift` | Compact job parser is the trickiest part — match the property-name index lookup exactly. |
| `viewmodel/DashboardViewModel.kt` | `ViewModels/DashboardViewModel.swift` | The refresh fan-out (first 5 + watched) and the watch/notify predicate must be identical. |
