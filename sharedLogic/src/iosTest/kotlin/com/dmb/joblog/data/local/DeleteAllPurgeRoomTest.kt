package com.dmb.joblog.data.local

import androidx.room.Room
import com.dmb.joblog.data.repository.JobOfferRepositoryImpl
import com.dmb.joblog.testutil.jobOffer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import platform.Foundation.NSData
import platform.Foundation.dataWithContentsOfFile
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class DeleteAllPurgeRoomTest {

    private val databasePath = temporaryDatabasePath()

    @AfterTest
    fun tearDown() = deleteDatabaseFiles(databasePath)

    private fun openDatabase(): AppDatabase = getRoomDatabase(
        Room.databaseBuilder<AppDatabase>(name = databasePath, factory = { AppDatabaseConstructor.initialize() })
            .setQueryCoroutineContext(Dispatchers.Default),
    )

    @OptIn(ExperimentalForeignApi::class)
    private fun fileContains(path: String, marker: String): Boolean {
        val data = NSData.dataWithContentsOfFile(path) ?: return false
        val bytes = data.bytes?.readBytes(data.length.toInt()) ?: return false
        return bytes.decodeToString().contains(marker)
    }

    private fun anyDatabaseFileContains(marker: String) =
        fileContains(databasePath, marker) || fileContains("$databasePath-wal", marker)

    private suspend fun JobOfferRepositoryImpl.addMarkedOffers() =
        repeat(20) { add(jobOffer(id = 0, title = "$MARKER title $it", company = "$MARKER company $it", notes = "$MARKER notes $it")) }

    @Test
    fun deleteAll_withPurge_leavesNoReadableTraceInTheDatabaseOrWalFile() = runTest {
        val database = openDatabase()
        try {
            val repository = JobOfferRepositoryImpl(database.jobOfferDao(), purgeDeletedData = { database.purgeDeletedData() })
            repository.addMarkedOffers()
            assertTrue(anyDatabaseFileContains(MARKER), "précondition : les données doivent être présentes dans les fichiers")

            repository.deleteAll()

            assertFalse(fileContains(databasePath, MARKER), "données encore lisibles dans le fichier de base")
            assertFalse(fileContains("$databasePath-wal", MARKER), "données encore lisibles dans le WAL")
        } finally {
            database.close()
        }
    }

    @Test
    fun deleteAll_withoutPurge_stillLeavesReadableData_controlCase() = runTest {
        val database = openDatabase()
        try {
            val repository = JobOfferRepositoryImpl(database.jobOfferDao())
            repository.addMarkedOffers()

            repository.deleteAll()

            assertTrue(anyDatabaseFileContains(MARKER), "témoin : sans purge, un simple DELETE laisse des traces lisibles")
        } finally {
            database.close()
        }
    }

    private companion object {
        const val MARKER = "PURGE-MARKER-7f3a"
    }
}
