#!/bin/bash

# Fix getSecondarySymbol
sed -i '/fun getSecondarySymbol(label: String): String? {/,/return null/c\
fun getSecondarySymbol(label: String): String? {\
    return when (label.lowercase()) {\
        "q" -> "1"\
        "w" -> "2"\
        "e" -> "3"\
        "r" -> "4"\
        "t" -> "5"\
        "y" -> "6"\
        "u" -> "7"\
        "i" -> "8"\
        "o" -> "9"\
        "p" -> "0"\
        "a" -> "@"\
        "s" -> "#"\
        "d" -> "$"\
        "f" -> "%"\
        "g" -> "&"\
        "h" -> "-"\
        "j" -> "+"\
        "k" -> "("\
        "l" -> ")"\
        "z" -> "*"\
        "x" -> "\\""\
        "c" -> "'\''"\
        "v" -> ":"\
        "b" -> ";"\
        "n" -> "!"\
        "m" -> "?"\
        else -> null\
    }\
}\
' app/src/main/java/com/example/keyboard/KeyboardComponents.kt

# Fix repeatingClickable
sed -i '/fun Modifier.repeatingClickable(/,/): Modifier = this.clickable { onClick() }/c\
fun Modifier.repeatingClickable(\
    scope: kotlinx.coroutines.CoroutineScope,\
    onClick: () -> Unit\
): Modifier = androidx.compose.ui.composed {\
    val currentClickListener by rememberUpdatedState(onClick)\
    androidx.compose.ui.input.pointer.pointerInput(Unit) {\
        androidx.compose.foundation.gestures.awaitEachGesture {\
            androidx.compose.foundation.gestures.awaitFirstDown(requireUnconsumed = false)\
            currentClickListener()\
            val job = scope.launch {\
                kotlinx.coroutines.delay(500)\
                while (true) {\
                    currentClickListener()\
                    kotlinx.coroutines.delay(100)\
                }\
            }\
            androidx.compose.foundation.gestures.waitForUpOrCancellation()\
            job.cancel()\
        }\
    }\
}\
' app/src/main/java/com/example/keyboard/KeyboardComponents.kt

