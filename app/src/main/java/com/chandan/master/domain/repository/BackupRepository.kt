package com.chandan.master.domain.repository

import com.chandan.master.domain.backup.BackupData
import com.chandan.master.domain.backup.RestorePlan
import com.chandan.master.domain.backup.RestoreSummary

data class BackupPreview(val data: BackupData, val plan: RestorePlan)

interface BackupRepository {
    /** Builds the backup JSON from the current database and settings. Never includes credentials or keys. */
    suspend fun createBackup(): String
    /** Parses and validates [text]; throws [com.chandan.master.domain.backup.BackupFormatException] if it is not a valid backup. */
    suspend fun inspect(text: String): BackupPreview
    /** Merges the backup into the current data inside one transaction (all or nothing). Existing records are never overwritten or deleted. */
    suspend fun restore(data: BackupData): RestoreSummary
}
