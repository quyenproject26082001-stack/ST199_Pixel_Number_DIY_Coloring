package com.skin.rbx.clothes.makek.ui.my_creation.fragment

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.skin.rbx.clothes.makek.R
import com.skin.rbx.clothes.makek.core.base.BaseFragment
import com.skin.rbx.clothes.makek.core.extensions.shareImagesPaths
import com.skin.rbx.clothes.makek.databinding.FragmentMyPixelBinding
import com.skin.rbx.clothes.makek.ui.my_creation.MyCreationActivity
import com.skin.rbx.clothes.makek.ui.pixel_coloring.PixelColoringActivity
import com.skin.rbx.clothes.makek.ui.pixel_coloring.PixelLevelAdapter
import com.skin.rbx.clothes.makek.ui.pixel_coloring.PixelLevelEntry
import com.skin.rbx.clothes.makek.ui.pixel_coloring.PixelProgressStore
import com.skin.rbx.clothes.makek.ui.pixel_coloring.PixelRepository
import com.skin.rbx.clothes.makek.ui.pixel_coloring.PixelRetryDialog
import com.skin.rbx.clothes.makek.ui.pixel_coloring.createCompletedPreview
import com.skin.rbx.clothes.makek.ui.pixel_coloring.saveShareArtwork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MyPixelFragment : BaseFragment<FragmentMyPixelBinding>() {
    private val repository by lazy { PixelRepository(requireContext()) }
    private val progressStore by lazy { PixelProgressStore(requireContext()) }
    private lateinit var pixelAdapter: PixelLevelAdapter
    private var loadJob: Job? = null
    private var dataDirty = true
    private val changedLevelIds = linkedSetOf<String>()

    override fun setViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
    ) = FragmentMyPixelBinding.inflate(inflater, container, false)

    override fun initView() {
        pixelAdapter = PixelLevelAdapter(repository, progressStore, lifecycleScope, ::openLevel)
        binding.pixelList.apply {
            adapter = pixelAdapter
            setHasFixedSize(true)
            itemAnimator = null
            setItemViewCacheSize(8)
            recycledViewPool.setMaxRecycledViews(0, 12)
        }
        (binding.pixelList.layoutManager as? GridLayoutManager)?.spanCount =
            if (resources.configuration.smallestScreenWidthDp >= 600) 3 else 2
        loadPixelItems()
    }

    override fun viewListener() = Unit

    override fun onResume() {
        super.onResume()
        loadPixelItems()
    }

    private fun loadPixelItems() {
        if (!dataDirty || loadJob?.isActive == true) return
        dataDirty = false
        val levelsToRefresh = changedLevelIds.toSet()
        changedLevelIds.clear()
        loadJob?.cancel()
        val isFirstLoad = pixelAdapter.currentList.isEmpty()
        binding.loading.isVisible = isFirstLoad
        if (isFirstLoad) {
            binding.pixelList.isVisible = false
            binding.emptyState.isVisible = false
        }
        val mode = requireArguments().getInt(ARG_MODE)
        loadJob = viewLifecycleOwner.lifecycleScope.launch {
            val items = withContext(Dispatchers.IO) {
                val progressIds = progressStore.getProgressIds()
                repository.loadCatalog().filter { entry ->
                    when (mode) {
                        MODE_IN_PROGRESS ->
                            entry.id in progressIds.startedIds &&
                                entry.id !in progressIds.completedIds

                        MODE_FINISHED -> entry.id in progressIds.completedIds
                        else -> false
                    }
                }
            }
            pixelAdapter.submitList(items) {
                levelsToRefresh.forEach(pixelAdapter::refreshProgress)
                binding.loading.isVisible = false
                binding.pixelList.isVisible = items.isNotEmpty()
                binding.emptyState.isVisible = items.isEmpty()
            }
        }
    }

    fun markDataDirty(levelId: String) {
        dataDirty = true
        changedLevelIds += levelId
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) loadPixelItems()
    }

    private fun openLevel(entry: PixelLevelEntry) {
        if (progressStore.isCompleted(entry.id)) {
            showRetryDialog(entry)
            return
        }
        launchLevel(entry)
    }

    private fun launchLevel(entry: PixelLevelEntry) {
        (activity as? MyCreationActivity)?.markPixelTabsDirty(entry.id)
        startActivity(
            Intent(requireContext(), PixelColoringActivity::class.java)
                .putExtra(PixelColoringActivity.EXTRA_LEVEL_ID, entry.id),
        )
        requireActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    private fun showRetryDialog(entry: PixelLevelEntry) {
        viewLifecycleOwner.lifecycleScope.launch {
            val preview = withContext(Dispatchers.IO) {
                createCompletedPreview(repository.loadLevel(entry.id))
            }
            if (!isAdded) return@launch
            val host = requireActivity()
            val dialog = PixelRetryDialog(host, preview)
            dialog.onRetryClick = {
                dialog.dismiss()
                progressStore.reset(entry.id)
                launchLevel(entry)
            }
            dialog.onShareClick = {
                viewLifecycleOwner.lifecycleScope.launch {
                    val path = withContext(Dispatchers.IO) {
                        saveShareArtwork(requireContext(), preview, entry.id)
                    }
                    if (path == null) {
                        (activity as? com.skin.rbx.clothes.makek.core.base.BaseActivity<*>)
                            ?.showToast(R.string.save_failed_please_try_again)
                    } else {
                        host.shareImagesPaths(arrayListOf(path))
                    }
                }
            }
            dialog.show()
        }
    }

    companion object {
        private const val ARG_MODE = "pixel_creation_mode"
        const val MODE_IN_PROGRESS = 0
        const val MODE_FINISHED = 1

        fun newInstance(mode: Int) = MyPixelFragment().apply {
            arguments = android.os.Bundle().apply { putInt(ARG_MODE, mode) }
        }
    }
}
