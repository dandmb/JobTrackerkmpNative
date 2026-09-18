package com.dmb.jobtracker.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

actual fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    // context injecté via Koin (voir AndroidPlatformModule)
    val appContext = AndroidAppContextHolder.context
    val dbFile = appContext.getDatabasePath(DB_FILE_NAME)
    return Room.databaseBuilder(
        context = appContext,
        klass = AppDatabase::class.java,
        name = dbFile.absolutePath
    )
}

// Petit holder pour éviter de faire passer le Context partout
object AndroidAppContextHolder {
    lateinit var context: Context
}