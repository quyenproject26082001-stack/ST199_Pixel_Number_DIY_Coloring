package com.skin.rbx.clothes.makek.core.utils.share.whatsapp

import android.content.Context
import com.skin.rbx.clothes.makek.data.local.PersistenceRepository

object LocalStorageUtils {

    fun readData(context: Context, key: String): Any? {
        return PersistenceRepository.getStringSetting(context, key, "")
            .takeIf { it.isNotEmpty() }
    }
}
