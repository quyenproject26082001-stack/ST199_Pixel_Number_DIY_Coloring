package com.skin.rbx.clothes.makek.ui.pixel_coloring

import android.content.Context

class PixelProgressStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "pixel_coloring_progress",
        Context.MODE_PRIVATE,
    )

    fun load(level: PixelLevel): Set<Int> {
        val max = level.width * level.height
        return preferences.getString("$SAVE_PREFIX${level.id}", "")
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
        preferences.edit().putString("$SAVE_PREFIX$levelId", value).apply()
    }

    fun setCompleted(levelId: String) {
        preferences.edit().putBoolean("$WON_PREFIX$levelId", true).apply()
    }

    fun isCompleted(levelId: String): Boolean = preferences.getBoolean("$WON_PREFIX$levelId", false)

    fun reset(levelId: String) {
        preferences.edit()
            .remove("$SAVE_PREFIX$levelId")
            .remove("$WON_PREFIX$levelId")
            .apply()
    }

    fun getProgressIds(): PixelProgressIds {
        val startedIds = linkedSetOf<String>()
        val completedIds = linkedSetOf<String>()
        preferences.all.forEach { (key, value) ->
            when {
                key.startsWith(SAVE_PREFIX) && value is String && value.isNotBlank() ->
                    startedIds += key.removePrefix(SAVE_PREFIX)

                key.startsWith(WON_PREFIX) && value == true ->
                    completedIds += key.removePrefix(WON_PREFIX)
            }
        }
        return PixelProgressIds(startedIds, completedIds)
    }

    companion object {
        private const val SAVE_PREFIX = "save_"
        private const val WON_PREFIX = "won_"
    }
}

data class PixelProgressIds(
    val startedIds: Set<String>,
    val completedIds: Set<String>,
)
