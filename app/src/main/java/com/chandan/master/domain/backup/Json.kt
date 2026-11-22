package com.chandan.master.domain.backup

/** Minimal strict JSON model/parser/writer with no dependencies (so it runs identically in unit tests and on Android). */
sealed interface JsonValue
data object JsonNull : JsonValue
data class JsonBool(val value: Boolean) : JsonValue
data class JsonNumber(val raw: String) : JsonValue {
    fun toLongOrNull(): Long? = raw.toLongOrNull()
}
data class JsonString(val value: String) : JsonValue
data class JsonArray(val items: List<JsonValue>) : JsonValue
data class JsonObject(val fields: Map<String, JsonValue>) : JsonValue

class JsonParseException(message: String) : Exception(message)

object Json {
    private const val MAX_DEPTH = 32

    fun parse(text: String): JsonValue {
        val p = Parser(text)
        p.skipWs()
        val v = p.value(0)
        p.skipWs()
        if (!p.atEnd()) throw JsonParseException("Unexpected data after the end of the document")
        return v
    }

    fun write(value: JsonValue, pretty: Boolean = false): String = StringBuilder().also { writeTo(it, value, pretty, 0) }.toString()

    // ---- builders
    fun obj(vararg fields: Pair<String, JsonValue>) = JsonObject(linkedMapOf(*fields))
    fun str(v: String) = JsonString(v)
    fun num(v: Long) = JsonNumber(v.toString())
    fun num(v: Int) = JsonNumber(v.toString())
    fun bool(v: Boolean) = JsonBool(v)
    fun numOrNull(v: Long?): JsonValue = if (v == null) JsonNull else num(v)
    fun numOrNull(v: Int?): JsonValue = if (v == null) JsonNull else num(v)

    private fun writeTo(sb: StringBuilder, v: JsonValue, pretty: Boolean, indent: Int) {
        fun nl(level: Int) { if (pretty) { sb.append('\n'); repeat(level) { sb.append("  ") } } }
        when (v) {
            JsonNull -> sb.append("null")
            is JsonBool -> sb.append(v.value)
            is JsonNumber -> sb.append(v.raw)
            is JsonString -> writeString(sb, v.value)
            is JsonArray -> {
                if (v.items.isEmpty()) { sb.append("[]"); return }
                sb.append('[')
                v.items.forEachIndexed { i, item -> if (i > 0) sb.append(','); nl(indent + 1); writeTo(sb, item, pretty, indent + 1) }
                nl(indent); sb.append(']')
            }
            is JsonObject -> {
                if (v.fields.isEmpty()) { sb.append("{}"); return }
                sb.append('{')
                var first = true
                for ((k, item) in v.fields) {
                    if (!first) sb.append(','); first = false
                    nl(indent + 1); writeString(sb, k); sb.append(if (pretty) ": " else ":"); writeTo(sb, item, pretty, indent + 1)
                }
                nl(indent); sb.append('}')
            }
        }
    }

    private fun writeString(sb: StringBuilder, s: String) {
        sb.append('"')
        for (c in s) when {
            c == '"' -> sb.append("\\\"")
            c == '\\' -> sb.append("\\\\")
            c == '\n' -> sb.append("\\n")
            c == '\r' -> sb.append("\\r")
            c == '\t' -> sb.append("\\t")
            c < ' ' -> sb.append("\\u%04x".format(c.code))
            else -> sb.append(c)
        }
        sb.append('"')
    }

    private class Parser(private val s: String) {
        private var i = 0
        fun atEnd() = i >= s.length
        fun skipWs() { while (i < s.length && (s[i] == ' ' || s[i] == '\n' || s[i] == '\r' || s[i] == '\t')) i++ }
        private fun fail(msg: String): Nothing = throw JsonParseException("$msg at position $i")

        fun value(depth: Int): JsonValue {
            if (depth > MAX_DEPTH) fail("Nesting too deep")
            if (atEnd()) fail("Unexpected end of data")
            return when (val c = s[i]) {
                '{' -> obj(depth)
                '[' -> arr(depth)
                '"' -> JsonString(string())
                't' -> literal("true", JsonBool(true))
                'f' -> literal("false", JsonBool(false))
                'n' -> literal("null", JsonNull)
                else -> if (c == '-' || c in '0'..'9') number() else fail("Unexpected character '$c'")
            }
        }

        private fun literal(word: String, v: JsonValue): JsonValue {
            if (!s.startsWith(word, i)) fail("Invalid literal")
            i += word.length; return v
        }

        private fun number(): JsonValue {
            val m = NUMBER.matchAt(s, i) ?: fail("Invalid number")
            i += m.value.length
            return JsonNumber(m.value)
        }

        private fun string(): String {
            i++ // opening quote
            val sb = StringBuilder()
            while (true) {
                if (atEnd()) fail("Unterminated string")
                val c = s[i++]
                when {
                    c == '"' -> return sb.toString()
                    c == '\\' -> {
                        if (atEnd()) fail("Unterminated escape")
                        when (val e = s[i++]) {
                            '"' -> sb.append('"'); '\\' -> sb.append('\\'); '/' -> sb.append('/')
                            'b' -> sb.append('\b'); 'f' -> sb.append('\u000C'); 'n' -> sb.append('\n')
                            'r' -> sb.append('\r'); 't' -> sb.append('\t')
                            'u' -> {
                                if (i + 4 > s.length) fail("Bad unicode escape")
                                val hex = s.substring(i, i + 4)
                                if (!hex.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) fail("Bad unicode escape")
                                sb.append(hex.toInt(16).toChar()); i += 4
                            }
                            else -> fail("Bad escape '\\$e'")
                        }
                    }
                    c < ' ' -> fail("Control character in string")
                    else -> sb.append(c)
                }
            }
        }

        private fun arr(depth: Int): JsonValue {
            i++; skipWs()
            val items = ArrayList<JsonValue>()
            if (!atEnd() && s[i] == ']') { i++; return JsonArray(items) }
            while (true) {
                skipWs(); items += value(depth + 1); skipWs()
                if (atEnd()) fail("Unterminated array")
                when (s[i++]) { ',' -> continue; ']' -> return JsonArray(items); else -> { i--; fail("Expected ',' or ']'") } }
            }
        }

        private fun obj(depth: Int): JsonValue {
            i++; skipWs()
            val fields = LinkedHashMap<String, JsonValue>()
            if (!atEnd() && s[i] == '}') { i++; return JsonObject(fields) }
            while (true) {
                skipWs()
                if (atEnd() || s[i] != '"') fail("Expected a string key")
                val key = string(); skipWs()
                if (atEnd() || s[i] != ':') fail("Expected ':'")
                i++; skipWs()
                if (key in fields) fail("Duplicate key '$key'")
                fields[key] = value(depth + 1); skipWs()
                if (atEnd()) fail("Unterminated object")
                when (s[i++]) { ',' -> continue; '}' -> return JsonObject(fields); else -> { i--; fail("Expected ',' or '}'") } }
            }
        }
    }

    private val NUMBER = Regex("-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?")
}
