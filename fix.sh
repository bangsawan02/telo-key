#!/bin/bash
cat << 'INNEREOF' >> app/src/main/java/com/example/keyboard/MyInputMethodService.kt

    private fun getSymbolRows(): List<List<String>> {
        return com.example.data.LayoutDefaults.getCustomLayout(this, "SymbolLayout", com.example.data.LayoutDefaults.defaults["SymbolLayout"])
    }

    private fun getLayoutRows(layoutType: String, showNumbers: Boolean): List<List<String>> {
        val baseRows = mutableListOf<List<String>>()

        if (showNumbers && layoutType != "NUMERIC") {
            val numRows = com.example.data.LayoutDefaults.getCustomLayout(this, "NumberRow", com.example.data.LayoutDefaults.defaults["NumberRow"])
            if (numRows.isNotEmpty()) {
                baseRows.add(numRows[0])
            }
        }

        val fallbackType = if (com.example.data.LayoutDefaults.defaults.containsKey(layoutType)) layoutType else "QWERTY"
        val customRows = com.example.data.LayoutDefaults.getCustomLayout(this, fallbackType, com.example.data.LayoutDefaults.defaults[fallbackType])
        baseRows.addAll(customRows)

        return baseRows
    }
INNEREOF
