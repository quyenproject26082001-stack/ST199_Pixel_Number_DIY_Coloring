package com.skin.rbx.clothes.makek.ui.outfit

import android.content.Context
import android.graphics.Bitmap
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.skin.rbx.clothes.makek.core.utils.key.ValueKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class OutfitPackage(
    val id: String,
    val previewPath: String,
    val entries: List<DownloadEntry>,
    val createdAt: Long = System.currentTimeMillis(),
    var isShowSelection: Boolean = false,
    var isSelected: Boolean = false,
)

object OutfitPackageRepository {
    private const val INDEX_FILE = "packages.json"
    private val gson = Gson()
    private val packageListType = object : TypeToken<List<OutfitPackage>>() {}.type
    private val changeVersion = MutableStateFlow(0L)

    fun observePackages(context: Context): Flow<ArrayList<OutfitPackage>> {
        val appContext = context.applicationContext
        return changeVersion
            .map { listPackages(appContext) }
            .distinctUntilChanged()
    }

    fun observePackage(context: Context, id: String): Flow<OutfitPackage?> {
        val appContext = context.applicationContext
        return changeVersion
            .map { getPackage(appContext, id) }
            .distinctUntilChanged()
    }

    suspend fun savePackage(
        context: Context,
        entries: List<DownloadEntry>,
        previewBitmap: Bitmap,
    ): OutfitPackage = withContext(Dispatchers.IO) {
        require(entries.isNotEmpty()) { "An outfit package must contain at least one item" }
        val root = rootDir(context).apply { mkdirs() }
        val id = System.currentTimeMillis().toString()
        val packageDir = File(root, id).apply { mkdirs() }
        val previewFile = File(packageDir, "preview.png")
        FileOutputStream(previewFile).use { output ->
            previewBitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
        val item = OutfitPackage(
            id = id,
            previewPath = previewFile.absolutePath,
            entries = entries,
        )
        val updated = listPackagesInternal(context).toMutableList().apply { add(0, item) }
        writePackages(context, updated)
        notifyChanged()
        item
    }

    suspend fun listPackages(context: Context): ArrayList<OutfitPackage> = withContext(Dispatchers.IO) {
        listPackagesInternal(context).toCollection(ArrayList())
    }

    suspend fun getPackage(context: Context, id: String): OutfitPackage? = withContext(Dispatchers.IO) {
        listPackagesInternal(context).firstOrNull { it.id == id }
    }

    suspend fun deletePackages(context: Context, ids: List<String>) = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext
        val root = rootDir(context)
        ids.forEach { File(root, it).deleteRecursively() }
        writePackages(context, listPackagesInternal(context).filterNot { it.id in ids })
        notifyChanged()
    }

    suspend fun updatePackage(context: Context, item: OutfitPackage) = withContext(Dispatchers.IO) {
        val updated = if (item.entries.isEmpty()) {
            File(rootDir(context), item.id).deleteRecursively()
            listPackagesInternal(context).filterNot { it.id == item.id }
        } else {
            listPackagesInternal(context).map { if (it.id == item.id) item else it }
        }
        writePackages(context, updated)
        notifyChanged()
    }

    private fun listPackagesInternal(context: Context): List<OutfitPackage> {
        val index = indexFile(context)
        if (!index.isFile || index.length() == 0L) return emptyList()
        val decoded = runCatching {
            gson.fromJson<List<OutfitPackage>>(index.readText(), packageListType).orEmpty()
        }.getOrDefault(emptyList())
        val valid = decoded.filter { it.entries.isNotEmpty() }
        if (valid.size != decoded.size) {
            decoded.filter { it.entries.isEmpty() }
                .forEach { File(rootDir(context), it.id).deleteRecursively() }
            writePackages(context, valid)
        }
        return valid
    }

    private fun writePackages(context: Context, items: List<OutfitPackage>) {
        val index = indexFile(context)
        index.parentFile?.mkdirs()
        index.writeText(gson.toJson(items))
    }

    private fun rootDir(context: Context) = File(context.filesDir, ValueKey.PACKAGE_ALBUM)
    private fun indexFile(context: Context) = File(rootDir(context), INDEX_FILE)
    private fun notifyChanged() = changeVersion.update { it + 1L }
}
