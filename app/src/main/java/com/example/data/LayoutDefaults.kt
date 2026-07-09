package com.example.data

import android.content.Context

object LayoutDefaults {
    val defaults = mapOf(
        "QWERTY" to """[["q","w","e","r","t","y","u","i","o","p"],["a","s","d","f","g","h","j","k","l"],["Shift","z","x","c","v","b","n","m","Backspace"],["Sym","Layout","Space",".","Enter"]]""",
        "AZERTY" to """[["a","z","e","r","t","y","u","i","o","p"],["q","s","d","f","g","h","j","k","l","m"],["Shift","w","x","c","v","b","n","Backspace"],["Sym","Layout","Space",".","Enter"]]""",
        "QWERTZ" to """[["q","w","e","r","t","z","u","i","o","p"],["a","s","d","f","g","h","j","k","l"],["Shift","y","x","c","v","b","n","m","Backspace"],["Sym","Layout","Space",".","Enter"]]""",
        "DVORAK" to """[["'",",",".","p","y","f","g","c","r","l"],["a","o","e","u","i","d","h","t","n","s"],["Shift",";","q","j","k","x","b","m","w","v","z","Backspace"],["Sym","Layout","Space",".","Enter"]]""",
        "COLEMAK" to """[["q","w","f","p","g","j","l","u","y",";"],["a","r","s","t","d","h","n","e","i","o"],["Shift","z","x","c","v","b","k","m","Backspace"],["Sym","Layout","Space",".","Enter"]]""",
        "NUMERIC" to """[["1","2","3"],["4","5","6"],["7","8","9"],["-","0","Backspace"],["Space","Enter"]]""",
        "SymbolLayout" to """[["1","2","3","4","5","6","7","8","9","0"],["@","#","$","%","&","-","+","(",")","/"],["*","\"","'",":",";","!","?","Backspace"],["ABC","Layout","Space",".","Enter"]]""",
        "MiscSymbols" to """[["~","`","|","•","π","÷","×","¶","∆","£"],["€","¥","¢","^","°","=","{","}","[","]"],["<",">","\\","\"","…","·","Backspace"],["ABC","Layout","Space",".","Enter"]]""",
        "EditLayout" to """[["✂️","📋","▲","📄","⌨️▼"],["↪️","◀","⛶","▶","↩️"],["SelAll","|◀","▼","▶|","⌫"],["Lock","⇞","_","⇟","↵"]]""",
        "FnLayout" to """[["Esc","F1","F2","F3","Info","⚙️"],["⇥","F4","F5","F6","^C","date"],["Caps","F7","F8","F9","SysRq","time"],["Insert","F10","F11","F12","ScrLck","find"],["Lock","Ctrl","_","Alt","NumLck","↵"]]""",
        "NumLayout" to """[["/","1","2","3","⌫"],["*","4","5","6","#"],["+","7","8","9","-"],["Lock",",","0",".","↵"]]""",
        "NumberRow" to """[["1","2","3","4","5","6","7","8","9","0"]]"""
    )

    fun getCustomLayout(context: Context, key: String, defaultJson: String?): List<List<String>> {
        if (defaultJson == null) return emptyList()
        val prefs = context.getSharedPreferences("keyboard_settings", Context.MODE_PRIVATE)
        val jsonString = prefs.getString("layout_$key", defaultJson) ?: defaultJson
        try {
            return parseLayoutJson(jsonString)
        } catch (e: Exception) {
            android.util.Log.e("LayoutDefaults", "Error parsing layout JSON for $key: $jsonString, resetting to default.", e)
            try {
                prefs.edit().remove("layout_$key").apply()
            } catch (ex: Exception) {
                // ignore
            }
            try {
                return parseLayoutJson(defaultJson)
            } catch (ex: Exception) {
                return emptyList()
            }
        }
    }

    private fun parseLayoutJson(jsonString: String): List<List<String>> {
        val jsonArray = org.json.JSONArray(jsonString)
        val result = mutableListOf<List<String>>()
        for (i in 0 until jsonArray.length()) {
            val rowArray = jsonArray.getJSONArray(i)
            val row = mutableListOf<String>()
            for (j in 0 until rowArray.length()) {
                row.add(rowArray.getString(j))
            }
            result.add(row)
        }
        return result
    }
}
