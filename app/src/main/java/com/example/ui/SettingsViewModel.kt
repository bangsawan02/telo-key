package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ServiceLocator
import com.example.data.DictionaryEntity
import com.example.data.LayoutEntity
import com.example.data.ThemeEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ServiceLocator.getRepository(application)

    val themeState: StateFlow<ThemeEntity> = repository.themeFlow
        .filterNotNull()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemeEntity()
        )

    val layoutState: StateFlow<LayoutEntity> = repository.layoutFlow
        .filterNotNull()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LayoutEntity()
        )

    val dictionariesState: StateFlow<List<DictionaryEntity>> = repository.dictionariesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _downloadingState = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val downloadingState: StateFlow<Map<String, Boolean>> = _downloadingState.asStateFlow()

    private val _downloadStatusMessage = MutableStateFlow<String?>(null)
    val downloadStatusMessage: StateFlow<String?> = _downloadStatusMessage.asStateFlow()

    fun updateTheme(
        backgroundColor: Long? = null,
        keyBackgroundColor: Long? = null,
        keyTextColor: Long? = null,
        activeKeyBackgroundColor: Long? = null,
        borderRadius: Int? = null,
        keyHeightDp: Int? = null
    ) {
        viewModelScope.launch {
            val current = themeState.value
            val updated = current.copy(
                backgroundColor = backgroundColor ?: current.backgroundColor,
                keyBackgroundColor = keyBackgroundColor ?: current.keyBackgroundColor,
                keyTextColor = keyTextColor ?: current.keyTextColor,
                activeKeyBackgroundColor = activeKeyBackgroundColor ?: current.activeKeyBackgroundColor,
                borderRadius = borderRadius ?: current.borderRadius,
                keyHeightDp = keyHeightDp ?: current.keyHeightDp
            )
            repository.saveTheme(updated)
        }
    }

    fun updateLayout(
        layoutType: String? = null,
        showNumberRow: Boolean? = null,
        keySpacingDp: Int? = null,
        fontScale: Float? = null
    ) {
        viewModelScope.launch {
            val current = layoutState.value
            val updated = current.copy(
                layoutType = layoutType ?: current.layoutType,
                showNumberRow = showNumberRow ?: current.showNumberRow,
                keySpacingDp = keySpacingDp ?: current.keySpacingDp,
                fontScale = fontScale ?: current.fontScale
            )
            repository.saveLayout(updated)
        }
    }

    fun downloadDictionary(dict: DictionaryEntity) {
        viewModelScope.launch {
            _downloadingState.update { it + (dict.languageCode to true) }
            _downloadStatusMessage.value = "Downloading ${dict.languageName}..."
            val result = repository.downloadDictionary(dict.languageCode, dict.downloadUrl)
            _downloadingState.update { it + (dict.languageCode to false) }
            _downloadStatusMessage.value = if (result.isSuccess) {
                "${dict.languageName} downloaded successfully! (${result.getOrNull()} words)"
            } else {
                "Failed to download ${dict.languageName}. Loaded local offline words instead."
            }
        }
    }

    fun deleteDictionary(dict: DictionaryEntity) {
        viewModelScope.launch {
            repository.deleteDictionary(dict.languageCode)
            _downloadStatusMessage.value = "Deleted ${dict.languageName} dictionary."
        }
    }

    fun addDictionary(languageCode: String, languageName: String, downloadUrl: String) {
        viewModelScope.launch {
            if (languageCode.isBlank() || languageName.isBlank() || downloadUrl.isBlank()) {
                _downloadStatusMessage.value = "Semua bidang (Kode, Nama, URL) harus diisi."
                return@launch
            }
            val cleanCode = languageCode.lowercase().trim()
            val existing = dictionariesState.value.any { it.languageCode == cleanCode }
            if (existing) {
                _downloadStatusMessage.value = "Kamus dengan kode '$cleanCode' sudah ada."
                return@launch
            }
            val newDict = DictionaryEntity(
                languageCode = cleanCode,
                languageName = languageName.trim(),
                isDownloaded = false,
                wordCount = 0,
                downloadUrl = downloadUrl.trim()
            )
            repository.addDictionary(newDict)
            _downloadStatusMessage.value = "Berhasil menambahkan kamus ${newDict.languageName}. Silakan unduh."
        }
    }

    fun removeDictionary(dict: DictionaryEntity) {
        viewModelScope.launch {
            repository.removeDictionary(dict.languageCode)
            _downloadStatusMessage.value = "Berhasil menghapus kamus ${dict.languageName} sepenuhnya."
        }
    }

    fun clearStatusMessage() {
        _downloadStatusMessage.value = null
    }
}
