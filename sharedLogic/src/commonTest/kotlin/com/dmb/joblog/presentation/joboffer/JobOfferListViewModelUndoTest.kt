package com.dmb.joblog.presentation.joboffer

import com.dmb.joblog.data.local.entity.JobOfferAttachmentEntity
import com.dmb.joblog.data.repository.JobOfferRepositoryImpl
import com.dmb.joblog.domain.usecase.AddJobOfferUseCase
import com.dmb.joblog.domain.usecase.DeleteJobOfferUseCase
import com.dmb.joblog.domain.usecase.GetAllJobOffersUseCase
import com.dmb.joblog.domain.usecase.UpdateJobOfferUseCase
import com.dmb.joblog.testutil.FakeJobOfferDao
import com.dmb.joblog.testutil.jobOfferEntity
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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class JobOfferListViewModelUndoTest {

    private lateinit var viewModel: JobOfferListViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        viewModel = JobOfferListViewModel(
            GetAllJobOffersUseCase(repository), AddJobOfferUseCase(repository),
            UpdateJobOfferUseCase(repository), DeleteJobOfferUseCase(repository),
        )
    }

    @AfterTest
    fun tearDown() { Dispatchers.resetMain() }

    private val dao = FakeJobOfferDao()
    private val repository = JobOfferRepositoryImpl(dao)
    private val originals = listOf("A", "B", "C", "D", "E").mapIndexed { i, t ->
        jobOfferEntity(id = (i + 1).toLong(), title = t, company = "Société $t", notes = "Notes $t", createdAtEpochMillis = (5 - i) * 1_000L)
    }

    private fun titles() = viewModel.state.value.offers.map { it.title }
    private fun offer(title: String) = viewModel.state.value.offers.first { it.title == title }

    private suspend fun kotlinx.coroutines.test.TestScope.loaded() {
        dao.entities.value = originals
        advanceUntilIdle()
    }

    @Test
    fun deleteAMiddleOfferThenUndo_itComesBackAtItsPosition() = runTest {
        loaded()
        val c = offer("C")

        viewModel.onDeleteOffer(c)
        advanceUntilIdle()
        assertEquals(listOf("A", "B", "D", "E"), titles())
        viewModel.onRestoreOffer(c)
        advanceUntilIdle()

        assertEquals(listOf("A", "B", "C", "D", "E"), titles())
        assertEquals(originals.toSet(), dao.entities.value.toSet(), "aucune valeur modifiée, createdAt compris")
        viewModel.onCleared()
    }

    @Test
    fun restoreByIdAfterTheScreenWasRecreated_bringsTheOfferBackAsDeleted() = runTest {
        loaded()
        val c = offer("C")

        viewModel.onDeleteOffer(c)
        advanceUntilIdle()
        viewModel.onRestoreDeletedOffer(c.id)
        advanceUntilIdle()

        assertEquals(listOf("A", "B", "C", "D", "E"), titles())
        assertEquals(originals.toSet(), dao.entities.value.toSet(), "aucune valeur modifiée, createdAt compris")
        viewModel.onCleared()
    }

    @Test
    fun restoreByIdOfAnUnknownDeletion_doesNothing() = runTest {
        loaded()

        viewModel.onRestoreDeletedOffer(42)
        advanceUntilIdle()

        assertEquals(listOf("A", "B", "C", "D", "E"), titles())
        viewModel.onCleared()
    }

    @Test
    fun severalDeletionsThenUndoInAnotherOrder_restoreTheOriginalListStepByStep() = runTest {
        loaded()
        val (a, b, d) = listOf(offer("A"), offer("B"), offer("D"))
        viewModel.onDeleteOffer(a); viewModel.onDeleteOffer(b); viewModel.onDeleteOffer(d)
        advanceUntilIdle()
        assertEquals(listOf("C", "E"), titles())

        viewModel.onRestoreOffer(b); advanceUntilIdle()
        assertEquals(listOf("B", "C", "E"), titles())
        viewModel.onRestoreOffer(d); advanceUntilIdle()
        assertEquals(listOf("B", "C", "D", "E"), titles())
        viewModel.onRestoreOffer(a); advanceUntilIdle()

        assertEquals(listOf("A", "B", "C", "D", "E"), titles())
        viewModel.onCleared()
    }

    @Test
    fun undoRightAfterTheDeletion_beforeAnyIdle_stillRestoresFaithfully() = runTest {
        loaded()
        val c = offer("C")

        viewModel.onDeleteOffer(c)
        viewModel.onRestoreOffer(c)
        advanceUntilIdle()

        assertEquals(listOf("A", "B", "C", "D", "E"), titles())
        assertEquals(3_000L, dao.entities.value.first { it.id == c.id }.createdAtEpochMillis)
        viewModel.onCleared()
    }

    @Test
    fun aDoubleUndo_doesNotDuplicateNorMoveTheOffer() = runTest {
        loaded()
        val c = offer("C")
        viewModel.onDeleteOffer(c); advanceUntilIdle()

        viewModel.onRestoreOffer(c)
        viewModel.onRestoreOffer(c)
        advanceUntilIdle()

        assertEquals(listOf("A", "B", "C", "D", "E"), titles())
        assertEquals(5, dao.entities.value.size)
        viewModel.onCleared()
    }

    @Test
    fun undoOfAnOfferThisViewModelNeverDeleted_fallsBackToAPlainAdd() = runTest {
        loaded()
        val stranger = offer("C").copy(id = 77, title = "Étrangère")

        viewModel.onRestoreOffer(stranger)
        advanceUntilIdle()

        assertTrue("Étrangère" in titles())
        viewModel.onCleared()
    }

    @Test
    fun deletionsBeyondTheUndoWindow_areForgottenOldestFirst_andFallBackToAPlainAdd() = runTest {
        dao.entities.value = (1..(MAX_UNDOABLE_DELETIONS + 2)).map {
            jobOfferEntity(id = it.toLong(), title = "O$it", createdAtEpochMillis = it * 1_000L)
        }
        advanceUntilIdle()
        val first = offer("O1")
        val last = offer("O${MAX_UNDOABLE_DELETIONS + 2}")
        viewModel.state.value.offers.reversed().forEach { viewModel.onDeleteOffer(it) }
        advanceUntilIdle()

        viewModel.onRestoreOffer(last); advanceUntilIdle()
        viewModel.onRestoreOffer(first); advanceUntilIdle()

        assertEquals(last.id, dao.entities.value.first { it.id == last.id }.id)
        assertEquals((MAX_UNDOABLE_DELETIONS + 2) * 1_000L, dao.entities.value.first { it.id == last.id }.createdAtEpochMillis)
        assertTrue(dao.entities.value.first { it.id == first.id }.createdAtEpochMillis != 1_000L, "l'oubliée est réhorodatée (ajout simple)")
        viewModel.onCleared()
    }

    @Test
    fun repeatedDeleteCallsForTheSameSwipe_doNotLoseTheOriginalTimestamp() = runTest {
        loaded()
        val c = offer("C")

        repeat(4) { viewModel.onDeleteOffer(c) }
        advanceUntilIdle()
        viewModel.onRestoreOffer(c)
        advanceUntilIdle()

        assertEquals(listOf("A", "B", "C", "D", "E"), titles())
        assertEquals(3_000L, dao.entities.value.first { it.id == c.id }.createdAtEpochMillis)
        viewModel.onCleared()
    }

    @Test
    fun repeatedDeleteCalls_areInterleavedWithOtherDeletionsAndUndoneInAnotherOrder() = runTest {
        loaded()
        val (b, d) = listOf(offer("B"), offer("D"))

        repeat(3) { viewModel.onDeleteOffer(d); viewModel.onDeleteOffer(b) }
        advanceUntilIdle()
        viewModel.onRestoreOffer(d); viewModel.onRestoreOffer(b)
        advanceUntilIdle()

        assertEquals(listOf("A", "B", "C", "D", "E"), titles())
        viewModel.onCleared()
    }

    @Test
    fun deleteThenUndo_restoresTheAttachedFiles() = runTest {
        loaded()
        dao.existingAttachmentIds += setOf(7L, 8L)
        dao.upsertAttachmentLink(JobOfferAttachmentEntity(jobOfferId = 3, role = "CV", attachmentId = 7))
        dao.upsertAttachmentLink(JobOfferAttachmentEntity(jobOfferId = 3, role = "COVER_LETTER", attachmentId = 8))
        advanceUntilIdle()
        val c = offer("C")
        assertEquals(7L, c.cvAttachmentId)

        viewModel.onDeleteOffer(c)
        advanceUntilIdle()
        assertTrue(dao.links.value.isEmpty())
        viewModel.onRestoreOffer(c)
        advanceUntilIdle()

        assertEquals(7L, offer("C").cvAttachmentId)
        assertEquals(8L, offer("C").coverLetterAttachmentId)
        viewModel.onCleared()
    }
}
