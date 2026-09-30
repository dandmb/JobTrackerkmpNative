package com.dmb.joblog.data.local

import androidx.room.Room
import com.dmb.joblog.data.files.DirectoryAttachmentFileStore
import com.dmb.joblog.data.files.writeBytes
import com.dmb.joblog.data.repository.AttachmentRepositoryImpl
import com.dmb.joblog.data.repository.JobOfferRepositoryImpl
import com.dmb.joblog.domain.model.AttachmentKind
import com.dmb.joblog.domain.repository.AttachmentImportResult
import com.dmb.joblog.i18n.AppLanguage
import com.dmb.joblog.testutil.jobOffer
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUUID
import platform.Foundation.dataWithContentsOfFile

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

    @OptIn(ExperimentalForeignApi::class)
    @Test
    fun deleteAll_throughTheRealWiring_removesAttachmentFilesFromDisk_andLeavesNoTrace() = runTest {
        val database = openDatabase()
        val filesRoot = NSTemporaryDirectory() + "purge_attachments_${NSUUID().UUIDString}"
        val fileStore = DirectoryAttachmentFileStore(filesRoot)
        try {
            val attachments = AttachmentRepositoryImpl(database.attachmentDao(), fileStore)
            val repository = JobOfferRepositoryImpl(
                database.jobOfferDao(),
                purgeDeletedData = { fileStore.deleteAll(); database.purgeDeletedData() },
            )
            val imported = listOf("$MARKER-cv.pdf", "$MARKER-letter.pdf").map { name ->
                val temporary = attachments.newTemporaryPath()
                writeBytes(temporary, "%PDF-1.7 $MARKER content".encodeToByteArray())
                val result = attachments.importCopiedFile(temporary, name, null, AttachmentKind.CV, true, AppLanguage.FR)
                (result as AttachmentImportResult.Imported).attachment
            }
            repository.add(jobOffer(id = 0, title = "$MARKER title", cvAttachmentId = imported[0].id, coverLetterAttachmentId = imported[1].id))
            assertTrue(imported.all { fileStore.exists(it.storageName) }, "précondition : fichiers présents sur disque")
            assertTrue(anyDatabaseFileContains(MARKER), "précondition : données présentes dans les fichiers de base")

            repository.deleteAll()

            assertFalse(NSFileManager.defaultManager.fileExistsAtPath(filesRoot), "dossier des pièces jointes encore présent")
            assertFalse(fileContains(databasePath, MARKER), "données encore lisibles dans le fichier de base")
            assertFalse(fileContains("$databasePath-wal", MARKER), "données encore lisibles dans le WAL")
        } finally {
            database.close()
            NSFileManager.defaultManager.removeItemAtPath(filesRoot, error = null)
        }
    }

    private companion object {
        const val MARKER = "PURGE-MARKER-7f3a"
    }
}
