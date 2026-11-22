package com.chandan.master.domain.auth

sealed interface PinVerification {
    data object Success : PinVerification
    /** Wrong PIN. [attemptsLeftBeforeDelay] is 0 once a delay applies ([lockedForMillis] > 0). */
    data class Wrong(val attemptsLeftBeforeDelay: Int, val lockedForMillis: Long) : PinVerification
    data class LockedOut(val retryAfterMillis: Long) : PinVerification
    /** Stored credentials cannot be read (e.g. the Keystore key was lost). The only way forward is a reset. */
    data object CredentialsUnavailable : PinVerification
    data object NoAccount : PinVerification
}
