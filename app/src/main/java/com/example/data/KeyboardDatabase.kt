package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [ThemeEntity::class, LayoutEntity::class, WordEntity::class, DictionaryEntity::class, ClipboardEntity::class],
    version = 3,
    exportSchema = false
)
abstract class KeyboardDatabase : RoomDatabase() {
    abstract fun keyboardDao(): KeyboardDao

    companion object {
        @Volatile
        private var INSTANCE: KeyboardDatabase? = null

        fun getDatabase(context: Context): KeyboardDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KeyboardDatabase::class.java,
                    "keyboard_database"
                )
                .fallbackToDestructiveMigration()
                .addCallback(DatabaseCallback())
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        val dao = database.keyboardDao()
                        
                        // Insert Default Theme Settings
                        dao.saveTheme(ThemeEntity())
                        
                        // Insert Default Layout Settings
                        dao.saveLayout(LayoutEntity())

                        // Pre-populate default fallback words for offline predictions
                        val fallbackEn = listOf(
                            "the", "be", "to", "of", "and", "a", "in", "that", "have", "i", "it", "for", "not", "on", "with", "he", "as", "you", 
                            "do", "at", "this", "but", "his", "by", "from", "they", "we", "say", "her", "she", "or", "an", "will", "my", "one", 
                            "all", "would", "there", "their", "what", "so", "up", "out", "if", "about", "who", "get", "which", "go", "me", "when", 
                            "make", "can", "like", "time", "no", "just", "him", "know", "take", "people", "into", "year", "your", "good", "some", 
                            "could", "them", "see", "other", "than", "then", "now", "look", "only", "come", "its", "over", "think", "also", "back", 
                            "after", "use", "two", "how", "our", "work", "first", "well", "way", "even", "new", "want", "because", "any", "these", 
                            "give", "day", "most", "us", "are", "was", "were", "been", "has", "had", "more", "many", "much", "very", "here"
                        )
                        val fallbackId = listOf(
                            "yang", "di", "dan", "itu", "dengan", "untuk", "tidak", "ini", "dari", "ke", "akan", "bisa", "ada", "mereka", "kita", 
                            "saya", "kami", "kamu", "dia", "pada", "oleh", "adalah", "sebagai", "dalam", "atau", "juga", "sudah", "mengapa", 
                            "bagaimana", "kapan", "siapa", "dimana", "bahwa", "sangat", "banyak", "orang", "hari", "tahun", "telah", "setelah", 
                            "satu", "dua", "tiga", "melihat", "melakukan", "membuat", "ingin", "harus", "jalan", "kerja", "baru", "lama", "kembali", 
                            "lebih", "tentang", "seperti", "jika", "maka", "tetapi", "lagi", "punya", "tahu", "dapat", "pernah", "saja", "selalu", 
                            "biasa", "luar", "atas", "bawah", "semua", "sebelum", "sesudah", "paling", "mungkin", "bukan", "hanya"
                        )

                        val wordsEn = fallbackEn.mapIndexed { index, word ->
                            WordEntity(word = word, language = "en", frequency = 100 - index)
                        }
                        val wordsId = fallbackId.mapIndexed { index, word ->
                            WordEntity(word = word, language = "id", frequency = 100 - index)
                        }
                        dao.insertWords(wordsEn)
                        dao.insertWords(wordsId)

                        // Pre-populate supported downloadable dictionaries
                        val defaultDicts = listOf(
                            DictionaryEntity(
                                languageCode = "en",
                                languageName = "English",
                                isDownloaded = true,
                                wordCount = fallbackEn.size,
                                downloadUrl = "https://raw.githubusercontent.com/first20hours/google-10000-english/master/google-10000-english-usa-no-swears-medium.txt"
                            ),
                            DictionaryEntity(
                                languageCode = "id",
                                languageName = "Bahasa Indonesia",
                                isDownloaded = true,
                                wordCount = fallbackId.size,
                                downloadUrl = "https://raw.githubusercontent.com/damz/indonesian-word-list/master/indonesian-word-list.txt"
                            ),
                            DictionaryEntity(
                                languageCode = "es",
                                languageName = "Español",
                                isDownloaded = false,
                                wordCount = 0,
                                downloadUrl = "https://raw.githubusercontent.com/yisus82/spanish-dictionary/master/clues.txt"
                            ),
                            DictionaryEntity(
                                languageCode = "fr",
                                languageName = "Français",
                                isDownloaded = false,
                                wordCount = 0,
                                downloadUrl = "https://raw.githubusercontent.com/hbenbel/French-Dictionary/master/dictionary/words.txt"
                            )
                        )
                        dao.saveDictionaries(defaultDicts)
                    }
                }
            }
        }
    }
}
