package com.dmb.joblog.data.local

import app.cash.turbine.test
import com.dmb.joblog.data.repository.JobOfferRepositoryImpl
import com.dmb.joblog.domain.model.ApplicationStatus
import com.dmb.joblog.domain.usecase.DeleteJobOfferUseCase
import com.dmb.joblog.testutil.jobOfferEntity
import kotlinx.coroutines.flow.first
import com.dmb.joblog.testutil.jobOffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
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
    fun deleteAll_afterSeveralAdds_emptiesGetAllInTheRealDatabase() = runTest {
        repository.add(jobOffer(title = "A", status = ApplicationStatus.APPLIED))
        repository.add(jobOffer(title = "B", status = ApplicationStatus.REJECTED, notes = "n", salaryRange = "50k+"))
        repository.add(jobOffer(title = "C", status = ApplicationStatus.INTERVIEW))

        repository.deleteAll()

        repository.getAll().test {
            assertEquals(emptyList(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        ApplicationStatus.entries.forEach {
            repository.getByStatus(it).test {
                assertEquals(emptyList(), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
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
        // createdAt = horloge système en millisecondes : il faut une VRAIE attente (dans runTest, `delay` est virtuel et
        // n'attend pas), sinon les deux ajouts peuvent partager la même milliseconde et l'ordre devient aléatoire.
        withContext(Dispatchers.Default) { delay(5) }
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

    // ---------- « Annuler » une suppression : position et valeurs d'origine (SQLite réel) ----------

    /** 5 offres A..E, de la plus récente (A) à la plus ancienne (E), createdAt explicites. */
    private suspend fun seedFive() {
        val dao = database.jobOfferDao()
        listOf("A", "B", "C", "D", "E").forEachIndexed { i, t ->
            dao.insert(jobOfferEntity(id = (i + 1).toLong(), title = t, company = "Société $t", notes = "Notes $t",
                salaryRange = "${40 + i}k+", createdAtEpochMillis = (5 - i) * 1_000L))
        }
    }

    private suspend fun titles() = repository.getAll().first().map { it.title }

    @Test
    fun undoDelete_ofAMiddleOffer_restoresItsPositionAndEveryValueInTheRealDatabase() = runTest {
        seedFive()
        val dao = database.jobOfferDao()
        val before = assertNotNull(dao.getById(3))
        val delete = DeleteJobOfferUseCase(repository)

        val deleted = delete(repository.getAll().first().first { it.id == 3L })
        assertEquals(listOf("A", "B", "D", "E"), titles())
        delete.restore(deleted)

        assertEquals(listOf("A", "B", "C", "D", "E"), titles())
        assertEquals(before, dao.getById(3), "entité identique à l'originale, createdAt compris")
    }

    @Test
    fun undoDelete_ofSeveralOffersInADifferentOrder_endsWithTheOriginalOrder() = runTest {
        seedFive()
        val delete = DeleteJobOfferUseCase(repository)
        val all = repository.getAll().first()
        val b = delete(all.first { it.title == "B" })
        val d = delete(all.first { it.title == "D" })
        val a = delete(all.first { it.title == "A" })

        delete.restore(d); delete.restore(a); delete.restore(b)

        assertEquals(listOf("A", "B", "C", "D", "E"), titles())
    }

    @Test
    fun getCreatedAt_andRestore_roundTripTheStoredTimestamp() = runTest {
        seedFive()

        assertEquals(3_000L, repository.getCreatedAt(3))
        assertNull(repository.getCreatedAt(99))
        repository.delete(jobOffer(id = 3))
        repository.restore(jobOffer(id = 3, title = "C", company = "Société C"), 3_000L)
        assertEquals(3_000L, repository.getCreatedAt(3))
    }
}
