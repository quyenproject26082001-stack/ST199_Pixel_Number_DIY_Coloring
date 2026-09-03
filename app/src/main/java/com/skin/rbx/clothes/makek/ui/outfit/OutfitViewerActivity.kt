package com.skin.rbx.clothes.makek.ui.outfit

import android.annotation.SuppressLint
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.webkit.WebViewAssetLoader
import com.google.gson.Gson
import com.skin.rbx.clothes.makek.core.helper.UnitHelper
import com.skin.rbx.clothes.makek.R
import com.skin.rbx.clothes.makek.core.base.BaseActivity
import com.skin.rbx.clothes.makek.core.extensions.checkInternet
import com.skin.rbx.clothes.makek.core.extensions.handleBackLeftToRight
import com.skin.rbx.clothes.makek.core.extensions.tap
import com.skin.rbx.clothes.makek.core.utils.DataLocal
import com.skin.rbx.clothes.makek.databinding.ActivityOutfitViewerBinding
import com.skin.rbx.clothes.makek.ui.clothes.ClothesCatalogResponse
import com.skin.rbx.clothes.makek.ui.clothes.ClothesMode
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OutfitViewerActivity : BaseActivity<ActivityOutfitViewerBinding>() {

    private val mode by lazy { intent.getStringExtra(EXTRA_MODE) ?: ClothesMode.BASIC_OUTFIT }
    private var shirtUrl: String? = null
    private var pantUrl: String? = null
    private val accessories = linkedMapOf<String, String>()
    private var editingType = ""
    private var pageReady = false
    private var darkTheme = false
    private var selectedCharacterIndex = 0
    private var featureFullWidth = 0
    private var featureAnimator: ValueAnimator? = null
    private var accessoryPickerCatalog: ClothesCatalogResponse? = null
    private var hasHandledNetworkLoss = false

    private val textureEditor = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != RESULT_OK) return@registerForActivityResult
        val url = result.data?.getStringExtra(TextureEditorActivity.EXTRA_RESULT_URL) ?: return@registerForActivityResult
        if (editingType == TYPE_SHIRT) shirtUrl = url else pantUrl = url
        applySelection()
    }

    override fun setViewBinding() = ActivityOutfitViewerBinding.inflate(LayoutInflater.from(this))

    @SuppressLint("SetJavaScriptEnabled")
    override fun initView() {
        shirtUrl = intent.getStringExtra(EXTRA_SHIRT_URL)
        pantUrl = intent.getStringExtra(EXTRA_PANT_URL)
        intent.getStringExtra(EXTRA_ACCESSORY_TYPE)?.let { type ->
            intent.getStringExtra(EXTRA_ACCESSORY_URL)?.let { accessories[type] = it }
        }
        if (mode == ClothesMode.ACCESSORY) {
            shirtUrl = DEFAULT_CLOTHES_URL
            pantUrl = DEFAULT_CLOTHES_URL
        }
        configureControls()
        configureWebView()
        renderCharacterButtons(selectedCharacterIndex)
    }

    override fun dataObservable() {
        DataLocal.isConnectInternet.observe(this) { isConnected ->
            if (isConnected) {
                hasHandledNetworkLoss = false
            } else if (!hasHandledNetworkLoss && !isFinishing) {
                hasHandledNetworkLoss = true
                checkInternet(onFailureConfirmed = { finish() }) {}
            }
        }
    }

    override fun viewListener() = with(binding) {
        btnBack.tap {  handleBackLeftToRight() }
        btnDownload.tap { saveOutfitPackage() }
        btnThemeDark.tap {   applyTheme(true) }
        btnThemeLight.tap { applyTheme(false) }
        btnType1.tap(500) { checkInternet {  selectCharacter(0) }}
        btnType2.tap(500) { checkInternet {  selectCharacter(1) }}
        btnType3.tap(500) { checkInternet {  selectCharacter(2) }}
        btnType4.tap(500) { checkInternet {  selectCharacter(3) }}
        btnShirt.tap { checkInternet { onClothesClicked(TYPE_SHIRT, btnShirt) } }
        btnPant.tap { checkInternet { onClothesClicked(TYPE_PANT, btnPant) } }
        btnAccessory.tap { checkInternet { onClothesClicked(TYPE_ACCESSORY, btnAccessory) } }
        btnImage.tap { checkInternet { openTextureEditor(OPTION_IMAGE) } }
        btnBrush.tap { checkInternet { openTextureEditor(OPTION_BRUSH) } }
        btnSticker.tap { checkInternet { openTextureEditor(OPTION_STICKER) } }
        btnEmoji.tap { checkInternet { openTextureEditor(OPTION_EMOJI) } }
        btnText.tap { checkInternet { openTextureEditor(OPTION_TEXT) } }
        btnClose.tap {
                if (mode != ClothesMode.ACCESSORY) updateClothesButtonState("")
                hideFeature()

        }
    }

    override fun initActionBar() = Unit

    private fun configureControls() = with(binding) {
        btnShirt.visibility = if (shirtUrl != null || mode == ClothesMode.ACCESSORY) View.VISIBLE else View.GONE
        btnPant.visibility = if (pantUrl != null || mode == ClothesMode.ACCESSORY) View.VISIBLE else View.GONE
        btnAccessory.visibility = if (mode == ClothesMode.ACCESSORY) View.VISIBLE else View.GONE
        updateClothesButtonState("")
        lnlFeature.visibility = View.INVISIBLE
        lnlFeature.post { featureFullWidth = lnlFeature.measuredWidth }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .addPathHandler("/internal/") { path ->
                val file = File(filesDir, "outfit/${File(path).name}")
                if (!file.isFile) null else WebResourceResponse(
                    MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension) ?: "image/png",
                    null,
                    FileInputStream(file),
                )
            }.build()
        binding.webView.apply {
            setBackgroundColor(Color.TRANSPARENT)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                    interceptRemoteAsset(request)?.let { return it }
                    return assetLoader.shouldInterceptRequest(request.url)
                }

                override fun onPageFinished(view: WebView, url: String) {
                    view.postDelayed({
                        lifecycleScope.launch {
                            showLoading()
                            pageReady = true
                            applyTheme(darkTheme)
                            view.evaluateJavascript("window.clearAccessoryItems()") {
                                selectCharacter(selectedCharacterIndex, forceApplySelection = true)
                                view.postDelayed(
                                    { lifecycleScope.launch { dismissLoading() } },
                                    WEB_APP_READY_DELAY_MS,
                                )
                            }
                        }
                    }, WEB_APP_READY_DELAY_MS)
                }
            }
            loadUrl(OUTFIT_VIEWER_URL)
        }
    }

    private fun onClothesClicked(type: String, target: View) {
        if (mode == ClothesMode.ACCESSORY) {
            updateClothesButtonState(type)
            openAccessoryPickerSheet(type)
            return
        }
        editingType = type
        updateClothesButtonState(type)
        showFeatureNextTo(target)
    }

    private fun updateClothesButtonState(selectedType: String) = with(binding) {
        val buttons = listOf(
            TYPE_SHIRT to btnShirt,
            TYPE_PANT to btnPant,
            TYPE_ACCESSORY to btnAccessory,
        )
        buttons.forEach { (type, button) ->
            val selected = type == selectedType
            button.setImageResource(
                when (type) {
                    TYPE_SHIRT -> when {
                        mode != ClothesMode.ACCESSORY && selected -> R.drawable.ic_shirt_slt_non_asr
                        mode != ClothesMode.ACCESSORY -> R.drawable.ic_shirt_uslt_non_asr
                        selected -> R.drawable.ic_shirt_slt
                        else -> R.drawable.ic_shirt_uslt
                    }
                    TYPE_PANT -> when {
                        mode != ClothesMode.ACCESSORY && selected -> R.drawable.ic_pant_slt_non_asr
                        mode != ClothesMode.ACCESSORY -> R.drawable.ic_pant_uslt_non_asr
                        selected -> R.drawable.ic_pant_slt
                        else -> R.drawable.ic_pant_uslt
                    }
                    else -> if (selected) R.drawable.ic_assessory_slt else R.drawable.ic_assessory_uslt
                }
            )
            val size = UnitHelper.dpToPxInt(
                resources,
                when {
                    mode != ClothesMode.ACCESSORY && (type == TYPE_SHIRT || type == TYPE_PANT) -> 41f
                    selected -> 48f
                    else -> 44f
                }
            )
            button.layoutParams = button.layoutParams.apply {
                width = size
                height = size
            }
        }
    }

    private fun openAccessoryPickerSheet(type: String) {
        lifecycleScope.launch {
            showLoading()
            val response = accessoryPickerCatalog ?: withContext(Dispatchers.IO) { loadAccessoryPickerCatalog() }
                .also { accessoryPickerCatalog = it }
            dismissLoading()
            AccessoryPickerBottomSheet(
                host = this@OutfitViewerActivity,
                pickType = type,
                response = response,
                selectedClothesUrl = if (type == TYPE_SHIRT) shirtUrl.orEmpty() else pantUrl.orEmpty(),
                selectedAccessoryUrls = accessories,
                onClothesSelected = { selectedType, url ->
                    if (selectedType == TYPE_SHIRT) shirtUrl = url else pantUrl = url
                    applySelection()
                },
                onAccessoriesChanged = { selected ->
                    val clearedTypes = accessories.keys.filterNot(selected::containsKey)
                    accessories.clear()
                    accessories.putAll(selected)
                    applySelection(clearedTypes)
                },
            ).apply {
                setOnDismissListener { updateClothesButtonState("") }
            }.show()
        }
    }

    private fun loadAccessoryPickerCatalog(): ClothesCatalogResponse = runCatching {
        val connection = URL(OutfitUrls.ST253_API).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            connection.inputStream.bufferedReader().use {
                Gson().fromJson(it, ClothesCatalogResponse::class.java)
            }
        } finally {
            connection.disconnect()
        }
    }.getOrDefault(ClothesCatalogResponse())

    private fun showFeatureNextTo(target: View) = with(binding) {
        featureAnimator?.cancel()
        if (featureFullWidth == 0) featureFullWidth = lnlFeature.measuredWidth
        val startWidth = lnlClothes.width.coerceAtMost(featureFullWidth)

        lnlFeature.translationY = 0f
        lnlFeature.layoutParams = lnlFeature.layoutParams.apply { width = startWidth }
        lnlFeature.alpha = .7f
        lnlFeature.visibility = View.VISIBLE

        val targetCenterY = lnlClothes.y + target.y + target.height / 2f
        val featureCenterY = lnlFeature.y + lnlFeature.height / 2f
        lnlFeature.translationY = targetCenterY - featureCenterY

        featureAnimator = ValueAnimator.ofInt(startWidth, featureFullWidth).apply {
            duration = FEATURE_ANIMATION_DURATION_MS
            addUpdateListener { animator ->
                lnlFeature.layoutParams = lnlFeature.layoutParams.apply {
                    width = animator.animatedValue as Int
                }
                lnlFeature.alpha = .7f + (.3f * animator.animatedFraction)
            }
            start()
        }
    }

    private fun hideFeature() = with(binding) {
        if (lnlFeature.visibility != View.VISIBLE) return@with
        featureAnimator?.cancel()
        val endWidth = lnlClothes.width.coerceAtMost(lnlFeature.width)
        var cancelled = false
        featureAnimator = ValueAnimator.ofInt(lnlFeature.width, endWidth).apply {
            duration = FEATURE_ANIMATION_DURATION_MS
            addUpdateListener { animator ->
                lnlFeature.layoutParams = lnlFeature.layoutParams.apply {
                    width = animator.animatedValue as Int
                }
                lnlFeature.alpha = 1f - (.3f * animator.animatedFraction)
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationCancel(animation: Animator) { cancelled = true }
                override fun onAnimationEnd(animation: Animator) {
                    if (cancelled) return
                    lnlFeature.visibility = View.INVISIBLE
                    lnlFeature.alpha = 1f
                    lnlFeature.layoutParams = lnlFeature.layoutParams.apply {
                        width = ViewGroup.LayoutParams.WRAP_CONTENT
                    }
                }
            })
            start()
        }
    }

    private fun openTextureEditor(option: Int) {
        val source = if (editingType == TYPE_SHIRT) shirtUrl else pantUrl
        source ?: return
        textureEditor.launch(Intent(this, TextureEditorActivity::class.java).apply {
            putExtra(TextureEditorActivity.EXTRA_TYPE, editingType)
            putExtra(TextureEditorActivity.EXTRA_SOURCE_URL, source)
            putExtra(TextureEditorActivity.EXTRA_OPTION, option)
            putExtra(TextureEditorActivity.EXTRA_DARK_THEME, darkTheme)
        })
    }

    private fun applySelection(clearedAccessoryTypes: Collection<String> = emptyList()) {
        if (!pageReady) return
        val items = JSONArray()
        shirtUrl?.let { items.put(JSONObject().put("key", TYPE_SHIRT).put("value", it)) }
        pantUrl?.let { items.put(JSONObject().put("key", TYPE_PANT).put("value", it)) }
        clearedAccessoryTypes.forEach { type ->
            items.put(JSONObject().put("key", type).put("value", ""))
        }
        accessories.forEach { (type, url) -> items.put(JSONObject().put("key", type).put("value", url)) }
        binding.webView.evaluateJavascript("window.setItems($items)", null)
    }

    private fun applyTheme(useDarkTheme: Boolean) = with(binding) {
        darkTheme = useDarkTheme
        val background = if (darkTheme) R.drawable.bg_app1 else R.drawable.bg_app
        root.setBackgroundResource(background)
        webView.setBackgroundResource(background)
        btnThemeDark.setImageResource(if (darkTheme) R.drawable.ic_dark_selected else R.drawable.ic_dark)
        btnThemeLight.setImageResource(if (darkTheme) R.drawable.ic_light else R.drawable.ic_light_selected)
        if (pageReady) webView.evaluateJavascript("window.updateTheme('${if (darkTheme) "dark" else "light"}')", null)
    }

    private fun selectCharacter(index: Int, forceApplySelection: Boolean = false) {
        selectedCharacterIndex = index
        renderCharacterButtons(index)
        if (!pageReady) return

        binding.webView.evaluateJavascript("window.switchCharacter('${CHARACTERS[index]}')") {
            if (forceApplySelection) applySelection()
        }
        if (!forceApplySelection && accessories.isNotEmpty()) {
            binding.webView.postDelayed(::applySelection, ACCESSORY_READY_DELAY_MS)
        }
    }

    private fun renderCharacterButtons(index: Int) {
        val buttons = listOf(binding.btnType1, binding.btnType2, binding.btnType3, binding.btnType4)
        buttons.forEachIndexed { buttonIndex, button ->
            val size = UnitHelper.dpToPxInt(
                resources,
                if (buttonIndex == index) 54f else 48f
            )
            button.layoutParams = button.layoutParams.apply {
                width = size
                height = size
            }
            button.setBackgroundResource(if (buttonIndex == index) R.drawable.ic_shape_slt else R.drawable.ic_shape_uslt)
        }
    }

    private fun openDownloads() {
        val entries = buildList {
            shirtUrl?.let { add(DownloadEntry(TYPE_SHIRT, it)) }
            pantUrl?.let { add(DownloadEntry(TYPE_PANT, it)) }
            accessories.forEach { (type, url) -> add(DownloadEntry(type, url, OutfitUrls.accessoryPreview(url))) }
        }
        if (entries.isEmpty()) return
        startActivity(Intent(this, OutfitDownloadActivity::class.java)
            .putExtra(OutfitDownloadActivity.EXTRA_ENTRIES, Gson().toJson(entries)))
       // finish()
    }

    private fun saveOutfitPackage() {
        val entries = buildPackageEntries()
        if (entries.isEmpty()) return
        lifecycleScope.launch {
            showLoading()
            val previewBitmap = captureOutfitPreview()
            runCatching {
                OutfitPackageRepository.savePackage(
                    context = this@OutfitViewerActivity,
                    entries = entries,
                    previewBitmap = previewBitmap.copy(Bitmap.Config.ARGB_8888, false),
                )
            }
            previewBitmap.recycle()
            dismissLoading()
            openDownloads()
        }
    }

    private fun buildPackageEntries(): List<DownloadEntry> = buildList {
        shirtUrl?.let { add(DownloadEntry(TYPE_SHIRT, it)) }
        pantUrl?.let { add(DownloadEntry(TYPE_PANT, it)) }
        accessories.forEach { (type, url) -> add(DownloadEntry(type, url, OutfitUrls.accessoryPreview(url))) }
    }

    private fun captureOutfitPreview(): Bitmap {
        val width = binding.webView.width.coerceAtLeast(1)
        val height = binding.webView.height.coerceAtLeast(1)
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
            binding.webView.draw(Canvas(bitmap))
        }
    }

    private fun interceptRemoteAsset(request: WebResourceRequest): WebResourceResponse? {
        val host = request.url.host ?: return null
        if (host != PRIMARY_HOST && host != PREVENTIVE_HOST) return null
        return runCatching {
            val connection = URL(request.url.toString()).openConnection() as HttpURLConnection
            connection.connectTimeout = 10_000
            connection.readTimeout = 20_000
            connection.connect()
            WebResourceResponse(
                connection.contentType?.substringBefore(';') ?: "application/octet-stream",
                connection.contentEncoding ?: "UTF-8",
                connection.responseCode,
                connection.responseMessage ?: "OK",
                mapOf("Access-Control-Allow-Origin" to "*", "Access-Control-Allow-Methods" to "GET, OPTIONS"),
                connection.inputStream,
            )
        }.getOrNull()
    }

    override fun onDestroy() {
        featureAnimator?.cancel()
        binding.webView.apply { stopLoading(); loadUrl("about:blank"); destroy() }
        super.onDestroy()
    }

    companion object {
        const val EXTRA_MODE = "outfit_mode"
        const val EXTRA_SHIRT_URL = "outfit_shirt_url"
        const val EXTRA_PANT_URL = "outfit_pant_url"
        const val EXTRA_ACCESSORY_TYPE = "outfit_accessory_type"
        const val EXTRA_ACCESSORY_URL = "outfit_accessory_url"
        const val TYPE_SHIRT = "shirt"
        const val TYPE_PANT = "pant"
        const val TYPE_ACCESSORY = "accessory"
        const val OUTFIT_VIEWER_URL = "https://appassets.androidplatform.net/assets/public/index.html"
        private const val DEFAULT_CLOTHES_URL = "https://lvtglobal.tech/public/app/ST253_ClothesSkinsMakerforRBX_v2/special/1.png"
        private const val PRIMARY_HOST = "lvtglobal.tech"
        private const val PREVENTIVE_HOST = "lvt-api-tech.io.vn"
        private const val WEB_APP_READY_DELAY_MS = 500L
        private const val ACCESSORY_READY_DELAY_MS = 500L
        private const val FEATURE_ANIMATION_DURATION_MS = 300L
        private const val OPTION_IMAGE = 0
        private const val OPTION_BRUSH = 1
        private const val OPTION_STICKER = 2
        private const val OPTION_EMOJI = 3
        private const val OPTION_TEXT = 4
        private val CHARACTERS = listOf("default", "man", "woman", "rounded")
    }
}
