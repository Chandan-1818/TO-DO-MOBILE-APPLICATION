package com.chandan.master.domain.model

/** Curated accent palettes. Deliberately contains no blue, cyan, teal or purple. */
enum class AccentPalette(val storageValue: String) {
    VERMILION("vermilion"),
    BURNT_ORANGE("burnt_orange"),
    AMBER("amber"),
    CRIMSON("crimson"),
    RUST("rust");

    companion object {
        val Default: AccentPalette = VERMILION

        fun fromStorageValue(value: String?): AccentPalette =
            entries.firstOrNull { it.storageValue == value } ?: Default
    }
}
