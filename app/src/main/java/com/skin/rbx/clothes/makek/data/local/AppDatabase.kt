package com.skin.rbx.clothes.makek.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.skin.rbx.clothes.makek.data.local.dao.AppSettingDao
import com.skin.rbx.clothes.makek.data.local.dao.CharacterDao
import com.skin.rbx.clothes.makek.data.local.dao.JsonCacheDao
import com.skin.rbx.clothes.makek.data.local.dao.MediaItemDao
import com.skin.rbx.clothes.makek.data.local.dao.SavedAvatarDao
import com.skin.rbx.clothes.makek.data.local.entity.AppSettingEntity
import com.skin.rbx.clothes.makek.data.local.entity.CharacterColorEntity
import com.skin.rbx.clothes.makek.data.local.entity.CharacterEntity
import com.skin.rbx.clothes.makek.data.local.entity.CharacterLayerEntity
import com.skin.rbx.clothes.makek.data.local.entity.CharacterLayerGroupEntity
import com.skin.rbx.clothes.makek.data.local.entity.JsonCacheEntity
import com.skin.rbx.clothes.makek.data.local.entity.MediaItemEntity
import com.skin.rbx.clothes.makek.data.local.entity.SavedAvatarEntity

@Database(
    entities = [
        AppSettingEntity::class,
        JsonCacheEntity::class,
        SavedAvatarEntity::class,
        MediaItemEntity::class,
        CharacterEntity::class,
        CharacterLayerGroupEntity::class,
        CharacterLayerEntity::class,
        CharacterColorEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appSettingDao(): AppSettingDao
    abstract fun jsonCacheDao(): JsonCacheDao
    abstract fun savedAvatarDao(): SavedAvatarDao
    abstract fun mediaItemDao(): MediaItemDao
    abstract fun characterDao(): CharacterDao
}
