package com.chandan.master.domain.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidatorsTest {
    @Test fun usernameAcceptsReasonableNames() {
        listOf("Chandan", "ronin_7", "Mary Ann", "a.b-c", "सुनील", "李雷").forEach { assertNull(it, UsernameValidator.validate(it)) }
    }
    @Test fun usernameRejections() {
        assertEquals(UsernameError.EMPTY, UsernameValidator.validate(""))
        assertEquals(UsernameError.EMPTY, UsernameValidator.validate("    "))
        assertEquals(UsernameError.TOO_SHORT, UsernameValidator.validate("a"))
        assertEquals(UsernameError.TOO_LONG, UsernameValidator.validate("x".repeat(25)))
        assertEquals(UsernameError.INVALID_CHARACTERS, UsernameValidator.validate("bad@name"))
        assertEquals(UsernameError.INVALID_CHARACTERS, UsernameValidator.validate("_leading"))
        assertEquals(UsernameError.INVALID_CHARACTERS, UsernameValidator.validate("emoji😀name"))
    }
    @Test fun usernameIsTrimmedAndCollapsedBeforeValidation() {
        assertEquals("Mary Ann", UsernameValidator.normalize("  Mary    Ann "))
        assertNull(UsernameValidator.validate("  abc  "))
        assertEquals(UsernameError.TOO_SHORT, UsernameValidator.validate("  a  "))
    }
    @Test fun usernameBoundaryLengths() {
        assertNull(UsernameValidator.validate("ab")); assertNull(UsernameValidator.validate("x".repeat(24)))
    }
    @Test fun pinAcceptsFourToSixDigits() {
        listOf("1234", "12345", "123456", "0000").forEach { assertNull(PinValidator.validate(it)) }
    }
    @Test fun pinRejections() {
        assertEquals(PinError.EMPTY, PinValidator.validate(""))
        assertEquals(PinError.TOO_SHORT, PinValidator.validate("123"))
        assertEquals(PinError.TOO_LONG, PinValidator.validate("1234567"))
        assertEquals(PinError.NOT_DIGITS, PinValidator.validate("12a4"))
        assertEquals(PinError.NOT_DIGITS, PinValidator.validate("12 4"))
        assertEquals(PinError.NOT_DIGITS, PinValidator.validate("١٢٣٤")) // non-ASCII digits are not accepted
    }
    @Test fun pinConfirmation() {
        assertTrue(PinValidator.matches("1234", "1234")); assertFalse(PinValidator.matches("1234", "1235")); assertFalse(PinValidator.matches("1234", ""))
    }
    @Test fun weakPinHint() {
        listOf("1111", "1234", "4321", "123456", "000000").forEach { assertTrue(it, PinValidator.isWeak(it)) }
        listOf("1357", "2580", "9021", "13").forEach { assertFalse(it, PinValidator.isWeak(it)) }
    }
}
