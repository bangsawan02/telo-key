package com.example.keyboard

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.*
import androidx.savedstate.*
import com.example.ServiceLocator
import com.example.data.LayoutEntity
import com.example.data.ThemeEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.window.Popup
import androidx.compose.ui.unit.IntOffset
import com.example.data.LayoutDefaults

class MyInputMethodService : InputMethodService(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry by lazy { LifecycleRegistry(this) }
    private val store by lazy { ViewModelStore() }
    private val savedStateRegistryController by lazy { SavedStateRegistryController.create(this) }

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private val repository by lazy { ServiceLocator.getRepository(this) }
    private val scope = CoroutineScope(Dispatchers.Main)

    // Keyboard Interactive States
    private val themeState = mutableStateOf(ThemeEntity())
    private val layoutState = mutableStateOf(LayoutEntity())
    private val downloadedLanguages = mutableStateListOf<String>()
    
    private val currentWordBuffer = StringBuilder()
    private val lastWordState = mutableStateOf<String?>(null)
    private var predictionsState = mutableStateOf<List<String>>(emptyList())
    private var updatePredictionsJob: kotlinx.coroutines.Job? = null
    private var isShifted = mutableStateOf(false)
    private var isSymbols = mutableStateOf(false)
    private var currentLangCode = mutableStateOf("en")

    private fun getSavedLanguage(): String {
        val prefs = getSharedPreferences("keyboard_settings", Context.MODE_PRIVATE)
        return prefs.getString("selected_language", "en") ?: "en"
    }

    private fun saveLanguage(langCode: String) {
        val prefs = getSharedPreferences("keyboard_settings", Context.MODE_PRIVATE)
        prefs.edit().putString("selected_language", langCode).apply()
    }
    private var showDiagnosticOverlay = mutableStateOf(false)
    private val showClipboardOverlay = mutableStateOf(false)
    private val showEmojiOverlay = mutableStateOf(false)
    private val showLayoutPicker = mutableStateOf(false)
    private val selectedAuxTab = mutableStateOf<String?>(null) // "Edit", "Clip", "Fn", "Num" or null
    private val isAuxLocked = mutableStateOf(false)
    private val isSelectModeActive = mutableStateOf(false)

    private lateinit var clipboardManager: ClipboardManager
    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener {
        checkNewClip()
    }

    private fun checkNewClip() {
        try {
            if (::clipboardManager.isInitialized && clipboardManager.hasPrimaryClip()) {
                val clipData = clipboardManager.primaryClip
                if (clipData != null && clipData.itemCount > 0) {
                    val text = clipData.getItemAt(0).text?.toString()
                    if (!text.isNullOrEmpty()) {
                        scope.launch {
                            repository.addClipboardItem(text)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MyInputMethodService", "Error checking primary clip", e)
        }
    }

    override fun onCreate() {
        KeyboardDiagnostics.updateServiceCreated(true)
        KeyboardDiagnostics.updateLifecycle("ON_CREATE")
        savedStateRegistryController.performAttach()
        savedStateRegistryController.performRestore(null)
        super.onCreate()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        currentLangCode.value = getSavedLanguage()

        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        try {
            clipboardManager.addPrimaryClipChangedListener(clipListener)
        } catch (e: Exception) {
            android.util.Log.e("MyInputMethodService", "Failed to add clip listener", e)
        }

        // Observe Room theme, layout, and downloaded languages in the Service Scope
        scope.launch {
            repository.themeFlow.collectLatest { theme ->
                if (theme != null) themeState.value = theme
            }
        }
        scope.launch {
            repository.layoutFlow.collectLatest { layout ->
                if (layout != null) layoutState.value = layout
            }
        }
        scope.launch {
            repository.dictionariesFlow.collectLatest { list ->
                downloadedLanguages.clear()
                // Default English is always available, plus downloaded ones
                downloadedLanguages.add("en")
                list.forEach { dict ->
                    if (dict.isDownloaded && dict.languageCode != "en") {
                        downloadedLanguages.add(dict.languageCode)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        KeyboardDiagnostics.updateLifecycle("ON_DESTROY")
        KeyboardDiagnostics.updateServiceCreated(false)
        KeyboardDiagnostics.updateServiceBound(false)
        KeyboardDiagnostics.updateInputConnection(false)
        try {
            if (::clipboardManager.isInitialized) {
                clipboardManager.removePrimaryClipChangedListener(clipListener)
            }
        } catch (e: Exception) {
            android.util.Log.e("MyInputMethodService", "Failed to remove clip listener", e)
        }
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }

    override fun onInitializeInterface() {
        super.onInitializeInterface()
        KeyboardDiagnostics.log("SYSTEM", "onInitializeInterface() called")
    }

    override fun onEvaluateInputViewShown(): Boolean {
        KeyboardDiagnostics.log("SYSTEM", "onEvaluateInputViewShown() evaluated -> true")
        return true
    }

    override fun onBindInput() {
        super.onBindInput()
        KeyboardDiagnostics.updateServiceBound(true)
        KeyboardDiagnostics.updateInputConnection(currentInputConnection != null)
        KeyboardDiagnostics.log("SYSTEM", "onBindInput() called - service bound to target input connection")
    }

    override fun onUnbindInput() {
        super.onUnbindInput()
        KeyboardDiagnostics.updateServiceBound(false)
        KeyboardDiagnostics.updateInputConnection(false)
        KeyboardDiagnostics.log("SYSTEM", "onUnbindInput() called - service unbound")
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        KeyboardDiagnostics.updateLifecycle("ON_START_INPUT")
        KeyboardDiagnostics.updateEditorInfo(attribute)
        KeyboardDiagnostics.updateInputConnection(currentInputConnection != null)
        KeyboardDiagnostics.log("SYSTEM", "onStartInput() called - restarting=$restarting")
    }

    override fun onFinishInput() {
        super.onFinishInput()
        KeyboardDiagnostics.updateLifecycle("ON_FINISH_INPUT")
        KeyboardDiagnostics.updateEditorInfo(null)
        KeyboardDiagnostics.log("SYSTEM", "onFinishInput() called")
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        KeyboardDiagnostics.updateLifecycle("ON_START_INPUT_VIEW")
        KeyboardDiagnostics.updateEditorInfo(info)
        KeyboardDiagnostics.updateInputConnection(currentInputConnection != null)
        super.onStartInputView(info, restarting)
        currentWordBuffer.clear()
        lastWordState.value = null
        isShifted.value = false
        isSymbols.value = false
        showClipboardOverlay.value = false
        showEmojiOverlay.value = false
        selectedAuxTab.value = null
        isAuxLocked.value = false
        isSelectModeActive.value = false
        checkNewClip()
        updatePredictions() // Immediately show start predictions
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        KeyboardDiagnostics.updateLifecycle("ON_FINISH_INPUT_VIEW")
        super.onFinishInputView(finishingInput)
        currentWordBuffer.clear()
        lastWordState.value = null
        predictionsState.value = emptyList()
        showClipboardOverlay.value = false
        showEmojiOverlay.value = false
        selectedAuxTab.value = null
        isAuxLocked.value = false
        isSelectModeActive.value = false
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    }

    @Composable
    private fun LayoutPickerOverlay(
        theme: ThemeEntity,
        onLayoutSelected: (String) -> Unit,
        onLanguageSelected: (String) -> Unit,
        onClose: () -> Unit
    ) {
        val bgColor = Color(theme.backgroundColor)
        val keyColor = Color(theme.keyBackgroundColor)
        val txtColor = Color(theme.keyTextColor)
        val activeColor = Color(theme.activeKeyBackgroundColor)

        val layouts = listOf("QWERTY", "AZERTY", "QWERTZ", "DVORAK", "COLEMAK", "NUMERIC", "MiscSymbols")
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .background(bgColor)
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Select Layout / Language", color = txtColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Box(
                    modifier = Modifier
                        .background(activeColor, RoundedCornerShape(4.dp))
                        .clickable { onClose() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("Done", color = txtColor, fontSize = 12.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(modifier = Modifier.fillMaxSize()) {
                // Layouts Column
                Column(modifier = Modifier.weight(1f)) {
                    Text("Layouts", color = txtColor.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(layouts) { l ->
                            val isSelected = layoutState.value.layoutType == l
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                                    .background(if (isSelected) activeColor else keyColor, RoundedCornerShape(4.dp))
                                    .clickable { onLayoutSelected(l) }
                                    .padding(8.dp)
                            ) {
                                Text(text = l, color = txtColor, fontSize = 13.sp)
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Languages Column
                Column(modifier = Modifier.weight(1f)) {
                    Text("Languages", color = txtColor.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(downloadedLanguages) { lang ->
                            val isSelected = currentLangCode.value == lang
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                                    .background(if (isSelected) activeColor else keyColor, RoundedCornerShape(4.dp))
                                    .clickable { onLanguageSelected(lang) }
                                    .padding(8.dp)
                            ) {
                                Text(text = lang.uppercase(), color = txtColor, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onCreateInputView(): View {
        KeyboardDiagnostics.updateLifecycle("ON_CREATE_INPUT_VIEW")
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        
        window?.window?.decorView?.let { decor ->
            decor.setViewTreeLifecycleOwner(this@MyInputMethodService)
            decor.setViewTreeViewModelStoreOwner(this@MyInputMethodService)
            decor.setViewTreeSavedStateRegistryOwner(this@MyInputMethodService)
        }

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@MyInputMethodService)
            setViewTreeViewModelStoreOwner(this@MyInputMethodService)
            setViewTreeSavedStateRegistryOwner(this@MyInputMethodService)
        }

        @OptIn(ExperimentalFoundationApi::class)
        composeView.setContent {
            val theme = themeState.value
            val layout = layoutState.value
            val predictions by predictionsState
            val shifted by isShifted
            val symbols by isSymbols
            val langCode by currentLangCode
            val isDiagVisible by showDiagnosticOverlay

            // Custom Simple Theme colors parsed safely
            val bgColor = Color(theme.backgroundColor)
            val keyColor = Color(theme.keyBackgroundColor)
            val activeColor = Color(theme.activeKeyBackgroundColor)
            val txtColor = Color(theme.keyTextColor)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bgColor)
                    .padding(vertical = 6.dp)
            ) {
                // Word Prediction Bar with Diag Toggle Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .background(txtColor.copy(alpha = 0.05f))
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Language Badge indicator
                    Box(
                        modifier = Modifier
                            .background(activeColor, shape = RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = langCode.uppercase(),
                            color = txtColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Diag Trigger Button
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFE53935), shape = RoundedCornerShape(4.dp))
                            .clickable { showDiagnosticOverlay.value = !isDiagVisible }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "DIAG",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    if (!isDiagVisible) {
                        if (predictions.isEmpty()) {
                            Text(
                                text = "Type some words to see predictions...",
                                color = txtColor.copy(alpha = 0.5f),
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Start
                            )
                        } else {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                predictions.forEach { prediction ->
                                    Box(
                                        modifier = Modifier
                                            .background(activeColor.copy(alpha = 0.5f), shape = RoundedCornerShape(4.dp))
                                            .clickable { handlePredictionClicked(prediction) }
                                            .padding(horizontal = 10.dp, vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = prediction,
                                            color = txtColor,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "Diagnostic Live Logs Overlay",
                            color = Color(0xFF4CAF50),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Start
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Switcher / Toolbar Tabs Row
                if (!isDiagVisible) {
                    val activeAuxTab by selectedAuxTab
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TabButton(
                            text = "Edit",
                            isActive = activeAuxTab == "Edit" && !showEmojiOverlay.value,
                            onClick = {
                                selectedAuxTab.value = if (activeAuxTab == "Edit") null else "Edit"
                                showEmojiOverlay.value = false
                                showLayoutPicker.value = false
                            },
                            textColor = txtColor,
                            activeColor = activeColor
                        )

                        TabButton(
                            icon = "📋",
                            isActive = activeAuxTab == "Clip" && !showEmojiOverlay.value,
                            onClick = {
                                selectedAuxTab.value = if (activeAuxTab == "Clip") null else "Clip"
                                showEmojiOverlay.value = false
                                showLayoutPicker.value = false
                            },
                            textColor = txtColor,
                            activeColor = activeColor
                        )

                        TabButton(
                            text = "Fn",
                            isActive = activeAuxTab == "Fn" && !showEmojiOverlay.value,
                            onClick = {
                                selectedAuxTab.value = if (activeAuxTab == "Fn") null else "Fn"
                                showEmojiOverlay.value = false
                                showLayoutPicker.value = false
                            },
                            textColor = txtColor,
                            activeColor = activeColor
                        )

                        TabButton(
                            icon = "⌨️",
                            isActive = activeAuxTab == "Num" && !showEmojiOverlay.value,
                            onClick = {
                                selectedAuxTab.value = if (activeAuxTab == "Num") null else "Num"
                                showEmojiOverlay.value = false
                                showLayoutPicker.value = false
                            },
                            textColor = txtColor,
                            activeColor = activeColor
                        )

                        TabButton(
                            icon = "😀",
                            isActive = showEmojiOverlay.value,
                            onClick = {
                                showEmojiOverlay.value = !showEmojiOverlay.value
                                selectedAuxTab.value = null
                                showClipboardOverlay.value = false
                                showLayoutPicker.value = false
                            },
                            textColor = txtColor,
                            activeColor = activeColor
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                if (isDiagVisible) {
                    // Display Diagnostic Overlay Panel
                    val diagState by KeyboardDiagnostics.state.collectAsState()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .padding(horizontal = 8.dp)
                            .background(Color(0xFF1E1E1E), shape = RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Lifecycle: ${diagState.currentLifecycleState}",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Conn: " + if (diagState.isInputConnectionActive) "ACTIVE ✅" else "NULL ❌",
                                color = if (diagState.isInputConnectionActive) Color.Green else Color.Red,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Target App: ${diagState.editorPackageName}",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "Input Type: ${diagState.editorInputType} | ID: ${diagState.editorFieldId}",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(Color.Black, shape = RoundedCornerShape(4.dp))
                                .padding(4.dp)
                        ) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                reverseLayout = false
                            ) {
                                items(diagState.logs) { logItem ->
                                    val logColor = when (logItem.level) {
                                        "LIFECYCLE" -> Color(0xFF64B5F6)
                                        "EDITOR_INFO" -> Color(0xFFFFB74D)
                                        "INPUT_CONNECTION" -> Color(0xFF81C784)
                                        else -> Color.LightGray
                                    }
                                    Text(
                                        text = "[${logItem.timestamp}] [${logItem.level}] ${logItem.message}",
                                        color = logColor,
                                        fontSize = 10.sp,
                                        lineHeight = 12.sp
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(Color.DarkGray, shape = RoundedCornerShape(4.dp))
                                    .clickable { KeyboardDiagnostics.clearLogs() }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Clear Logs", color = Color.White, fontSize = 11.sp)
                            }
                            Box(
                                modifier = Modifier
                                    .background(Color.DarkGray, shape = RoundedCornerShape(4.dp))
                                    .clickable { showDiagnosticOverlay.value = false }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Close Diag", color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }
                } else {
                    val activeAuxTab by selectedAuxTab
                    when (activeAuxTab) {
                        "Edit" -> {
                            EditLayout(
                                theme = theme,
                                isSelectMode = isSelectModeActive.value,
                                onToggleSelectMode = { isSelectModeActive.value = !isSelectModeActive.value },
                                onKeyAction = { label -> handleEditKeyAction(label) }
                            )
                        }
                        "Clip" -> {
                            ClipboardOverlay(
                                theme = theme,
                                repository = repository,
                                onPaste = { snippet ->
                                    val connection = currentInputConnection
                                    if (connection != null) {
                                        connection.commitText(snippet, 1)
                                        resetBuffer()
                                        updatePredictions()
                                    }
                                    if (!isAuxLocked.value) {
                                        selectedAuxTab.value = null
                                    }
                                },
                                onClose = {
                                    selectedAuxTab.value = null
                                }
                            )
                        }
                        "Fn" -> {
                            FnLayout(
                                theme = theme,
                                fontScale = layout.fontScale,
                                isLocked = isAuxLocked.value,
                                onToggleLock = { isAuxLocked.value = !isAuxLocked.value },
                                onKeyAction = { label -> handleFnKeyAction(label) }
                            )
                        }
                        "Num" -> {
                            NumLayout(
                                theme = theme,
                                isLocked = isAuxLocked.value,
                                onToggleLock = { isAuxLocked.value = !isAuxLocked.value },
                                onKeyAction = { label -> handleNumKeyAction(label) }
                            )
                        }
                        else -> {
                            val clipVisible by showClipboardOverlay
                            val emojiVisible by showEmojiOverlay
                            val layoutVisible by showLayoutPicker

                            if (layoutVisible) {
                                LayoutPickerOverlay(
                                    theme = theme,
                                    onLayoutSelected = { lType ->
                                        scope.launch {
                                            val current = layoutState.value
                                            repository.saveLayout(current.copy(layoutType = lType))
                                        }
                                        showLayoutPicker.value = false
                                    },
                                    onLanguageSelected = { lang ->
                                        currentLangCode.value = lang
                                        saveLanguage(lang)
                                        lastWordState.value = null
                                        resetBuffer()
                                        updatePredictions()
                                        showLayoutPicker.value = false
                                    },
                                    onClose = {
                                        showLayoutPicker.value = false
                                    }
                                )
                            } else if (clipVisible) {
                                ClipboardOverlay(
                                    theme = theme,
                                    repository = repository,
                                    onPaste = { snippet ->
                                        val connection = currentInputConnection
                                        if (connection != null) {
                                            connection.commitText(snippet, 1)
                                            resetBuffer()
                                            updatePredictions()
                                        }
                                    },
                                    onClose = {
                                        showClipboardOverlay.value = false
                                    }
                                )
                            } else if (emojiVisible) {
                                EmojiOverlay(
                                    theme = theme,
                                    onEmojiSelected = { emoji ->
                                        val connection = currentInputConnection
                                        if (connection != null) {
                                            connection.commitText(emoji, 1)
                                            resetBuffer()
                                            updatePredictions()
                                        }
                                    },
                                    onClose = {
                                        showEmojiOverlay.value = false
                                    },
                                    onOpenLayoutPicker = {
                                        showEmojiOverlay.value = false
                                        showLayoutPicker.value = true
                                    }
                                )
                            } else {
                                // Render Rows Based on Keyboard Type (Symbols, Numbers, Alphabetic Layouts)
                                val rows = if (symbols) {
                                    getSymbolRows(androidx.compose.ui.platform.LocalContext.current)
                                } else {
                                    getLayoutRows(androidx.compose.ui.platform.LocalContext.current, layout.layoutType, layout.showNumberRow)
                                }

                                rows.forEach { row ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = (layout.keySpacingDp / 2).dp),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        row.forEach { keyLabel ->
                                            val displayLabel = getKeyDisplayLabel(keyLabel, shifted)
                                            val keyWeight = getKeyWeight(keyLabel)
                                            val secSym = getSecondarySymbol(keyLabel)

                                            val keyScope = rememberCoroutineScope()
                                            var showPopup by remember { mutableStateOf(false) }
                                            val density = androidx.compose.ui.platform.LocalDensity.current
                                            val popupOffsetPx = remember {
                                                density.run { IntOffset(0, -60.dp.roundToPx()) }
                                            }

                                            val isBackspace = keyLabel == "Backspace"
                                            val keyModifier = if (isBackspace) {
                                                Modifier
                                                    .weight(keyWeight)
                                                    .height(theme.keyHeightDp.dp)
                                                    .padding(horizontal = (layout.keySpacingDp / 2).dp)
                                                    .background(
                                                        color = keyColor,
                                                        shape = RoundedCornerShape(theme.borderRadius.dp)
                                                    )
                                                    .repeatingClickable(
                                                        scope = keyScope,
                                                        onClick = { handleKeyAction(keyLabel) }
                                                    )
                                            } else {
                                                Modifier
                                                    .weight(keyWeight)
                                                    .height(theme.keyHeightDp.dp)
                                                    .padding(horizontal = (layout.keySpacingDp / 2).dp)
                                                    .background(
                                                        color = if (keyLabel == "Space") activeColor else keyColor,
                                                        shape = RoundedCornerShape(theme.borderRadius.dp)
                                                    )
                                                    .combinedClickable(
                                                        interactionSource = remember { MutableInteractionSource() },
                                                        indication = null,
                                                        onClick = {
                                                            if (keyLabel.length == 1) {
                                                                showPopup = true
                                                                keyScope.launch {
                                                                    delay(150)
                                                                    showPopup = false
                                                                }
                                                            }
                                                            handleKeyAction(keyLabel)
                                                        },
                                                        onLongClick = {
                                                            if (secSym != null && !symbols) {
                                                                handleKeyPress(secSym)
                                                            }
                                                        }
                                                    )
                                            }

                                            Box(
                                                modifier = keyModifier,
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (showPopup && keyLabel.length == 1) {
                                                    Popup(
                                                        alignment = Alignment.TopCenter,
                                                        offset = popupOffsetPx
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .background(
                                                                    color = activeColor,
                                                                    shape = RoundedCornerShape(8.dp)
                                                                )
                                                                .padding(horizontal = 16.dp, vertical = 10.dp),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = displayLabel,
                                                                color = txtColor,
                                                                fontSize = 24.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                }
                                                Text(
                                                    text = displayLabel,
                                                    color = txtColor,
                                                    fontSize = (if (keyLabel.length > 1 && keyLabel != "Space") 14 * layout.fontScale else 18 * layout.fontScale).sp,
                                                    fontWeight = FontWeight.Medium,
                                                    textAlign = TextAlign.Center
                                                )
                                                if (secSym != null && !symbols && keyLabel.length == 1) {
                                                    Text(
                                                        text = secSym,
                                                        color = txtColor.copy(alpha = 0.4f),
                                                        fontSize = (10 * layout.fontScale).sp,
                                                        modifier = Modifier
                                                            .align(Alignment.TopEnd)
                                                            .padding(top = 2.dp, end = 4.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return composeView
    }

    private fun handleEditKeyAction(label: String) {
        val connection = currentInputConnection ?: return
        val selectMode = isSelectModeActive.value

        fun sendKeyWithSelect(keyCode: Int) {
            if (selectMode) {
                connection.sendKeyEvent(KeyEvent(System.currentTimeMillis(), System.currentTimeMillis(), KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SHIFT_LEFT, 0, 0))
                connection.sendKeyEvent(KeyEvent(System.currentTimeMillis(), System.currentTimeMillis(), KeyEvent.ACTION_DOWN, keyCode, 0, KeyEvent.META_SHIFT_ON))
                connection.sendKeyEvent(KeyEvent(System.currentTimeMillis(), System.currentTimeMillis(), KeyEvent.ACTION_UP, keyCode, 0, KeyEvent.META_SHIFT_ON))
                connection.sendKeyEvent(KeyEvent(System.currentTimeMillis(), System.currentTimeMillis(), KeyEvent.ACTION_UP, KeyEvent.KEYCODE_SHIFT_LEFT, 0, 0))
            } else {
                connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
                connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
            }
        }

        when (label) {
            "✂️", "Cut" -> {
                connection.performContextMenuAction(android.R.id.cut)
            }
            "📋", "Paste" -> {
                connection.performContextMenuAction(android.R.id.paste)
            }
            "▲" -> {
                sendKeyWithSelect(KeyEvent.KEYCODE_DPAD_UP)
            }
            "📄", "Copy" -> {
                connection.performContextMenuAction(android.R.id.copy)
            }
            "⌨️▼", "Hide" -> {
                requestHideSelf(0)
            }
            "↪️", "Redo" -> {
                connection.sendKeyEvent(KeyEvent(System.currentTimeMillis(), System.currentTimeMillis(), KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_CTRL_LEFT, 0, 0))
                connection.sendKeyEvent(KeyEvent(System.currentTimeMillis(), System.currentTimeMillis(), KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_Y, 0, KeyEvent.META_CTRL_ON))
                connection.sendKeyEvent(KeyEvent(System.currentTimeMillis(), System.currentTimeMillis(), KeyEvent.ACTION_UP, KeyEvent.KEYCODE_Y, 0, KeyEvent.META_CTRL_ON))
                connection.sendKeyEvent(KeyEvent(System.currentTimeMillis(), System.currentTimeMillis(), KeyEvent.ACTION_UP, KeyEvent.KEYCODE_CTRL_LEFT, 0, 0))
            }
            "◀" -> {
                sendKeyWithSelect(KeyEvent.KEYCODE_DPAD_LEFT)
            }
            "▶" -> {
                sendKeyWithSelect(KeyEvent.KEYCODE_DPAD_RIGHT)
            }
            "↩️", "Undo" -> {
                connection.sendKeyEvent(KeyEvent(System.currentTimeMillis(), System.currentTimeMillis(), KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_CTRL_LEFT, 0, 0))
                connection.sendKeyEvent(KeyEvent(System.currentTimeMillis(), System.currentTimeMillis(), KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_Z, 0, KeyEvent.META_CTRL_ON))
                connection.sendKeyEvent(KeyEvent(System.currentTimeMillis(), System.currentTimeMillis(), KeyEvent.ACTION_UP, KeyEvent.KEYCODE_Z, 0, KeyEvent.META_CTRL_ON))
                connection.sendKeyEvent(KeyEvent(System.currentTimeMillis(), System.currentTimeMillis(), KeyEvent.ACTION_UP, KeyEvent.KEYCODE_CTRL_LEFT, 0, 0))
            }
            "SelAll", "All" -> {
                connection.performContextMenuAction(android.R.id.selectAll)
            }
            "|◀", "Home" -> {
                sendKeyWithSelect(KeyEvent.KEYCODE_MOVE_HOME)
            }
            "▼" -> {
                sendKeyWithSelect(KeyEvent.KEYCODE_DPAD_DOWN)
            }
            "▶|", "End" -> {
                sendKeyWithSelect(KeyEvent.KEYCODE_MOVE_END)
            }
            "⌫", "Del" -> {
                connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_FORWARD_DEL))
                connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_FORWARD_DEL))
            }
            "PgUp" -> {
                sendKeyWithSelect(KeyEvent.KEYCODE_PAGE_UP)
            }
            "_" -> {
                connection.commitText("_", 1)
            }
            "PgDn" -> {
                sendKeyWithSelect(KeyEvent.KEYCODE_PAGE_DOWN)
            }
            "↵" -> {
                connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
            }
        }
        
        if (!isAuxLocked.value && label !in listOf("▲", "◀", "▶", "▼", "Home", "End", "PgUp", "PgDn", "|◀", "▶|", "⛶")) {
            selectedAuxTab.value = null
        }
    }

    private fun handleFnKeyAction(label: String) {
        val connection = currentInputConnection ?: return
        when (label) {
            "Esc" -> {
                connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ESCAPE))
                connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ESCAPE))
            }
            "F1" -> sendFnKey(KeyEvent.KEYCODE_F1)
            "F2" -> sendFnKey(KeyEvent.KEYCODE_F2)
            "F3" -> sendFnKey(KeyEvent.KEYCODE_F3)
            "F4" -> sendFnKey(KeyEvent.KEYCODE_F4)
            "F5" -> sendFnKey(KeyEvent.KEYCODE_F5)
            "F6" -> sendFnKey(KeyEvent.KEYCODE_F6)
            "F7" -> sendFnKey(KeyEvent.KEYCODE_F7)
            "F8" -> sendFnKey(KeyEvent.KEYCODE_F8)
            "F9" -> sendFnKey(KeyEvent.KEYCODE_F9)
            "F10" -> sendFnKey(KeyEvent.KEYCODE_F10)
            "F11" -> sendFnKey(KeyEvent.KEYCODE_F11)
            "F12" -> sendFnKey(KeyEvent.KEYCODE_F12)
            "Info" -> {
                showDiagnosticOverlay.value = !showDiagnosticOverlay.value
            }
            "⚙️" -> {
                try {
                    val intent = packageManager.getLaunchIntentForPackage(packageName)
                    intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                } catch (e: Exception) {
                    android.util.Log.e("MyInputMethodService", "Error launching settings activity", e)
                }
            }
            "⇥" -> {
                connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_TAB))
                connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_TAB))
            }
            "^C" -> {
                connection.performContextMenuAction(android.R.id.copy)
            }
            "date" -> {
                val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                connection.commitText(dateStr, 1)
            }
            "Caps" -> {
                connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_CAPS_LOCK))
                connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_CAPS_LOCK))
            }
            "SysRq" -> {
                connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SYSRQ))
                connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_SYSRQ))
            }
            "time" -> {
                val timeStr = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                connection.commitText(timeStr, 1)
            }
            "Insert" -> {
                connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_INSERT))
                connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_INSERT))
            }
            "ScrLck" -> {
                connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SCROLL_LOCK))
                connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_SCROLL_LOCK))
            }
            "find" -> {
                connection.performEditorAction(EditorInfo.IME_ACTION_SEARCH)
            }
            "Ctrl" -> {
                connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_CTRL_LEFT))
                connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_CTRL_LEFT))
            }
            "_" -> {
                connection.commitText("_", 1)
            }
            "Alt" -> {
                connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ALT_LEFT))
                connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ALT_LEFT))
            }
            "NumLck" -> {
                connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_NUM_LOCK))
                connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_NUM_LOCK))
            }
            "↵" -> {
                connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
            }
        }

        if (!isAuxLocked.value && label !in listOf("Ctrl", "Alt", "Caps", "NumLck")) {
            selectedAuxTab.value = null
        }
    }

    private fun sendFnKey(keyCode: Int) {
        val connection = currentInputConnection ?: return
        connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
    }

    private fun handleNumKeyAction(label: String) {
        val connection = currentInputConnection ?: return
        when (label) {
            "⌫" -> {
                connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
                connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
            }
            "↵" -> {
                connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
            }
            else -> {
                connection.commitText(label, 1)
            }
        }
        if (!isAuxLocked.value && label !in listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0", ",", ".", "+", "-", "*", "/", "#")) {
            selectedAuxTab.value = null
        }
    }

    private fun handleKeyAction(keyLabel: String) {
        when (keyLabel) {
            "Shift" -> {
                isShifted.value = !isShifted.value
            }
            "Backspace" -> {
                handleBackspace()
            }
            "Space" -> {
                handleSpace()
            }
            "Enter" -> {
                handleEnter()
            }
            "Sym" -> {
                isSymbols.value = true
            }
            "ABC" -> {
                isSymbols.value = false
            }
            "Layout" -> {
                showLayoutPicker.value = !showLayoutPicker.value
                showEmojiOverlay.value = false
                showClipboardOverlay.value = false
                selectedAuxTab.value = null
            }
            "Emoji" -> {
                showEmojiOverlay.value = !showEmojiOverlay.value
                showClipboardOverlay.value = false
            }
            else -> {
                val typed = if (isShifted.value && keyLabel.length == 1) keyLabel.uppercase() else keyLabel
                handleKeyPress(typed)
                if (isShifted.value) {
                    isShifted.value = false // Auto-lowercase after typing one capital
                }
            }
        }
    }

    private fun handleKeyPress(text: String) {
        val connection = currentInputConnection ?: return
        connection.commitText(text, 1)

        // Build word buffer to feed predictions
        if (text.length == 1 && text[0].isLetter()) {
            currentWordBuffer.append(text)
            updatePredictions()
        } else {
            lastWordState.value = null
            resetBuffer()
            updatePredictions()
        }
    }

    private fun handleBackspace() {
        val connection = currentInputConnection ?: return
        connection.deleteSurroundingText(1, 0)

        if (currentWordBuffer.isNotEmpty()) {
            currentWordBuffer.deleteCharAt(currentWordBuffer.length - 1)
            updatePredictions()
        } else {
            if (lastWordState.value != null || predictionsState.value.isNotEmpty()) {
                lastWordState.value = null
                resetBuffer()
                updatePredictions()
            }
        }
    }

    private fun handleSpace() {
        val connection = currentInputConnection ?: return
        connection.commitText(" ", 1)
        if (currentWordBuffer.isNotEmpty()) {
            val finishedWord = currentWordBuffer.toString()
            val prev = lastWordState.value
            scope.launch {
                repository.learnWord(finishedWord)
                if (prev != null) {
                    repository.learnBigram(prev, finishedWord, currentLangCode.value)
                }
            }
            lastWordState.value = finishedWord
            resetBuffer()
            updatePredictions()
        } else {
            lastWordState.value = null
            updatePredictions()
        }
    }

    private fun handleEnter() {
        val connection = currentInputConnection ?: return
        connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
        connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
        lastWordState.value = null
        resetBuffer()
        updatePredictions()
    }

    private fun handlePredictionClicked(predictedWord: String) {
        val connection = currentInputConnection ?: return
        connection.deleteSurroundingText(currentWordBuffer.length, 0)
        connection.commitText("$predictedWord ", 1)
        
        val prev = lastWordState.value
        scope.launch {
            repository.learnWord(predictedWord)
            if (prev != null) {
                repository.learnBigram(prev, predictedWord, currentLangCode.value)
            }
        }
        lastWordState.value = predictedWord
        resetBuffer()
        updatePredictions()
    }

    private fun resetBuffer() {
        currentWordBuffer.clear()
        predictionsState.value = emptyList()
    }

    private fun updatePredictions() {
        updatePredictionsJob?.cancel()
        val query = currentWordBuffer.toString()
        updatePredictionsJob = scope.launch {
            val list = if (query.isEmpty()) {
                repository.getNextWordPredictions(lastWordState.value, currentLangCode.value)
            } else {
                repository.searchPredictions(query, currentLangCode.value)
            }
            // Adjust casing of predictions based on typed prefix casing or shift state
            val adjustedList = list.map { word ->
                when {
                    query.isNotEmpty() && query.all { it.isUpperCase() } -> word.uppercase()
                    isShifted.value || (query.isNotEmpty() && query[0].isUpperCase()) -> {
                        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                    }
                    else -> word
                }
            }
            predictionsState.value = adjustedList
        }
    }

    private fun cycleLanguages() {
        if (downloadedLanguages.isEmpty()) return
        val current = currentLangCode.value
        val currentIndex = downloadedLanguages.indexOf(current)
        val nextIndex = if (currentIndex == -1) 0 else (currentIndex + 1) % downloadedLanguages.size
        val nextLang = downloadedLanguages[nextIndex]
        currentLangCode.value = nextLang
        saveLanguage(nextLang)
        lastWordState.value = null
        resetBuffer()
        updatePredictions()
    }

}
