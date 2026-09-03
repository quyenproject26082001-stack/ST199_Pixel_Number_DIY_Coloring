package com.skin.rbx.clothes.makek.ui.my_creation.view_model

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skin.rbx.clothes.makek.core.base.BaseActivity
import com.skin.rbx.clothes.makek.core.helper.InternetHelper
import com.skin.rbx.clothes.makek.core.helper.MediaHelper
import com.skin.rbx.clothes.makek.core.utils.key.ValueKey
import com.skin.rbx.clothes.makek.core.utils.state.HandleState
import com.skin.rbx.clothes.makek.data.local.PersistenceRepository
import com.skin.rbx.clothes.makek.data.model.MyAlbumModel
import com.skin.rbx.clothes.makek.data.model.custom.CustomizeModel
import com.skin.rbx.clothes.makek.data.model.custom.SuggestionModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MyAvatarViewModel : ViewModel() {
    private val _myAvatarList = MutableStateFlow<ArrayList<MyAlbumModel>>(arrayListOf())
    val myAvatarList = _myAvatarList.asStateFlow()
    private val _isLastItem = MutableStateFlow<Boolean>(false)
    val isLastItem: StateFlow<Boolean> = _isLastItem


    var isApi: Boolean = false
    var positionCharacter = -1
    var editModel = SuggestionModel()
    private var loadJob: Job? = null

    fun loadMyAvatar(context: Context) {
        val appContext = context.applicationContext
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                val paths = PersistenceRepository.getSavedAvatarPaths(appContext)
                _myAvatarList.value =
                    paths.map { MyAlbumModel(it) }.toCollection(ArrayList())
                checkLastItem()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                android.util.Log.e("MyAvatarViewModel", "Failed to load saved avatars", error)
                _myAvatarList.value = arrayListOf()
                checkLastItem()
            }
        }
    }

    private fun checkLastItem() {
        _isLastItem.value = _myAvatarList.value.any { !it.isSelected }
    }

    suspend fun deleteItem(context: Context, pathList: ArrayList<String>) {
        loadJob?.cancel()
        PersistenceRepository.deleteSavedAvatarRows(context.applicationContext, pathList)
        _myAvatarList.value = _myAvatarList.value
            .filterNot { it.path in pathList }
            .toCollection(ArrayList())
        checkLastItem()
    }

    suspend fun editItem(
        context: Context,
        pathInternal: String,
        allData: ArrayList<CustomizeModel>
    ): Boolean {
        val savedEditModel = PersistenceRepository.getSavedAvatar(
            context.applicationContext,
            pathInternal
        ) ?: return false
        val savedCharacterPosition =
            allData.indexOfFirst { it.avatar == savedEditModel.avatarPath }
        if (savedCharacterPosition < 0) {
            positionCharacter = -1
            isApi = false
            return false
        }

        editModel = savedEditModel
        positionCharacter = savedCharacterPosition
        isApi = allData[positionCharacter].isFromAPI
        MediaHelper.writeModelToFile(context, ValueKey.SUGGESTION_FILE_INTERNAL, editModel)
        return true
    }

    fun checkDataInternet(context: BaseActivity<*>, action: (() -> Unit)) {
        if (!isApi) {
            action.invoke()
            return
        }
        InternetHelper.checkInternet(context) { result ->
            if (result == HandleState.SUCCESS) {
                action.invoke()
            } else {
                // Show No Internet dialog
                val dialog = com.skin.rbx.clothes.makek.dialog.YesNoDialog(
                    context,
                    com.skin.rbx.clothes.makek.R.string.no_internet,
                    com.skin.rbx.clothes.makek.R.string.please_check_your_internet,
                    isError = true
                )
                dialog.show()
                dialog.onYesClick = {
                    dialog.dismiss()
                }
            }
        }
    }

    fun showLongClick(positionSelect: Int) {
        _myAvatarList.value = _myAvatarList.value.mapIndexed { position, item ->
            item.copy(isSelected = position == positionSelect, isShowSelection = true)
        }.toCollection(ArrayList())
        checkLastItem()
    }

    fun selectAll(shouldSelect: Boolean) {
        _myAvatarList.value = _myAvatarList.value.map {
            it.copy(isSelected = shouldSelect, isShowSelection = true)
        }.toCollection(ArrayList())
        checkLastItem()
    }

    fun toggleSelect(position: Int) {
        val list = _myAvatarList.value.toMutableList()
        list[position] = list[position].copy(isSelected = !list[position].isSelected, isShowSelection = true)
        _myAvatarList.value = list.toCollection(ArrayList())
        checkLastItem()
    }

    fun getPathSelected() : ArrayList<String>{
        return _myAvatarList.value
            .filter { it.isSelected }
            .map { it.path }
            .toCollection(ArrayList())
    }

    fun clearSelection() {
        _myAvatarList.value = _myAvatarList.value.map {
            it.copy(isSelected = false, isShowSelection = false)
        }.toCollection(ArrayList())
        checkLastItem()
    }
}
