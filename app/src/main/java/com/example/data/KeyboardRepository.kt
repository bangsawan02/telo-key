package com.example.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class KeyboardRepository(private val dao: KeyboardDao) {

    init {
        CoroutineScope(Dispatchers.IO).launch {
            initializeDatabaseIfNeeded()
        }
    }

    private suspend fun initializeDatabaseIfNeeded() {
        try {
            if (dao.getTheme() == null) {
                dao.saveTheme(ThemeEntity())
            }
            if (dao.getLayout() == null) {
                dao.saveLayout(LayoutEntity())
            }
            val dicts = dao.getAllDictionaries()
            if (dicts.isEmpty()) {
                val fallbackEn = getFallbackWordsFor("en")
                val fallbackId = getFallbackWordsFor("id")

                val wordsEn = fallbackEn.mapIndexed { index, word ->
                    WordEntity(word = word, language = "en", frequency = 100 - index)
                }
                val wordsId = fallbackId.mapIndexed { index, word ->
                    WordEntity(word = word, language = "id", frequency = 100 - index)
                }
                dao.insertWords(wordsEn)
                dao.insertWords(wordsId)

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
                Log.d("KeyboardRepository", "Successfully auto-initialized keyboard database with defaults.")
            }
        } catch (e: Exception) {
            Log.e("KeyboardRepository", "Error auto-initializing database", e)
        }
    }

    val themeFlow: Flow<ThemeEntity?> = dao.getThemeFlow()
    val layoutFlow: Flow<LayoutEntity?> = dao.getLayoutFlow()
    val dictionariesFlow: Flow<List<DictionaryEntity>> = dao.getAllDictionariesFlow()

    suspend fun getTheme(): ThemeEntity {
        return dao.getTheme() ?: ThemeEntity()
    }

    suspend fun saveTheme(theme: ThemeEntity) {
        dao.saveTheme(theme)
    }

    suspend fun getLayout(): LayoutEntity {
        return dao.getLayout() ?: LayoutEntity()
    }

    suspend fun saveLayout(layout: LayoutEntity) {
        dao.saveLayout(layout)
    }

    suspend fun searchPredictions(prefix: String, language: String): List<String> = withContext(Dispatchers.IO) {
        if (prefix.isBlank()) return@withContext emptyList<String>()
        val words = dao.searchWords(prefix.lowercase().trim(), language)
        words.map { it.word }
    }

    suspend fun getNextWordPredictions(lastWord: String?, language: String): List<String> = withContext(Dispatchers.IO) {
        val predictions = mutableListOf<String>()
        
        // 1. Try to get learned bigrams if lastWord is present
        if (lastWord != null && lastWord.isNotBlank()) {
            val bigramLang = "bigram_$language"
            val prefix = "${lastWord.lowercase().trim()} "
            val bigramEntities = dao.searchWords(prefix, bigramLang, limit = 5)
            bigramEntities.forEach { entity ->
                val parts = entity.word.split(" ")
                if (parts.size >= 2) {
                    val nextWord = parts[1]
                    if (nextWord.isNotBlank() && !predictions.contains(nextWord)) {
                        predictions.add(nextWord)
                    }
                }
            }
        }
        
        // 2. Fill remaining slots with top words in this language
        if (predictions.size < 5) {
            val topWords = dao.searchWords("", language, limit = 10)
            topWords.forEach { entity ->
                if (!predictions.contains(entity.word) && entity.word.isNotBlank()) {
                    predictions.add(entity.word)
                }
            }
        }
        
        predictions.take(5)
    }

    suspend fun learnWord(word: String) = withContext(Dispatchers.IO) {
        val trimmed = word.trim().lowercase()
        if (trimmed.length > 1 && trimmed.all { it.isLetter() }) {
            val existing = dao.searchWords(trimmed, "user", limit = 1)
            val freq = if (existing.isNotEmpty() && existing[0].word == trimmed) {
                existing[0].frequency + 10
            } else {
                100
            }
            dao.deleteWord(trimmed, "user")
            dao.insertWord(WordEntity(word = trimmed, language = "user", frequency = freq))
        }
    }

    suspend fun learnBigram(prevWord: String, currentWord: String, language: String) = withContext(Dispatchers.IO) {
        val first = prevWord.trim().lowercase()
        val second = currentWord.trim().lowercase()
        if (first.length > 1 && first.all { it.isLetter() } &&
            second.length > 1 && second.all { it.isLetter() }) {
            
            val bigramLang = "bigram_$language"
            val bigramPhrase = "$first $second"
            
            val existing = dao.searchWords(bigramPhrase, bigramLang, limit = 1)
            val freq = if (existing.isNotEmpty() && existing[0].word == bigramPhrase) {
                existing[0].frequency + 15
            } else {
                20
            }
            dao.deleteWord(bigramPhrase, bigramLang)
            dao.insertWord(WordEntity(word = bigramPhrase, language = bigramLang, frequency = freq))
        }
    }

    suspend fun downloadDictionary(languageCode: String, urlString: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            Log.d("KeyboardRepository", "Downloading dictionary from $urlString")
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.requestMethod = "GET"

            val wordsToInsert = mutableListOf<WordEntity>()
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                    var line: String?
                    var counter = 0
                    while (reader.readLine().also { line = it } != null) {
                        val word = line?.trim()?.lowercase() ?: continue
                        if (word.isNotEmpty() && word.length > 1 && word.all { it.isLetter() }) {
                            // Assign decreasing frequency for first items
                            val freq = if (counter < 1000) 100 - (counter / 10) else 1
                            wordsToInsert.add(WordEntity(word = word, language = languageCode, frequency = freq))
                            counter++
                            if (wordsToInsert.size >= 1000) {
                                dao.insertWords(wordsToInsert)
                                wordsToInsert.clear()
                            }
                        }
                    }
                    if (wordsToInsert.isNotEmpty()) {
                        dao.insertWords(wordsToInsert)
                    }
                    val totalCount = dao.searchWords("", languageCode, 100000).size
                    dao.updateDictionaryStatus(languageCode, true, totalCount)
                    return@withContext Result.success(totalCount)
                }
            } else {
                throw Exception("HTTP Error: ${connection.responseCode}")
            }
        } catch (e: Exception) {
            Log.e("KeyboardRepository", "Failed to download online, using high quality fallback", e)
            // Use high quality fallback wordlist
            val fallbackWords = getFallbackWordsFor(languageCode)
            val wordsToInsert = fallbackWords.mapIndexed { index, word ->
                WordEntity(word = word, language = languageCode, frequency = 100 - index)
            }
            dao.deleteWordsByLanguage(languageCode)
            dao.insertWords(wordsToInsert)
            dao.updateDictionaryStatus(languageCode, true, wordsToInsert.size)
            return@withContext Result.success(wordsToInsert.size)
        }
    }

    suspend fun deleteDictionary(languageCode: String) = withContext(Dispatchers.IO) {
        dao.deleteWordsByLanguage(languageCode)
        dao.updateDictionaryStatus(languageCode, false, 0)
    }

    suspend fun addDictionary(dictionary: DictionaryEntity) = withContext(Dispatchers.IO) {
        dao.saveDictionaries(listOf(dictionary))
    }

    suspend fun removeDictionary(languageCode: String) = withContext(Dispatchers.IO) {
        dao.deleteWordsByLanguage(languageCode)
        dao.deleteDictionaryMetadata(languageCode)
    }

    private fun getFallbackWordsFor(lang: String): List<String> {
        return when (lang) {
            "en" -> listOf(
                "the", "be", "to", "of", "and", "a", "in", "that", "have", "i", "it", "for", "not", "on", "with", "he", "as", "you", 
                "do", "at", "this", "but", "his", "by", "from", "they", "we", "say", "her", "she", "or", "an", "will", "my", "one", 
                "all", "would", "there", "their", "what", "so", "up", "out", "if", "about", "who", "get", "which", "go", "me", "when", 
                "make", "can", "like", "time", "no", "just", "him", "know", "take", "people", "into", "year", "your", "good", "some", 
                "could", "them", "see", "other", "than", "then", "now", "look", "only", "come", "its", "over", "think", "also", "back", 
                "after", "use", "two", "how", "our", "work", "first", "well", "way", "even", "new", "want", "because", "any", "these", 
                "give", "day", "most", "us", "are", "was", "were", "been", "has", "had", "more", "some", "many", "much", "very", "here"
            )
            "id" -> listOf(
                "yang", "di", "dan", "itu", "dengan", "untuk", "tidak", "ini", "dari", "ke", "akan", "bisa", "ada", "mereka", "kita", 
                "saya", "kami", "kamu", "dia", "pada", "oleh", "adalah", "sebagai", "dalam", "atau", "juga", "sudah", "mengapa", 
                "bagaimana", "kapan", "siapa", "dimana", "bahwa", "sangat", "banyak", "orang", "hari", "tahun", "telah", "setelah", 
                "satu", "dua", "tiga", "melihat", "melakukan", "membuat", "ingin", "harus", "jalan", "kerja", "baru", "lama", "kembali", 
                "lebih", "tentang", "seperti", "jika", "maka", "tetapi", "lagi", "punya", "tahu", "dapat", "pernah", "saja", "selalu", 
                "biasa", "luar", "atas", "bawah", "semua", "kembali", "sebelum", "sesudah", "paling", "mungkin", "bukan", "hanya"
            )
            "es" -> listOf(
                "el", "la", "los", "las", "un", "una", "de", "en", "y", "que", "ser", "haber", "estar", "tener", "hacer", "poder", 
                "decir", "ir", "ver", "dar", "saber", "querer", "llegar", "pasar", "deber", "poner", "parecer", "quedar", "creer", 
                "hablar", "llevar", "encontrar", "como", "para", "por", "con", "su", "al", "del", "lo", "mas", "pero", "este", 
                "esta", "todo", "todos", "sobre", "entre", "cuando", "donde", "quien", "porque", "muy", "bien", "siempre", "nunca", 
                "nuevo", "viejo", "grande", "pequeño", "bueno", "malo", "mejor", "peor", "ahora", "después", "antes", "mientras"
            )
            "fr" -> listOf(
                "le", "la", "les", "un", "une", "de", "en", "et", "que", "est", "etre", "avoir", "faire", "dire", "pouvoir", "aller", 
                "voir", "savoir", "vouloir", "venir", "devoir", "prendre", "trouver", "donner", "falloir", "parler", "mettre", "passer", 
                "comme", "pour", "par", "avec", "sur", "dans", "mais", "tout", "plus", "bien", "tres", "toujours", "jamais", "nouveau", 
                "vieux", "grand", "petit", "homme", "femme", "jour", "temps", "chose", "mon", "ton", "son", "notre", "votre", "leur", 
                "ce", "cette", "ces", "qui", "quoi", "dont", "ou", "quand", "comment", "pourquoi", "si", "donc", "alors", "après"
            )
            else -> emptyList()
        }
    }

    val clipboardHistoryFlow: Flow<List<ClipboardEntity>> = dao.getClipboardHistoryFlow()

    suspend fun addClipboardItem(text: String) = withContext(Dispatchers.IO) {
        val trimmed = text.trim()
        if (trimmed.isNotEmpty()) {
            dao.insertClipboardItem(ClipboardEntity(text = trimmed))
            dao.pruneClipboardHistory(25) // Keep last 25 items
        }
    }

    suspend fun deleteClipboardItem(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteClipboardItem(id)
    }

    suspend fun clearClipboardHistory() = withContext(Dispatchers.IO) {
        dao.clearClipboardHistory()
    }
}
