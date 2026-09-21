package com.dmb.joblog.domain.usecase

import com.dmb.joblog.domain.model.ApplicationStatus
import com.dmb.joblog.testutil.FakeJobOfferRepository
import com.dmb.joblog.testutil.jobOffer
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class UpdateJobOfferUseCaseTest {

    private val original = jobOffer(id = 7, title = "Dev", company = "Acme")
    private val repository = FakeJobOfferRepository(listOf(original))
    private val updateJobOffer = UpdateJobOfferUseCase(repository)

    @Test
    fun invoke_validOffer_delegatesToRepositoryUpdate() = runTest {
        val modified = original.copy(status = ApplicationStatus.INTERVIEW)

        updateJobOffer(modified)

        assertEquals(listOf(modified), repository.updateCalls)
        assertEquals(listOf(modified), repository.offers.value)
    }

    @Test
    fun invoke_validOfferChangingEveryField_isAccepted() = runTest {
        val modified = jobOffer(id = 7, title = "Lead", company = "Zeta", location = "Lyon", notes = "n")

        updateJobOffer(modified)

        assertEquals(modified, repository.offers.value.single())
    }

    @Test
    fun invoke_emptyTitle_throwsIllegalArgumentWithTitleMessage() = runTest {
        val error = assertFailsWith<IllegalArgumentException> { updateJobOffer(original.copy(title = "")) }

        assertEquals("The job title must not be blank", error.message)
    }

    @Test
    fun invoke_blankTitle_throwsIllegalArgumentWithTitleMessage() = runTest {
        val error = assertFailsWith<IllegalArgumentException> { updateJobOffer(original.copy(title = "  ")) }

        assertEquals("The job title must not be blank", error.message)
    }

    @Test
    fun invoke_emptyCompany_throwsIllegalArgumentWithCompanyMessage() = runTest {
        val error = assertFailsWith<IllegalArgumentException> { updateJobOffer(original.copy(company = "")) }

        assertEquals("The company name must not be blank", error.message)
    }

    @Test
    fun invoke_blankCompany_throwsIllegalArgumentWithCompanyMessage() = runTest {
        val error = assertFailsWith<IllegalArgumentException> { updateJobOffer(original.copy(company = " \t ")) }

        assertEquals("The company name must not be blank", error.message)
    }

    @Test
    fun invoke_titleAndCompanyBothBlank_reportsTitleFirst() = runTest {
        val error = assertFailsWith<IllegalArgumentException> {
            updateJobOffer(original.copy(title = "", company = ""))
        }

        assertEquals("The job title must not be blank", error.message)
    }

    @Test
    fun invoke_invalidOffer_neverCallsRepositoryAndLeavesDataUntouched() = runTest {
        assertFailsWith<IllegalArgumentException> { updateJobOffer(original.copy(title = "")) }

        assertTrue(repository.updateCalls.isEmpty())
        assertEquals(listOf(original), repository.offers.value)
    }

    @Test
    fun invoke_repositoryFails_propagatesTheOriginalException() = runTest {
        val failure = IllegalStateException("base verrouillée")
        repository.failure = failure

        val error = assertFailsWith<IllegalStateException> { updateJobOffer(original) }

        assertEquals(failure, error)
    }
}
