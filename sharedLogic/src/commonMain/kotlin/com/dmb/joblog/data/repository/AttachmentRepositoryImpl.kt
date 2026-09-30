package com.dmb.joblog.data.repository

import com.dmb.joblog.data.files.AttachmentFileStore
import com.dmb.joblog.data.local.dao.AttachmentDao
import com.dmb.joblog.data.local.entity.AttachmentEntity
import com.dmb.joblog.domain.attachment.AttachmentRules
import com.dmb.joblog.domain.attachment.AttachmentValidation
import com.dmb.joblog.domain.model.Attachment
import com.dmb.joblog.domain.model.AttachmentFormat
import com.dmb.joblog.domain.model.AttachmentKind
import com.dmb.joblog.domain.repository.AttachmentDeletionResult
import com.dmb.joblog.domain.repository.AttachmentImportResult
import com.dmb.joblog.domain.repository.AttachmentRepository
import com.dmb.joblog.i18n.AppLanguage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal class AttachmentRepositoryImpl(
    private val dao: AttachmentDao,
    private val fileStore: AttachmentFileStore,
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    @OptIn(ExperimentalUuidApi::class)
    private val newStorageId: () -> String = { Uuid.random().toString() },
) : AttachmentRepository {

    override fun getAll(): Flow<List<Attachment>> =
        dao.getAllWithUsage().map { list ->
            list.map { it.attachment.toDomain(usageCount = it.usageCount, isMissing = !fileStore.exists(it.attachment.storageName)) }
        }

    override fun newTemporaryPath(): String = fileStore.newTemporaryPath()

    override fun pathOf(attachment: Attachment): String = fileStore.pathOf(attachment.storageName)

    override suspend fun importCopiedFile(
        temporaryPath: String,
        originalFileName: String?,
        mimeType: String?,
        kind: AttachmentKind,
        inLibrary: Boolean,
        language: AppLanguage,
    ): AttachmentImportResult {
        try {
            val size = fileStore.sizeOf(temporaryPath)
            val header = if (size > 0) fileStore.readHeader(temporaryPath, AttachmentRules.HEADER_BYTE_COUNT) else ByteArray(0)
            val format = when (val validation = AttachmentRules.validate(originalFileName, mimeType, size, header, language)) {
                is AttachmentValidation.Invalid -> {
                    fileStore.deleteTemporary(temporaryPath)
                    return AttachmentImportResult.Rejected(validation.message)
                }
                is AttachmentValidation.Valid -> validation.format
            }
            val storageName = "${newStorageId()}.${format.extension}"
            fileStore.commitTemporary(temporaryPath, storageName)
            val entity = AttachmentEntity(
                displayName = AttachmentRules.defaultDisplayName(originalFileName, kind, language),
                kind = kind.name,
                format = format.name,
                sizeBytes = size,
                addedAtEpochMillis = now(),
                inLibrary = inLibrary,
                storageName = storageName,
            )
            val id = try {
                dao.insert(entity)
            } catch (e: Exception) {
                fileStore.delete(storageName)
                throw e
            }
            return AttachmentImportResult.Imported(entity.copy(id = id).toDomain(usageCount = 0, isMissing = false))
        } catch (e: CancellationException) {
            fileStore.deleteTemporary(temporaryPath)
            throw e
        } catch (e: Exception) {
            fileStore.deleteTemporary(temporaryPath)
            return AttachmentImportResult.Rejected(AttachmentRules.importFailedMessage(language))
        }
    }

    override suspend fun rename(attachmentId: Long, displayName: String) {
        val name = displayName.trim()
        if (name.isNotEmpty()) dao.rename(attachmentId, name)
    }

    override suspend fun deleteFromLibrary(attachmentId: Long): AttachmentDeletionResult {
        val usage = dao.usageCount(attachmentId)
        if (usage > 0) return AttachmentDeletionResult.StillInUse(usage)
        val entity = dao.getById(attachmentId) ?: return AttachmentDeletionResult.Deleted
        dao.deleteById(attachmentId)
        fileStore.delete(entity.storageName)
        return AttachmentDeletionResult.Deleted
    }

    override suspend fun deleteIfUnusedOneTime(attachmentIds: Set<Long>) {
        for (id in attachmentIds) {
            val entity = dao.getById(id) ?: continue
            if (!entity.inLibrary && dao.usageCount(id) == 0) {
                dao.deleteById(id)
                fileStore.delete(entity.storageName)
            }
        }
    }

    override suspend fun cleanUp() {
        fileStore.clearTemporaryFiles()
        dao.getOrphanOneTimeAttachments().forEach { orphan ->
            dao.deleteById(orphan.id)
            fileStore.delete(orphan.storageName)
        }
        val known = dao.getAllStorageNames().toSet()
        fileStore.storedNames().filterNot { it in known }.forEach(fileStore::delete)
    }
}

internal fun AttachmentEntity.toDomain(usageCount: Int, isMissing: Boolean): Attachment = Attachment(
    id = id,
    displayName = displayName,
    kind = AttachmentKind.valueOf(kind),
    format = AttachmentFormat.valueOf(format),
    sizeBytes = sizeBytes,
    addedAtEpochMillis = addedAtEpochMillis,
    inLibrary = inLibrary,
    storageName = storageName,
    usageCount = usageCount,
    isMissing = isMissing,
)
