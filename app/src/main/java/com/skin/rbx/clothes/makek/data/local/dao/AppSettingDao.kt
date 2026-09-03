package com.skin.rbx.clothes.makek.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.skin.rbx.clothes.makek.data.local.entity.AppSettingEntity

@Dao
interface AppSettingDao {
    @Query("SELECT value FROM app_settings WHERE `key` = :key LIMIT 1")
    suspend fun getValue(key: String): String?

    @Upsert
    suspend fun upsert(setting: AppSettingEntity)
}
