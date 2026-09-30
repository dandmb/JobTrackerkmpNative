package com.dmb.joblog.data.local

import androidx.room.Room
import com.dmb.joblog.data.local.entity.AttachmentEntity
import com.dmb.joblog.data.local.entity.JobOfferAttachmentEntity
import com.dmb.joblog.data.mapper.toEntity
import com.dmb.joblog.testutil.jobOffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNotNull

internal class AttachmentRoomTest {

    private val databasePath = temporaryDatabasePath()

    @AfterTest
    fun tearDown() = deleteDatabaseFiles(databasePath)

    private fun openDatabase(): AppDatabase = getRoomDatabase(
        Room.databaseBuilder<AppDatabase>(name = databasePath, factory = { AppDatabaseConstructor.initialize() })
            .setQueryCoroutineContext(Dispatchers.Default),
    )

    private fun attachment(storageName: String) = AttachmentEntity(
        displayName = "CV", kind = "CV", format = "PDF", sizeBytes = 10, addedAtEpochMillis = 1, inLibrary = true, storageName = storageName,
    )

    @Test
    fun deletingAnApplication_removesItsLinks_butKeepsTheAttachment() = runTest {
        val database = openDatabase()
        try {
            val offers = database.jobOfferDao()
            val attachments = database.attachmentDao()
            val offerId = offers.insert(jobOffer().toEntity())
            val attachmentId = attachments.insert(attachment("a.pdf"))
            offers.upsertAttachmentLink(JobOfferAttachmentEntity(offerId, "CV", attachmentId))
            assertEquals(1, attachments.usageCount(attachmentId))

            offers.delete(offers.getById(offerId)!!)

            assertEquals(0, attachments.usageCount(attachmentId))
            assertNotNull(attachments.getById(attachmentId))
        } finally {
            database.close()
        }
    }

    @Test
    fun deletingAnAttachmentStillLinked_isRefusedByTheDatabase() = runTest {
        val database = openDatabase()
        try {
            val offers = database.jobOfferDao()
            val attachments = database.attachmentDao()
            val offerId = offers.insert(jobOffer().toEntity())
            val attachmentId = attachments.insert(attachment("b.pdf"))
            offers.upsertAttachmentLink(JobOfferAttachmentEntity(offerId, "CV", attachmentId))

            assertFails { attachments.deleteById(attachmentId) }

            assertNotNull(attachments.getById(attachmentId))
        } finally {
            database.close()
        }
    }

    @Test
    fun linkToAMissingAttachment_isRefusedByTheDatabase() = runTest {
        val database = openDatabase()
        try {
            val offerId = database.jobOfferDao().insert(jobOffer().toEntity())

            assertFails { database.jobOfferDao().upsertAttachmentLink(JobOfferAttachmentEntity(offerId, "CV", 999)) }
        } finally {
            database.close()
        }
    }

    @Test
    fun usageCountAndOrphans_areComputedFromTheLinks() = runTest {
        val database = openDatabase()
        try {
            val offers = database.jobOfferDao()
            val attachments = database.attachmentDao()
            val offerId = offers.insert(jobOffer().toEntity())
            val used = attachments.insert(attachment("u.pdf").copy(inLibrary = false))
            val orphan = attachments.insert(attachment("o.pdf").copy(inLibrary = false))
            attachments.insert(attachment("l.pdf"))
            offers.upsertAttachmentLink(JobOfferAttachmentEntity(offerId, "CV", used))

            assertEquals(listOf(orphan), attachments.getOrphanOneTimeAttachments().map { it.id })
        } finally {
            database.close()
        }
    }
}
