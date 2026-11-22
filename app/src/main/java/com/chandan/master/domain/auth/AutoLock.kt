package com.chandan.master.domain.auth

enum class AutoLockDelay(val storageValue: String, val millis: Long) {
    IMMEDIATELY("immediately", 0L),
    SECONDS_30("30s", 30_000L),
    MINUTE_1("1m", 60_000L),
    MINUTES_5("5m", 5 * 60_000L);

    companion object {
        val Default = MINUTE_1
        fun fromStorageValue(value: String?): AutoLockDelay = entries.firstOrNull { it.storageValue == value } ?: Default
    }
}

object AutoLockPolicy {
    /**
     * Decides, when the app returns to the foreground, whether to lock. Times are monotonic
     * (SystemClock.elapsedRealtime), so changing the wall clock cannot extend the unlocked window.
     * A decreasing value means the device rebooted, which always locks.
     */
    fun shouldLock(backgroundedAtElapsed: Long?, nowElapsed: Long, delay: AutoLockDelay): Boolean = when {
        backgroundedAtElapsed == null -> false
        nowElapsed < backgroundedAtElapsed -> true
        else -> nowElapsed - backgroundedAtElapsed >= delay.millis
    }
}
