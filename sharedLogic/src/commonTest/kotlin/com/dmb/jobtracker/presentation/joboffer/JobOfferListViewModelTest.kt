package com.dmb.jobtracker.presentation.joboffer

import app.cash.turbine.test
import com.dmb.jobtracker.domain.model.ApplicationStatus
import com.dmb.jobtracker.domain.usecase.AddJobOfferUseCase
import com.dmb.jobtracker.domain.usecase.DeleteJobOfferUseCase
import com.dmb.jobtracker.domain.usecase.GetAllJobOffersUseCase
import com.dmb.jobtracker.domain.usecase.UpdateJobOfferUseCase
import com.dmb.jobtracker.testutil.FakeJobOfferRepository
import com.dmb.jobtracker.testutil.jobOffer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
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

/**
 * Le ViewModel crée son scope sur `Dispatchers.Main` : on le remplace par un `StandardTestDispatcher`, ce qui
 * donne le contrôle du temps (rien ne s'exécute avant `advanceUntilIdle()`) et évite tout test dépendant de l'horloge.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class JobOfferListViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModelFor(repository: FakeJobOfferRepository) = JobOfferListViewModel(
        getAllJobOffers = GetAllJobOffersUseCase(repository),
        addJobOffer = AddJobOfferUseCase(repository),
        updateJobOffer = UpdateJobOfferUseCase(repository),
        deleteJobOffer = DeleteJobOfferUseCase(repository),
    )

    // ---------- état initial et chargement ----------

    @Test
    fun init_beforeAnyEmission_stateIsLoadingWithNoOffersAndNoError() = runTest {
        val viewModel = viewModelFor(FakeJobOfferRepository(listOf(jobOffer(id = 1))))

        assertEquals(JobOfferListState(offers = emptyList(), isLoading = true, errorMessage = null), viewModel.state.value)
        viewModel.onCleared()
    }

    @Test
    fun init_afterFirstEmission_stateContainsOffersAndIsNoLongerLoading() = runTest {
        val offers = listOf(jobOffer(id = 1, title = "A"), jobOffer(id = 2, title = "B"))
        val viewModel = viewModelFor(FakeJobOfferRepository(offers))

        advanceUntilIdle()

        assertEquals(offers, viewModel.state.value.offers)
        assertFalse(viewModel.state.value.isLoading)
        assertNull(viewModel.state.value.errorMessage)
        viewModel.onCleared()
    }

    @Test
    fun init_emptyRepository_stateIsLoadedAndEmpty() = runTest {
        val viewModel = viewModelFor(FakeJobOfferRepository())

        advanceUntilIdle()

        assertEquals(JobOfferListState(offers = emptyList(), isLoading = false, errorMessage = null), viewModel.state.value)
        viewModel.onCleared()
    }

    @Test
    fun state_observedWithTurbine_goesFromLoadingToLoaded() = runTest {
        val offers = listOf(jobOffer(id = 1))
        val viewModel = viewModelFor(FakeJobOfferRepository(offers))

        viewModel.state.test {
            assertTrue(awaitItem().isLoading)
            advanceUntilIdle()
            val loaded = awaitItem()
            assertFalse(loaded.isLoading)
            assertEquals(offers, loaded.offers)
            cancelAndIgnoreRemainingEvents()
        }
        viewModel.onCleared()
    }

    @Test
    fun state_repositoryEmitsAgain_followsEachNewList() = runTest {
        val repository = FakeJobOfferRepository()
        val viewModel = viewModelFor(repository)
        advanceUntilIdle()

        repository.offers.value = listOf(jobOffer(id = 1, title = "A"))
        advanceUntilIdle()
        assertEquals(listOf("A"), viewModel.state.value.offers.map { it.title })

        repository.offers.value = listOf(jobOffer(id = 1, title = "A"), jobOffer(id = 2, title = "B"))
        advanceUntilIdle()
        assertEquals(listOf("A", "B"), viewModel.state.value.offers.map { it.title })
        viewModel.onCleared()
    }

    @Test
    fun init_upstreamFlowFails_stateShowsTheErrorMessageAndStopsLoading() = runTest {
        val repository = FakeJobOfferRepository()
        repository.getAllOverride = flow { throw IllegalStateException("base illisible") }
        val viewModel = viewModelFor(repository)

        advanceUntilIdle()

        assertEquals("base illisible", viewModel.state.value.errorMessage)
        assertFalse(viewModel.state.value.isLoading)
        assertTrue(viewModel.state.value.offers.isEmpty())
        viewModel.onCleared()
    }

    @Test
    fun init_upstreamFlowFailsWithoutMessage_stopsLoadingAndShowsTheFallbackMessage() = runTest {
        val repository = FakeJobOfferRepository()
        repository.getAllOverride = flow { throw IllegalStateException() }
        val viewModel = viewModelFor(repository)

        advanceUntilIdle()

        assertFalse(viewModel.state.value.isLoading)
        assertEquals("Une erreur est survenue, réessaie.", viewModel.state.value.errorMessage)
        viewModel.onCleared()
    }

    @Test
    fun init_upstreamFlowFailsAfterFirstEmission_keepsThePreviousOffers() = runTest {
        val repository = FakeJobOfferRepository()
        repository.getAllOverride = flow {
            emit(listOf(jobOffer(id = 1, title = "Gardée")))
            throw IllegalStateException("coupure")
        }
        val viewModel = viewModelFor(repository)

        advanceUntilIdle()

        assertEquals(listOf("Gardée"), viewModel.state.value.offers.map { it.title })
        assertEquals("coupure", viewModel.state.value.errorMessage)
        assertFalse(viewModel.state.value.isLoading)
        viewModel.onCleared()
    }

    // ---------- ajout ----------

    @Test
    fun onAddOffer_validOffer_isSentToTheRepositoryAndAppearsInState() = runTest {
        val repository = FakeJobOfferRepository()
        val viewModel = viewModelFor(repository)
        advanceUntilIdle()

        viewModel.onAddOffer(jobOffer(title = "Nouvelle", company = "Acme"))
        advanceUntilIdle()

        assertEquals(1, repository.addCalls.size)
        assertEquals(listOf("Nouvelle"), viewModel.state.value.offers.map { it.title })
        assertNull(viewModel.state.value.errorMessage)
        viewModel.onCleared()
    }

    @Test
    fun onAddOffer_beforeTheDispatcherRuns_nothingHappensYet() = runTest {
        val repository = FakeJobOfferRepository()
        val viewModel = viewModelFor(repository)
        advanceUntilIdle()

        viewModel.onAddOffer(jobOffer())

        assertTrue(repository.addCalls.isEmpty(), "l'ajout doit être asynchrone (lancé dans le scope du ViewModel)")
        viewModel.onCleared()
    }

    @Test
    fun onAddOffer_blankTitle_setsTheValidationMessageAndDoesNotCallTheRepository() = runTest {
        val repository = FakeJobOfferRepository()
        val viewModel = viewModelFor(repository)
        advanceUntilIdle()

        viewModel.onAddOffer(jobOffer(title = "  "))
        advanceUntilIdle()

        assertEquals("Le titre du poste ne peut pas être vide", viewModel.state.value.errorMessage)
        assertTrue(repository.addCalls.isEmpty())
        assertTrue(viewModel.state.value.offers.isEmpty())
        viewModel.onCleared()
    }

    @Test
    fun onAddOffer_blankCompany_setsTheValidationMessage() = runTest {
        val viewModel = viewModelFor(FakeJobOfferRepository())
        advanceUntilIdle()

        viewModel.onAddOffer(jobOffer(company = ""))
        advanceUntilIdle()

        assertEquals("Le nom de l'entreprise ne peut pas être vide", viewModel.state.value.errorMessage)
        viewModel.onCleared()
    }

    @Test
    fun onAddOffer_validationErrorKeepsExistingOffersAndLoadingFlag() = runTest {
        val existing = listOf(jobOffer(id = 1, title = "Existante"))
        val viewModel = viewModelFor(FakeJobOfferRepository(existing))
        advanceUntilIdle()

        viewModel.onAddOffer(jobOffer(title = ""))
        advanceUntilIdle()

        assertEquals(existing, viewModel.state.value.offers)
        assertFalse(viewModel.state.value.isLoading)
        viewModel.onCleared()
    }

    @Test
    fun onAddOffer_errorThenSuccessfulChange_errorMessageIsClearedByTheNextEmission() = runTest {
        val repository = FakeJobOfferRepository()
        val viewModel = viewModelFor(repository)
        advanceUntilIdle()
        viewModel.onAddOffer(jobOffer(title = ""))
        advanceUntilIdle()
        assertEquals("Le titre du poste ne peut pas être vide", viewModel.state.value.errorMessage)

        viewModel.onAddOffer(jobOffer(title = "Valide"))
        advanceUntilIdle()

        assertNull(viewModel.state.value.errorMessage)
        assertEquals(listOf("Valide"), viewModel.state.value.offers.map { it.title })
        viewModel.onCleared()
    }

    @Test
    fun onAddOffer_offerWithExistingId_isRestoredWithThatId() = runTest {
        // Scénario « Annuler » : l'offre supprimée est ré-ajoutée telle quelle.
        val deleted = jobOffer(id = 7, title = "Restaurée")
        val viewModel = viewModelFor(FakeJobOfferRepository())
        advanceUntilIdle()

        viewModel.onAddOffer(deleted)
        advanceUntilIdle()

        assertEquals(listOf(deleted), viewModel.state.value.offers)
        viewModel.onCleared()
    }

    // ---------- mise à jour ----------

    @Test
    fun onUpdateOffer_validOffer_replacesItInState() = runTest {
        val original = jobOffer(id = 1, title = "V1")
        val viewModel = viewModelFor(FakeJobOfferRepository(listOf(original)))
        advanceUntilIdle()

        viewModel.onUpdateOffer(original.copy(title = "V2", location = "Lyon"))
        advanceUntilIdle()

        assertEquals("V2", viewModel.state.value.offers.single().title)
        assertEquals("Lyon", viewModel.state.value.offers.single().location)
        assertNull(viewModel.state.value.errorMessage)
        viewModel.onCleared()
    }

    @Test
    fun onUpdateOffer_blankTitle_setsTheValidationMessageAndKeepsTheOriginal() = runTest {
        val original = jobOffer(id = 1, title = "V1")
        val repository = FakeJobOfferRepository(listOf(original))
        val viewModel = viewModelFor(repository)
        advanceUntilIdle()

        viewModel.onUpdateOffer(original.copy(title = ""))
        advanceUntilIdle()

        assertEquals("Le titre du poste ne peut pas être vide", viewModel.state.value.errorMessage)
        assertTrue(repository.updateCalls.isEmpty())
        assertEquals(listOf(original), viewModel.state.value.offers)
        viewModel.onCleared()
    }

    @Test
    fun onUpdateOffer_blankCompany_setsTheValidationMessage() = runTest {
        val original = jobOffer(id = 1)
        val viewModel = viewModelFor(FakeJobOfferRepository(listOf(original)))
        advanceUntilIdle()

        viewModel.onUpdateOffer(original.copy(company = " "))
        advanceUntilIdle()

        assertEquals("Le nom de l'entreprise ne peut pas être vide", viewModel.state.value.errorMessage)
        viewModel.onCleared()
    }

    // ---------- changement de statut ----------

    @Test
    fun onStatusChanged_updatesOnlyTheStatusOfTheGivenOffer() = runTest {
        val target = jobOffer(id = 1, title = "Cible", location = "Paris", status = ApplicationStatus.APPLIED)
        val other = jobOffer(id = 2, title = "Autre", status = ApplicationStatus.APPLIED)
        val repository = FakeJobOfferRepository(listOf(target, other))
        val viewModel = viewModelFor(repository)
        advanceUntilIdle()

        viewModel.onStatusChanged(target, ApplicationStatus.INTERVIEW)
        advanceUntilIdle()

        assertEquals(listOf(target.copy(status = ApplicationStatus.INTERVIEW)), repository.updateCalls)
        assertEquals(ApplicationStatus.INTERVIEW, viewModel.state.value.offers.first { it.id == 1L }.status)
        assertEquals(ApplicationStatus.APPLIED, viewModel.state.value.offers.first { it.id == 2L }.status)
        viewModel.onCleared()
    }

    @Test
    fun onStatusChanged_toEveryStatus_isReflectedInState() = runTest {
        val offer = jobOffer(id = 1, status = ApplicationStatus.PENDING)
        val viewModel = viewModelFor(FakeJobOfferRepository(listOf(offer)))
        advanceUntilIdle()

        ApplicationStatus.entries.forEach { status ->
            viewModel.onStatusChanged(offer, status)
            advanceUntilIdle()
            assertEquals(status, viewModel.state.value.offers.single().status)
        }
        viewModel.onCleared()
    }

    // ---------- suppression ----------

    @Test
    fun onDeleteOffer_removesTheOfferFromState() = runTest {
        val first = jobOffer(id = 1, title = "A")
        val second = jobOffer(id = 2, title = "B")
        val repository = FakeJobOfferRepository(listOf(first, second))
        val viewModel = viewModelFor(repository)
        advanceUntilIdle()

        viewModel.onDeleteOffer(first)
        advanceUntilIdle()

        assertEquals(listOf(first), repository.deleteCalls)
        assertEquals(listOf(second), viewModel.state.value.offers)
        viewModel.onCleared()
    }

    @Test
    fun onDeleteOffer_lastOffer_leavesALoadedEmptyList() = runTest {
        val only = jobOffer(id = 1)
        val viewModel = viewModelFor(FakeJobOfferRepository(listOf(only)))
        advanceUntilIdle()

        viewModel.onDeleteOffer(only)
        advanceUntilIdle()

        assertEquals(JobOfferListState(offers = emptyList(), isLoading = false, errorMessage = null), viewModel.state.value)
        viewModel.onCleared()
    }

    @Test
    fun onDeleteOffer_thenRestoreWithSameId_bringsTheOfferBack() = runTest {
        val offer = jobOffer(id = 3, title = "Yo-yo")
        val viewModel = viewModelFor(FakeJobOfferRepository(listOf(offer)))
        advanceUntilIdle()

        viewModel.onDeleteOffer(offer)
        advanceUntilIdle()
        assertTrue(viewModel.state.value.offers.isEmpty())

        viewModel.onAddOffer(offer)
        advanceUntilIdle()

        assertEquals(listOf(offer), viewModel.state.value.offers)
        viewModel.onCleared()
    }

    // ---------- erreurs du repository (pas de validation) : jamais d'exception qui s'échappe du scope ----------

    @Test
    fun onStatusChanged_repositoryFails_setsErrorMessageInsteadOfLettingTheExceptionEscape() = runTest {
        val offer = jobOffer(id = 1, status = ApplicationStatus.APPLIED)
        val repository = FakeJobOfferRepository(listOf(offer))
        val viewModel = viewModelFor(repository)
        advanceUntilIdle()
        repository.failure = IllegalStateException("base verrouillée")

        viewModel.onStatusChanged(offer, ApplicationStatus.INTERVIEW)
        advanceUntilIdle()

        assertEquals("base verrouillée", viewModel.state.value.errorMessage)
        assertEquals(listOf(offer), viewModel.state.value.offers)
        viewModel.onCleared()
    }

    @Test
    fun onDeleteOffer_repositoryFails_setsErrorMessageAndKeepsTheOffer() = runTest {
        val offer = jobOffer(id = 1)
        val repository = FakeJobOfferRepository(listOf(offer))
        val viewModel = viewModelFor(repository)
        advanceUntilIdle()
        repository.failure = IllegalStateException("suppression impossible")

        viewModel.onDeleteOffer(offer)
        advanceUntilIdle()

        assertEquals("suppression impossible", viewModel.state.value.errorMessage)
        assertEquals(listOf(offer), viewModel.state.value.offers)
        viewModel.onCleared()
    }

    @Test
    fun onAddOffer_repositoryFailsWithNonValidationError_setsErrorMessage() = runTest {
        val repository = FakeJobOfferRepository()
        val viewModel = viewModelFor(repository)
        advanceUntilIdle()
        repository.failure = IllegalStateException("disque plein")

        viewModel.onAddOffer(jobOffer())
        advanceUntilIdle()

        assertEquals("disque plein", viewModel.state.value.errorMessage)
        assertTrue(viewModel.state.value.offers.isEmpty())
        viewModel.onCleared()
    }

    @Test
    fun onUpdateOffer_repositoryFailsWithNonValidationError_setsErrorMessage() = runTest {
        val offer = jobOffer(id = 1)
        val repository = FakeJobOfferRepository(listOf(offer))
        val viewModel = viewModelFor(repository)
        advanceUntilIdle()
        repository.failure = IllegalStateException("écriture refusée")

        viewModel.onUpdateOffer(offer.copy(title = "Modifiée"))
        advanceUntilIdle()

        assertEquals("écriture refusée", viewModel.state.value.errorMessage)
        assertEquals(listOf(offer), viewModel.state.value.offers)
        viewModel.onCleared()
    }

    // ---------- message de repli quand l'exception n'a pas de message exploitable ----------

    @Test
    fun onDeleteOffer_repositoryFailsWithoutMessage_showsTheFallbackMessageAndKeepsTheOffer() = runTest {
        val offer = jobOffer(id = 1)
        val repository = FakeJobOfferRepository(listOf(offer))
        val viewModel = viewModelFor(repository)
        advanceUntilIdle()
        repository.failure = IllegalStateException()

        viewModel.onDeleteOffer(offer)
        advanceUntilIdle()

        assertEquals("Une erreur est survenue, réessaie.", viewModel.state.value.errorMessage)
        assertEquals(listOf(offer), viewModel.state.value.offers)
        viewModel.onCleared()
    }

    @Test
    fun onStatusChanged_repositoryFailsWithoutMessage_showsTheFallbackMessage() = runTest {
        val offer = jobOffer(id = 1)
        val repository = FakeJobOfferRepository(listOf(offer))
        val viewModel = viewModelFor(repository)
        advanceUntilIdle()
        repository.failure = IllegalStateException()

        viewModel.onStatusChanged(offer, ApplicationStatus.INTERVIEW)
        advanceUntilIdle()

        assertEquals("Une erreur est survenue, réessaie.", viewModel.state.value.errorMessage)
        viewModel.onCleared()
    }

    @Test
    fun onAddOffer_repositoryFailsWithoutMessage_showsTheFallbackMessage() = runTest {
        val repository = FakeJobOfferRepository()
        val viewModel = viewModelFor(repository)
        advanceUntilIdle()
        repository.failure = RuntimeException()

        viewModel.onAddOffer(jobOffer())
        advanceUntilIdle()

        assertEquals("Une erreur est survenue, réessaie.", viewModel.state.value.errorMessage)
        viewModel.onCleared()
    }

    @Test
    fun onUpdateOffer_repositoryFailsWithoutMessage_showsTheFallbackMessage() = runTest {
        val offer = jobOffer(id = 1)
        val repository = FakeJobOfferRepository(listOf(offer))
        val viewModel = viewModelFor(repository)
        advanceUntilIdle()
        repository.failure = RuntimeException()

        viewModel.onUpdateOffer(offer.copy(title = "Modifiée"))
        advanceUntilIdle()

        assertEquals("Une erreur est survenue, réessaie.", viewModel.state.value.errorMessage)
        viewModel.onCleared()
    }

    @Test
    fun onDeleteOffer_repositoryFailsWithBlankMessage_showsTheFallbackMessage() = runTest {
        val offer = jobOffer(id = 1)
        val repository = FakeJobOfferRepository(listOf(offer))
        val viewModel = viewModelFor(repository)
        advanceUntilIdle()
        repository.failure = IllegalStateException("   ")

        viewModel.onDeleteOffer(offer)
        advanceUntilIdle()

        assertEquals("Une erreur est survenue, réessaie.", viewModel.state.value.errorMessage)
        viewModel.onCleared()
    }

    @Test
    fun onDeleteOffer_repositoryFailsWithARealMessage_keepsThatMessageInsteadOfTheFallback() = runTest {
        val offer = jobOffer(id = 1)
        val repository = FakeJobOfferRepository(listOf(offer))
        val viewModel = viewModelFor(repository)
        advanceUntilIdle()
        repository.failure = IllegalStateException("disque plein")

        viewModel.onDeleteOffer(offer)
        advanceUntilIdle()

        assertEquals("disque plein", viewModel.state.value.errorMessage)
        viewModel.onCleared()
    }

    @Test
    fun fallbackMessage_isTheDocumentedFrenchSentence() {
        assertEquals("Une erreur est survenue, réessaie.", DEFAULT_ERROR_MESSAGE)
    }

    @Test
    fun viewModel_afterARepositoryFailure_stillHandlesLaterActions() = runTest {
        val offer = jobOffer(id = 1, status = ApplicationStatus.APPLIED)
        val repository = FakeJobOfferRepository(listOf(offer))
        val viewModel = viewModelFor(repository)
        advanceUntilIdle()
        repository.failure = IllegalStateException("panne passagère")
        viewModel.onStatusChanged(offer, ApplicationStatus.INTERVIEW)
        advanceUntilIdle()

        repository.failure = null
        viewModel.onStatusChanged(offer, ApplicationStatus.ACCEPTED)
        advanceUntilIdle()

        assertEquals(ApplicationStatus.ACCEPTED, viewModel.state.value.offers.single().status)
        assertNull(viewModel.state.value.errorMessage, "l'erreur est effacée par l'émission qui suit le succès")
        viewModel.onCleared()
    }

    @Test
    fun onDeleteOffer_cancelledByOnCleared_isNotReportedAsAnError() = runTest {
        val offer = jobOffer(id = 1)
        val repository = FakeJobOfferRepository(listOf(offer))
        val viewModel = viewModelFor(repository)
        advanceUntilIdle()
        repository.gate = CompletableDeferred()      // l'action reste suspendue dans le repository

        viewModel.onDeleteOffer(offer)
        runCurrent()                                 // l'action démarre et attend la porte
        viewModel.onCleared()                        // annulation du scope pendant l'action
        advanceUntilIdle()

        assertNull(viewModel.state.value.errorMessage, "une annulation n'est pas une erreur à afficher")
        assertTrue(repository.deleteCalls.isEmpty())
    }

    // ---------- cycle de vie ----------

    @Test
    fun onCleared_stopsObservingTheRepository() = runTest {
        val repository = FakeJobOfferRepository(listOf(jobOffer(id = 1, title = "Avant")))
        val viewModel = viewModelFor(repository)
        advanceUntilIdle()

        viewModel.onCleared()
        repository.offers.value = listOf(jobOffer(id = 1, title = "Après"))
        advanceUntilIdle()

        assertEquals("Avant", viewModel.state.value.offers.single().title)
    }

    @Test
    fun onCleared_pendingActionsAreCancelled() = runTest {
        val repository = FakeJobOfferRepository()
        val viewModel = viewModelFor(repository)
        advanceUntilIdle()

        viewModel.onAddOffer(jobOffer())
        viewModel.onCleared()
        advanceUntilIdle()

        assertTrue(repository.addCalls.isEmpty())
    }

    // ---------- séquences ----------

    @Test
    fun sequence_addStatusChangeThenDelete_endsWithAnEmptyLoadedState() = runTest {
        val repository = FakeJobOfferRepository()
        val viewModel = viewModelFor(repository)
        advanceUntilIdle()

        viewModel.onAddOffer(jobOffer(title = "Cycle de vie", status = ApplicationStatus.APPLIED))
        advanceUntilIdle()
        val added = viewModel.state.value.offers.single()
        assertTrue(added.id != 0L, "le repository attribue un id à l'ajout")

        viewModel.onStatusChanged(added, ApplicationStatus.ACCEPTED)
        advanceUntilIdle()
        assertEquals(ApplicationStatus.ACCEPTED, viewModel.state.value.offers.single().status)

        viewModel.onDeleteOffer(viewModel.state.value.offers.single())
        advanceUntilIdle()
        assertTrue(viewModel.state.value.offers.isEmpty())
        assertFalse(viewModel.state.value.isLoading)
        viewModel.onCleared()
    }
}
