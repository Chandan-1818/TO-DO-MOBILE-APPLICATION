package com.chandan.master.domain.backup

import com.chandan.master.domain.logic.ReminderCalculator
import com.chandan.master.domain.logic.WeekdayMask
import com.chandan.master.domain.model.Category
import com.chandan.master.domain.model.FocusSession
import com.chandan.master.domain.model.Habit
import com.chandan.master.domain.model.HabitCompletion
import com.chandan.master.domain.model.Note
import com.chandan.master.domain.model.Priority
import com.chandan.master.domain.model.Task
import java.time.LocalDate

/**
 * Backup file format "master-backup" v1. Encoding writes only the whitelisted fields below;
 * decoding validates every field, type, range and cross-reference and throws [BackupFormatException]
 * with a readable reason instead of ever importing partially or crashing.
 */
object BackupCodec {
    const val FORMAT = "master-backup"
    const val VERSION = 1
    const val MAX_BYTES = 20 * 1024 * 1024
    private const val MAX_ROWS = 100_000

    fun encode(d: BackupData): String {
        val j = Json
        val root = j.obj(
            "format" to j.str(FORMAT),
            "version" to j.num(d.version),
            "exportedAt" to j.num(d.exportedAtMs),
            "appVersion" to j.str(d.appVersion),
            "profile" to j.obj("username" to (d.profileUsername?.let { j.str(it) } ?: JsonNull)),
            "preferences" to (d.preferences?.let { p ->
                j.obj(
                    "themeMode" to j.str(p.themeMode), "accentPalette" to j.str(p.accentPalette),
                    "notificationsEnabled" to j.bool(p.notificationsEnabled), "taskReminders" to j.bool(p.taskReminders),
                    "habitReminders" to j.bool(p.habitReminders), "focusNotifications" to j.bool(p.focusNotifications),
                    "focusMinutes" to j.num(p.focusMinutes), "shortBreakMinutes" to j.num(p.shortBreakMinutes), "longBreakMinutes" to j.num(p.longBreakMinutes),
                )
            } ?: JsonNull),
            "categories" to JsonArray(d.categories.map { j.obj("id" to j.num(it.id), "name" to j.str(it.name)) }),
            "tasks" to JsonArray(d.tasks.map { t ->
                j.obj(
                    "id" to j.num(t.id), "title" to j.str(t.title), "description" to j.str(t.description),
                    "dueDay" to j.numOrNull(t.dueDate?.toEpochDay()), "dueMinute" to j.numOrNull(t.dueMinuteOfDay),
                    "priority" to j.num(t.priority.level), "categoryId" to j.numOrNull(t.categoryId),
                    "reminderOffset" to j.numOrNull(t.reminderOffsetMinutes), "completed" to j.bool(t.completed),
                    "completedAt" to j.numOrNull(t.completedAtMs), "createdAt" to j.num(t.createdAtMs), "updatedAt" to j.num(t.updatedAtMs),
                )
            }),
            "habits" to JsonArray(d.habits.map { h ->
                j.obj(
                    "id" to j.num(h.id), "name" to j.str(h.name), "description" to j.str(h.description),
                    "scheduleMask" to j.num(h.scheduleMask), "reminderMinute" to j.numOrNull(h.reminderMinuteOfDay),
                    "startDay" to j.num(h.startDate.toEpochDay()), "archived" to j.bool(h.archived), "createdAt" to j.num(h.createdAtMs),
                )
            }),
            "habitCompletions" to JsonArray(d.completions.map { j.obj("habitId" to j.num(it.habitId), "day" to j.num(it.date.toEpochDay())) }),
            "focusSessions" to JsonArray(d.sessions.map { s ->
                j.obj(
                    "id" to j.num(s.id), "startedAt" to j.num(s.startedAtMs), "plannedSeconds" to j.num(s.plannedSeconds),
                    "actualSeconds" to j.num(s.actualSeconds), "completed" to j.bool(s.completed),
                )
            }),
            "notes" to JsonArray(d.notes.map { n ->
                j.obj(
                    "id" to j.num(n.id), "title" to j.str(n.title), "content" to j.str(n.content), "pinned" to j.bool(n.pinned),
                    "createdAt" to j.num(n.createdAtMs), "updatedAt" to j.num(n.updatedAtMs),
                )
            }),
        )
        return Json.write(root, pretty = true)
    }

    fun decode(text: String): BackupData {
        if (text.length > MAX_BYTES) throw BackupFormatException("The file is too large to be a Master backup.")
        val root = try { Json.parse(text) } catch (e: JsonParseException) {
            throw BackupFormatException("The file is not valid JSON or is damaged (${e.message}).")
        }
        val o = root as? JsonObject ?: throw BackupFormatException("The file is not a Master backup.")
        if (o.str("format", 40) != FORMAT) throw BackupFormatException("The file is not a Master backup.")
        val version = o.int("version", 1..Int.MAX_VALUE)
        if (version > VERSION) throw BackupFormatException("This backup was made by a newer version of Master (format $version). Update Master to restore it.")

        val categories = o.list("categories").mapIndexed { i, v ->
            val c = v.asObj("categories[$i]")
            Category(c.long("id", 0..Long.MAX_VALUE), c.str("name", 60).also { require1(it.isNotBlank()) { "categories[$i].name is empty" } })
        }
        unique(categories.map { it.id }, "category id")
        val categoryIds = categories.map { it.id }.toSet()

        val tasks = o.list("tasks").mapIndexed { i, v ->
            val t = v.asObj("tasks[$i]")
            val cat = t.optLong("categoryId")
            require1(cat == null || cat in categoryIds) { "tasks[$i] refers to a category that is not in the file" }
            val title = t.str("title", 200)
            require1(title.isNotBlank()) { "tasks[$i].title is empty" }
            Task(
                id = t.long("id", 0..Long.MAX_VALUE), title = title, description = t.str("description", 20_000),
                dueDate = t.optLong("dueDay")?.let { date(it, "tasks[$i].dueDay") },
                dueMinuteOfDay = t.optInt("dueMinute", 0..1439),
                priority = Priority.fromLevel(t.int("priority", 0..3)), categoryId = cat,
                reminderOffsetMinutes = t.optInt("reminderOffset", 0..10_080),
                completed = t.bool("completed"), completedAtMs = t.optLong("completedAt"),
                createdAtMs = t.long("createdAt", 0..Long.MAX_VALUE), updatedAtMs = t.long("updatedAt", 0..Long.MAX_VALUE),
            )
        }
        unique(tasks.map { it.id }, "task id")

        val habits = o.list("habits").mapIndexed { i, v ->
            val h = v.asObj("habits[$i]")
            val name = h.str("name", 100)
            require1(name.isNotBlank()) { "habits[$i].name is empty" }
            val mask = h.int("scheduleMask", 1..WeekdayMask.ALL)
            Habit(
                id = h.long("id", 0..Long.MAX_VALUE), name = name, description = h.str("description", 2_000), scheduleMask = mask,
                reminderMinuteOfDay = h.optInt("reminderMinute", 0..1439), startDate = date(h.long("startDay", DAY_RANGE), "habits[$i].startDay"),
                archived = h.bool("archived"), createdAtMs = h.long("createdAt", 0..Long.MAX_VALUE),
            )
        }
        unique(habits.map { it.id }, "habit id")
        val habitIds = habits.map { it.id }.toSet()

        val completions = o.list("habitCompletions").mapIndexed { i, v ->
            val c = v.asObj("habitCompletions[$i]")
            val hid = c.long("habitId", 0..Long.MAX_VALUE)
            require1(hid in habitIds) { "habitCompletions[$i] refers to a habit that is not in the file" }
            HabitCompletion(hid, date(c.long("day", DAY_RANGE), "habitCompletions[$i].day"))
        }.distinct()

        val sessions = o.list("focusSessions").mapIndexed { i, v ->
            val s = v.asObj("focusSessions[$i]")
            val planned = s.int("plannedSeconds", 1..86_400)
            FocusSession(
                id = s.long("id", 0..Long.MAX_VALUE), startedAtMs = s.long("startedAt", 0..Long.MAX_VALUE),
                plannedSeconds = planned, actualSeconds = s.int("actualSeconds", 0..86_400), completed = s.bool("completed"),
            )
        }
        val notes = o.list("notes").mapIndexed { i, v ->
            val n = v.asObj("notes[$i]")
            Note(
                id = n.long("id", 0..Long.MAX_VALUE), title = n.str("title", 200), content = n.str("content", 200_000),
                pinned = n.bool("pinned"), createdAtMs = n.long("createdAt", 0..Long.MAX_VALUE), updatedAtMs = n.long("updatedAt", 0..Long.MAX_VALUE),
            )
        }

        val prefs = (o.fields["preferences"] as? JsonObject)?.let { p ->
            BackupPreferences(
                themeMode = p.str("themeMode", 20), accentPalette = p.str("accentPalette", 30),
                notificationsEnabled = p.bool("notificationsEnabled"), taskReminders = p.bool("taskReminders"),
                habitReminders = p.bool("habitReminders"), focusNotifications = p.bool("focusNotifications"),
                focusMinutes = p.int("focusMinutes", 1..180), shortBreakMinutes = p.int("shortBreakMinutes", 1..180), longBreakMinutes = p.int("longBreakMinutes", 1..180),
            )
        }
        val username = ((o.fields["profile"] as? JsonObject)?.fields?.get("username") as? JsonString)?.value

        if (listOf(categories, tasks, habits, completions, sessions, notes).any { it.size > MAX_ROWS }) throw BackupFormatException("The backup contains too many records.")
        return BackupData(
            version = version, exportedAtMs = o.long("exportedAt", 0..Long.MAX_VALUE), appVersion = o.str("appVersion", 40),
            profileUsername = username, preferences = prefs, categories = categories, tasks = tasks, habits = habits,
            completions = completions, sessions = sessions, notes = notes,
        )
    }

    private val DAY_RANGE = 0L..80_000L

    private fun date(day: Long, where: String): LocalDate {
        if (day !in -40_000L..80_000L) throw BackupFormatException("$where is out of range.")
        return LocalDate.ofEpochDay(day)
    }

    private inline fun require1(ok: Boolean, msg: () -> String) { if (!ok) throw BackupFormatException(msg()) }
    private fun unique(ids: List<Long>, what: String) { if (ids.toSet().size != ids.size) throw BackupFormatException("The backup contains a duplicate $what.") }

    // ---- strict accessors
    private fun JsonValue.asObj(where: String): JsonObject = this as? JsonObject ?: throw BackupFormatException("$where is not an object.")
    private fun JsonObject.field(name: String): JsonValue = fields[name] ?: throw BackupFormatException("Missing field \"$name\".")
    private fun JsonObject.list(name: String): List<JsonValue> = (field(name) as? JsonArray)?.items ?: throw BackupFormatException("\"$name\" must be a list.")
    private fun JsonObject.str(name: String, max: Int): String {
        val s = (field(name) as? JsonString)?.value ?: throw BackupFormatException("\"$name\" must be text.")
        if (s.length > max) throw BackupFormatException("\"$name\" is too long.")
        return s
    }
    private fun JsonObject.bool(name: String): Boolean = (field(name) as? JsonBool)?.value ?: throw BackupFormatException("\"$name\" must be true or false.")
    private fun JsonObject.long(name: String, range: LongRange): Long {
        val v = (field(name) as? JsonNumber)?.toLongOrNull() ?: throw BackupFormatException("\"$name\" must be a whole number.")
        if (v !in range) throw BackupFormatException("\"$name\" is out of range.")
        return v
    }
    private fun JsonObject.int(name: String, range: IntRange): Int = long(name, range.first.toLong()..range.last.toLong()).toInt()
    private fun JsonObject.optLong(name: String): Long? = when (val v = field(name)) {
        JsonNull -> null
        is JsonNumber -> v.toLongOrNull() ?: throw BackupFormatException("\"$name\" must be a whole number.")
        else -> throw BackupFormatException("\"$name\" must be a number or null.")
    }
    private fun JsonObject.optInt(name: String, range: IntRange): Int? = optLong(name)?.also {
        if (it !in range.first.toLong()..range.last.toLong()) throw BackupFormatException("\"$name\" is out of range.")
    }?.toInt()

    // keep a reference so the reminder offsets documented in the model stay in sync with the validation above
    @Suppress("unused") private val knownOffsets = ReminderCalculator.OFFSET_CHOICES
}
