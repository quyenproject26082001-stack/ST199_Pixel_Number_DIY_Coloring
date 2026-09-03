package com.skin.rbx.clothes.makek.ui.my_creation

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityOptionsCompat
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.PopupWindow
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.room.util.findColumnIndexBySuffix
import com.lvt.ads.util.Admob
import com.skin.rbx.clothes.makek.R
import com.skin.rbx.clothes.makek.core.base.BaseActivity
import com.skin.rbx.clothes.makek.core.extensions.checkPermissions
import com.skin.rbx.clothes.makek.core.extensions.goToSettings
import com.skin.rbx.clothes.makek.core.extensions.gone
import com.skin.rbx.clothes.makek.core.extensions.hideNavigation
import com.skin.rbx.clothes.makek.core.extensions.invisible
import com.skin.rbx.clothes.makek.core.extensions.loadNativeCollabAds
import com.skin.rbx.clothes.makek.core.extensions.requestPermission
import com.skin.rbx.clothes.makek.core.extensions.select
import com.skin.rbx.clothes.makek.core.extensions.setImageActionBar
import com.skin.rbx.clothes.makek.core.extensions.setTextActionBar
import com.skin.rbx.clothes.makek.core.extensions.tap

import com.skin.rbx.clothes.makek.core.extensions.startIntentWithClearTop
import com.skin.rbx.clothes.makek.core.extensions.visible
import com.skin.rbx.clothes.makek.core.helper.LanguageHelper
import com.skin.rbx.clothes.makek.core.helper.UnitHelper
import com.skin.rbx.clothes.makek.core.utils.key.IntentKey
import com.skin.rbx.clothes.makek.core.utils.key.RequestKey
import com.skin.rbx.clothes.makek.core.utils.key.ValueKey
import com.skin.rbx.clothes.makek.core.utils.share.whatsapp.WhatsappSharingActivity
import com.skin.rbx.clothes.makek.core.utils.state.HandleState
import com.skin.rbx.clothes.makek.databinding.ActivityAlbumBinding
import com.skin.rbx.clothes.makek.dialog.YesNoDialog
import com.skin.rbx.clothes.makek.ui.home.HomeActivity
import com.skin.rbx.clothes.makek.ui.view.ViewActivity
import com.skin.rbx.clothes.makek.dialog.CreateNameDialog
import com.skin.rbx.clothes.makek.ui.my_creation.adapter.MyAvatarAdapter
import com.skin.rbx.clothes.makek.ui.my_creation.adapter.TypeAdapter
import com.skin.rbx.clothes.makek.ui.my_creation.fragment.MyAvatarFragment
import com.skin.rbx.clothes.makek.ui.my_creation.fragment.MyDesignFragment
import com.skin.rbx.clothes.makek.ui.my_creation.fragment.MyPackageFragment
import com.skin.rbx.clothes.makek.ui.my_creation.view_model.MyAvatarViewModel
import com.skin.rbx.clothes.makek.ui.my_creation.view_model.MyCreationViewModel
import com.skin.rbx.clothes.makek.ui.permission.PermissionViewModel
import com.skin.rbx.clothes.makek.ui.outfit.DownloadEntry
import com.skin.rbx.clothes.makek.ui.outfit.OutfitDownloadHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.text.replace

class MyCreationActivity : WhatsappSharingActivity<ActivityAlbumBinding>() {
    private val viewModel: MyCreationViewModel by viewModels()
    private val permissionViewModel: PermissionViewModel by viewModels()

    private val viewActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        val avatarFragment = supportFragmentManager.findFragmentByTag("MyAvatarFragment")
        val designFragment = supportFragmentManager.findFragmentByTag("MyDesignFragment")
        val packageFragment = supportFragmentManager.findFragmentByTag("MyPackageFragment")
        when {
            avatarFragment is MyAvatarFragment && avatarFragment.isVisible -> avatarFragment.resetSelectionMode()
            designFragment is MyDesignFragment && designFragment.isVisible -> designFragment.resetSelectionMode()
            packageFragment is MyPackageFragment && packageFragment.isVisible -> packageFragment.resetSelectionMode()
        }
        exitSelectionMode()
    }

    fun launchViewActivity(intent: Intent, options: ActivityOptionsCompat? = null) {
        viewActivityLauncher.launch(intent, options)
    }

    private var myAvatarFragment: MyAvatarFragment? = null
    private var myDesignFragment: MyDesignFragment? = null
    private var myPackageFragment: MyPackageFragment? = null
    private var isInSelectionMode = false
    private var isAllSelected = false
    private var isAvatarListEmpty = true
    private var pendingDownloadList: ArrayList<String>? = null
    private var pendingPackageDownloadEntries: ArrayList<DownloadEntry>? = null

    override fun setViewBinding(): ActivityAlbumBinding {
        return ActivityAlbumBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        val initialTab = intent.getIntExtra(IntentKey.TAB_KEY, ValueKey.PACKAGE_TYPE)
        viewModel.setTypeStatus(initialTab)
        viewModel.setStatusFrom(intent.getBooleanExtra(IntentKey.FROM_SAVE, false))

        // Hide action bar buttons by default (only show in selection mode)
        binding.actionBar.apply {
            btnActionBarNextRight.gone()
            btnActionBarRight.gone()
            //tvCenter.visible()
            tvCenter.setText(R.string.my_creation)
        }
        binding.lnlBottom.invisible()
        binding.lnlBottom.isSelected = true

    }

    override fun dataObservable() {
        binding.apply {
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.CREATED) {
                    launch {
                        viewModel.typeStatus.collect { type ->
                            if (type != -1) {
                                when (type) {
                                    ValueKey.PACKAGE_TYPE -> {
                                        imvTabBackground.setImageResource(R.drawable.tab_package_slt)
                                        frmList.setBackgroundColor(Color.parseColor("#FFEED2"))
                                        setupSelectedTab(tvPK)
                                        setupUnselectedTab(tvMyPride)
                                        setupUnselectedTab(tvMyDesign)
                                        showFragment(ValueKey.PACKAGE_TYPE)
                                    }

                                    ValueKey.AVATAR_TYPE -> {
                                        imvTabBackground.setImageResource(R.drawable.tab_avatar_slt)
                                        frmList.setBackgroundColor(Color.parseColor("#D2F7FF"))
                                        setupUnselectedTab(tvPK)
                                        setupSelectedTab(tvMyPride)
                                        setupUnselectedTab(tvMyDesign)
                                        showFragment(ValueKey.AVATAR_TYPE)
                                    }

                                    ValueKey.MY_DESIGN_TYPE -> {
                                        imvTabBackground.setImageResource(R.drawable.tab_design_slt)
                                        frmList.setBackgroundColor(Color.parseColor("#FFD2D2"))
                                        setupUnselectedTab(tvPK)
                                        setupUnselectedTab(tvMyPride)
                                        setupSelectedTab(tvMyDesign)
                                        showFragment(ValueKey.MY_DESIGN_TYPE)
                                    }
                                }
                                updateBottomButtonsVisibility()
                            }
                        }
                    }
                    launch {
                        viewModel.downloadState.collect { state ->
                            when (state) {
                                HandleState.LOADING -> {
                                    showLoading()
                                }

                                HandleState.SUCCESS -> {
                                    dismissLoading()
                                    hideNavigation()
                                    showToast(R.string.download_success)
                                    if (isInSelectionMode) {
                                        binding.actionBar.btnActionBarLeft.performClick()
                                    }
                                }

                                else -> {
                                    dismissLoading()
                                    hideNavigation()
                                    showToast(R.string.download_failed_please_try_again_later)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun viewListener() {
        binding.apply {
            actionBar.apply {
                btnActionBarLeft.tap {
                    if (isInSelectionMode) {
                        val avatarFragment =
                            supportFragmentManager.findFragmentByTag("MyAvatarFragment")
                        val designFragment =
                            supportFragmentManager.findFragmentByTag("MyDesignFragment")
                        val packageFragment =
                            supportFragmentManager.findFragmentByTag("MyPackageFragment")
                        when {
                            avatarFragment is MyAvatarFragment && avatarFragment.isVisible -> avatarFragment.resetSelectionMode()
                            designFragment is MyDesignFragment && designFragment.isVisible -> designFragment.resetSelectionMode()
                            packageFragment is MyPackageFragment && packageFragment.isVisible -> packageFragment.resetSelectionMode()
                        }
                    } else {
                        startIntentWithClearTop(HomeActivity::class.java)
                    }
                }

                // Select All button
                btnActionBarRight.tap(1000) {
                    if (isInSelectionMode) {
                        handleSelectAllFromCurrentFragment()
                    } else {
                        val paths = getSelectedPathsFromCurrentFragment()
                        checkStoragePermissionForDownload(paths)
                    }
                }

                // Delete button in selection mode
                btnActionBarNextRight.tap(1000){
                    if (isInSelectionMode) {
                        handleDeleteSelectedFromCurrentFragment()
                    } else {
                        val paths = getSelectedPathsFromCurrentFragment()
                        handleShare(paths)
                    }
                }

                btnActionBarNextRight1.tap {
                    handleDeleteSelectedFromCurrentFragment()
                }
            }

            btnPK.tap { switchTab(ValueKey.PACKAGE_TYPE) }
            btnMyPixel.tap { switchTab(ValueKey.AVATAR_TYPE) }
            btnMyDesign.tap { switchTab(ValueKey.MY_DESIGN_TYPE) }

            // WhatsApp, Telegram buttons in lnlBottom
            val layoutBottom = lnlBottom.getChildAt(0)
            layoutBottom.findViewById<View>(R.id.btnRight)?.tap(800) {
                val paths = getAllPathsFromCurrentFragment()
                val paths1 = getSelectedPathsFromCurrentFragment()

                handleAddToWhatsApp(
                    if (!isInSelectionMode) paths else {
                        paths1
                    }
                )
            }
            layoutBottom.findViewById<View>(R.id.btnLeft)?.tap(800) {
                val paths = getAllPathsFromCurrentFragment()
                val paths1 = getSelectedPathsFromCurrentFragment()
                handleAddToTelegram(
                    if (!isInSelectionMode) paths else {
                        paths1
                    }
                )
            }

            // Share/Download buttons in flBottomView (select mode)
            bottomView.btnViewShare.tap(800) {
                val paths = getSelectedPathsFromCurrentFragment()
                handleShare(paths)
            }
            bottomView.btnDownload.tap(800) {
                if (viewModel.typeStatus.value == ValueKey.PACKAGE_TYPE) {
                    val entries = getSelectedPackageEntries()
                    if (entries.isEmpty()) {
                        showToast(R.string.please_select_an_image)
                        return@tap
                    }
                    checkStoragePermissionForPackageDownload(entries)
                    return@tap
                }
                val paths = getSelectedPathsFromCurrentFragment()
                if (paths.isEmpty()) {
                    showToast(R.string.please_select_an_image); return@tap
                }
                checkStoragePermissionForDownload(paths)
            }
        }
    }

    private fun switchTab(targetType: Int) {
        if (viewModel.typeStatus.value == targetType) return
        resetCurrentSelectionMode()
        viewModel.setTypeStatus(targetType)
    }

    private fun resetCurrentSelectionMode() {
        if (!isInSelectionMode) return

        when (viewModel.typeStatus.value) {
            ValueKey.AVATAR_TYPE ->
                (supportFragmentManager.findFragmentByTag("MyAvatarFragment") as? MyAvatarFragment)
                    ?.resetSelectionMode()

            ValueKey.MY_DESIGN_TYPE ->
                (supportFragmentManager.findFragmentByTag("MyDesignFragment") as? MyDesignFragment)
                    ?.resetSelectionMode()

            ValueKey.PACKAGE_TYPE ->
                (supportFragmentManager.findFragmentByTag("MyPackageFragment") as? MyPackageFragment)
                    ?.resetSelectionMode()
        }
        exitSelectionMode()
    }

    private fun handleShareFromCurrentFragment() {
        val selectedPaths = getSelectedPathsFromCurrentFragment()
        handleShare(selectedPaths)
    }

    private fun handleDownloadFromCurrentFragment() {
        val selectedPaths = getSelectedPathsFromCurrentFragment()
        if (selectedPaths.isEmpty()) {
            showToast(R.string.please_select_an_image)
            return
        }
        checkStoragePermissionForDownload(selectedPaths)
    }

    private fun checkStoragePermissionForDownload(list: ArrayList<String>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ không cần quyền WRITE_EXTERNAL_STORAGE
            handleDownload(list)
        } else {
            // Android 8-9 cần check quyền
            val perms = permissionViewModel.getStoragePermissions()
            if (checkPermissions(perms)) {
                handleDownload(list)
            } else if (permissionViewModel.needGoToSettings(sharePreference, true)) {
                goToSettings()
            } else {
                // Lưu lại list để download sau khi được cấp quyền
                pendingDownloadList = list
                requestPermission(perms, RequestKey.STORAGE_PERMISSION_CODE)
            }
        }
    }

    private fun checkStoragePermissionForPackageDownload(entries: ArrayList<DownloadEntry>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            downloadPackageEntries(entries)
            return
        }

        val permissions = permissionViewModel.getStoragePermissions()
        when {
            checkPermissions(permissions) -> downloadPackageEntries(entries)
            permissionViewModel.needGoToSettings(sharePreference, true) -> goToSettings()
            else -> {
                pendingPackageDownloadEntries = entries
                requestPermission(permissions, RequestKey.STORAGE_PERMISSION_CODE)
            }
        }
    }

    private fun downloadPackageEntries(entries: List<DownloadEntry>) {
        lifecycleScope.launch {
            showLoading()
            val allSuccessful = withContext(Dispatchers.IO) {
                var successful = true
                entries.forEach { entry ->
                    if (!OutfitDownloadHelper.save(this@MyCreationActivity, entry)) {
                        successful = false
                    }
                }
                successful
            }
            dismissLoading()
            showToast(
                if (allSuccessful) R.string.download_success
                else R.string.download_failed_please_try_again_later
            )
        }
    }

    private fun handleSelectAllFromCurrentFragment() {
        val avatarFragment = supportFragmentManager.findFragmentByTag("MyAvatarFragment")
        val designFragment = supportFragmentManager.findFragmentByTag("MyDesignFragment")
        val packageFragment = supportFragmentManager.findFragmentByTag("MyPackageFragment")

        val doSelect: (() -> Unit)
        val doDeselect: (() -> Unit)

        when {
            avatarFragment is MyAvatarFragment && avatarFragment.isVisible -> {
                doSelect = { avatarFragment.selectAllItems() }
                doDeselect = { avatarFragment.deselectAllItems() }
            }

            designFragment is MyDesignFragment && designFragment.isVisible -> {
                doSelect = { designFragment.selectAllItems() }
                doDeselect = { designFragment.deselectAllItems() }
            }

            packageFragment is MyPackageFragment && packageFragment.isVisible -> {
                doSelect = { packageFragment.selectAllItems() }
                doDeselect = { packageFragment.deselectAllItems() }
            }

            else -> return
        }

        if (isAllSelected) {
            doDeselect()
            isAllSelected = false
            binding.actionBar.btnActionBarRight.setImageResource(R.drawable.ic_not_select_all)
        } else {
            doSelect()
            isAllSelected = true
            binding.actionBar.btnActionBarRight.setImageResource(R.drawable.ic_select_all)
        }
    }

    private fun handleDeleteSelectedFromCurrentFragment() {
        val avatarFragment = supportFragmentManager.findFragmentByTag("MyAvatarFragment")
        val designFragment = supportFragmentManager.findFragmentByTag("MyDesignFragment")
        val packageFragment = supportFragmentManager.findFragmentByTag("MyPackageFragment")

        when {
            avatarFragment is MyAvatarFragment && avatarFragment.isVisible -> avatarFragment.deleteSelectedItems()
            designFragment is MyDesignFragment && designFragment.isVisible -> designFragment.deleteSelectedItems()
            packageFragment is MyPackageFragment && packageFragment.isVisible -> packageFragment.deleteSelectedItems()
        }
    }

    private fun getAllPathsFromCurrentFragment(): ArrayList<String> {
        val avatarFragment = supportFragmentManager.findFragmentByTag("MyAvatarFragment")
        val designFragment = supportFragmentManager.findFragmentByTag("MyDesignFragment")
        val packageFragment = supportFragmentManager.findFragmentByTag("MyPackageFragment")
        return when {
            avatarFragment is MyAvatarFragment && avatarFragment.isVisible -> avatarFragment.getAllPaths()
            designFragment is MyDesignFragment && designFragment.isVisible -> designFragment.getAllPaths()
            packageFragment is MyPackageFragment && packageFragment.isVisible -> packageFragment.getAllPaths()
            else -> arrayListOf()
        }
    }

    private fun getSelectedPathsFromCurrentFragment(): ArrayList<String> {
        val avatarFragment = supportFragmentManager.findFragmentByTag("MyAvatarFragment")
        val designFragment = supportFragmentManager.findFragmentByTag("MyDesignFragment")
        val packageFragment = supportFragmentManager.findFragmentByTag("MyPackageFragment")

        return when {
            avatarFragment is MyAvatarFragment && avatarFragment.isVisible -> avatarFragment.getSelectedPaths()
            designFragment is MyDesignFragment && designFragment.isVisible -> designFragment.getSelectedPaths()
            packageFragment is MyPackageFragment && packageFragment.isVisible -> packageFragment.getSelectedPaths()
            else -> arrayListOf()
        }
    }

    private fun getSelectedPackageEntries(): ArrayList<DownloadEntry> {
        val packageFragment =
            supportFragmentManager.findFragmentByTag("MyPackageFragment") as? MyPackageFragment
        return if (packageFragment?.isVisible == true) {
            packageFragment.getSelectedEntries()
        } else {
            arrayListOf()
        }
    }

    override fun initActionBar() {

        binding.actionBar.apply {
            btnActionBarLeft.visible()
            btnActionBarNextRight.gone()
            tvCenter.visible()
            tvCenter.setText(R.string.my_creation)
            // Delete All button - hidden initially, only shown in selection mode
            btnActionBarRight.setImageResource(R.drawable.ic_delete_creation)
            btnActionBarRight.translationX = -0 * resources.displayMetrics.density
            btnActionBarRight.translationY = 0 * resources.displayMetrics.density
            btnActionBarRight.invisible()
        }
    }

    override fun initText() {
        binding.apply {
            tvMyPride.select()
            tvMyDesign.select()
            tvPK.select()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == RequestKey.STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                permissionViewModel.updateStorageGranted(sharePreference, true)
                showToast(R.string.granted_storage)
                // Thực hiện download sau khi được cấp quyền
                pendingPackageDownloadEntries?.let { entries ->
                    pendingPackageDownloadEntries = null
                    pendingDownloadList = null
                    downloadPackageEntries(entries)
                    return
                }
                pendingDownloadList?.let { list ->
                    handleDownload(list)
                    pendingDownloadList = null
                }
            } else {
                permissionViewModel.updateStorageGranted(sharePreference, false)
                pendingDownloadList = null
                pendingPackageDownloadEntries = null
            }
        }
    }

    fun handleShare(list: ArrayList<String>) {
        if (list.isEmpty()) {
            showToast(R.string.please_select_an_image)
            return
        }
        viewModel.shareImages(this, list)
    }

    fun handleAddToTelegram(list: ArrayList<String>) {
        if (list.isEmpty()) {
            showToast(R.string.please_select_an_image)
            return
        }
        viewModel.addToTelegram(this, list)
        if (isInSelectionMode) {
            binding.actionBar.btnActionBarLeft.performClick()
        }
    }

    fun handleAddToWhatsApp(list: ArrayList<String>) {
        if (list.size < 3) {
            showToast(R.string.limit_3_items)
            return
        }
        if (list.size > 30) {
            showToast(R.string.limit_30_items)
            return
        }

        val dialog = CreateNameDialog(this)
        LanguageHelper.setLocale(this)
        dialog.show()

        fun dismissDialog() {
            dialog.dismiss()
            hideNavigation()
        }
        dialog.onNoClick = {
            dismissDialog()
        }
        dialog.onDismissClick = {
            dismissDialog()
        }

        dialog.onYesClick = { packageName ->
            dismissDialog()
            viewModel.addToWhatsapp(this, packageName, list) { stickerPack ->
                if (stickerPack != null) {
                    addToWhatsapp(stickerPack)
                    if (isInSelectionMode) {
                        binding.actionBar.btnActionBarLeft.performClick()
                    }
                }
            }
        }
    }

    private fun handleDownload(list: ArrayList<String>) {
        if (list.isEmpty()) {
            showToast(R.string.please_select_an_image)
            return
        }
        viewModel.downloadFiles(this, list)
    }

    private fun showFragment(type: Int) {
        myAvatarFragment = myAvatarFragment
            ?: supportFragmentManager.findFragmentByTag("MyAvatarFragment") as? MyAvatarFragment
        myDesignFragment = myDesignFragment
            ?: supportFragmentManager.findFragmentByTag("MyDesignFragment") as? MyDesignFragment
        myPackageFragment = myPackageFragment
            ?: supportFragmentManager.findFragmentByTag("MyPackageFragment") as? MyPackageFragment

        val transaction = supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)

        when (type) {
            ValueKey.PACKAGE_TYPE -> {
                val packageFragment = myPackageFragment ?: MyPackageFragment().also {
                    myPackageFragment = it
                    transaction.add(R.id.frmList, it, "MyPackageFragment")
                }
                myAvatarFragment?.let {
                    transaction.hide(it)
                    transaction.setMaxLifecycle(it, Lifecycle.State.CREATED)
                }
                myDesignFragment?.let {
                    transaction.hide(it)
                    transaction.setMaxLifecycle(it, Lifecycle.State.CREATED)
                }
                transaction.show(packageFragment)
                transaction.setMaxLifecycle(packageFragment, Lifecycle.State.RESUMED)
            }

            ValueKey.AVATAR_TYPE -> {
                val avatarFragment = myAvatarFragment ?: MyAvatarFragment().also {
                    myAvatarFragment = it
                    transaction.add(R.id.frmList, it, "MyAvatarFragment")
                }
                myPackageFragment?.let {
                    transaction.hide(it)
                    transaction.setMaxLifecycle(it, Lifecycle.State.CREATED)
                }
                myDesignFragment?.let {
                    transaction.hide(it)
                    transaction.setMaxLifecycle(it, Lifecycle.State.CREATED)
                }
                transaction.show(avatarFragment)
                transaction.setMaxLifecycle(avatarFragment, Lifecycle.State.RESUMED)
            }

            ValueKey.MY_DESIGN_TYPE -> {
                val designFragment = myDesignFragment ?: MyDesignFragment().also {
                    myDesignFragment = it
                    transaction.add(R.id.frmList, it, "MyDesignFragment")
                }
                myPackageFragment?.let {
                    transaction.hide(it)
                    transaction.setMaxLifecycle(it, Lifecycle.State.CREATED)
                }
                myAvatarFragment?.let {
                    transaction.hide(it)
                    transaction.setMaxLifecycle(it, Lifecycle.State.CREATED)
                }
                transaction.show(designFragment)
                transaction.setMaxLifecycle(designFragment, Lifecycle.State.RESUMED)
            }
        }

        transaction.commit()
    }

    @SuppressLint("MissingSuperCall", "GestureBackNavigation")
    override fun onBackPressed() {
        startIntentWithClearTop(HomeActivity::class.java)
    }

    fun initNativeCollab() {
//        Admob.getInstance().loadNativeCollapNotBanner(this,getString(R.string.native_cl_creation), binding.flNativeCollab)
    }

    override fun initAds() {
//        initNativeCollab()
//        Admob.getInstance().loadNativeAd(
//            this,
//            getString(R.string.native_creation),
//            binding.nativeAds,
//            R.layout.ads_native_banner
//        )
    }

    override fun onRestart() {
        super.onRestart()
        android.util.Log.w(
            "MyCreationActivity",
            "🔄 onRestart() called - Activity restarting after being stopped"
        )
        android.util.Log.w(
            "MyCreationActivity",
            "Current tab: ${
                when (viewModel.typeStatus.value) {
                    ValueKey.AVATAR_TYPE -> "MyAvatar"
                    ValueKey.PACKAGE_TYPE -> "MyPackage"
                    else -> "MyDesign"
                }
            }"
        )
        android.util.Log.w("MyCreationActivity", "Selection mode: $isInSelectionMode")

        // Check permission status
        val hasPermission =
            checkPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE))
        android.util.Log.w("MyCreationActivity", "📱 Storage permission: $hasPermission")

        // Exit selection mode when returning from another activity
        if (isInSelectionMode) {
            val avatarFragment = supportFragmentManager.findFragmentByTag("MyAvatarFragment")
            val designFragment = supportFragmentManager.findFragmentByTag("MyDesignFragment")
            val packageFragment = supportFragmentManager.findFragmentByTag("MyPackageFragment")

            when {
                avatarFragment is MyAvatarFragment && avatarFragment.isVisible -> avatarFragment.resetSelectionMode()
                designFragment is MyDesignFragment && designFragment.isVisible -> designFragment.resetSelectionMode()
                packageFragment is MyPackageFragment && packageFragment.isVisible -> packageFragment.resetSelectionMode()
            }
            exitSelectionMode()
        }

//        initNativeCollab()
        android.util.Log.w("MyCreationActivity", "🔄 onRestart() END")
    }

    override fun onStart() {
        super.onStart()
        android.util.Log.w("MyCreationActivity", "🔵 onStart() called - Activity becoming visible")
    }

    override fun onResume() {
        super.onResume()
        android.util.Log.w("MyCreationActivity", "🟢 onResume() called - Activity in foreground")
    }

    override fun onPause() {
        super.onPause()
        android.util.Log.w("MyCreationActivity", "🟡 onPause() called - Activity losing focus")
    }

    override fun onStop() {
        super.onStop()
        android.util.Log.w("MyCreationActivity", "🔴 onStop() called - Activity no longer visible")
    }

    fun enterSelectionMode() {
        isInSelectionMode = true
        isAllSelected = false
        binding.actionBar.apply {
            btnActionBarNextRight.visible()
            btnActionBarNextRight1.gone()
            btnActionBarRight.visible()
            btnActionBarNextRight.setImageResource(R.drawable.ic_delete)
            btnActionBarRight.setImageResource(R.drawable.ic_not_select_all)

            btnActionBarNextRight.translationY = -0 * resources.displayMetrics.density
        }
        updateBottomButtonsVisibility()
    }

    fun exitSelectionMode() {
        isInSelectionMode = false
        isAllSelected = false
        binding.actionBar.apply {
            btnActionBarNextRight.gone()
            btnActionBarNextRight1.gone()
            btnActionBarRight.gone()
        }
        binding.flBottomView.gone()
        updateBottomButtonsVisibility()
        binding.lnlBottom.translationY = 0f
    }

    fun refreshBottomButtonsVisibility() {
        updateBottomButtonsVisibility()
    }

    fun updateAvatarEmptyState(isEmpty: Boolean) {
        isAvatarListEmpty = isEmpty
        updateBottomButtonsVisibility()
    }

    private fun updateBottomButtonsVisibility() {
        val layoutBottom = binding.lnlBottom.getChildAt(0)
        val btnRight = layoutBottom.findViewById<View>(R.id.btnRight)
        val btnLeft = layoutBottom.findViewById<View>(R.id.btnLeft)

        // Keep layout_bottom.xml's own backgrounds and labels unchanged.
        btnLeft?.visible()
        btnRight?.visible()

        if (isInSelectionMode) {
            if (viewModel.typeStatus.value == ValueKey.AVATAR_TYPE && !isAvatarListEmpty) {
                binding.lnlBottom.visible()
                binding.flBottomView.visible()
                (binding.lnlBottom.layoutParams as ViewGroup.MarginLayoutParams).apply {
                    bottomMargin = UnitHelper.dpToPxInt(resources, 12f)
                    binding.lnlBottom.layoutParams = this
                }
            } else {
                binding.lnlBottom.gone()
                binding.flBottomView.visible()
            }
        } else {
            binding.flBottomView.gone()
            (binding.lnlBottom.layoutParams as ViewGroup.MarginLayoutParams).apply {
                bottomMargin = if (viewModel.typeStatus.value == ValueKey.AVATAR_TYPE) {
                    UnitHelper.dpToPxInt(resources, 44f)
                } else {
                    0
                }
                binding.lnlBottom.layoutParams = this
            }
            if (viewModel.typeStatus.value == ValueKey.AVATAR_TYPE && !isAvatarListEmpty) {
                binding.lnlBottom.visible()
            } else {
                binding.lnlBottom.gone()
            }
        }

        // ActionBar download/share buttons: only in selection mode (avatar tab only)
        binding.actionBar.apply {
            if (isInSelectionMode && (viewModel.typeStatus.value == ValueKey.AVATAR_TYPE || viewModel.typeStatus.value == ValueKey.MY_DESIGN_TYPE || viewModel.typeStatus.value == ValueKey.PACKAGE_TYPE)) {
                btnActionBarNextRight.setImageResource(R.drawable.ic_delete)
                btnActionBarRight.setImageResource(if (isAllSelected) R.drawable.ic_select_all else R.drawable.ic_not_select_all)
                btnActionBarNextRight.visible()
                btnActionBarNextRight1.gone()
                btnActionBarRight.visible()
            } else {
                btnActionBarNextRight.setImageResource(R.drawable.ic_download_actionbar)
                btnActionBarNextRight1.setImageResource(R.drawable.ic_share_actionbar)
                btnActionBarNextRight.invisible()
                btnActionBarNextRight1.invisible()
            }
        }
    }

    private fun setupSelectedTab(textView: com.skin.rbx.clothes.makek.core.custom.text.OuterStrokeTextView) {
        textView.setTextColor(Color.parseColor("#000000"))
        //textView.setShadowLayer(2f, 0f, 2f, Color.WHITE)
        //textView.setupSelectedTab()
    }

    private fun setupUnselectedTab(textView: com.skin.rbx.clothes.makek.core.custom.text.OuterStrokeTextView) {
        textView.setTextColor(Color.parseColor("#000000"))
        //textView.setupUnselectedTab()
    }

    // Public method to update select all icon based on selection state
    fun updateSelectAllIcon(allSelected: Boolean) {
        isAllSelected = allSelected
        if (allSelected) {
            binding.actionBar.btnActionBarRight.setImageResource(R.drawable.ic_select_all)
        } else {
            binding.actionBar.btnActionBarRight.setImageResource(R.drawable.ic_not_select_all)
        }
    }
}
