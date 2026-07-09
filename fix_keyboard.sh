#!/bin/bash
cat << 'INNEREOF' > app/src/main/java/com/example/keyboard/MyInputMethodService_appended.kt
}

@Composable
fun TabButton(
    text: String = "",
    icon: String = "",
    isActive: Boolean,
    onClick: () -> Unit,
    textColor: Color,
    activeColor: Color
) {
    val bg = if (isActive) activeColor else Color.Transparent
    Box(
        modifier = Modifier
            .height(36.dp)
            .background(bg, RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        if (text.isNotEmpty()) {
            Text(text = text, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        } else if (icon.isNotEmpty()) {
            Text(text = icon, fontSize = 16.sp)
        }
    }
}

@Composable
fun EmojiOverlay(theme: com.example.data.ThemeEntity, onKeyAction: (String) -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().height(250.dp).background(Color(theme.backgroundColor)), contentAlignment = Alignment.Center) {
        Text("Emoji Overlay", color = Color(theme.keyTextColor))
    }
}

@Composable
fun ClipboardOverlay(theme: com.example.data.ThemeEntity, onKeyAction: (String) -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().height(250.dp).background(Color(theme.backgroundColor)), contentAlignment = Alignment.Center) {
        Text("Clipboard Overlay", color = Color(theme.keyTextColor))
    }
}

fun getSymbolRows(): List<List<String>> {
    return emptyList()
}

fun getLayoutRows(layoutType: String, showNumbers: Boolean): List<List<String>> {
    return emptyList()
}

fun getKeyDisplayLabel(label: String, shiftState: Boolean): String {
    return when {
        label == "Shift" -> if (shiftState) "⬆" else "⇧"
        label == "Backspace" -> "⌫"
        label == "Enter" -> "↵"
        label == "Space" -> " "
        label == "Lang" -> "🌐"
        label == "Sym" -> "?123"
        label == "ABC" -> "ABC"
        label == "Clip" -> "📋"
        label.length == 1 && shiftState -> label.uppercase()
        else -> label
    }
}

fun getKeyWeight(label: String): Float {
    val isSpecial = label in listOf("Shift", "Backspace", "Enter", "Sym", "ABC", "Lang", "Clip")
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

// Provide the getLayoutRows and getSymbolRows implementations that use the Context
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

INNEREOF

sed -i '/} \/\/ Close the MyInputMethodService class!/,$d' app/src/main/java/com/example/keyboard/MyInputMethodService.kt
cat app/src/main/java/com/example/keyboard/MyInputMethodService_appended.kt >> app/src/main/java/com/example/keyboard/MyInputMethodService.kt
