package com.dmb.joblog.domain.usecase

import com.dmb.joblog.testutil.FakeJobOfferRepository
import com.dmb.joblog.testutil.jobOffer
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DeleteJobOfferUseCaseTest {

    private val first = jobOffer(id = 1, title = "A")
    private val second = jobOffer(id = 2, title = "B")
    private val repository = FakeJobOfferRepository(listOf(first, second))
    private val deleteJobOffer = DeleteJobOfferUseCase(repository)

    @Test
    fun invoke_existingOffer_delegatesToRepositoryAndRemovesIt() = runTest {
        deleteJobOffer(first)

        assertEquals(listOf(first), repository.deleteCalls)
        assertEquals(listOf(second), repository.offers.value)
    }

    @Test
    fun invoke_unknownOffer_leavesOtherOffersUntouched() = runTest {
        deleteJobOffer(jobOffer(id = 99))

        assertEquals(listOf(first, second), repository.offers.value)
    }

    @Test
    fun invoke_offerWithBlankTitle_isStillDeleted() = runTest {
        // Contrairement à add/update, la suppression ne valide rien : on doit pouvoir supprimer une donnée invalide.
        val invalid = jobOffer(id = 3, title = "", company = "")
        repository.offers.value = repository.offers.value + invalid

        deleteJobOffer(invalid)

        assertTrue(repository.offers.value.none { it.id == 3L })
    }

    @Test
    fun invoke_repositoryFails_propagatesTheOriginalException() = runTest {
        val failure = IllegalStateException("suppression impossible")
        repository.failure = failure

        val error = assertFailsWith<IllegalStateException> { deleteJobOffer(first) }

        assertEquals(failure, error)
        assertEquals(listOf(first, second), repository.offers.value)
    }

    // ---------- « Annuler » : restauration fidèle ----------

    @Test
    fun invoke_returnsTheDeletedOfferWithItsOriginalCreationTimestamp() = runTest {
        val expected = repository.createdAtById.getValue(first.id)

        val deleted = deleteJobOffer(first)

        assertEquals(first, deleted.offer)
        assertEquals(expected, deleted.createdAtEpochMillis)
    }

    @Test
    fun invoke_unknownOffer_returnsANullTimestamp() = runTest {
        val deleted = deleteJobOffer(jobOffer(id = 99))

        assertEquals(null, deleted.createdAtEpochMillis)
    }

    @Test
    fun restore_passesTheOriginalTimestampAndTheUntouchedOfferToTheRepository() = runTest {
        val original = first.copy(url = "https://x.fr", location = "Lyon", source = "LinkedIn", salaryRange = "50k+",
            interviewDate = kotlinx.datetime.LocalDate(2026, 9, 10), notes = "n", status = com.dmb.joblog.domain.model.ApplicationStatus.INTERVIEW)
        repository.offers.value = listOf(original, second)
        val originalCreatedAt = repository.createdAtById.getValue(original.id)

        val deleted = deleteJobOffer(original)
        deleteJobOffer.restore(deleted)

        assertEquals(listOf(original to originalCreatedAt), repository.restoreCalls)
        assertTrue(repository.offers.value.contains(original), "tous les champs sont restaurés à l'identique")
        assertEquals(originalCreatedAt, repository.createdAtById[original.id])
    }

    @Test
    fun restore_withoutAKnownTimestamp_fallsBackToASimpleAdd() = runTest {
        deleteJobOffer.restore(com.dmb.joblog.domain.model.DeletedJobOffer(jobOffer(id = 42, title = "Orpheline"), null))

        assertTrue(repository.restoreCalls.isEmpty())
        assertEquals(1, repository.addCalls.size)
        assertEquals("Orpheline", repository.addCalls.single().title)
    }

    @Test
    fun restore_doesNotValidateTheOffer_soALegacyInvalidOneIsRestoredAsIs() = runTest {
        val legacy = jobOffer(id = 7, title = "", company = "")
        repository.offers.value = listOf(legacy)

        deleteJobOffer.restore(deleteJobOffer(legacy))

        assertTrue(repository.offers.value.contains(legacy))
    }

    @Test
    fun addIfMissing_addsAnAbsentOffer() = runTest {
        deleteJobOffer.addIfMissing(jobOffer(id = 50, title = "Nouvelle"))

        assertEquals(listOf("Nouvelle"), repository.addCalls.map { it.title })
    }

    @Test
    fun addIfMissing_doesNothingForAnOfferAlreadyInTheRepository() = runTest {
        deleteJobOffer.addIfMissing(first)

        assertTrue(repository.addCalls.isEmpty(), "réinsérer une offre présente la réhorodaterait")
    }
}
