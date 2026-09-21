package com.dmb.jobtracker.domain.usecase

import com.dmb.jobtracker.domain.model.ApplicationStatus
import com.dmb.jobtracker.testutil.FakeJobOfferRepository
import com.dmb.jobtracker.testutil.jobOffer
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DeleteAllJobOffersUseCaseTest {

    private val repository = FakeJobOfferRepository(
        ApplicationStatus.entries.mapIndexed { i, status -> jobOffer(id = i + 1L, title = "Offre $i", status = status) },
    )
    private val deleteAll = DeleteAllJobOffersUseCase(repository)

    @Test
    fun invoke_severalOffers_leavesTheListEmpty() = runTest {
        deleteAll()

        assertTrue(repository.offers.value.isEmpty())
        assertEquals(1, repository.deleteAllCalls)
    }

    @Test
    fun invoke_emptyRepository_doesNotFail() = runTest {
        repository.offers.value = emptyList()

        deleteAll()

        assertTrue(repository.offers.value.isEmpty())
    }

    @Test
    fun invoke_calledTwice_staysEmpty() = runTest {
        deleteAll()
        deleteAll()

        assertTrue(repository.offers.value.isEmpty())
    }

    @Test
    fun invoke_thenAddingAnOffer_worksAgain() = runTest {
        deleteAll()

        repository.add(jobOffer(title = "Après"))

        assertEquals(listOf("Après"), repository.offers.value.map { it.title })
    }

    @Test
    fun invoke_repositoryFails_propagatesTheExceptionAndKeepsTheData() = runTest {
        val failure = IllegalStateException("disque plein")
        repository.failure = failure

        val error = assertFailsWith<IllegalStateException> { deleteAll() }

        assertEquals(failure, error)
        assertEquals(ApplicationStatus.entries.size, repository.offers.value.size)
    }
}
