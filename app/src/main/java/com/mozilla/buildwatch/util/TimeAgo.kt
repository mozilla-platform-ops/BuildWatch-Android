package com.mozilla.buildwatch.util

import java.time.Instant
import java.time.temporal.ChronoUnit

// Mirrors iOS Extensions.swift shortTimeAgo(): "Xs", "Xm", "Xh", "Xd"
fun Instant.shortTimeAgo(): String {
    val now = Instant.now()
    val seconds = ChronoUnit.SECONDS.between(this, now).coerceAtLeast(0)
    if (seconds < 60) return "${seconds}s"
    val minutes = seconds / 60
    if (minutes < 60) return "${minutes}m"
    val hours = minutes / 60
    if (hours < 24) return "${hours}h"
    return "${hours / 24}d"
}
