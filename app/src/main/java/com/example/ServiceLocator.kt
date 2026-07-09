package com.example

import android.content.Context
import com.example.data.KeyboardDatabase
import com.example.data.KeyboardRepository

object ServiceLocator {
    @Volatile
    private var repository: KeyboardRepository? = null

    fun getRepository(context: Context): KeyboardRepository {
        return repository ?: synchronized(this) {
            val db = KeyboardDatabase.getDatabase(context.applicationContext)
            val repo = KeyboardRepository(db.keyboardDao())
            repository = repo
            repo
        }
    }
}
