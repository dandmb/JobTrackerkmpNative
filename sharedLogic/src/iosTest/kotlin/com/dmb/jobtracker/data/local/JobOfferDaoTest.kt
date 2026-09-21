package com.dmb.jobtracker.data.local

import app.cash.turbine.test
import com.dmb.jobtracker.data.local.dao.JobOfferDao
import com.dmb.jobtracker.domain.model.ApplicationStatus
import com.dmb.jobtracker.testutil.jobOfferEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Tests du DAO contre un VRAI SQLite en mémoire (les requêtes `@Query` sont réellement exécutées). */
internal class JobOfferDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: JobOfferDao

    @BeforeTest
    fun setUp() {
        database = inMemoryDatabase()
        dao = database.jobOfferDao()
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    // ---------- insert ----------

    @Test
    fun insert_newEntity_returnsAGeneratedPositiveId() = runTest {
        val id = dao.insert(jobOfferEntity(id = 0))

        assertTrue(id > 0)
    }

    @Test
    fun insert_twoNewEntities_generateDistinctIncreasingIds() = runTest {
        val first = dao.insert(jobOfferEntity(title = "A"))
        val second = dao.insert(jobOfferEntity(title = "B"))

        assertTrue(second > first)
    }

    @Test
    fun insert_thenGetById_returnsTheEntityWithEveryFieldIntact() = runTest {
        val entity = jobOfferEntity(
            title = "Dev", company = "Acme", url = "https://a.io", location = "Paris", source = "LinkedIn",
            salaryRange = "55k - 70k", appliedDateEpochDays = 20_701, interviewDateEpochDays = 20_708,
            resultDateEpochDays = 20_715, status = ApplicationStatus.INTERVIEW, notes = "à relancer", createdAtEpochMillis = 1_234,
        )

        val id = dao.insert(entity)

        assertEquals(entity.copy(id = id), dao.getById(id))
    }

    @Test
    fun insert_entityWithNullOptionalColumns_roundTripsNulls() = runTest {
        val id = dao.insert(jobOfferEntity())

        val stored = assertNotNull(dao.getById(id))
        assertNull(stored.url)
        assertNull(stored.location)
        assertNull(stored.source)
        assertNull(stored.salaryRange)
        assertNull(stored.interviewDateEpochDays)
        assertNull(stored.resultDateEpochDays)
        assertNull(stored.notes)
    }

    @Test
    fun insert_negativeEpochDay_isStoredAndReadBackUnchanged() = runTest {
        val id = dao.insert(jobOfferEntity(appliedDateEpochDays = -1))

        assertEquals(-1L, dao.getById(id)?.appliedDateEpochDays)
    }

    @Test
    fun insert_everyStatus_isStoredAndReadBackUnchanged() = runTest {
        ApplicationStatus.entries.forEach { status ->
            val id = dao.insert(jobOfferEntity(status = status))

            assertEquals(status, dao.getById(id)?.status)
        }
    }

    @Test
    fun insert_entityWithExistingId_replacesTheRowInsteadOfDuplicatingIt() = runTest {
        val id = dao.insert(jobOfferEntity(title = "V1"))

        dao.insert(jobOfferEntity(id = id, title = "V2"))

        val rows = dao.getAll().first()
        assertEquals(1, rows.size)
        assertEquals("V2", rows.single().title)
    }

    // ---------- getById ----------

    @Test
    fun getById_unknownId_returnsNull() = runTest {
        assertNull(dao.getById(999))
    }

    // ---------- getAll ----------

    @Test
    fun getAll_emptyTable_emitsEmptyList() = runTest {
        dao.getAll().test {
            assertEquals(emptyList(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getAll_severalRows_areSortedByCreationTimeMostRecentFirst() = runTest {
        dao.insert(jobOfferEntity(title = "ancienne", createdAtEpochMillis = 100))
        dao.insert(jobOfferEntity(title = "récente", createdAtEpochMillis = 300))
        dao.insert(jobOfferEntity(title = "milieu", createdAtEpochMillis = 200))

        dao.getAll().test {
            assertEquals(listOf("récente", "milieu", "ancienne"), awaitItem().map { it.title })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getAll_afterInsertUpdateAndDelete_reEmitsEachTime() = runTest {
        dao.getAll().test {
            assertEquals(emptyList(), awaitItem())

            val id = dao.insert(jobOfferEntity(title = "V1"))
            assertEquals(listOf("V1"), awaitItem().map { it.title })

            dao.update(jobOfferEntity(id = id, title = "V2"))
            assertEquals(listOf("V2"), awaitItem().map { it.title })

            dao.delete(jobOfferEntity(id = id))
            assertEquals(emptyList(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ---------- getByStatus ----------

    @Test
    fun getByStatus_mixedStatuses_returnsOnlyTheMatchingRowsMostRecentFirst() = runTest {
        dao.insert(jobOfferEntity(title = "refusée ancienne", status = ApplicationStatus.REJECTED, createdAtEpochMillis = 1))
        dao.insert(jobOfferEntity(title = "postulée", status = ApplicationStatus.APPLIED, createdAtEpochMillis = 2))
        dao.insert(jobOfferEntity(title = "refusée récente", status = ApplicationStatus.REJECTED, createdAtEpochMillis = 3))

        dao.getByStatus("REJECTED").test {
            assertEquals(listOf("refusée récente", "refusée ancienne"), awaitItem().map { it.title })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getByStatus_statusWithNoRow_emitsEmptyList() = runTest {
        dao.insert(jobOfferEntity(status = ApplicationStatus.APPLIED))

        dao.getByStatus("ACCEPTED").test {
            assertEquals(emptyList(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getByStatus_unknownStatusName_emitsEmptyList() = runTest {
        dao.insert(jobOfferEntity(status = ApplicationStatus.APPLIED))

        dao.getByStatus("ARCHIVED").test {
            assertEquals(emptyList(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getByStatus_isCaseSensitive() = runTest {
        dao.insert(jobOfferEntity(status = ApplicationStatus.APPLIED))

        dao.getByStatus("applied").test {
            assertEquals(emptyList(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ---------- update ----------

    @Test
    fun update_existingRow_changesEveryColumn() = runTest {
        val id = dao.insert(jobOfferEntity(title = "V1", status = ApplicationStatus.APPLIED))

        dao.update(
            jobOfferEntity(
                id = id, title = "V2", company = "Zeta", location = "Lyon", salaryRange = "1k+",
                status = ApplicationStatus.ACCEPTED, notes = "ok", createdAtEpochMillis = 999,
            ),
        )

        val stored = assertNotNull(dao.getById(id))
        assertEquals("V2", stored.title)
        assertEquals("Zeta", stored.company)
        assertEquals("Lyon", stored.location)
        assertEquals("1k+", stored.salaryRange)
        assertEquals(ApplicationStatus.ACCEPTED, stored.status)
        assertEquals("ok", stored.notes)
        assertEquals(999L, stored.createdAtEpochMillis)
    }

    @Test
    fun update_unknownId_doesNothing() = runTest {
        dao.update(jobOfferEntity(id = 404, title = "fantôme"))

        assertNull(dao.getById(404))
    }

    @Test
    fun update_oneRow_leavesOtherRowsUntouched() = runTest {
        val first = dao.insert(jobOfferEntity(title = "A"))
        val second = dao.insert(jobOfferEntity(title = "B"))

        dao.update(jobOfferEntity(id = first, title = "A modifiée"))

        assertEquals("B", dao.getById(second)?.title)
    }

    // ---------- delete ----------

    @Test
    fun delete_existingRow_removesOnlyThatRow() = runTest {
        val first = dao.insert(jobOfferEntity(title = "A"))
        val second = dao.insert(jobOfferEntity(title = "B"))

        dao.delete(jobOfferEntity(id = first))

        assertNull(dao.getById(first))
        assertNotNull(dao.getById(second))
    }

    @Test
    fun delete_unknownId_doesNothing() = runTest {
        val id = dao.insert(jobOfferEntity())

        dao.delete(jobOfferEntity(id = 404))

        assertNotNull(dao.getById(id))
    }

    @Test
    fun delete_matchesByPrimaryKeyOnly_otherFieldsOfTheEntityAreIrrelevant() = runTest {
        val id = dao.insert(jobOfferEntity(title = "Vrai titre"))

        dao.delete(jobOfferEntity(id = id, title = "Titre différent", company = "Autre"))

        assertNull(dao.getById(id))
    }
}
