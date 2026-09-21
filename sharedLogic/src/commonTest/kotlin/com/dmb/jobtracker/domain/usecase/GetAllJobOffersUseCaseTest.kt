package com.dmb.jobtracker.domain.usecase

import app.cash.turbine.test
import com.dmb.jobtracker.testutil.FakeJobOfferRepository
import com.dmb.jobtracker.testutil.jobOffer
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetAllJobOffersUseCaseTest {

    @Test
    fun invoke_emptyRepository_emitsEmptyList() = runTest {
        val getAll = GetAllJobOffersUseCase(FakeJobOfferRepository())

        getAll().test {
            assertEquals(emptyList(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun invoke_populatedRepository_emitsAllOffersInRepositoryOrder() = runTest {
        val offers = listOf(jobOffer(id = 2, title = "B"), jobOffer(id = 1, title = "A"))
        val getAll = GetAllJobOffersUseCase(FakeJobOfferRepository(offers))

        getAll().test {
            assertEquals(offers, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun invoke_repositoryChanges_emitsEachNewList() = runTest {
        val repository = FakeJobOfferRepository()
        val getAll = GetAllJobOffersUseCase(repository)
        val added = jobOffer(id = 1)

        getAll().test {
            assertEquals(emptyList(), awaitItem())
            repository.offers.value = listOf(added)
            assertEquals(listOf(added), awaitItem())
            repository.offers.value = emptyList()
            assertEquals(emptyList(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun invoke_upstreamFlowFails_propagatesTheErrorToCollector() = runTest {
        val repository = FakeJobOfferRepository()
        repository.getAllOverride = flow { throw IllegalStateException("flux cassé") }

        GetAllJobOffersUseCase(repository)().test {
            assertEquals("flux cassé", awaitError().message)
        }
    }
}
