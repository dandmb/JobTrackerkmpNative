package com.dmb.jobtracker.data.repository

import app.cash.turbine.test
import com.dmb.jobtracker.domain.model.ApplicationStatus
import com.dmb.jobtracker.testutil.FakeJobOfferDao
import com.dmb.jobtracker.testutil.jobOffer
import com.dmb.jobtracker.testutil.jobOfferEntity
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JobOfferRepositoryImplTest {

    private val dao = FakeJobOfferDao()
    private val repository = JobOfferRepositoryImpl(dao)

    // ---------- add ----------

    @Test
    fun add_newOffer_insertsMappedEntityAndReturnsGeneratedId() = runTest {
        val offer = jobOffer(title = "Dev", company = "Acme", location = "Paris", appliedDate = LocalDate(2026, 9, 5))

        val id = repository.add(offer)

        assertEquals(1L, id)
        val inserted = dao.inserted.single()
        assertEquals("Dev", inserted.title)
        assertEquals("Acme", inserted.company)
        assertEquals("Paris", inserted.location)
        assertEquals(20_701L, inserted.appliedDateEpochDays)
        assertEquals(0L, inserted.id)
    }

    @Test
    fun add_newOffer_stampsCreatedAtWithCurrentTime() = runTest {
        val before = Clock.System.now().toEpochMilliseconds()

        repository.add(jobOffer())

        val after = Clock.System.now().toEpochMilliseconds()
        assertTrue(dao.inserted.single().createdAtEpochMillis in before..after)
    }

    @Test
    fun add_offerWithExistingId_keepsThatIdSoARestoredOfferGetsItsPlaceBack() = runTest {
        val id = repository.add(jobOffer(id = 42, title = "Restaurée"))

        assertEquals(42L, id)
        assertEquals(42L, dao.inserted.single().id)
    }

    @Test
    fun add_offerWithExistingId_replacesTheRowInsteadOfDuplicatingIt() = runTest {
        repository.add(jobOffer(id = 42, title = "V1"))
        repository.add(jobOffer(id = 42, title = "V2"))

        val rows = dao.entities.value
        assertEquals(1, rows.size)
        assertEquals("V2", rows.single().title)
    }

    @Test
    fun add_twoOffers_generateDistinctIds() = runTest {
        val first = repository.add(jobOffer(title = "A"))
        val second = repository.add(jobOffer(title = "B"))

        assertTrue(first != second)
    }

    // ---------- getAll ----------

    @Test
    fun getAll_emptyDao_emitsEmptyList() = runTest {
        repository.getAll().test {
            assertEquals(emptyList(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getAll_populatedDao_mapsEntitiesToDomain() = runTest {
        dao.entities.value = listOf(
            jobOfferEntity(
                id = 1, title = "A", interviewDateEpochDays = 20_708, status = ApplicationStatus.INTERVIEW,
                createdAtEpochMillis = 10,
            ),
        )

        repository.getAll().test {
            val offers = awaitItem()
            assertEquals(
                listOf(
                    jobOffer(
                        id = 1, title = "A", interviewDate = LocalDate(2026, 9, 12),
                        status = ApplicationStatus.INTERVIEW,
                    ),
                ),
                offers,
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getAll_severalEntities_keepsTheDaoOrderMostRecentlyCreatedFirst() = runTest {
        dao.entities.value = listOf(
            jobOfferEntity(id = 1, title = "ancienne", createdAtEpochMillis = 1),
            jobOfferEntity(id = 2, title = "récente", createdAtEpochMillis = 2),
        )

        repository.getAll().test {
            assertEquals(listOf("récente", "ancienne"), awaitItem().map { it.title })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getAll_daoChanges_emitsTheUpdatedList() = runTest {
        repository.getAll().test {
            assertEquals(emptyList(), awaitItem())

            repository.add(jobOffer(title = "Nouvelle"))

            assertEquals(listOf("Nouvelle"), awaitItem().map { it.title })
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ---------- getByStatus ----------

    @Test
    fun getByStatus_givenStatus_passesItsNameToTheDao() = runTest {
        repository.getByStatus(ApplicationStatus.REJECTED).test {
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(listOf("REJECTED"), dao.getByStatusCalls)
    }

    @Test
    fun getByStatus_mixedStatuses_emitsOnlyMatchingOffersMappedToDomain() = runTest {
        dao.entities.value = listOf(
            jobOfferEntity(id = 1, title = "Refusée", status = ApplicationStatus.REJECTED, createdAtEpochMillis = 1),
            jobOfferEntity(id = 2, title = "Postulée", status = ApplicationStatus.APPLIED, createdAtEpochMillis = 2),
        )

        repository.getByStatus(ApplicationStatus.REJECTED).test {
            val offers = awaitItem()
            assertEquals(listOf("Refusée"), offers.map { it.title })
            assertEquals(ApplicationStatus.REJECTED, offers.single().status)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ---------- update ----------

    @Test
    fun update_existingOffer_preservesTheOriginalCreatedAt() = runTest {
        dao.entities.value = listOf(jobOfferEntity(id = 5, title = "V1", createdAtEpochMillis = 1_234))

        repository.update(jobOffer(id = 5, title = "V2"))

        assertEquals(1_234L, dao.updated.single().createdAtEpochMillis)
    }

    @Test
    fun update_existingOffer_writesTheNewValuesAndKeepsTheSameRow() = runTest {
        dao.entities.value = listOf(
            jobOfferEntity(id = 5, title = "V1", status = ApplicationStatus.APPLIED, createdAtEpochMillis = 1_234),
        )

        repository.update(jobOffer(id = 5, title = "V2", status = ApplicationStatus.INTERVIEW, location = "Lyon"))

        val row = dao.entities.value.single()
        assertEquals(5L, row.id)
        assertEquals("V2", row.title)
        assertEquals(ApplicationStatus.INTERVIEW, row.status)
        assertEquals("Lyon", row.location)
    }

    @Test
    fun update_looksUpTheExistingRowByOfferId() = runTest {
        dao.entities.value = listOf(jobOfferEntity(id = 5))

        repository.update(jobOffer(id = 5))

        assertEquals(listOf(5L), dao.getByIdCalls)
    }

    @Test
    fun update_unknownOffer_stampsCreatedAtWithCurrentTimeAndDoesNotCreateARow() = runTest {
        val before = Clock.System.now().toEpochMilliseconds()

        repository.update(jobOffer(id = 404))

        val after = Clock.System.now().toEpochMilliseconds()
        assertTrue(dao.updated.single().createdAtEpochMillis in before..after)
        assertTrue(dao.entities.value.isEmpty())
    }

    @Test
    fun update_statusChangedOnly_keepsOtherFieldsIntactAfterRoundTrip() = runTest {
        val original = jobOffer(
            id = 8, location = "Nice", salaryRange = "1-2", interviewDate = LocalDate(2026, 10, 1),
            notes = "n", status = ApplicationStatus.APPLIED,
        )
        dao.entities.value = listOf(jobOfferEntity(id = 8, createdAtEpochMillis = 55))

        repository.update(original.copy(status = ApplicationStatus.ACCEPTED))

        repository.getAll().test {
            assertEquals(listOf(original.copy(status = ApplicationStatus.ACCEPTED)), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ---------- delete ----------

    @Test
    fun delete_existingOffer_removesTheRowByPrimaryKey() = runTest {
        dao.entities.value = listOf(jobOfferEntity(id = 1), jobOfferEntity(id = 2))

        repository.delete(jobOffer(id = 1))

        assertEquals(listOf(2L), dao.entities.value.map { it.id })
        assertEquals(1L, dao.deleted.single().id)
    }

    @Test
    fun delete_unknownOffer_leavesTheTableUntouched() = runTest {
        dao.entities.value = listOf(jobOfferEntity(id = 1))

        repository.delete(jobOffer(id = 99))

        assertEquals(listOf(1L), dao.entities.value.map { it.id })
    }

    // ---------- scénario complet ----------

    @Test
    fun addThenDeleteThenAddAgainWithSameId_restoresTheOffer() = runTest {
        // Scénario « Annuler » du snackbar : l'offre supprimée est ré-ajoutée avec le même id.
        val id = repository.add(jobOffer(title = "À restaurer"))
        val saved = jobOffer(id = id, title = "À restaurer")
        repository.delete(saved)
        assertTrue(dao.entities.value.isEmpty())

        repository.add(saved)

        repository.getAll().test {
            assertEquals(listOf(saved), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
