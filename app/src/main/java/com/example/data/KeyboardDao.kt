package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface KeyboardDao {
    // Theme
    @Query("SELECT * FROM theme_settings WHERE id = 1")
    fun getThemeFlow(): Flow<ThemeEntity?>

    @Query("SELECT * FROM theme_settings WHERE id = 1")
    suspend fun getTheme(): ThemeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveTheme(theme: ThemeEntity)

    // Layout
    @Query("SELECT * FROM layout_settings WHERE id = 1")
    fun getLayoutFlow(): Flow<LayoutEntity?>

    @Query("SELECT * FROM layout_settings WHERE id = 1")
    suspend fun getLayout(): LayoutEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveLayout(layout: LayoutEntity)

    // Dictionaries
    @Query("SELECT * FROM dictionary_metadata")
    fun getAllDictionariesFlow(): Flow<List<DictionaryEntity>>

    @Query("SELECT * FROM dictionary_metadata")
    suspend fun getAllDictionaries(): List<DictionaryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDictionaries(dictionaries: List<DictionaryEntity>)

    @Query("UPDATE dictionary_metadata SET isDownloaded = :isDownloaded, wordCount = :wordCount WHERE languageCode = :languageCode")
    suspend fun updateDictionaryStatus(languageCode: String, isDownloaded: Boolean, wordCount: Int)

    @Query("DELETE FROM dictionary_metadata WHERE languageCode = :languageCode")
    suspend fun deleteDictionaryMetadata(languageCode: String)

    // Words (Autocomplete Search)
    @Query("SELECT * FROM dictionary_words WHERE word LIKE :prefix || '%' AND (language = :language OR language = 'user') ORDER BY frequency DESC, word ASC LIMIT :limit")
    suspend fun searchWords(prefix: String, language: String, limit: Int = 5): List<WordEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWords(words: List<WordEntity>)

    @Query("DELETE FROM dictionary_words WHERE word = :word AND language = :language")
    suspend fun deleteWord(word: String, language: String)

    @Query("DELETE FROM dictionary_words WHERE language = :language")
    suspend fun deleteWordsByLanguage(language: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWord(word: WordEntity)

    // Clipboard History
    @Query("SELECT * FROM clipboard_history ORDER BY timestamp DESC")
    fun getClipboardHistoryFlow(): Flow<List<ClipboardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClipboardItem(item: ClipboardEntity)

    @Query("DELETE FROM clipboard_history WHERE id = :id")
    suspend fun deleteClipboardItem(id: Long)

    @Query("DELETE FROM clipboard_history")
    suspend fun clearClipboardHistory()

    @Query("DELETE FROM clipboard_history WHERE id NOT IN (SELECT id FROM clipboard_history ORDER BY timestamp DESC LIMIT :limit)")
    suspend fun pruneClipboardHistory(limit: Int)
}
