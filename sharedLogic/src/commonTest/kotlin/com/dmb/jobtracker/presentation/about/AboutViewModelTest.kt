package com.dmb.jobtracker.presentation.about

import com.dmb.jobtracker.domain.model.ApplicationStatus
import com.dmb.jobtracker.domain.usecase.AddJobOfferUseCase
import com.dmb.jobtracker.domain.usecase.DeleteAllJobOffersUseCase
import com.dmb.jobtracker.domain.usecase.GetAllJobOffersUseCase
import com.dmb.jobtracker.domain.usecase.DeleteJobOfferUseCase
import com.dmb.jobtracker.domain.usecase.UpdateJobOfferUseCase
import com.dmb.jobtracker.presentation.joboffer.JobOfferListViewModel
import com.dmb.jobtracker.testutil.FakeJobOfferRepository
import com.dmb.jobtracker.testutil.jobOffer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Même principe que `JobOfferListViewModelTest` : `Dispatchers.Main` remplacé par un `StandardTestDispatcher`. */
@OptIn(ExperimentalCoroutinesApi::class)
class AboutViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val repository = FakeJobOfferRepository(
        listOf(jobOffer(id = 1, title = "A"), jobOffer(id = 2, title = "B", status = ApplicationStatus.ACCEPTED)),
    )
    private val viewModel = AboutViewModel(DeleteAllJobOffersUseCase(repository))

    private fun AboutViewModel.confirmEverything() {
        onDeleteAllRequested()
        onDeleteAllFirstConfirmed()
        onDeleteAllFinalConfirmed()
    }

    // ---------- état initial ----------

    @Test
    fun initialState_isIdleWithNothingDeletedAndNoError() {
        assertEquals(AboutState(), viewModel.state.value)
        assertEquals(DeleteAllStep.IDLE, viewModel.state.value.deleteStep)
        assertFalse(viewModel.state.value.isDeleting)
        assertFalse(viewModel.state.value.dataDeleted)
        assertNull(viewModel.state.value.errorMessage)
    }

    // ---------- double confirmation ----------

    @Test
    fun onDeleteAllRequested_opensTheFirstConfirmationWithoutDeletingAnything() = runTest {
        viewModel.onDeleteAllRequested()
        advanceUntilIdle()

        assertEquals(DeleteAllStep.FIRST_CONFIRMATION, viewModel.state.value.deleteStep)
        assertEquals(0, repository.deleteAllCalls)
        assertEquals(2, repository.offers.value.size)
    }

    @Test
    fun onDeleteAllFirstConfirmed_movesToTheFinalConfirmationWithoutDeletingAnything() = runTest {
        viewModel.onDeleteAllRequested()
        viewModel.onDeleteAllFirstConfirmed()
        advanceUntilIdle()

        assertEquals(DeleteAllStep.FINAL_CONFIRMATION, viewModel.state.value.deleteStep)
        assertEquals(0, repository.deleteAllCalls)
        assertEquals(2, repository.offers.value.size)
    }

    @Test
    fun cancelAtTheFirstStep_returnsToIdleAndDeletesNothing() = runTest {
        viewModel.onDeleteAllRequested()
        viewModel.onDeleteAllCancelled()
        advanceUntilIdle()

        assertEquals(DeleteAllStep.IDLE, viewModel.state.value.deleteStep)
        assertEquals(0, repository.deleteAllCalls)
        assertEquals(2, repository.offers.value.size)
    }

    @Test
    fun cancelAtTheFinalStep_returnsToIdleAndDeletesNothing() = runTest {
        viewModel.onDeleteAllRequested()
        viewModel.onDeleteAllFirstConfirmed()
        viewModel.onDeleteAllCancelled()
        advanceUntilIdle()

        assertEquals(DeleteAllStep.IDLE, viewModel.state.value.deleteStep)
        assertEquals(0, repository.deleteAllCalls)
        assertEquals(2, repository.offers.value.size)
        assertFalse(viewModel.state.value.dataDeleted)
    }

    @Test
    fun finalConfirmedWithoutAnyRequest_isIgnored() = runTest {
        viewModel.onDeleteAllFinalConfirmed()
        advanceUntilIdle()

        assertEquals(0, repository.deleteAllCalls)
        assertEquals(2, repository.offers.value.size)
    }

    @Test
    fun finalConfirmedAfterOnlyTheFirstRequest_isIgnoredSoTheFirstConfirmationCannotBeSkipped() = runTest {
        viewModel.onDeleteAllRequested()
        viewModel.onDeleteAllFinalConfirmed()
        advanceUntilIdle()

        assertEquals(0, repository.deleteAllCalls)
        assertEquals(DeleteAllStep.FIRST_CONFIRMATION, viewModel.state.value.deleteStep)
    }

    @Test
    fun firstConfirmedWithoutAnyRequest_isIgnored() = runTest {
        viewModel.onDeleteAllFirstConfirmed()

        assertEquals(DeleteAllStep.IDLE, viewModel.state.value.deleteStep)
    }

    @Test
    fun cancelThenFinalConfirmed_isIgnored() = runTest {
        viewModel.onDeleteAllRequested()
        viewModel.onDeleteAllFirstConfirmed()
        viewModel.onDeleteAllCancelled()
        viewModel.onDeleteAllFinalConfirmed()
        advanceUntilIdle()

        assertEquals(0, repository.deleteAllCalls)
    }

    // ---------- suppression ----------

    @Test
    fun fullConfirmation_emptiesTheRepositoryAndReportsSuccess() = runTest {
        viewModel.confirmEverything()
        advanceUntilIdle()

        assertTrue(repository.offers.value.isEmpty())
        assertEquals(1, repository.deleteAllCalls)
        assertEquals(AboutState(dataDeleted = true), viewModel.state.value)
    }

    @Test
    fun fullConfirmation_theListViewModelThenObservesAnEmptyList() = runTest {
        val list = JobOfferListViewModel(
            GetAllJobOffersUseCase(repository), AddJobOfferUseCase(repository),
            UpdateJobOfferUseCase(repository), DeleteJobOfferUseCase(repository),
        )
        advanceUntilIdle()
        assertEquals(2, list.state.value.offers.size)

        viewModel.confirmEverything()
        advanceUntilIdle()

        assertTrue(list.state.value.offers.isEmpty())
        list.onCleared()
    }

    @Test
    fun finalConfirmed_showsIsDeletingWhileTheDeletionIsInFlight() = runTest {
        val gate = CompletableDeferred<Unit>()
        repository.gate = gate

        viewModel.confirmEverything()
        runCurrent()

        assertTrue(viewModel.state.value.isDeleting)
        assertFalse(viewModel.state.value.dataDeleted)
        assertEquals(DeleteAllStep.IDLE, viewModel.state.value.deleteStep)

        gate.complete(Unit)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isDeleting)
        assertTrue(viewModel.state.value.dataDeleted)
    }

    @Test
    fun requestsDuringADeletion_areIgnored() = runTest {
        repository.gate = CompletableDeferred()
        viewModel.confirmEverything()
        runCurrent()

        viewModel.onDeleteAllRequested()
        viewModel.onDeleteAllCancelled()

        assertTrue(viewModel.state.value.isDeleting)
        assertEquals(DeleteAllStep.IDLE, viewModel.state.value.deleteStep)
    }

    @Test
    fun finalConfirmedTwice_deletesOnlyOnce() = runTest {
        viewModel.onDeleteAllRequested()
        viewModel.onDeleteAllFirstConfirmed()
        viewModel.onDeleteAllFinalConfirmed()
        viewModel.onDeleteAllFinalConfirmed()
        advanceUntilIdle()

        assertEquals(1, repository.deleteAllCalls)
    }

    @Test
    fun repositoryFailure_reportsTheErrorAndKeepsTheData() = runTest {
        repository.failure = IllegalStateException("disque plein")

        viewModel.confirmEverything()
        advanceUntilIdle()

        assertEquals("disque plein", viewModel.state.value.errorMessage)
        assertFalse(viewModel.state.value.dataDeleted)
        assertFalse(viewModel.state.value.isDeleting)
        assertEquals(2, repository.offers.value.size)
    }

    @Test
    fun repositoryFailureWithoutMessage_usesTheDefaultMessage() = runTest {
        repository.failure = IllegalStateException()

        viewModel.confirmEverything()
        advanceUntilIdle()

        assertEquals(DELETE_ALL_ERROR_MESSAGE, viewModel.state.value.errorMessage)
    }

    @Test
    fun repositoryFailureWithBlankMessage_usesTheDefaultMessage() = runTest {
        repository.failure = IllegalStateException("   ")

        viewModel.confirmEverything()
        advanceUntilIdle()

        assertEquals(DELETE_ALL_ERROR_MESSAGE, viewModel.state.value.errorMessage)
    }

    @Test
    fun newRequestAfterAnError_clearsTheErrorAndCanSucceed() = runTest {
        repository.failure = IllegalStateException("boum")
        viewModel.confirmEverything()
        advanceUntilIdle()
        repository.failure = null

        viewModel.onDeleteAllRequested()
        assertNull(viewModel.state.value.errorMessage)
        viewModel.onDeleteAllFirstConfirmed()
        viewModel.onDeleteAllFinalConfirmed()
        advanceUntilIdle()

        assertTrue(repository.offers.value.isEmpty())
        assertTrue(viewModel.state.value.dataDeleted)
        assertNull(viewModel.state.value.errorMessage)
    }

    @Test
    fun newRequestAfterASuccess_resetsTheSuccessFlag() = runTest {
        viewModel.confirmEverything()
        advanceUntilIdle()
        assertTrue(viewModel.state.value.dataDeleted)

        viewModel.onDeleteAllRequested()

        assertFalse(viewModel.state.value.dataDeleted)
    }

    @Test
    fun deletingWhenNothingIsStored_stillSucceeds() = runTest {
        repository.offers.value = emptyList()

        viewModel.confirmEverything()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.dataDeleted)
    }

    @Test
    fun onCleared_duringADeletion_doesNotReportAnErrorNorSuccess() = runTest {
        repository.gate = CompletableDeferred()
        viewModel.confirmEverything()
        runCurrent()

        viewModel.onCleared()
        advanceUntilIdle()

        assertNull(viewModel.state.value.errorMessage)
        assertFalse(viewModel.state.value.dataDeleted)
    }
}
