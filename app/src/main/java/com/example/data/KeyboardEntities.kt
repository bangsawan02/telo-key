package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "theme_settings")
data class ThemeEntity(
    @PrimaryKey val id: Int = 1,
    val backgroundColor: Long = 0xFF1C1C1E,
    val keyBackgroundColor: Long = 0xFF2C2C2E,
    val keyTextColor: Long = 0xFFFFFFFF,
    val activeKeyBackgroundColor: Long = 0xFF48484A,
    val borderRadius: Int = 6,
    val keyHeightDp: Int = 54
)

@Entity(tableName = "layout_settings")
data class LayoutEntity(
    @PrimaryKey val id: Int = 1,
    val layoutType: String = "QWERTY", // "QWERTY", "AZERTY", "QWERTZ", "DVORAK", "COLEMAK", "NUMERIC"
    val showNumberRow: Boolean = true,
    val keySpacingDp: Int = 4,
    val fontScale: Float = 1.0f
)

@Entity(tableName = "dictionary_words")
data class WordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val word: String,
    val language: String, // "en", "id", "es", "fr"
    val frequency: Int = 1 // higher means more common
)

@Entity(tableName = "dictionary_metadata")
data class DictionaryEntity(
    @PrimaryKey val languageCode: String, // "en", "id", "es", "fr", etc.
    val languageName: String,
    val isDownloaded: Boolean = false,
    val wordCount: Int = 0,
    val downloadUrl: String
)

@Entity(tableName = "clipboard_history")
data class ClipboardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

