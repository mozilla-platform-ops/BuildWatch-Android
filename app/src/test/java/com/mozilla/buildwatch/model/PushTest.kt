package com.mozilla.buildwatch.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PushTest {

    private fun revision(comments: String) = PushRevision(
        revision = "abc123",
        author = "test@mozilla.com",
        comments = comments,
    )

    // cleanTryMessage tests

    @Test fun `cleanTryMessage strips Fuzzy prefix`() {
        assertEquals("linux64", Push.cleanTryMessage("Fuzzy linux64"))
    }

    @Test fun `cleanTryMessage strips try prefix`() {
        assertEquals("win64", Push.cleanTryMessage("try: win64"))
    }

    @Test fun `cleanTryMessage extracts after query= and strips leading quotes`() {
        // after query= → "'linux64 opt'" → split → ["'linux64", "opt'"]
        // strip leading ' only → ["linux64", "opt'"] → join → "linux64 · opt'"
        assertEquals("linux64 · opt'", Push.cleanTryMessage("try: -b do -p linux64 query='linux64 opt'"))
    }

    @Test fun `cleanTryMessage splits plain message with dot separator`() {
        // no prefix stripped; all tokens joined with " · "
        assertEquals("Bug · 1234567 · - · Fix · crash", Push.cleanTryMessage("Bug 1234567 - Fix crash"))
    }

    // displayTitle tests

    @Test fun `displayTitle prefers real patch message over try boilerplate`() {
        val push = Push(
            id = 1, revision = "abc", author = "a@mozilla.com", pushTimestamp = 0,
            revisions = listOf(
                revision("try: linux64"),
                revision("Bug 1234 - Fix thing"),
            ),
        )
        assertEquals("Bug 1234 - Fix thing", push.displayTitle)
    }

    @Test fun `displayTitle falls back to cleaned try message when all are boilerplate`() {
        val push = Push(
            id = 1, revision = "abc", author = "a@mozilla.com", pushTimestamp = 0,
            revisions = listOf(revision("try: linux64 win64")),
        )
        assertEquals("linux64 · win64", push.displayTitle)
    }

    @Test fun `displayTitle skips empty revisions`() {
        val push = Push(
            id = 1, revision = "abc", author = "a@mozilla.com", pushTimestamp = 0,
            revisions = listOf(revision(""), revision("Real commit message")),
        )
        assertEquals("Real commit message", push.displayTitle)
    }

    // authorHandle test

    @Test fun `authorHandle strips domain`() {
        val push = Push(
            id = 1, revision = "abc", author = "rcurran@mozilla.com", pushTimestamp = 0,
            revisions = emptyList(),
        )
        assertEquals("rcurran", push.authorHandle)
    }

    // bugNumber test

    @Test fun `bugNumber extracts from comments`() {
        val rev = revision("Bug 1234567 - Fix the thing\n\nMore details.")
        assertEquals("1234567", rev.bugNumber)
    }

    @Test fun `bugNumber returns null when absent`() {
        val rev = revision("No bug here")
        assertEquals(null, rev.bugNumber)
    }
}
