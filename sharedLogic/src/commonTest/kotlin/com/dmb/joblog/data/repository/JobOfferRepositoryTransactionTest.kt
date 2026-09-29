package com.dmb.joblog.data.repository

import com.dmb.joblog.data.local.DatabaseTransaction
import com.dmb.joblog.data.local.dao.JobOfferDao
import com.dmb.joblog.data.local.entity.JobOfferAttachmentEntity
import com.dmb.joblog.data.local.entity.JobOfferEntity
import com.dmb.joblog.testutil.FakeJobOfferDao
import com.dmb.joblog.testutil.jobOffer
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JobOfferRepositoryTransactionTest {

    private var inTransaction = false
    private val writesOutsideTransaction = mutableListOf<String>()
    private val fake = FakeJobOfferDao().apply { existingAttachmentIds += setOf(4L, 9L) }

    private fun write(name: String) { if (!inTransaction) writesOutsideTransaction += name }

    private val dao: JobOfferDao = object : JobOfferDao by fake {
        override suspend fun insert(offer: JobOfferEntity): Long { write("insert"); return fake.insert(offer) }
        override suspend fun update(offer: JobOfferEntity) { write("update"); fake.update(offer) }
        override suspend fun upsertAttachmentLink(link: JobOfferAttachmentEntity) { write("link"); fake.upsertAttachmentLink(link) }
        override suspend fun deleteAttachmentLink(jobOfferId: Long, role: String) { write("unlink"); fake.deleteAttachmentLink(jobOfferId, role) }
    }

    private val transaction = DatabaseTransaction { block ->
        inTransaction = true
        try { block() } finally { inTransaction = false }
    }

    private val detachedCalls = mutableListOf<Boolean>()
    private val repository = JobOfferRepositoryImpl(dao, inTransaction = transaction, onAttachmentsDetached = { detachedCalls += inTransaction })

    @Test
    fun add_writesTheOfferAndItsDocumentLinksInOneTransaction() = runTest {
        val id = repository.add(jobOffer(cvAttachmentId = 4, coverLetterAttachmentId = 9))

        assertTrue(id > 0)
        assertEquals(emptyList(), writesOutsideTransaction)
    }

    @Test
    fun update_writesInOneTransaction_andCleansDetachedFilesOnlyAfterCommit() = runTest {
        val id = repository.add(jobOffer(cvAttachmentId = 4))

        repository.update(jobOffer(id = id, cvAttachmentId = null))

        assertEquals(emptyList(), writesOutsideTransaction)
        assertEquals(listOf(false), detachedCalls, "les fichiers ne sont pas transactionnels : nettoyage après validation")
    }

    @Test
    fun restore_writesInOneTransaction() = runTest {
        repository.restore(jobOffer(id = 7, cvAttachmentId = 4), createdAtEpochMillis = 1_000)

        assertEquals(emptyList(), writesOutsideTransaction)
    }
}
