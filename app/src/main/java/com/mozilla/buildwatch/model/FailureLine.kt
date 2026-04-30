package com.mozilla.buildwatch.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TextLogError(
    val id: Int,
    val line: String,
    @SerialName("line_number") val lineNumber: Int,
    val job: Int,
) {
    // Groups errors by test name when the line follows the "PREFIX | test | details" format.
    // Falls back to the first 100 chars of the raw line.
    val groupKey: String
        get() {
            val parts = line.split(" | ")
            if (parts.size >= 2) {
                val prefix = parts[0]
                if (prefix.contains("TEST-UNEXPECTED") || prefix.contains("PROCESS-CRASH")) {
                    return parts[1].trim()
                }
            }
            return line.take(100).trim()
        }
}

data class FailureGroup(
    val id: String,
    val pattern: String,
    val affectedJobCount: Int,
    val exampleLine: String,
)
