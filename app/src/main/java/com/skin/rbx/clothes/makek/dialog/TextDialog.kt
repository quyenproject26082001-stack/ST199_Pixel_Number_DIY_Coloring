package com.skin.rbx.clothes.makek.dialog

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.skin.rbx.clothes.makek.R
import com.skin.rbx.clothes.makek.core.base.BaseDialog
import com.skin.rbx.clothes.makek.core.extensions.dp
import com.skin.rbx.clothes.makek.core.helper.BitmapHelper
import com.skin.rbx.clothes.makek.databinding.C253DialogTextBinding
import com.skin.rbx.clothes.makek.databinding.C253ItemFontBinding
import com.skin.rbx.clothes.makek.databinding.C253ItemTextColorBinding

class TextDialog(private val host: Activity) :
    BaseDialog<C253DialogTextBinding>(host, maxWidth = true, maxHeight = true) {

    override val layoutId = R.layout.c253_dialog_text
    override val isCancelOnTouchOutside = false
    override val isCancelableByBack = false

    var onDoneClick: (Bitmap?, String) -> Unit = { _, _ -> }
    var onBackClick: () -> Unit = {}

    private val colors = mutableListOf<Int?>(
        null,
        ContextCompat.getColor(host, R.color.black),
        ContextCompat.getColor(host, R.color.white),
        ContextCompat.getColor(host, R.color.color_19),
        ContextCompat.getColor(host, R.color.color_2),
        ContextCompat.getColor(host, R.color.color_3),
        ContextCompat.getColor(host, R.color.color_4),
        ContextCompat.getColor(host, R.color.color_5),
        ContextCompat.getColor(host, R.color.color_6),
        ContextCompat.getColor(host, R.color.color_7),
        ContextCompat.getColor(host, R.color.color_8),
    )
    private val fonts = listOf(
        R.font.roboto_regular,
        R.font.aldrich,
        R.font.brush_script,
        R.font.nova_script,
        R.font.carattere,
        R.font.digital_numbers,
        R.font.dynalight,
        R.font.edwardian_script_itc,
        R.font.vni_ongdo,
    )
    private var selectedColor = 1
    private var selectedFont = 0
    private lateinit var colorAdapter: TextColorAdapter
    private lateinit var fontAdapter: TextFontAdapter

    override fun initView() {
        colorAdapter = TextColorAdapter(colors, ::selectColor)
        fontAdapter = TextFontAdapter(fonts, ::selectFont)
        binding.rcvTextColor.adapter = colorAdapter
        binding.rcvFont.adapter = fontAdapter
        showFontTab()
        selectColor(selectedColor)
        selectFont(selectedFont)
        binding.btnDone.visibility = View.INVISIBLE
        binding.rcvFont.post { binding.rcvFont.scrollToPosition(0) }
    }

    override fun initAction() {
        with(binding) {
            tvFont.setOnClickListener { showFontTab() }
            tvColor.setOnClickListener { showColorTab() }
            btnBack.setOnClickListener {
                dismiss()
                onBackClick()
            }
            edtText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    tvGetText.text = s
                    btnDone.visibility = if (s.isNullOrBlank()) View.INVISIBLE else View.VISIBLE
                }
                override fun afterTextChanged(s: Editable?) = Unit
            })
            btnDone.setOnClickListener {
                val text = edtText.text?.toString().orEmpty()
                if (text.isBlank()) {
                    onDoneClick(null, text)
                    dismiss()
                    return@setOnClickListener
                }
                edtText.visibility = View.INVISIBLE
                tvGetText.visibility = View.VISIBLE
                tvGetText.post {
                    onDoneClick(BitmapHelper.getBitmapFromEditText(tvGetText), text)
                    dismiss()
                }
            }
            root.setOnClickListener { focusTextInput() }
            edtText.requestFocus()
            edtText.postDelayed({
                (host.getSystemService(Activity.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager)
                    .showSoftInput(edtText, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
            }, 300)
        }
    }

    override fun onDismissListener() = Unit

    private fun focusTextInput() = with(binding.edtText) {
        requestFocus()
        setSelection(text.length)
        post {
            (host.getSystemService(Activity.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager)
                .showSoftInput(this, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun showFontTab() = with(binding) {
        sectionTab.setBackgroundResource(R.drawable.bg_font_slt)
        rcvFont.visibility = View.VISIBLE
        rcvTextColor.visibility = View.GONE
        rcvFont.scrollToPosition(selectedFont)
    }

    private fun showColorTab() = with(binding) {
        sectionTab.setBackgroundResource(R.drawable.bg_color_slt)
        rcvFont.visibility = View.GONE
        rcvTextColor.visibility = View.VISIBLE
        rcvTextColor.scrollToPosition(selectedColor)
    }

    private fun selectColor(position: Int) {
        if (position == 0) {
            ChooseColorDialog(host).apply {
                onDoneEvent = { color ->
                    colors[0] = color
                    applyColor(0, color)
                    dismiss()
                }
                show()
            }
            return
        }
        applyColor(position, colors[position] ?: Color.BLACK)
    }

    private fun applyColor(position: Int, color: Int) {
        selectedColor = position
        colorAdapter.selected = position
        binding.edtText.setTextColor(color)
        binding.tvGetText.setTextColor(color)
    }

    private fun selectFont(position: Int) {
        selectedFont = position
        fontAdapter.selected = position
        val typeface = ResourcesCompat.getFont(host, fonts[position])
        binding.edtText.typeface = typeface
        binding.tvGetText.typeface = typeface
    }

    private class TextColorAdapter(
        private val items: List<Int?>,
        private val onClick: (Int) -> Unit,
    ) : RecyclerView.Adapter<TextColorAdapter.Holder>() {
        var selected = 1
            set(value) { field = value; notifyDataSetChanged() }
        override fun getItemCount() = items.size
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
            C253ItemTextColorBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
        override fun onBindViewHolder(holder: Holder, position: Int) =
            holder.bind(items[position], position == 0, position == selected) { onClick(position) }

        class Holder(private val binding: C253ItemTextColorBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(color: Int?, isAddColor: Boolean, selected: Boolean, onClick: () -> Unit) {
                binding.root.setBackgroundResource(
                    if (selected) R.drawable.bg_text_color_selected else R.drawable.bg_text_color_unselected
                )
                val inset = (if (selected) 4 else 1).dp(binding.root.context)
                (binding.cardColor.layoutParams as android.widget.FrameLayout.LayoutParams).apply {
                    setMargins(inset, inset, inset, inset)
                    binding.cardColor.layoutParams = this
                }
                binding.btnAddColor.visibility = if (isAddColor) View.VISIBLE else View.GONE
                binding.cardColor.setCardBackgroundColor(color ?: Color.WHITE)
                binding.root.setOnClickListener { onClick() }
            }
        }
    }

    private class TextFontAdapter(
        private val items: List<Int>,
        private val onClick: (Int) -> Unit,
    ) : RecyclerView.Adapter<TextFontAdapter.Holder>() {
        var selected = 0
            set(value) { field = value; notifyDataSetChanged() }
        override fun getItemCount() = items.size
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
            C253ItemFontBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
        override fun onBindViewHolder(holder: Holder, position: Int) =
            holder.bind(items[position], position == selected) { onClick(position) }

        class Holder(private val binding: C253ItemFontBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(font: Int, selected: Boolean, onClick: () -> Unit) {
                binding.root.setBackgroundResource(
                    if (selected) R.drawable.bg_text_font_selected else R.drawable.bg_text_font_unselected
                )
                binding.tvFont.typeface = ResourcesCompat.getFont(binding.root.context, font)
                binding.root.setOnClickListener { onClick() }
            }
        }
    }
}
