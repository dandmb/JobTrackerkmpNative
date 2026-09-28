package com.dmb.joblog.domain.usecase

import com.dmb.joblog.testutil.FakeJobOfferRepository
import com.dmb.joblog.testutil.jobOffer
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AddJobOfferUseCaseTest {

    private val repository = FakeJobOfferRepository()
    private val addJobOffer = AddJobOfferUseCase(repository)

    @Test
    fun invoke_validOffer_delegatesToRepositoryAndReturnsGeneratedId() = runTest {
        val offer = jobOffer(title = "Dev Android", company = "Spotify")

        val id = addJobOffer(offer)

        assertEquals(1L, id)
        assertEquals(listOf(offer), repository.addCalls)
    }

    @Test
    fun invoke_validOfferWithOnlyRequiredFields_isAccepted() = runTest {
        val offer = jobOffer(url = null, location = null, source = null, salaryRange = null, notes = null)

        addJobOffer(offer)

        assertEquals(1, repository.addCalls.size)
    }

    @Test
    fun invoke_offerWithExistingId_keepsItsIdForRestore() = runTest {
        val id = addJobOffer(jobOffer(id = 42))

        assertEquals(42L, id)
    }

    @Test
    fun invoke_titleWithSurroundingSpacesButContent_isAccepted() = runTest {
        addJobOffer(jobOffer(title = "  Dev  "))

        assertEquals(1, repository.addCalls.size)
    }

    @Test
    fun invoke_emptyTitle_throwsIllegalArgumentWithTitleMessage() = runTest {
        val error = assertFailsWith<IllegalArgumentException> { addJobOffer(jobOffer(title = "")) }

        assertEquals("The job title must not be blank", error.message)
    }

    @Test
    fun invoke_blankTitle_throwsIllegalArgumentWithTitleMessage() = runTest {
        val error = assertFailsWith<IllegalArgumentException> { addJobOffer(jobOffer(title = "   \t")) }

        assertEquals("The job title must not be blank", error.message)
    }

    @Test
    fun invoke_emptyCompany_throwsIllegalArgumentWithCompanyMessage() = runTest {
        val error = assertFailsWith<IllegalArgumentException> { addJobOffer(jobOffer(company = "")) }

        assertEquals("The company name must not be blank", error.message)
    }

    @Test
    fun invoke_blankCompany_throwsIllegalArgumentWithCompanyMessage() = runTest {
        val error = assertFailsWith<IllegalArgumentException> { addJobOffer(jobOffer(company = "  ")) }

        assertEquals("The company name must not be blank", error.message)
    }

    @Test
    fun invoke_titleAndCompanyBothBlank_reportsTitleFirst() = runTest {
        val error = assertFailsWith<IllegalArgumentException> {
            addJobOffer(jobOffer(title = "", company = ""))
        }

        assertEquals("The job title must not be blank", error.message)
    }

    @Test
    fun invoke_invalidOffer_neverCallsRepository() = runTest {
        assertFailsWith<IllegalArgumentException> { addJobOffer(jobOffer(title = "")) }
        assertFailsWith<IllegalArgumentException> { addJobOffer(jobOffer(company = "")) }

        assertTrue(repository.addCalls.isEmpty())
        assertTrue(repository.offers.value.isEmpty())
    }

    @Test
    fun invoke_repositoryFails_propagatesTheOriginalException() = runTest {
        val failure = IllegalStateException("disque plein")
        repository.failure = failure

        val error = assertFailsWith<IllegalStateException> { addJobOffer(jobOffer()) }

        assertEquals(failure, error)
    }
}
