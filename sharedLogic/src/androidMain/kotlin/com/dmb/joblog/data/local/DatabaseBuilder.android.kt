package com.dmb.joblog.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

actual fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val appContext = AndroidAppContextHolder.context
    val dbFile = appContext.getDatabasePath(DB_FILE_NAME)
    return Room.databaseBuilder(
        context = appContext,
        klass = AppDatabase::class.java,
        name = dbFile.absolutePath
    )
}

object AndroidAppContextHolder {
    lateinit var context: Context
}