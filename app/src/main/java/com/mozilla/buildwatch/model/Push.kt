package com.mozilla.buildwatch.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class Push(
    val id: Int,
    val revision: String,
    val author: String,
    @SerialName("push_timestamp") val pushTimestamp: Int,
    val revisions: List<PushRevision>,
) {
    val shortRevision: String get() = revision.take(12)
    val date: Instant get() = Instant.ofEpochSecond(pushTimestamp.toLong())
    val authorHandle: String get() = author.substringBefore("@")

    val displayTitle: String
        get() {
            for (rev in revisions) {
                val msg = rev.shortMessage.trim()
                if (msg.isEmpty()) continue
                if (!msg.startsWith("Fuzzy") && !msg.startsWith("try:") && !msg.startsWith("a=try")) {
                    return msg
                }
            }
            return cleanTryMessage(revisions.firstOrNull()?.shortMessage ?: "")
        }

    companion object {
        fun cleanTryMessage(raw: String): String {
            var s = raw
            if (s.startsWith("Fuzzy ")) s = s.drop(6)
            if (s.startsWith("try: ")) s = s.drop(5)
            val queryIdx = s.indexOf("query=")
            if (queryIdx >= 0) s = s.substring(queryIdx + 6)
            return s.split(" ")
                .map { if (it.startsWith("'")) it.drop(1) else it }
                .filter { it.isNotEmpty() }
                .joinToString(" · ")
                .trim()
        }
    }
}

@Serializable
data class PushRevision(
    val revision: String,
    val author: String,
    val comments: String,
) {
    val shortMessage: String
        get() = comments.lineSequence().firstOrNull { it.isNotEmpty() } ?: comments

    val bugNumber: String?
        get() = Regex("""Bug (\d+)""").find(comments)?.groupValues?.get(1)
}

@Serializable
data class PushesResponse(val results: List<Push>)
