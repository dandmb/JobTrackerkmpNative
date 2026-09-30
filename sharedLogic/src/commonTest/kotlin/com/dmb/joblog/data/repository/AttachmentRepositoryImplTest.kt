package com.dmb.joblog.data.repository

import com.dmb.joblog.domain.model.AttachmentFormat
import com.dmb.joblog.domain.model.AttachmentKind
import com.dmb.joblog.domain.repository.AttachmentDeletionResult
import com.dmb.joblog.domain.repository.AttachmentImportResult
import com.dmb.joblog.i18n.AppLanguage
import com.dmb.joblog.testutil.FakeAttachmentDao
import com.dmb.joblog.testutil.FakeAttachmentFileStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AttachmentRepositoryImplTest {

    private val dao = FakeAttachmentDao()
    private val store = FakeAttachmentFileStore()
    private var storageIds = 0
    private val repository = AttachmentRepositoryImpl(dao, store, now = { 42L }, newStorageId = { "id${++storageIds}" })

    private val pdfBytes = "%PDF-1.7 minimal".encodeToByteArray()

    private suspend fun import(
        bytes: ByteArray = pdfBytes,
        name: String? = "CV Data - FR.pdf",
        kind: AttachmentKind = AttachmentKind.CV,
        inLibrary: Boolean = true,
    ) = repository.importCopiedFile(store.writeTemporary(bytes), name, null, kind, inLibrary, AppLanguage.FR)

    @Test
    fun importValidFile_movesItIntoStorage_andRecordsItsMetadata() = runTest {
        val result = import()

        val attachment = assertIs<AttachmentImportResult.Imported>(result).attachment
        assertEquals("CV Data - FR", attachment.displayName)
        assertEquals(AttachmentKind.CV, attachment.kind)
        assertEquals(AttachmentFormat.PDF, attachment.format)
        assertEquals(pdfBytes.size.toLong(), attachment.sizeBytes)
        assertEquals(42L, attachment.addedAtEpochMillis)
        assertTrue(attachment.inLibrary)
        assertEquals("id1.pdf", attachment.storageName)
        assertEquals(setOf("id1.pdf"), store.stored.keys)
        assertTrue(store.temporary.isEmpty(), "le fichier temporaire est déplacé, pas dupliqué")
    }

    @Test
    fun importRejectedFile_deletesTheTemporaryCopy_andRecordsNothing() = runTest {
        val result = import(bytes = "not a pdf".encodeToByteArray())

        assertIs<AttachmentImportResult.Rejected>(result)
        assertTrue(store.temporary.isEmpty())
        assertTrue(store.stored.isEmpty())
        assertTrue(dao.entities.value.isEmpty())
    }

    @Test
    fun importTooLargeFile_isRejectedWithTheSizeMessage() = runTest {
        val tooLarge = ByteArray((10 * 1024 * 1024 + 1)).also { pdfBytes.copyInto(it) }

        val result = import(bytes = tooLarge)

        assertEquals("Ce fichier fait 10 Mo : la taille maximale est de 10 Mo.", assertIs<AttachmentImportResult.Rejected>(result).message)
        assertTrue(store.stored.isEmpty())
    }

    @Test
    fun importWhenTheDatabaseFails_removesTheStoredFile() = runTest {
        dao.failNextInsert = true

        val result = import()

        assertIs<AttachmentImportResult.Rejected>(result)
        assertTrue(store.stored.isEmpty(), "aucun fichier orphelin si l'enregistrement échoue")
    }

    @Test
    fun getAll_flagsFilesMissingFromStorage() = runTest {
        val kept = assertIs<AttachmentImportResult.Imported>(import()).attachment
        val lost = assertIs<AttachmentImportResult.Imported>(import(name = "old.pdf")).attachment
        store.stored.remove(lost.storageName)

        val all = repository.getAll().first().associateBy { it.id }

        assertFalse(all.getValue(kept.id).isMissing)
        assertTrue(all.getValue(lost.id).isMissing)
    }

    @Test
    fun rename_trimsTheName_andIgnoresABlankOne() = runTest {
        val attachment = assertIs<AttachmentImportResult.Imported>(import()).attachment

        repository.rename(attachment.id, "  Lettre générique  ")
        repository.rename(attachment.id, "   ")

        assertEquals("Lettre générique", dao.entities.value.single().displayName)
    }

    @Test
    fun deleteFromLibrary_isRefusedWhileTheFileIsStillAttached() = runTest {
        val attachment = assertIs<AttachmentImportResult.Imported>(import()).attachment
        dao.setUsage(attachment.id, 2)

        val result = repository.deleteFromLibrary(attachment.id)

        assertEquals(AttachmentDeletionResult.StillInUse(2), result)
        assertEquals(1, dao.entities.value.size)
        assertTrue(store.exists(attachment.storageName))
    }

    @Test
    fun deleteFromLibrary_removesTheRowAndTheFile_whenUnused() = runTest {
        val attachment = assertIs<AttachmentImportResult.Imported>(import()).attachment

        assertEquals(AttachmentDeletionResult.Deleted, repository.deleteFromLibrary(attachment.id))

        assertTrue(dao.entities.value.isEmpty())
        assertFalse(store.exists(attachment.storageName))
    }

    @Test
    fun deleteIfUnusedOneTime_onlyDeletesUnusedOneTimeFiles() = runTest {
        val library = assertIs<AttachmentImportResult.Imported>(import(inLibrary = true)).attachment
        val oneTimeUnused = assertIs<AttachmentImportResult.Imported>(import(inLibrary = false)).attachment
        val oneTimeUsed = assertIs<AttachmentImportResult.Imported>(import(inLibrary = false)).attachment
        dao.setUsage(oneTimeUsed.id, 1)

        repository.deleteIfUnusedOneTime(setOf(library.id, oneTimeUnused.id, oneTimeUsed.id))

        assertEquals(setOf(library.id, oneTimeUsed.id), dao.entities.value.map { it.id }.toSet())
        assertEquals(setOf(library.storageName, oneTimeUsed.storageName), store.stored.keys)
    }

    @Test
    fun cleanUp_removesTemporaryFiles_orphanOneTimeFiles_andStrayStoredFiles() = runTest {
        val library = assertIs<AttachmentImportResult.Imported>(import(inLibrary = true)).attachment
        val orphan = assertIs<AttachmentImportResult.Imported>(import(inLibrary = false)).attachment
        store.writeTemporary(pdfBytes)
        store.stored["stray.pdf"] = pdfBytes

        repository.cleanUp()

        assertTrue(store.temporary.isEmpty())
        assertEquals(listOf(library.id), dao.entities.value.map { it.id })
        assertEquals(setOf(library.storageName), store.stored.keys)
        assertFalse(store.exists(orphan.storageName))
    }
}
