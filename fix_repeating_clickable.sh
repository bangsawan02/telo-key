#!/bin/bash
sed -i '/fun Modifier.repeatingClickable/,/): Modifier = this.clickable { onClick() }/c\
fun Modifier.repeatingClickable(\
    scope: kotlinx.coroutines.CoroutineScope,\
    onClick: () -> Unit\
): Modifier = this.clickable { onClick() }\
' app/src/main/java/com/example/keyboard/KeyboardComponents.kt
