package com.dmb.joblog.data.local

import androidx.room.execSQL
import androidx.room.useWriterConnection

internal fun interface DeletedDataPurger {
    suspend fun purge()
}

// VACUUM est refusé par SQLite à l'intérieur d'une transaction : ne jamais appeler ceci depuis withTransaction.
internal suspend fun AppDatabase.purgeDeletedData() {
    useWriterConnection { connection ->
        connection.execSQL("VACUUM")
        connection.usePrepared("PRAGMA wal_checkpoint(TRUNCATE)") { it.step() }
    }
}
