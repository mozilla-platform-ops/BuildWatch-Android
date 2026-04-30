package com.mozilla.buildwatch.model

import kotlinx.serialization.Serializable

// Note: color/icon mappings live in ui/theme/JobColors.kt to keep the model layer UI-free.

enum class JobResult(val serialName: String) {
    SUCCESS("success"),
    TEST_FAILED("testfailed"),
    BUSTED("busted"),
    EXCEPTION("exception"),
    RETRY("retry"),
    USER_CANCEL("usercancel"),
    UNKNOWN("unknown");

    val isFailure: Boolean get() = this == TEST_FAILED || this == BUSTED || this == EXCEPTION

    val displayName: String
        get() = when (this) {
            SUCCESS -> "Success"
            TEST_FAILED -> "Test Failed"
            BUSTED -> "Busted"
            EXCEPTION -> "Exception"
            RETRY -> "Retry"
            USER_CANCEL -> "Cancelled"
            UNKNOWN -> "Unknown"
        }

    companion object {
        fun fromString(value: String?): JobResult =
            entries.firstOrNull { it.serialName == value } ?: UNKNOWN
    }
}

enum class JobState(val serialName: String) {
    PENDING("pending"),
    RUNNING("running"),
    COMPLETED("completed");

    companion object {
        fun fromString(value: String?): JobState =
            entries.firstOrNull { it.serialName == value } ?: PENDING
    }
}

data class Job(
    val id: Int,
    val pushId: Int,
    val taskId: String?,
    val platform: String,
    val platformOption: String,
    val jobTypeName: String,
    val jobTypeSymbol: String,
    val jobGroupName: String,
    val jobGroupSymbol: String,
    val state: JobState,
    val result: JobResult,
    val startTimestamp: Int?,
    val endTimestamp: Int?,
    val tier: Int,
) {
    val isRunning: Boolean get() = state == JobState.RUNNING
    val isPending: Boolean get() = state == JobState.PENDING

    val displayResult: JobResult
        get() = if (state == JobState.COMPLETED) result else JobResult.UNKNOWN

    val platformDisplay: String
        get() = if (platformOption.isEmpty()) platform else "$platform $platformOption"

    val duration: Long?
        get() {
            val s = startTimestamp ?: return null
            val e = endTimestamp ?: return null
            return if (e > s) (e - s).toLong() else null
        }

    val durationString: String?
        get() {
            val secs = duration ?: return null
            val minutes = secs / 60
            val seconds = secs % 60
            return if (minutes == 0L) "${seconds}s" else "${minutes}m ${seconds}s"
        }
}

data class PlatformGroup(
    val platform: String,
    val option: String,
    val jobs: List<Job>,
) {
    val id: String get() = "$platform-$option"
    val displayName: String get() = if (option.isEmpty()) platform else "$platform $option"

    val failureCount: Int get() = jobs.count { it.result.isFailure && it.state == JobState.COMPLETED }
    val pendingCount: Int get() = jobs.count { it.isPending }
    val runningCount: Int get() = jobs.count { it.isRunning }
    val successCount: Int get() = jobs.count { it.result == JobResult.SUCCESS && it.state == JobState.COMPLETED }

    enum class OverallStatus { PASSING, FAILING, RUNNING, PENDING }

    val overallStatus: OverallStatus
        get() = when {
            failureCount > 0 -> OverallStatus.FAILING
            runningCount > 0 -> OverallStatus.RUNNING
            pendingCount > 0 -> OverallStatus.PENDING
            else -> OverallStatus.PASSING
        }
}
