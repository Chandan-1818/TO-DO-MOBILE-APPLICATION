package com.chandan.master.domain.auth

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory lock state. It starts LOCKED on every process start, so a killed/restarted app always asks for the PIN.
 * Times are monotonic (SystemClock.elapsedRealtime) and supplied by the caller, which keeps this class testable.
 */
@Singleton
class SessionLockManager @Inject constructor() {
    private val _isLocked = MutableStateFlow(true)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()
    private var backgroundedAtElapsed: Long? = null

    fun unlock() { _isLocked.value = false; backgroundedAtElapsed = null }
    fun lock() { _isLocked.value = true }

    fun onAppBackgrounded(nowElapsed: Long) { if (backgroundedAtElapsed == null) backgroundedAtElapsed = nowElapsed }

    fun onAppForegrounded(nowElapsed: Long, delay: AutoLockDelay) {
        if (AutoLockPolicy.shouldLock(backgroundedAtElapsed, nowElapsed, delay)) lock()
        backgroundedAtElapsed = null
    }
}
