package com.mozilla.buildwatch.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.OfflineBolt
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.mozilla.buildwatch.model.JobResult
import com.mozilla.buildwatch.model.PlatformGroup

val ColorSuccess = Color(0xFF3FE0B0)   // green — mirrors iOS (0.25, 0.88, 0.69)
val ColorTestFailed = Color(0xFF4F9E5F) // red — mirrors iOS (1.0, 0.31, 0.37)
val ColorBusted = Color(0xFFFF9400)    // orange
val ColorException = Color(0xFF9C27B0) // purple
val ColorRetry = Color(0xFFFFEB3B)     // yellow
val ColorNeutral = Color(0xFF9E9E9E)   // gray
val ColorRunning = Color(0xFF2196F3)   // blue

// Fix: use actual red for failures
private val ColorFailed = Color(0xFFFF4F5E)

fun JobResult.toColor(): Color = when (this) {
    JobResult.SUCCESS -> ColorSuccess
    JobResult.TEST_FAILED -> ColorFailed
    JobResult.BUSTED -> ColorBusted
    JobResult.EXCEPTION -> ColorException
    JobResult.RETRY -> ColorRetry
    JobResult.USER_CANCEL, JobResult.UNKNOWN -> ColorNeutral
}

fun JobResult.toIcon(): ImageVector = when (this) {
    JobResult.SUCCESS -> Icons.Filled.CheckCircle
    JobResult.TEST_FAILED -> Icons.Filled.Error
    JobResult.BUSTED -> Icons.Filled.Warning
    JobResult.EXCEPTION -> Icons.Filled.OfflineBolt
    JobResult.RETRY -> Icons.Filled.Replay
    JobResult.USER_CANCEL -> Icons.Filled.RemoveCircle
    JobResult.UNKNOWN -> Icons.Filled.Help
}

fun PlatformGroup.OverallStatus.toColor(): Color = when (this) {
    PlatformGroup.OverallStatus.PASSING -> ColorSuccess
    PlatformGroup.OverallStatus.FAILING -> ColorFailed
    PlatformGroup.OverallStatus.RUNNING -> ColorRunning
    PlatformGroup.OverallStatus.PENDING -> ColorNeutral
}
