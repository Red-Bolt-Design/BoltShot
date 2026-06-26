package com.redbolt.screenshot.handler

enum class BoltThemeId(val storageValue: String, val displayName: String) {
    BOLT_RED("bolt_red", "BOLT RED"),
    GLYPH("glyph", "GLYPH"),
    MONO("mono", "MONO"),
    ASH("ash", "ASH"),
    EMBER("ember", "EMBER"),
    FROST("frost", "FROST"),
    ;

    companion object {
        fun fromStorage(value: String?): BoltThemeId {
            return entries.firstOrNull { it.storageValue == value } ?: BOLT_RED
        }
    }
}
