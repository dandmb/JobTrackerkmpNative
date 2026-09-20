package com.dmb.jobtracker.data.local

import app.cash.turbine.test
import com.dmb.jobtracker.data.repository.JobOfferRepositoryImpl
import com.dmb.jobtracker.domain.model.ApplicationStatus
import com.dmb.jobtracker.testutil.jobOffer
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Le repository de production contre le DAO Room RÉEL (SQLite en mémoire) : complète les tests avec DAO fake. */
internal class JobOfferRepositoryRoomTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: JobOfferRepositoryImpl

    @BeforeTest
    fun setUp() {
        database = inMemoryDatabase()
        repository = JobOfferRepositoryImpl(database.jobOfferDao())
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun add_thenGetAll_returnsTheOfferWithEveryFieldAndAGeneratedId() = runTest {
        val offer = jobOffer(
            title = "Dev", company = "Acme", url = "u", location = "Paris", source = "s", salaryRange = "55k+",
            interviewDate = LocalDate(2026, 9, 12), resultDate = LocalDate(2026, 9, 19),
            status = ApplicationStatus.INTERVIEW, notes = "n",
        )

        val id = repository.add(offer)

        repository.getAll().test {
            assertEquals(listOf(offer.copy(id = id)), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun add_offerWithNullOptionalFields_roundTripsThem() = runTest {
        repository.add(jobOffer())

        repository.getAll().test {
            val stored = awaitItem().single()
            assertNull(stored.location)
            assertNull(stored.interviewDate)
            assertNull(stored.resultDate)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun update_existingOffer_preservesTheCreatedAtStoredInTheDatabase() = runTest {
        val id = repository.add(jobOffer(title = "V1"))
        val createdAt = assertNotNull(database.jobOfferDao().getById(id)).createdAtEpochMillis

        repository.update(jobOffer(id = id, title = "V2", status = ApplicationStatus.ACCEPTED))

        val stored = assertNotNull(database.jobOfferDao().getById(id))
        assertEquals("V2", stored.title)
        assertEquals(ApplicationStatus.ACCEPTED, stored.status)
        assertEquals(createdAt, stored.createdAtEpochMillis)
    }

    @Test
    fun delete_existingOffer_removesItFromGetAll() = runTest {
        val id = repository.add(jobOffer(title = "À supprimer"))

        repository.delete(jobOffer(id = id))

        repository.getAll().test {
            assertEquals(emptyList(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun deleteThenAddAgainWithSameId_restoresTheOffer() = runTest {
        // Scénario « Annuler » : la carte supprimée est ré-ajoutée avec son id d'origine.
        val id = repository.add(jobOffer(title = "À restaurer"))
        val saved = jobOffer(id = id, title = "À restaurer")
        repository.delete(saved)

        repository.add(saved)

        repository.getAll().test {
            assertEquals(listOf(saved), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getByStatus_returnsOnlyOffersOfThatStatus() = runTest {
        repository.add(jobOffer(title = "Refusée", status = ApplicationStatus.REJECTED))
        repository.add(jobOffer(title = "Postulée", status = ApplicationStatus.APPLIED))

        repository.getByStatus(ApplicationStatus.REJECTED).test {
            assertEquals(listOf("Refusée"), awaitItem().map { it.title })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getAll_offersAddedLater_appearFirst() = runTest {
        repository.add(jobOffer(title = "première"))
        // createdAt = horloge système en millisecondes : on laisse passer au moins 2 ms
        kotlinx.coroutines.delay(5)
        repository.add(jobOffer(title = "seconde"))

        repository.getAll().test {
            assertEquals(listOf("seconde", "première"), awaitItem().map { it.title })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getAll_afterAdd_emitsTheNewListToAnActiveCollector() = runTest {
        repository.getAll().test {
            assertTrue(awaitItem().isEmpty())

            repository.add(jobOffer(title = "Nouvelle"))

            assertEquals(listOf("Nouvelle"), awaitItem().map { it.title })
            cancelAndIgnoreRemainingEvents()
        }
    }
}
