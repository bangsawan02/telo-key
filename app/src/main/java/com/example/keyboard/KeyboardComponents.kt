package com.example.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import com.example.data.ThemeEntity
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

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

data class EmojiCategory(
    val name: String,
    val icon: String,
    val emojis: List<String>
)

@Composable
fun EmojiOverlay(theme: ThemeEntity, onEmojiSelected: (String) -> Unit, onClose: () -> Unit, onOpenLayoutPicker: () -> Unit) {
    val categories = remember {
        listOf(
            EmojiCategory("Smileys", "😀", listOf(
                "😀", "😂", "🤣", "😊", "😍", "🥰", "😎", "🤔", "😅", "😆", "😉", "😋",
                "😘", "😗", "😙", "😚", "🙂", "🤗", "🤩", "😶", "🙄", "😏", "😣", "😥",
                "😮", "🤐", "😯", "😪", "😫", "🥱", "😴", "😌", "😛", "😜", "😝", "🤤",
                "😒", "😓", "😔", "😕", "🙃", "🤑", "😲", "☹️", "🙁", "😖", "😞", "😟",
                "😤", "😢", "😭", "😦", "😧", "😨", "😩", "🤯", "😬", "😰", "😱", "🥵",
                "🥶", "😳", "🤪", "😵", "😡", "😠", "🤬", "😷", "🤒", "🤕", "🤢", "🤮"
            )),
            EmojiCategory("Animals", "🐶", listOf(
                "🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼", "🐨", "🐯", "🦁", "🐮",
                "🐷", "🐸", "🐵", "🐔", "🐧", "🐦", "🦆", "🦅", "🦉", "🦇", "🐺", "🐗",
                "🐴", "🦄", "🐝", "🐛", "🦋", "🐌", "🐞", "🐜", "🦟", "🦗", "🕷", "🕸",
                "🦂", "🐢", "🐍", "🦎", "🐙", "🦑", "🦞", "🦀", "🐡", "🐠", "🐟", "🐬"
            )),
            EmojiCategory("Food", "🍎", listOf(
                "🍏", "🍎", "🍐", "🍊", "🍋", "🍌", "🍉", "🍇", "🍓", "🫐", "🍈", "🍒",
                "🍑", "🥭", "🍍", "🥥", "🥝", "🍅", "🍆", "🥑", "🥦", "🥬", "🥒", "🌶",
                "🍆", "🌽", "🥕", "🫒", "🥔", "🍠", "🥐", "🥯", "🍞", "🥖", "🥨", "🧀",
                "🥚", "🍳", "🧈", "🥞", "🧇", "🥓", "🥩", "🍗", "🍖", "🌭", "🍔", "🍟"
            )),
            EmojiCategory("Sports", "⚽", listOf(
                "⚽", "🏀", "🏈", "⚾", "🥎", "🎾", "🏐", "🏉", "🎱", "🪀", "🏓", "🏸",
                "🏒", "🏑", "🥍", "🏏", "🪃", "🥅", "⛳", "🪁", "🏹", "🎣", "🤿", "🥊",
                "🥋", "🎽", "🛹", "🛼", "🛷", "⛸", "🎿", "🏂", "🏋️", "🤸‍♀️", "🧘"
            )),
            EmojiCategory("Travel", "🚗", listOf(
                "🚗", "🚕", "🚙", "🚌", "🚎", "🏎️", "🚓", "🚑", "🚒", "🚐", "🛻", "🚚",
                "🚛", "🚜", "🛵", "🏍️", "🚲", "🚨", "🚥", "🚦", "🛑", "🚧", "⚓",
                "⛵", "🛶", "✈️", "🚀", "🚁", "🚂", "🚇", "🚊", "🚉", "🚡", "🚠", "🛸"
            )),
            EmojiCategory("Objects", "💡", listOf(
                "⌚", "📱", "💻", "⌨️", "🖱️", "💿", "🎥", "🎬", "📺", "📷", "🔦", "🕯️",
                "💡", "📕", "📖", "✉️", "📦", "✏️", "🔑", "🔒", "❤️", "🧡", "💛", "💚",
                "💙", "💜", "🖤", "🤍", "🤎", "💖", "🌟", "⭐", "✨", "💥", "🔥", "💨"
            ))
        )
    }

    var selectedCategoryIndex by remember { mutableStateOf(0) }
    val currentCategory = categories[selectedCategoryIndex]

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .background(Color(theme.backgroundColor))
    ) {
        // Categories & Close Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(Color(theme.activeKeyBackgroundColor)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category list (Horizontal scroll)
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentPadding = PaddingValues(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(categories.size) { index ->
                    val cat = categories[index]
                    val isSelected = index == selectedCategoryIndex
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (isSelected) Color(theme.backgroundColor) else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedCategoryIndex = index }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = cat.icon, fontSize = 16.sp)
                            Text(
                                text = cat.name,
                                color = Color(theme.keyTextColor),
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // Close Button
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .clickable { onClose() }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Close",
                    color = Color(theme.keyTextColor),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
            
            // Layout Button
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .clickable { onOpenLayoutPicker() }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "⌨️", fontSize = 16.sp)
            }
        }

        // Emoji Grid
        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
            columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(minSize = 44.dp),
            contentPadding = PaddingValues(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(currentCategory.emojis.size) { index ->
                val emoji = currentCategory.emojis[index]
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clickable { onEmojiSelected(emoji) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = emoji, fontSize = 24.sp)
                }
            }
        }
    }
}

@Composable
fun ClipboardOverlay(theme: ThemeEntity, repository: com.example.data.KeyboardRepository, onPaste: (String) -> Unit, onClose: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().height(250.dp).background(Color(theme.backgroundColor)), contentAlignment = Alignment.Center) {
        Text("Clipboard Overlay", color = Color(theme.keyTextColor))
    }
}

fun getSymbolRows(context: android.content.Context): List<List<String>> {
    return com.example.data.LayoutDefaults.getCustomLayout(context, "SymbolLayout", com.example.data.LayoutDefaults.defaults["SymbolLayout"] ?: "[]")
}

fun getLayoutRows(context: android.content.Context, layoutType: String, showNumbers: Boolean): List<List<String>> {
    val baseRows = mutableListOf<List<String>>()

    if (showNumbers && layoutType != "NUMERIC") {
        val numRows = com.example.data.LayoutDefaults.getCustomLayout(context, "NumberRow", com.example.data.LayoutDefaults.defaults["NumberRow"] ?: "[]")
        if (numRows.isNotEmpty()) {
            baseRows.add(numRows[0])
        }
    }

    val fallbackType = if (com.example.data.LayoutDefaults.defaults.containsKey(layoutType)) layoutType else "QWERTY"
    val customRows = com.example.data.LayoutDefaults.getCustomLayout(context, fallbackType, com.example.data.LayoutDefaults.defaults[fallbackType] ?: "[]")
    baseRows.addAll(customRows)

    return baseRows
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
        label == "Layout" -> "⌨️"
        label == "Clip" -> "📋"
        label.length == 1 && shiftState -> label.uppercase()
        else -> label
    }
}

fun getKeyWeight(label: String): Float {
    val isSpecial = label in listOf("Shift", "Backspace", "Enter", "Sym", "ABC", "Lang", "Clip", "Layout")
    return if (label == "Space") 3f else if (isSpecial) 1.5f else 1f
}

fun getSecondarySymbol(label: String): String? {
    return when (label.lowercase()) {
        "q" -> "1"
        "w" -> "2"
        "e" -> "3"
        "r" -> "4"
        "t" -> "5"
        "y" -> "6"
        "u" -> "7"
        "i" -> "8"
        "o" -> "9"
        "p" -> "0"
        "a" -> "@"
        "s" -> "#"
        "d" -> "$"
        "f" -> "%"
        "g" -> "&"
        "h" -> "-"
        "j" -> "+"
        "k" -> "("
        "l" -> ")"
        "z" -> "*"
        "x" -> "\""
        "c" -> "'"
        "v" -> ":"
        "b" -> ";"
        "n" -> "!"
        "m" -> "?"
        else -> null
    }
}

fun Modifier.repeatingClickable(
    scope: kotlinx.coroutines.CoroutineScope,
    onClick: () -> Unit
): Modifier = composed {
    val currentClickListener by rememberUpdatedState(onClick)
    pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            currentClickListener()
            val job = scope.launch {
                delay(500)
                while (true) {
                    currentClickListener()
                    delay(100)
                }
            }
            waitForUpOrCancellation()
            job.cancel()
        }
    }
}

@Composable
fun EditLayout(
    theme: ThemeEntity,
    isSelectMode: Boolean,
    onToggleSelectMode: () -> Unit,
    onKeyAction: (String) -> Unit
) {
    val keyColor = Color(theme.keyBackgroundColor)
    val activeColor = Color(theme.activeKeyBackgroundColor)
    val txtColor = Color(theme.keyTextColor)
    val context = androidx.compose.ui.platform.LocalContext.current

    val rows = com.example.data.LayoutDefaults.getCustomLayout(context, "EditLayout", com.example.data.LayoutDefaults.defaults["EditLayout"] ?: "[]")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                row.forEach { label ->
                    val isAction = label in listOf("SelAll", "Undo", "Redo", "✂️", "📋", "📄", "All")
                    val bg = if (isAction) activeColor else keyColor
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .background(bg, RoundedCornerShape(8.dp))
                            .clickable { onKeyAction(label) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = label, color = txtColor, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun FnLayout(
    theme: ThemeEntity,
    fontScale: Float,
    isLocked: Boolean,
    onToggleLock: () -> Unit,
    onKeyAction: (String) -> Unit
) {
    val keyColor = Color(theme.keyBackgroundColor)
    val activeColor = Color(theme.activeKeyBackgroundColor)
    val txtColor = Color(theme.keyTextColor)
    val context = androidx.compose.ui.platform.LocalContext.current

    val rows = com.example.data.LayoutDefaults.getCustomLayout(context, "FnLayout", com.example.data.LayoutDefaults.defaults["FnLayout"] ?: "[]")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                row.forEach { label ->
                    val bg = if (label == "Esc" || label == "Lock") activeColor else keyColor
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .background(bg, RoundedCornerShape(8.dp))
                            .clickable { onKeyAction(label) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = label, color = txtColor, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun NumLayout(
    theme: ThemeEntity,
    isLocked: Boolean,
    onToggleLock: () -> Unit,
    onKeyAction: (String) -> Unit
) {
    val keyColor = Color(theme.keyBackgroundColor)
    val activeColor = Color(theme.activeKeyBackgroundColor)
    val txtColor = Color(theme.keyTextColor)
    val context = androidx.compose.ui.platform.LocalContext.current

    val rows = com.example.data.LayoutDefaults.getCustomLayout(context, "NumLayout", com.example.data.LayoutDefaults.defaults["NumLayout"] ?: "[]")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                row.forEach { label ->
                    val bg = if (label in listOf("↵", "⌫", "Lock", "⛶", "↩️")) activeColor else keyColor
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .background(bg, RoundedCornerShape(8.dp))
                            .clickable { onKeyAction(label) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = label, color = txtColor, fontSize = 18.sp)
                    }
                }
            }
        }
    }
}
