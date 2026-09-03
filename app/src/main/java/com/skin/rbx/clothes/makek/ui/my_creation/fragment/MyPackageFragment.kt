package com.skin.rbx.clothes.makek.ui.my_creation.fragment

import android.content.Intent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.skin.rbx.clothes.makek.R
import com.skin.rbx.clothes.makek.core.base.BaseFragment
import com.skin.rbx.clothes.makek.core.extensions.gone
import com.skin.rbx.clothes.makek.core.extensions.select
import com.skin.rbx.clothes.makek.core.extensions.tap
import com.skin.rbx.clothes.makek.databinding.FragmentMyPackageBinding
import com.skin.rbx.clothes.makek.ui.my_creation.MyCreationActivity
import com.skin.rbx.clothes.makek.ui.my_creation.adapter.MyPackageAdapter
import com.skin.rbx.clothes.makek.ui.my_creation.view_model.MyCreationViewModel
import com.skin.rbx.clothes.makek.ui.my_creation.view_model.MyPackageViewModel
import com.skin.rbx.clothes.makek.ui.outfit.ViewPackageAllActivity
import com.skin.rbx.clothes.makek.ui.outfit.DownloadEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MyPackageFragment : BaseFragment<FragmentMyPackageBinding>() {
    private val viewModel: MyPackageViewModel by viewModels()
    private val myCreationViewModel: MyCreationViewModel by activityViewModels()
    private val packageAdapter by lazy { MyPackageAdapter() }
    private val myAlbumActivity: MyCreationActivity get() = requireActivity() as MyCreationActivity

    override fun setViewBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentMyPackageBinding.inflate(inflater, container, false)

    override fun initView() {
        binding.tvNoitem.select()
        binding.rcvMyPackage.apply {
            adapter = packageAdapter
            itemAnimator = null
            setHasFixedSize(true)
            clipToPadding = false
        }
    }

    override fun dataObservable() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.packageList.collect { list ->
                    packageAdapter.submitList(list)
                    binding.layoutNoItem.isVisible = list.isEmpty()
                    myAlbumActivity.refreshBottomButtonsVisibility()
                }
            }
        }
    }

    override fun viewListener() {
        binding.rcvMyPackage.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(recyclerView: RecyclerView, motionEvent: MotionEvent): Boolean {
                return when {
                    motionEvent.action != MotionEvent.ACTION_UP || recyclerView.findChildViewUnder(motionEvent.x, motionEvent.y) != null -> false
                    else -> { resetSelectionMode(); true }
                }
            }
            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
            override fun onTouchEvent(recyclerView: RecyclerView, motionEvent: MotionEvent) {}
        })
        packageAdapter.onItemClick = { id ->
            startActivity(Intent(myAlbumActivity, ViewPackageAllActivity::class.java).putExtra(ViewPackageAllActivity.EXTRA_PACKAGE_ID, id))
        }
        packageAdapter.onItemTick = { position ->
            viewModel.toggleSelect(position)
            myAlbumActivity.updateSelectAllIcon(viewModel.packageList.value.all { it.isSelected })
        }
        packageAdapter.onLongClick = { position ->
            viewModel.showLongClick(position)
            myAlbumActivity.enterSelectionMode()
            binding.rcvMyPackage.clipToPadding = true
            myAlbumActivity.updateSelectAllIcon(viewModel.packageList.value.all { it.isSelected })
        }
    }

    fun getAllPaths(): ArrayList<String> = viewModel.getAllPreviewPaths()
    fun getSelectedPaths(): ArrayList<String> = viewModel.getSelectedPreviewPaths()
    fun getSelectedEntries(): ArrayList<DownloadEntry> = viewModel.getSelectedEntries()
    fun selectAllItems() = viewModel.selectAll(true)
    fun deselectAllItems() = viewModel.selectAll(false)

    fun deleteSelectedItems() {
        lifecycleScope.launch(Dispatchers.IO) {
            viewModel.deleteSelected(myAlbumActivity)
            withContext(Dispatchers.Main) { resetSelectionMode() }
        }
    }

    fun resetSelectionMode() {
        viewModel.clearSelection()
        myAlbumActivity.exitSelectionMode()
        binding.rcvMyPackage.clipToPadding = false
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (hidden) {
            resetSelectionMode()
        } else {
            myAlbumActivity.refreshBottomButtonsVisibility()
        }
    }

    override fun onStart() {
        super.onStart()
        if (!isHidden) viewModel.observePackages(myAlbumActivity)
    }
}
