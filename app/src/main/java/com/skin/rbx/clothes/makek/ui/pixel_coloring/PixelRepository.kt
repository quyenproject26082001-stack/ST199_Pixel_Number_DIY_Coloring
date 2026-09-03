package com.skin.rbx.clothes.makek.ui.pixel_coloring

import android.content.Context
import android.util.LruCache
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class PixelRepository(context: Context) {
    private val appContext = context.applicationContext
    private val gson = Gson()
    private val cache = LruCache<String, PixelLevel>(24)

    fun loadCatalog(): List<PixelLevelEntry> =
        appContext.assets.open("$ASSET_ROOT/data/levels.json").bufferedReader().use { reader ->
            val type = object : TypeToken<List<PixelLevelEntry>>() {}.type
            gson.fromJson<List<PixelLevelEntry>>(reader, type).orEmpty()
        }

    fun loadLevel(id: String): PixelLevel {
        synchronized(cache) { cache.get(id)?.let { return it } }
        val level = appContext.assets.open("$ASSET_ROOT/data/$id.json").bufferedReader().use {
            gson.fromJson(it, PixelLevel::class.java)
        }
        synchronized(cache) { cache.put(id, level) }
        return level
    }

    fun levelFromJson(json: String): PixelLevel = gson.fromJson(json, PixelLevel::class.java)
    fun levelToJson(level: PixelLevel): String = gson.toJson(level)

    companion object {
        const val ASSET_ROOT = "pixel_coloring"
    }
}
