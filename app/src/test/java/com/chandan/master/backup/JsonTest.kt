package com.chandan.master.backup

import com.chandan.master.domain.backup.Json
import com.chandan.master.domain.backup.JsonArray
import com.chandan.master.domain.backup.JsonObject
import com.chandan.master.domain.backup.JsonParseException
import com.chandan.master.domain.backup.JsonString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class JsonTest {
    private fun bad(text: String) { try { Json.parse(text); fail("expected failure for: $text") } catch (e: JsonParseException) { /* ok */ } }

    @Test fun roundTripsTextWithEscapesUnicodeAndEmoji() {
        val tricky = "line1\nline2\t\"quoted\" back\\slash \u0001 सुनील 李雷 😀 </script>"
        val text = Json.write(Json.obj("k" to Json.str(tricky), "n" to Json.num(-42L), "b" to Json.bool(true), "z" to com.chandan.master.domain.backup.JsonNull))
        val back = Json.parse(text) as JsonObject
        assertEquals(tricky, (back.fields["k"] as JsonString).value)
        assertEquals("-42", (back.fields["n"] as com.chandan.master.domain.backup.JsonNumber).raw)
    }

    @Test fun prettyOutputParsesToTheSameValue() {
        val v = Json.obj("a" to JsonArray(listOf(Json.num(1), Json.num(2))), "o" to Json.obj("x" to Json.str("y")))
        assertEquals(v, Json.parse(Json.write(v, pretty = true)))
        assertEquals(v, Json.parse(Json.write(v)))
    }

    @Test fun parsesStandardEscapes() {
        assertEquals("A\u00e9/", (Json.parse("\"\\u0041\\u00e9\\/\"") as JsonString).value)
    }

    @Test fun rejectsMalformedDocuments() {
        listOf("", "{", "[1,2", "{\"a\":}", "{\"a\" 1}", "{'a':1}", "[1,]", "{\"a\":1,}", "tru", "01", "1.", "\"abc", "\"a\nb\"", "{\"a\":1}{", "nul", "\"\\x\"", "\"\\u12\"", "{\"a\":1,\"a\":2}", "[1] 2").forEach { bad(it) }
    }

    @Test fun rejectsExcessiveNesting() { bad("[".repeat(100) + "]".repeat(100)) }

    @Test fun emptyContainersAndWhitespace() {
        assertTrue(Json.parse(" { } ") is JsonObject); assertTrue(Json.parse("\n[\n]\n") is JsonArray)
    }
}
