package com.chandan.master.backup

import com.chandan.master.domain.backup.BackupCodec
import com.chandan.master.domain.backup.BackupData
import com.chandan.master.domain.backup.BackupFormatException
import com.chandan.master.domain.backup.BackupPreferences
import com.chandan.master.domain.backup.Json
import com.chandan.master.domain.backup.JsonArray
import com.chandan.master.domain.backup.JsonObject
import com.chandan.master.domain.model.Category
import com.chandan.master.domain.model.FocusSession
import com.chandan.master.domain.model.Habit
import com.chandan.master.domain.model.HabitCompletion
import com.chandan.master.domain.model.Note
import com.chandan.master.domain.model.Priority
import com.chandan.master.domain.model.Task
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BackupCodecTest {
    private val d = LocalDate.of(2026, 3, 10)
    private fun sample() = BackupData(
        exportedAtMs = 1_700_000_000_000, appVersion = "0.3.0", profileUsername = "सुनील",
        preferences = BackupPreferences("dark", "rust", true, true, false, true, 50, 10, 30),
        categories = listOf(Category(1, "Work"), Category(2, "Health 😀")),
        tasks = listOf(
            Task(10, "Write \"report\"\nline2", "desc", d, 14 * 60, Priority.URGENT, 1, 60, false, null, 5, 6),
            Task(11, "Done", "", null, null, Priority.LOW, null, null, true, 99, 7, 8),
        ),
        habits = listOf(Habit(20, "Read", "20 pages", 31, 21 * 60, d.minusDays(10), false, 3)),
        completions = listOf(HabitCompletion(20, d), HabitCompletion(20, d.minusDays(1))),
        sessions = listOf(FocusSession(30, 1_700_000_100_000, 1500, 1500, true), FocusSession(31, 1_700_000_200_000, 1500, 400, false)),
        notes = listOf(Note(40, "Idea", "multi\nline \\ content", true, 11, 12)),
    )

    private fun failsWith(text: String, contains: String? = null) {
        try { BackupCodec.decode(text); fail("expected BackupFormatException") }
        catch (e: BackupFormatException) { if (contains != null) assertTrue("message was: ${e.message}", e.message!!.contains(contains, ignoreCase = true)) }
    }

    @Test fun roundTripPreservesEverything() {
        val data = sample()
        assertEquals(data, BackupCodec.decode(BackupCodec.encode(data)))
    }

    @Test fun roundTripOfAnEmptyBackup() {
        val empty = BackupData(exportedAtMs = 0, appVersion = "x", profileUsername = null, preferences = null, categories = emptyList(), tasks = emptyList(), habits = emptyList(), completions = emptyList(), sessions = emptyList(), notes = emptyList())
        assertEquals(empty, BackupCodec.decode(BackupCodec.encode(empty)))
    }

    @Test fun exportContainsOnlyWhitelistedKeysAndNoSecrets() {
        val text = BackupCodec.encode(sample())
        val root = Json.parse(text) as JsonObject
        assertEquals(setOf("format", "version", "exportedAt", "appVersion", "profile", "preferences", "categories", "tasks", "habits", "habitCompletions", "focusSessions", "notes"), root.fields.keys)
        val keys = HashSet<String>()
        fun walk(v: com.chandan.master.domain.backup.JsonValue) {
            when (v) {
                is JsonObject -> v.fields.forEach { (k, x) -> keys += k; walk(x) }
                is JsonArray -> v.items.forEach { walk(it) }
                else -> Unit
            }
        }
        walk(root)
        val forbiddenWords = listOf("hash", "salt", "password", "secret", "keystore", "biometric", "token", "credential")
        keys.forEach { k -> forbiddenWords.forEach { w -> assertFalse("key '$k' looks like a secret", k.lowercase().contains(w)) } }
        assertFalse("a key named exactly 'pin'", keys.any { it.equals("pin", ignoreCase = true) || it.equals("pinCode", ignoreCase = true) })
        assertTrue("pinned (the note flag) is allowed", "pinned" in keys)
        assertEquals(setOf("username"), ((root.fields["profile"]) as JsonObject).fields.keys)
    }

    @Test fun rejectsGarbageTruncatedAndWrongFiles() {
        failsWith("", "json"); failsWith("not json at all"); failsWith("[1,2,3]", "not a master backup")
        val good = BackupCodec.encode(sample())
        failsWith(good.substring(0, good.length / 2), "damaged")
        failsWith(good.replace("\"master-backup\"", "\"something-else\""), "not a master backup")
        failsWith("{\"hello\":1}")
    }

    @Test fun rejectsBackupsFromANewerFormat() {
        failsWith(BackupCodec.encode(sample().copy(version = 99)), "newer")
    }

    @Test fun rejectsMissingFieldsWrongTypesAndOutOfRangeValues() {
        val good = BackupCodec.encode(sample())
        failsWith(good.replace("\"tasks\"", "\"taskz\""), "tasks")
        failsWith(good.replace("\"priority\": 3", "\"priority\": 9"), "priority")
        failsWith(good.replace("\"priority\": 3", "\"priority\": \"high\""), "priority")
        failsWith(good.replace("\"scheduleMask\": 31", "\"scheduleMask\": 0"), "scheduleMask")
        failsWith(good.replace("\"dueMinute\": 840", "\"dueMinute\": 5000"), "dueMinute")
        failsWith(good.replace("\"completed\": true", "\"completed\": \"yes\""), "completed")
        failsWith(good.replace("\"plannedSeconds\": 1500", "\"plannedSeconds\": -5"), "plannedSeconds")
    }

    @Test fun rejectsBrokenReferencesAndDuplicateIds() {
        val good = BackupCodec.encode(sample())
        failsWith(good.replace("\"categoryId\": 1", "\"categoryId\": 77"), "category")
        failsWith(good.replace("\"habitId\": 20", "\"habitId\": 21"), "habit")
        failsWith(good.replace("\"id\": 11,", "\"id\": 10,"), "duplicate")
    }

    @Test fun rejectsEmptyTitlesAndOversizedFields() {
        failsWith(BackupCodec.encode(sample().copy(tasks = listOf(Task(1, "   ")))), "title")
        failsWith(BackupCodec.encode(sample().copy(tasks = listOf(Task(1, "x".repeat(201))))), "too long")
        failsWith(BackupCodec.encode(sample().copy(categories = listOf(Category(1, "")))), "name")
    }

    @Test fun duplicateCompletionsInTheFileAreCollapsed() {
        val data = sample().copy(completions = listOf(HabitCompletion(20, d), HabitCompletion(20, d)))
        assertEquals(1, BackupCodec.decode(BackupCodec.encode(data)).completions.size)
    }

    @Test fun absurdlyLargeInputIsRefusedBeforeParsing() {
        failsWith(" ".repeat(BackupCodec.MAX_BYTES + 1), "too large")
    }

    @Test fun extraUnknownTopLevelFieldsAreIgnoredForForwardCompatibility() {
        val root = Json.parse(BackupCodec.encode(sample())) as JsonObject
        val extended = JsonObject(root.fields + ("futureThing" to JsonArray(emptyList())))
        assertEquals(sample(), BackupCodec.decode(Json.write(extended)))
    }
}
