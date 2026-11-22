package com.chandan.master.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Register every schema migration here, in order. Never use destructive migration. */
object MasterMigrations {
    /** Statements for version 2, written to match exactly what Room generates for the entities (names, types, nullability, FKs, indices). */
    val V2_STATEMENTS: List<String> = listOf(
        "CREATE TABLE IF NOT EXISTS `categories` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL)",
        "CREATE TABLE IF NOT EXISTS `tasks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `due_day` INTEGER, `due_minute` INTEGER, `priority` INTEGER NOT NULL, `category_id` INTEGER, `reminder_offset` INTEGER, `completed` INTEGER NOT NULL, `completed_at` INTEGER, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, FOREIGN KEY(`category_id`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )",
        "CREATE INDEX IF NOT EXISTS `index_tasks_category_id` ON `tasks` (`category_id`)",
        "CREATE INDEX IF NOT EXISTS `index_tasks_due_day` ON `tasks` (`due_day`)",
        "CREATE TABLE IF NOT EXISTS `habits` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `description` TEXT NOT NULL, `schedule_mask` INTEGER NOT NULL, `reminder_minute` INTEGER, `start_day` INTEGER NOT NULL, `archived` INTEGER NOT NULL, `created_at` INTEGER NOT NULL)",
        "CREATE TABLE IF NOT EXISTS `habit_completions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `habit_id` INTEGER NOT NULL, `day` INTEGER NOT NULL, FOREIGN KEY(`habit_id`) REFERENCES `habits`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        "CREATE UNIQUE INDEX IF NOT EXISTS `index_habit_completions_habit_id_day` ON `habit_completions` (`habit_id`, `day`)",
        "CREATE TABLE IF NOT EXISTS `focus_sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `started_at` INTEGER NOT NULL, `planned_seconds` INTEGER NOT NULL, `actual_seconds` INTEGER NOT NULL, `completed` INTEGER NOT NULL)",
        "CREATE UNIQUE INDEX IF NOT EXISTS `index_focus_sessions_started_at` ON `focus_sessions` (`started_at`)",
        "CREATE TABLE IF NOT EXISTS `notes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `content` TEXT NOT NULL, `pinned` INTEGER NOT NULL, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL)",
    )

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            V2_STATEMENTS.forEach { db.execSQL(it) }
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2)
}
