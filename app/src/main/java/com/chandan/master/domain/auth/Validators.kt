package com.chandan.master.domain.auth

enum class UsernameError { EMPTY, TOO_SHORT, TOO_LONG, INVALID_CHARACTERS }

object UsernameValidator {
    const val MIN_LENGTH = 2
    const val MAX_LENGTH = 24
    private val allowed = Regex("^[\\p{L}\\p{N}][\\p{L}\\p{M}\\p{N} ._-]*$")

    fun normalize(raw: String): String = raw.trim().replace(Regex("\\s+"), " ")

    /** Returns null when [raw] is an acceptable username. */
    fun validate(raw: String): UsernameError? {
        val name = normalize(raw)
        return when {
            name.isEmpty() -> UsernameError.EMPTY
            name.length < MIN_LENGTH -> UsernameError.TOO_SHORT
            name.length > MAX_LENGTH -> UsernameError.TOO_LONG
            !allowed.matches(name) -> UsernameError.INVALID_CHARACTERS
            else -> null
        }
    }
}

enum class PinError { EMPTY, TOO_SHORT, TOO_LONG, NOT_DIGITS }

object PinValidator {
    const val MIN_LENGTH = 4
    const val MAX_LENGTH = 6
    const val RECOMMENDED_LENGTH = 4

    fun validate(pin: String): PinError? = when {
        pin.isEmpty() -> PinError.EMPTY
        !pin.all { it in '0'..'9' } -> PinError.NOT_DIGITS
        pin.length < MIN_LENGTH -> PinError.TOO_SHORT
        pin.length > MAX_LENGTH -> PinError.TOO_LONG
        else -> null
    }

    fun matches(pin: String, confirmation: String): Boolean = pin == confirmation

    /** Soft hint only (never blocks): all-identical digits or a straight ascending/descending run. */
    fun isWeak(pin: String): Boolean {
        if (pin.length < 2 || validate(pin) != null) return false
        val allSame = pin.all { it == pin[0] }
        val asc = pin.zipWithNext().all { (a, b) -> b - a == 1 }
        val desc = pin.zipWithNext().all { (a, b) -> a - b == 1 }
        return allSame || asc || desc
    }
}
