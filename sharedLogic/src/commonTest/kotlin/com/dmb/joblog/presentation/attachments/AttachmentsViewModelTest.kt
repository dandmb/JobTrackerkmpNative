package com.dmb.joblog.presentation.attachments

import com.dmb.joblog.data.repository.AttachmentRepositoryImpl
import com.dmb.joblog.domain.model.AttachmentKind
import com.dmb.joblog.domain.repository.AttachmentDeletionResult
import com.dmb.joblog.domain.repository.AttachmentImportResult
import com.dmb.joblog.i18n.AppLanguage
import com.dmb.joblog.testutil.FakeAttachmentDao
import com.dmb.joblog.testutil.FakeAttachmentFileStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AttachmentsViewModelTest {

    private val dao = FakeAttachmentDao()
    private val store = FakeAttachmentFileStore()
    private lateinit var viewModel: AttachmentsViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        viewModel = AttachmentsViewModel(AttachmentRepositoryImpl(dao, store))
    }

    @AfterTest
    fun tearDown() {
        viewModel.onCleared()
        Dispatchers.resetMain()
    }

    private suspend fun import(name: String, kind: AttachmentKind, inLibrary: Boolean) =
        viewModel.importCopiedFile(store.writeTemporary("%PDF-1.7".encodeToByteArray()), name, null, kind, inLibrary, AppLanguage.EN)

    @Test
    fun state_splitsTheLibraryByKind_andExcludesOneTimeFiles() = runTest {
        import("CV Data.pdf", AttachmentKind.CV, inLibrary = true)
        import("Letter.pdf", AttachmentKind.COVER_LETTER, inLibrary = true)
        import("Once.pdf", AttachmentKind.CV, inLibrary = false)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(listOf("CV Data"), state.library(AttachmentKind.CV).map { it.displayName })
        assertEquals(listOf("Letter"), state.library(AttachmentKind.COVER_LETTER).map { it.displayName })
        assertEquals(3, state.attachments.size)
        assertEquals(16L, state.librarySizeBytes)
    }

    @Test
    fun librarySize_ignoresDocumentsWhoseFileIsMissing() = runTest {
        val lost = assertIs<AttachmentImportResult.Imported>(import("Lost.pdf", AttachmentKind.CV, inLibrary = true)).attachment
        store.stored.remove(lost.storageName)
        import("Kept.pdf", AttachmentKind.COVER_LETTER, inLibrary = true)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.byId(lost.id)!!.isMissing)
        assertEquals(8L, state.librarySizeBytes, "seul le fichier présent compte dans le total")
    }

    @Test
    fun byId_findsAnyAttachment_includingOneTimeFiles() = runTest {
        val once = assertIs<AttachmentImportResult.Imported>(import("Once.pdf", AttachmentKind.CV, inLibrary = false)).attachment
        advanceUntilIdle()

        assertEquals(once.id, viewModel.state.value.byId(once.id)?.id)
        assertEquals(null, viewModel.state.value.byId(null))
    }

    @Test
    fun renameAndDelete_goThroughTheRepository() = runTest {
        val cv = assertIs<AttachmentImportResult.Imported>(import("CV.pdf", AttachmentKind.CV, inLibrary = true)).attachment
        viewModel.rename(cv.id, "CV Data - FR")
        advanceUntilIdle()
        assertEquals("CV Data - FR", viewModel.state.value.byId(cv.id)?.displayName)

        assertEquals(AttachmentDeletionResult.Deleted, viewModel.deleteFromLibrary(cv.id))
        advanceUntilIdle()
        assertTrue(viewModel.state.value.attachments.isEmpty())
    }

    @Test
    fun pathOf_pointsInsideTheAppStorage() = runTest {
        val cv = assertIs<AttachmentImportResult.Imported>(import("CV.pdf", AttachmentKind.CV, inLibrary = true)).attachment
        assertEquals("files/${cv.storageName}", viewModel.pathOf(cv))
    }
}
