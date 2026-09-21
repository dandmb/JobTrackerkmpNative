package com.dmb.jobtracker.presentation.joboffer

import com.dmb.jobtracker.data.repository.JobOfferRepositoryImpl
import com.dmb.jobtracker.domain.usecase.AddJobOfferUseCase
import com.dmb.jobtracker.domain.usecase.DeleteJobOfferUseCase
import com.dmb.jobtracker.domain.usecase.GetAllJobOffersUseCase
import com.dmb.jobtracker.domain.usecase.UpdateJobOfferUseCase
import com.dmb.jobtracker.testutil.FakeJobOfferDao
import com.dmb.jobtracker.testutil.jobOfferEntity
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

/**
 * Parcours « glisser pour supprimer » puis « Annuler » de bout en bout dans le ViewModel de la liste : vrai repository,
 * DAO fake qui reproduit le tri par date de création décroissante. La liste observée (`state.offers`) doit retrouver
 * l'ordre d'origine, quel que soit l'ordre des annulations.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class JobOfferListViewModelUndoTest {

    private lateinit var viewModel: JobOfferListViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        // Créé APRÈS setMain : le ViewModel lance sa collecte sur Dispatchers.Main dès son init.
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
        viewModel.onRestoreOffer(c)   // aucune attente entre les deux : le Mutex ordonne suppression puis restauration
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
        viewModel.onRestoreOffer(c)   // 2e tap : plus de suppression connue, mais l'offre est déjà là
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
        viewModel.state.value.offers.reversed().forEach { viewModel.onDeleteOffer(it) }   // O1 d'abord (la plus ancienne suppression)
        advanceUntilIdle()

        viewModel.onRestoreOffer(last); advanceUntilIdle()   // récente : restaurée fidèlement
        viewModel.onRestoreOffer(first); advanceUntilIdle()  // trop ancienne : oubliée → ajout simple

        assertEquals(last.id, dao.entities.value.first { it.id == last.id }.id)
        assertEquals((MAX_UNDOABLE_DELETIONS + 2) * 1_000L, dao.entities.value.first { it.id == last.id }.createdAtEpochMillis)
        assertTrue(dao.entities.value.first { it.id == first.id }.createdAtEpochMillis != 1_000L, "l'oubliée est réhorodatée (ajout simple)")
        viewModel.onCleared()
    }

    @Test
    fun repeatedDeleteCallsForTheSameSwipe_doNotLoseTheOriginalTimestamp() = runTest {
        // Défaut constaté sur émulateur : un seul glissement appelle onDeleteOffer 4 fois. Les appels suivants voient une
        // ligne déjà supprimée (horodatage null) et ne doivent pas écraser l'horodatage d'origine.
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
}
