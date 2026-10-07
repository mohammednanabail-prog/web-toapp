package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.builder.ApkBuildManager
import com.example.model.AppBuildConfig
import com.example.model.BuildState
import com.example.model.BuiltAppRecord
import com.example.model.SampleTemplates
import com.example.model.SourceType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val buildManager = ApkBuildManager(application)

    private val _sourceType = MutableStateFlow(SourceType.URL)
    val sourceType: StateFlow<SourceType> = _sourceType.asStateFlow()

    private val _urlInput = MutableStateFlow("https://")
    val urlInput: StateFlow<String> = _urlInput.asStateFlow()

    private val _htmlCode = MutableStateFlow(SampleTemplates.MINI_GAME)
    val htmlCode: StateFlow<String> = _htmlCode.asStateFlow()

    private val _cssCode = MutableStateFlow("")
    val cssCode: StateFlow<String> = _cssCode.asStateFlow()

    private val _jsCode = MutableStateFlow("")
    val jsCode: StateFlow<String> = _jsCode.asStateFlow()

    private val _appName = MutableStateFlow("تطبيقي الذكي")
    val appName: StateFlow<String> = _appName.asStateFlow()

    private val _customPackageName = MutableStateFlow("")
    val customPackageName: StateFlow<String> = _customPackageName.asStateFlow()

    private val _iconBitmap = MutableStateFlow<Bitmap?>(null)
    val iconBitmap: StateFlow<Bitmap?> = _iconBitmap.asStateFlow()

    private val _enableDownloads = MutableStateFlow(true)
    val enableDownloads: StateFlow<Boolean> = _enableDownloads.asStateFlow()

    private val _enableShare = MutableStateFlow(true)
    val enableShare: StateFlow<Boolean> = _enableShare.asStateFlow()

    private val _enablePrint = MutableStateFlow(true)
    val enablePrint: StateFlow<Boolean> = _enablePrint.asStateFlow()

    private val _enableOfflineCache = MutableStateFlow(true)
    val enableOfflineCache: StateFlow<Boolean> = _enableOfflineCache.asStateFlow()

    private val _buildState = MutableStateFlow<BuildState>(BuildState.Idle)
    val buildState: StateFlow<BuildState> = _buildState.asStateFlow()

    private val _history = MutableStateFlow<List<BuiltAppRecord>>(emptyList())
    val history: StateFlow<List<BuiltAppRecord>> = _history.asStateFlow()

    init {
        loadHistory()
    }

    fun setSourceType(type: SourceType) {
        _sourceType.value = type
    }

    fun setUrl(url: String) {
        _urlInput.value = url
    }

    fun setHtmlCode(code: String) {
        _htmlCode.value = code
    }

    fun setCssCode(code: String) {
        _cssCode.value = code
    }

    fun setJsCode(code: String) {
        _jsCode.value = code
    }

    fun setAppName(name: String) {
        _appName.value = name
    }

    fun setCustomPackageName(pkg: String) {
        _customPackageName.value = pkg
    }

    fun setIconBitmap(bitmap: Bitmap?) {
        _iconBitmap.value = bitmap
    }

    fun toggleDownloads(enabled: Boolean) {
        _enableDownloads.value = enabled
    }

    fun toggleShare(enabled: Boolean) {
        _enableShare.value = enabled
    }

    fun togglePrint(enabled: Boolean) {
        _enablePrint.value = enabled
    }

    fun toggleOfflineCache(enabled: Boolean) {
        _enableOfflineCache.value = enabled
    }

    fun loadTemplate(templateCode: String) {
        _htmlCode.value = templateCode
        _cssCode.value = ""
        _jsCode.value = ""
    }

    fun resetBuildState() {
        _buildState.value = BuildState.Idle
    }

    fun loadHistory() {
        viewModelScope.launch {
            _history.value = buildManager.getBuildHistory()
        }
    }

    fun deleteHistoryItem(item: BuiltAppRecord) {
        viewModelScope.launch {
            buildManager.deleteHistoryRecord(item)
            loadHistory()
        }
    }

    fun startBuild() {
        val currentAppName = _appName.value.trim().ifEmpty { "WebApp" }
        val currentUrl = _urlInput.value.trim()

        if (_sourceType.value == SourceType.URL && (currentUrl.isEmpty() || currentUrl == "https://")) {
            _buildState.value = BuildState.Error("يرجى إدخال رابط ويب صالح (URL) للبدء!")
            return
        }

        if (_sourceType.value == SourceType.HTML_CODE && _htmlCode.value.isBlank()) {
            _buildState.value = BuildState.Error("يرجى كتابة أو لصق كود HTML للبدء!")
            return
        }

        val config = AppBuildConfig(
            appName = currentAppName,
            sourceType = _sourceType.value,
            url = if (currentUrl.startsWith("http://") || currentUrl.startsWith("https://")) currentUrl else "https://$currentUrl",
            htmlCode = _htmlCode.value,
            cssCode = _cssCode.value,
            jsCode = _jsCode.value,
            customPackageName = _customPackageName.value,
            iconBitmap = _iconBitmap.value,
            enableDownloads = _enableDownloads.value,
            enableShare = _enableShare.value,
            enablePrint = _enablePrint.value,
            enableOfflineCache = _enableOfflineCache.value
        )

        viewModelScope.launch {
            buildManager.buildApk(config).collect { state ->
                _buildState.value = state
                if (state is BuildState.Success) {
                    loadHistory()
                }
            }
        }
    }
}
