package com.skin.rbx.clothes.makek.ui.outfit

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.skin.rbx.clothes.makek.R
import com.skin.rbx.clothes.makek.core.base.BaseActivity
import com.skin.rbx.clothes.makek.core.extensions.setImageActionBar
import com.skin.rbx.clothes.makek.core.extensions.setTextActionBar
import com.skin.rbx.clothes.makek.core.extensions.startIntentWithClearTop
import com.skin.rbx.clothes.makek.core.extensions.tap
import com.skin.rbx.clothes.makek.core.extensions.visible
import com.skin.rbx.clothes.makek.databinding.ActivityOutfitHowToBinding
import com.skin.rbx.clothes.makek.databinding.ItemOutfitHowToBinding
import com.skin.rbx.clothes.makek.ui.home.HomeActivity

class OutfitHowToActivity : BaseActivity<ActivityOutfitHowToBinding>() {
    private val pages = listOf(
        R.drawable.use1 to R.string.title_how_1,
        R.drawable.use2 to R.string.title_how_2,
        R.drawable.use3 to R.string.title_how_3,
        R.drawable.use4 to R.string.title_how_4,
        R.drawable.use5 to R.string.title_how_5,
        R.drawable.use6 to R.string.title_how_6,
    )

    override fun setViewBinding() =
        ActivityOutfitHowToBinding.inflate(LayoutInflater.from(this))

    override fun initView() {
        binding.viewPager.adapter = PageAdapter(pages)
        binding.dotsIndicator.attachTo(binding.viewPager)
        binding.tvUse.setText(pages.first().second)
    }

    override fun viewListener() = with(binding) {
        actionBar.btnActionBarRight.tap { goHome() }
        btnPrevious.tap {
            if (viewPager.currentItem > 0) {
                viewPager.setCurrentItem(viewPager.currentItem - 1, true)
            }
        }
        btnNext.tap {
            if (viewPager.currentItem < pages.lastIndex) {
                viewPager.setCurrentItem(viewPager.currentItem + 1, true)
            }
        }
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                btnPrevious.visibility = if (position == 0) View.INVISIBLE else View.VISIBLE
                btnNext.visibility = if (position == pages.lastIndex) View.INVISIBLE else View.VISIBLE
                tvUse.setText(pages[position].second)
            }
        })
    }

    override fun initActionBar() = with(binding.actionBar) {
        binding.layoutActionBar.bringToFront()
        btnActionBarRight.visible()
        //tvCenter.visible()
        setImageActionBar(btnActionBarRight, R.drawable.ic_home)
        //setTextActionBar(tvCenter, getString(R.string.how_to_use))
    }

    private fun goHome() {
        startIntentWithClearTop(HomeActivity::class.java)
        finish()
    }

    private class PageAdapter(private val pages: List<Pair<Int, Int>>) :
        RecyclerView.Adapter<PageAdapter.Holder>() {
        override fun getItemCount() = pages.size
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
            ItemOutfitHowToBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
        override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(pages[position])
        class Holder(private val binding: ItemOutfitHowToBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(page: Pair<Int, Int>) {
                binding.imvImage.setImageResource(page.first)
            }
        }
    }
}
