package com.dmb.joblog.data.local

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection

internal fun interface DatabaseTransaction {
    suspend fun run(block: suspend () -> Unit)
}

internal fun AppDatabase.writeTransaction(): DatabaseTransaction = DatabaseTransaction { block ->
    useWriterConnection { transactor -> transactor.immediateTransaction { block() } }
}
