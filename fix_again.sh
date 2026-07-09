#!/bin/bash
cat << 'INNEREOF' >> app/src/main/java/com/example/keyboard/MyInputMethodService.kt

} // End of MyInputMethodService class

@Composable
fun TabButton(
    text: String = "",
    icon: String = "",
    isActive: Boolean,
    onClick: () -> Unit,
    textColor: androidx.compose.ui.graphics.Color,
    activeColor: androidx.compose.ui.graphics.Color
) {
    val bg = if (isActive) activeColor else androidx.compose.ui.graphics.Color.Transparent
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .height(36.dp)
            .background(bg, androidx.compose.foundation.shape.RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        if (text.isNotEmpty()) {
            androidx.compose.material3.Text(text = text, color = textColor, fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        } else if (icon.isNotEmpty()) {
            androidx.compose.material3.Text(text = icon, fontSize = 16.sp)
        }
    }
}

@Composable
fun EmojiOverlay(theme: com.example.data.ThemeEntity, onKeyAction: (String) -> Unit) {
    androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxWidth().height(250.dp).background(androidx.compose.ui.graphics.Color(theme.backgroundColor)), contentAlignment = androidx.compose.ui.Alignment.Center) {
        androidx.compose.material3.Text("Emoji Overlay", color = androidx.compose.ui.graphics.Color(theme.keyTextColor))
    }
}

@Composable
fun ClipboardOverlay(theme: com.example.data.ThemeEntity, onKeyAction: (String) -> Unit) {
    androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxWidth().height(250.dp).background(androidx.compose.ui.graphics.Color(theme.backgroundColor)), contentAlignment = androidx.compose.ui.Alignment.Center) {
        androidx.compose.material3.Text("Clipboard Overlay", color = androidx.compose.ui.graphics.Color(theme.keyTextColor))
    }
}

fun getSymbolRows(context: android.content.Context): List<List<String>> {
    return com.example.data.LayoutDefaults.getCustomLayout(context, "SymbolLayout", com.example.data.LayoutDefaults.defaults["SymbolLayout"])
}

fun getLayoutRows(context: android.content.Context, layoutType: String, showNumbers: Boolean): List<List<String>> {
    val baseRows = mutableListOf<List<String>>()

    if (showNumbers && layoutType != "NUMERIC") {
        val numRows = com.example.data.LayoutDefaults.getCustomLayout(context, "NumberRow", com.example.data.LayoutDefaults.defaults["NumberRow"])
        if (numRows.isNotEmpty()) {
            baseRows.add(numRows[0])
        }
    }

    val fallbackType = if (com.example.data.LayoutDefaults.defaults.containsKey(layoutType)) layoutType else "QWERTY"
    val customRows = com.example.data.LayoutDefaults.getCustomLayout(context, fallbackType, com.example.data.LayoutDefaults.defaults[fallbackType])
    baseRows.addAll(customRows)

    return baseRows
}

fun getKeyDisplayLabel(label: String, shiftState: ShiftState): String {
    return when {
        label == "Shift" -> when(shiftState) {
            ShiftState.UNSHIFTED -> "⇧"
            ShiftState.SHIFTED -> "⬆"
            ShiftState.CAPS_LOCK -> "⇪"
        }
        label == "Backspace" -> "⌫"
        label == "Enter" -> "↵"
        label == "Space" -> " "
        label == "Lang" -> "🌐"
        label == "Sym" -> "?123"
        label == "ABC" -> "ABC"
        label == "Clip" -> "📋"
        label.length == 1 && (shiftState == ShiftState.SHIFTED || shiftState == ShiftState.CAPS_LOCK) -> label.uppercase()
        else -> label
    }
}

fun getKeyWeight(label: String, isSpecial: Boolean): Float {
    return if (label == "Space") 4f else if (isSpecial) 1.5f else 1f
}

fun getSecondarySymbol(label: String): String? {
    return null
}

fun Modifier.repeatingClickable(
    interactionSource: androidx.compose.foundation.interaction.MutableInteractionSource,
    enabled: Boolean = true,
    initialDelayMillis: Long = 500,
    maxDelayMillis: Long = 100,
    onClick: () -> Unit
): Modifier = this.clickable { onClick() }

INNEREOF
