package com.chandan.master.domain.auth

import kotlin.math.max

data class AttemptState(val failedAttempts: Int = 0, val lockedUntilEpochMillis: Long = 0L)

/** Escalating, never-permanent delays: ordinary typos cost nothing, guessing becomes impractical. */
object LockoutPolicy {
    const val FREE_ATTEMPTS = 5
    const val MAX_DELAY_MILLIS = 30 * 60_000L

    fun delayMillisAfter(failedAttempts: Int): Long = when {
        failedAttempts < FREE_ATTEMPTS -> 0L
        failedAttempts == 5 -> 30_000L
        failedAttempts == 6 -> 60_000L
        failedAttempts == 7 -> 5 * 60_000L
        failedAttempts == 8 -> 15 * 60_000L
        else -> MAX_DELAY_MILLIS
    }

    fun freeAttemptsLeft(failedAttempts: Int): Int = max(0, FREE_ATTEMPTS - failedAttempts)
}

object AttemptTracker {
    /**
     * Milliseconds still locked. Clamped to [LockoutPolicy.MAX_DELAY_MILLIS] so that a stored
     * far-future timestamp (e.g. after the clock was moved) can never lock the user out for longer than the maximum delay.
     */
    fun remainingLockMillis(state: AttemptState, nowEpochMillis: Long): Long =
        (state.lockedUntilEpochMillis - nowEpochMillis).coerceIn(0L, LockoutPolicy.MAX_DELAY_MILLIS)

    fun onFailure(state: AttemptState, nowEpochMillis: Long): AttemptState {
        val failed = state.failedAttempts + 1
        val delay = LockoutPolicy.delayMillisAfter(failed)
        return AttemptState(failed, if (delay == 0L) 0L else nowEpochMillis + delay)
    }

    fun onSuccess(): AttemptState = AttemptState()
}
