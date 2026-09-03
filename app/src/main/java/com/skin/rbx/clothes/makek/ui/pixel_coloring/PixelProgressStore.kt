package com.skin.rbx.clothes.makek.ui.pixel_coloring

import android.content.Context

class PixelProgressStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "pixel_coloring_progress",
        Context.MODE_PRIVATE,
    )

    fun load(level: PixelLevel): Set<Int> {
        val max = level.width * level.height
        return preferences.getString("save_${level.id}", "")
            .orEmpty()
            .split(',')
            .mapNotNull { it.toIntOrNull() }
            .filterTo(linkedSetOf()) { index ->
                index in 0 until max && level.grid[index / level.width][index % level.width] != 0
            }
    }

    fun save(levelId: String, painted: BooleanArray) {
        val value = buildString {
            painted.forEachIndexed { index, isPainted ->
                if (isPainted) {
                    if (isNotEmpty()) append(',')
                    append(index)
                }
            }
        }
        preferences.edit().putString("save_$levelId", value).apply()
    }

    fun setCompleted(levelId: String) {
        preferences.edit().putBoolean("won_$levelId", true).apply()
    }

    fun isCompleted(levelId: String): Boolean = preferences.getBoolean("won_$levelId", false)
}
