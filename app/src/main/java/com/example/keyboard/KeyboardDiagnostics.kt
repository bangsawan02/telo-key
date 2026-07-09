package com.example.keyboard

import android.util.Log
import android.view.inputmethod.EditorInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object KeyboardDiagnostics {
    private const val TAG = "KeyboardDiagnostics"

    data class DiagnosticLog(
        val timestamp: String,
        val level: String,
        val message: String
    )

    data class DiagnosticState(
        val serviceCreated: Boolean = false,
        val currentLifecycleState: String = "DESTROYED",
        val isInputConnectionActive: Boolean = false,
        val editorPackageName: String = "None",
        val editorInputType: String = "None",
        val editorFieldId: Int = -1,
        val isServiceBound: Boolean = false,
        val logs: List<DiagnosticLog> = emptyList()
    )

    private val _state = MutableStateFlow(DiagnosticState())
    val state = _state.asStateFlow()

    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    fun log(level: String, message: String) {
        Log.d(TAG, "[$level] $message")
        val timestamp = dateFormat.format(Date())
        val newLog = DiagnosticLog(timestamp, level, message)
        _state.update { current ->
            val updatedLogs = (listOf(newLog) + current.logs).take(150) // keep last 150 logs
            current.copy(logs = updatedLogs)
        }
    }

    fun updateLifecycle(stateName: String) {
        log("LIFECYCLE", "State transition: $stateName")
        _state.update { it.copy(currentLifecycleState = stateName) }
    }

    fun updateServiceCreated(created: Boolean) {
        log("SERVICE", "Service created status: $created")
        _state.update { it.copy(serviceCreated = created) }
    }

    fun updateServiceBound(bound: Boolean) {
        log("BIND", "Service binding status changed: $bound")
        _state.update { it.copy(isServiceBound = bound) }
    }

    fun updateInputConnection(active: Boolean) {
        log("INPUT_CONNECTION", "Active status: $active")
        _state.update { it.copy(isInputConnectionActive = active) }
    }

    fun updateEditorInfo(info: EditorInfo?) {
        if (info == null) {
            log("EDITOR_INFO", "EditorInfo cleared (null)")
            _state.update {
                it.copy(
                    editorPackageName = "None",
                    editorInputType = "None",
                    editorFieldId = -1
                )
            }
        } else {
            val inputTypeStr = getEditorInputTypeString(info.inputType)
            log("EDITOR_INFO", "Focused application: ${info.packageName}, InputType: $inputTypeStr, Field ID: ${info.fieldId}")
            _state.update {
                it.copy(
                    editorPackageName = info.packageName ?: "Unknown",
                    editorInputType = inputTypeStr,
                    editorFieldId = info.fieldId
                )
            }
        }
    }

    private fun getEditorInputTypeString(inputType: Int): String {
        val mask = inputType and EditorInfo.TYPE_MASK_CLASS
        return when (mask) {
            EditorInfo.TYPE_CLASS_NUMBER -> "NUMBER"
            EditorInfo.TYPE_CLASS_DATETIME -> "DATETIME"
            EditorInfo.TYPE_CLASS_PHONE -> "PHONE"
            EditorInfo.TYPE_CLASS_TEXT -> {
                val variation = inputType and EditorInfo.TYPE_MASK_VARIATION
                when (variation) {
                    EditorInfo.TYPE_TEXT_VARIATION_PASSWORD,
                    EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
                    EditorInfo.TYPE_TEXT_VARIATION_WEB_PASSWORD -> "TEXT_PASSWORD"
                    EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
                    EditorInfo.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS -> "TEXT_EMAIL"
                    EditorInfo.TYPE_TEXT_VARIATION_URI -> "TEXT_URI"
                    else -> "TEXT"
                }
            }
            else -> "UNKNOWN ($inputType)"
        }
    }

    fun clearLogs() {
        _state.update { it.copy(logs = emptyList()) }
        log("SYSTEM", "Diagnostics logs cleared")
    }
}
