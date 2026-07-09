#!/bin/bash
sed -i 's/getSymbolRows()/getSymbolRows(androidx.compose.ui.platform.LocalContext.current)/g' app/src/main/java/com/example/keyboard/MyInputMethodService.kt
sed -i 's/getLayoutRows(layout.layoutType, layout.showNumberRow)/getLayoutRows(androidx.compose.ui.platform.LocalContext.current, layout.layoutType, layout.showNumberRow)/g' app/src/main/java/com/example/keyboard/MyInputMethodService.kt
