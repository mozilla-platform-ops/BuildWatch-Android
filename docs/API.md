# BuildWatch API Contract

Language-neutral reference for the data sources and domain rules that drive BuildWatch. Update this file when an API shape or rule changes so both the iOS and Android clients stay in sync.

---

## HTTP Conventions

- **Base URL**: `https://treeherder.mozilla.org/api`
- **Timeout**: 30 seconds
- **User-Agent**: `BuildWatch-iOS/1.0 (https://github.com/rcurran/BuildWatch)` (Android should use an equivalent string)
- **Auth**: None required for read endpoints. The retrigger POST requires a valid session (see below).
- All responses are JSON.

---

## Endpoints

### 1. Fetch Pushes

Fetches a list of try pushes, optionally filtered by author.

```
GET /api/project/try/push/
```

**Query parameters**

| Name     | Type    | Required | Notes                                              |
|----------|---------|----------|----------------------------------------------------|
| `count`  | integer | no       | Number of pushes to return. Default: 20.           |
| `author` | string  | no       | Filter to a specific author, e.g. `rcurran@mozilla.com`. Omit to see all pushes. |

**Response**

```json
{
  "results": [
    {
      "id": 123456,
      "revision": "abcdef1234567890abcdef1234567890abcdef12",
      "author": "rcurran@mozilla.com",
      "push_timestamp": 1714000000,
      "revisions": [
        {
          "revision": "abcdef1234567890abcdef1234567890abcdef12",
          "author": "rcurran@mozilla.com",
          "comments": "Bug 1234567 - Some patch description r=reviewer\n\nMore body text."
        }
      ]
    }
  ]
}
```

**Field notes**
- `push_timestamp` is a Unix epoch integer (seconds).
- `revisions` is ordered with the top commit first. Use the first non-empty, non-boilerplate commit message for `displayTitle` (see Domain Rules below).
- `comments` may be multi-line. The first non-empty line is the short message.

---

### 2. Fetch Jobs (Compact List)

Fetches all jobs for a specific push. Returns a compact list format (property names separate from rows) to reduce response size.

```
GET /api/project/try/jobs/
```

**Query parameters**

| Name                | Type    | Required | Value            |
|---------------------|---------|----------|------------------|
| `push_id`           | integer | yes      | Push `id` field. |
| `count`             | integer | yes      | Use `2000`.      |
| `return_type`       | string  | yes      | Must be `"list"` to get compact format. |
| `exclusion_profile` | string  | yes      | Use `"false"` to include all jobs. |

**Response** (compact list format)

```json
{
  "job_property_names": ["id", "result_set_id", "task_id", "platform", "platform_option",
                          "job_type_name", "job_type_symbol", "job_group_name", "job_group_symbol",
                          "state", "result", "start_timestamp", "end_timestamp", "tier"],
  "results": [
    [8675309, 123456, "XYZ...", "linux64", "opt", "mochitest-browser-chrome-e10s-7", "bc7", "Browser Chrome", "bc", "completed", "testfailed", 1714000100, 1714001200, 1]
  ]
}
```

**Parsing** — match each row value to its property name by index. Property names are not guaranteed to be in the order shown above; use the `job_property_names` array as the key.

**Key field names and types**

| Property           | Type    | Notes                                                    |
|--------------------|---------|----------------------------------------------------------|
| `id`               | integer | Job ID.                                                  |
| `result_set_id`    | integer | Push ID (legacy name). Fallback to `push_id` if absent.  |
| `task_id`          | string  | Taskcluster task ID. May be null.                        |
| `platform`         | string  | e.g. `"linux64"`, `"win64"`, `"macosx1015-64"`.         |
| `platform_option`  | string  | e.g. `"opt"`, `"debug"`, `"asan"`. May be empty.        |
| `job_type_name`    | string  | Human-readable job name, e.g. `"mochitest-browser-chrome-e10s-7"`. |
| `job_type_symbol`  | string  | Short symbol, e.g. `"bc7"`.                              |
| `job_group_name`   | string  | Group name, e.g. `"Browser Chrome"`.                    |
| `job_group_symbol` | string  | Group symbol, e.g. `"bc"`.                              |
| `state`            | string  | `"pending"`, `"running"`, `"completed"`.                |
| `result`           | string  | See `JobResult` values below. Meaningful only when `state == "completed"`. |
| `start_timestamp`  | integer | Unix epoch seconds. May be null/absent.                 |
| `end_timestamp`    | integer | Unix epoch seconds. May be null/absent.                 |
| `tier`             | integer | `1` = default. `2` = informational. Default: `1` if absent. |

---

### 3. Fetch Text Log Errors

Fetches error lines from the text log for a specific job. Used to build the failure summary.

```
GET /api/project/try/jobs/{jobId}/text_log_errors/
```

**Path parameters**

| Name    | Type    | Notes   |
|---------|---------|---------|
| `jobId` | integer | Job ID. |

**Response**

```json
[
  {
    "id": 9876,
    "line": "TEST-UNEXPECTED-FAIL | dom/some/test.js | Expected true got false",
    "line_number": 4201,
    "job": 8675309
  }
]
```

**Field notes**
- `line_number` is the line number within the log file.
- `job` is the job ID (matches the path parameter).
- Apply the `groupKey` rule (see Domain Rules) to group errors across jobs into failure categories.

The app fetches text log errors for up to 15 failed jobs in parallel, then groups the results.

---

### 4. Retrigger Job

Re-queues a job for another run.

```
POST /api/project/try/jobs/{jobId}/retrigger/
```

**Headers**
- `Content-Type: application/json`

**Body**: empty (no body required)

**Response**: HTTP 200 on success. Any other status should be treated as an error.

**Note**: This endpoint may require a valid TreeHerder session/cookie in production. The iOS app sends the request without explicit auth; behavior depends on whether the device has an active session. The Android app should behave the same way.

---

## Deep Link URLs

These URLs are opened in an in-app browser (SFSafariViewController on iOS, Chrome Custom Tabs on Android).

| Context                     | URL pattern                                                                |
|-----------------------------|----------------------------------------------------------------------------|
| Push on TreeHerder          | `https://treeherder.mozilla.org/#/jobs?repo=try&revision={push.revision}` |
| Task on Taskcluster         | `https://firefox-ci-tc.services.mozilla.com/tasks/{job.taskId}`           |
| Bug on Bugzilla             | `https://bugzilla.mozilla.org/show_bug.cgi?id={bugNumber}`                |

---

## Domain Rules

These rules are implemented identically in both clients. Treat any divergence as a bug.

### `displayTitle` — human-readable push title

Walk `push.revisions` in order. Return the first commit message that does **not** start with `"Fuzzy"`, `"try:"`, or `"a=try"`. If every revision is boilerplate (or there are none), fall back to `cleanTryMessage` on the first revision's short message.

### `cleanTryMessage(raw)` — strip try-selector boilerplate

```
1. If raw starts with "Fuzzy " → strip the prefix (6 chars)
2. If raw starts with "try: "  → strip the prefix (5 chars)
3. If raw contains "query="    → discard everything up to and including "query="
4. Split on spaces. For each token, if it starts with "'" strip the leading "'".
5. Drop empty tokens. Join remaining with " · ".
6. Trim whitespace.
```

### `shortMessage` — first line of a commit's `comments`

Split `comments` on `"\n"`, return the first non-empty line.

### `bugNumber` — extract bug ID from commit comments

Match the regex `Bug (\d+)` (case-sensitive). Return the captured digit string, or null if no match. The first matching revision wins (used in PushDetailView to surface an "Open Bug" action).

### `groupKey` — failure-line grouping key

For a `TextLogError`, compute the grouping key as follows:

```
1. Split line on " | " (space-pipe-space).
2. If parts.count >= 2 AND parts[0] contains "TEST-UNEXPECTED" or "PROCESS-CRASH":
       → return parts[1], trimmed.
3. Otherwise:
       → return the first 100 characters of line, trimmed.
```

Errors with the same `groupKey` are grouped into one `FailureGroup`. The `affectedJobCount` for a group is the count of distinct job IDs that contributed at least one error with that key. Sort `FailureGroup` by `affectedJobCount` descending.

### `JobResult` values and semantics

| Value          | Meaning                             | Visual hint        | Is failure? |
|----------------|-------------------------------------|--------------------|-------------|
| `success`      | All assertions passed               | green              | no          |
| `testfailed`   | One or more test assertions failed  | red                | **yes**     |
| `busted`       | Job infrastructure/build error      | orange             | **yes**     |
| `exception`    | Unexpected internal exception       | purple             | **yes**     |
| `retry`        | Job was automatically retried       | yellow             | no          |
| `usercancel`   | Job was cancelled by a user         | gray               | no          |
| `unknown`      | Result not yet available or unknown | gray               | no          |

`isFailure` = `result IN (testfailed, busted, exception)`.

For a job with `state != "completed"`, treat `displayResult` as `unknown` regardless of the `result` field value.

### `PlatformGroup.overallStatus` — worst-result reduction

Given a group's jobs:

```
if any completed job has isFailure → failing  (red)
else if any job is running         → running  (blue)
else if any job is pending         → pending  (gray)
else                               → passing  (green)
```

### `platformGroups` — grouping and filtering

Group jobs by `(platform, platformOption)` key. **Only include tier-1 jobs** (`tier == 1`). Sort groups alphabetically by `displayName` (= `platform` if `platformOption` is empty, else `"platform platformOption"`).

### Watch / completion notification predicate

A notification fires when ALL of the following are true:
1. `watchedPushIds.contains(push.id)`
2. `!notifiedPushIds.contains(push.id)` (de-duplication guard)
3. `jobsByPush[push.id]` is non-nil and non-empty
4. No job in the list has `state == "running"` or `state == "pending"`

On firing: remove the push from `watchedPushIds`, add to `notifiedPushIds`. Notification title is `"Try push passed"` if `failures == 0`, else `"Try push failed"`. Body is `push.displayTitle`.

---

## Persistence Keys

| Key                  | Type           | Stored value                                    |
|----------------------|----------------|-------------------------------------------------|
| `username`           | string         | Full email, e.g. `rcurran@mozilla.com`. Displayed as LDAP handle (`@mozilla.com` stripped). |
| `watchedPushIds`     | array of ints  | Push IDs currently being watched for completion. |
| `notificationsEnabled` | boolean      | User's notification opt-in preference.          |
