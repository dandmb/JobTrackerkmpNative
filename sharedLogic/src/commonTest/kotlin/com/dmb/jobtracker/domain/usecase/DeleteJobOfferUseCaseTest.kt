package com.dmb.jobtracker.domain.usecase

import com.dmb.jobtracker.testutil.FakeJobOfferRepository
import com.dmb.jobtracker.testutil.jobOffer
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
}
