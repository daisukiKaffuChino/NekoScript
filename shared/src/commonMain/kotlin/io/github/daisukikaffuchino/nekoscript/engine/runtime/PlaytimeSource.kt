package io.github.daisukikaffuchino.nekoscript.engine.runtime

import kotlin.time.TimeSource

/** Session-duration clock used to accumulate save-slot playtime. */
fun interface PlaytimeSource {
    fun mark(): PlaytimeMark

    companion object {
        fun monotonic(): PlaytimeSource = PlaytimeSource {
            MonotonicPlaytimeMark(TimeSource.Monotonic.markNow())
        }
    }
}

/** One immutable playtime mark. */
fun interface PlaytimeMark {
    fun elapsedMillis(): Long
}

private class MonotonicPlaytimeMark(
    private val mark: kotlin.time.TimeMark,
) : PlaytimeMark {
    override fun elapsedMillis(): Long = mark.elapsedNow().inWholeMilliseconds
}
