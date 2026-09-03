package com.skin.rbx.clothes.makek.ui.my_creation.view_model

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skin.rbx.clothes.makek.ui.outfit.OutfitPackage
import com.skin.rbx.clothes.makek.ui.outfit.OutfitPackageRepository
import com.skin.rbx.clothes.makek.ui.outfit.DownloadEntry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MyPackageViewModel : ViewModel() {
    private val _packageList = MutableStateFlow<ArrayList<OutfitPackage>>(arrayListOf())
    val packageList = _packageList.asStateFlow()
    private var loadJob: Job? = null

    fun observePackages(context: Context) {
        val appContext = context.applicationContext
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                OutfitPackageRepository.observePackages(appContext).collectLatest { packages ->
                    val currentById = _packageList.value.associateBy { it.id }
                    _packageList.value = packages.map { item ->
                        currentById[item.id]?.let { current ->
                            item.copy(
                                isSelected = current.isSelected,
                                isShowSelection = current.isShowSelection,
                            )
                        } ?: item
                    }.toCollection(ArrayList())
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _packageList.value = arrayListOf()
            }
        }
    }

    fun showLongClick(positionSelect: Int) {
        _packageList.value = _packageList.value.mapIndexed { index, item ->
            item.copy(isSelected = index == positionSelect, isShowSelection = true)
        }.toCollection(ArrayList())
    }

    fun toggleSelect(position: Int) {
        val list = _packageList.value.toMutableList()
        list[position] = list[position].copy(
            isSelected = !list[position].isSelected,
            isShowSelection = true,
        )
        _packageList.value = list.toCollection(ArrayList())
    }

    fun selectAll(shouldSelect: Boolean) {
        _packageList.value = _packageList.value.map {
            it.copy(isSelected = shouldSelect, isShowSelection = true)
        }.toCollection(ArrayList())
    }

    fun clearSelection() {
        _packageList.value = _packageList.value.map {
            it.copy(isSelected = false, isShowSelection = false)
        }.toCollection(ArrayList())
    }

    fun getSelectedIds(): ArrayList<String> = _packageList.value
        .filter { it.isSelected }
        .map { it.id }
        .toCollection(ArrayList())

    fun getSelectedPreviewPaths(): ArrayList<String> = _packageList.value
        .filter { it.isSelected }
        .map { it.previewPath }
        .toCollection(ArrayList())

    fun getSelectedEntries(): ArrayList<DownloadEntry> = _packageList.value
        .filter { it.isSelected }
        .flatMap { it.entries }
        .toCollection(ArrayList())

    fun getAllPreviewPaths(): ArrayList<String> = _packageList.value
        .map { it.previewPath }
        .toCollection(ArrayList())

    suspend fun deleteSelected(context: Context) {
        val ids = getSelectedIds()
        loadJob?.cancel()
        OutfitPackageRepository.deletePackages(context.applicationContext, ids)
        _packageList.value = _packageList.value
            .filterNot { it.id in ids }
            .toCollection(ArrayList())
    }
}
