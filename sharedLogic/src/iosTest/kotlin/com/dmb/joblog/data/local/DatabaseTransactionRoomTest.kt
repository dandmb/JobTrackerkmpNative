package com.dmb.joblog.data.local

import com.dmb.joblog.testutil.jobOfferEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

internal class DatabaseTransactionRoomTest {

    private val database = inMemoryDatabase()
    private val dao = database.jobOfferDao()

    @AfterTest
    fun tearDown() = database.close()

    @Test
    fun aFailureInsideTheTransaction_rollsBackTheDaoWritesMadeBeforeIt() = runTest {
        assertFails {
            database.writeTransaction().run {
                dao.insert(jobOfferEntity(title = "Rollback"))
                error("échec simulé entre l'offre et ses liens")
            }
        }

        assertTrue(dao.getAll().first().isEmpty())
    }

    @Test
    fun aSuccessfulTransaction_commitsEveryDaoWrite() = runTest {
        database.writeTransaction().run {
            dao.insert(jobOfferEntity(title = "A"))
            dao.insert(jobOfferEntity(title = "B"))
        }

        assertEquals(setOf("A", "B"), dao.getAll().first().map { it.title }.toSet())
    }
}
