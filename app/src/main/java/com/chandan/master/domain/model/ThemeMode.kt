package com.chandan.master.domain.model

enum class ThemeMode(val storageValue: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        val Default: ThemeMode = SYSTEM

        /** Unknown, missing or corrupted stored values fall back to [Default]. */
        fun fromStorageValue(value: String?): ThemeMode =
            entries.firstOrNull { it.storageValue == value } ?: Default
    }
}
