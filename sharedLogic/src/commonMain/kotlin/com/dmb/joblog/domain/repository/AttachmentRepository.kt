package com.dmb.joblog.domain.repository

import com.dmb.joblog.domain.model.Attachment
import com.dmb.joblog.domain.model.AttachmentKind
import com.dmb.joblog.i18n.AppLanguage
import com.rickclephas.kmp.nativecoroutines.NativeCoroutines
import kotlinx.coroutines.flow.Flow

sealed class AttachmentImportResult {
    data class Imported(val attachment: Attachment) : AttachmentImportResult()
    data class Rejected(val message: String) : AttachmentImportResult()
}

sealed class AttachmentDeletionResult {
    data object Deleted : AttachmentDeletionResult()
    data class StillInUse(val usageCount: Int) : AttachmentDeletionResult()
}

interface AttachmentRepository {
    @NativeCoroutines
    fun getAll(): Flow<List<Attachment>>

    fun newTemporaryPath(): String

    fun pathOf(attachment: Attachment): String

    @NativeCoroutines
    suspend fun importCopiedFile(
        temporaryPath: String,
        originalFileName: String?,
        mimeType: String?,
        kind: AttachmentKind,
        inLibrary: Boolean,
        language: AppLanguage,
    ): AttachmentImportResult

    @NativeCoroutines
    suspend fun rename(attachmentId: Long, displayName: String)

    @NativeCoroutines
    suspend fun deleteFromLibrary(attachmentId: Long): AttachmentDeletionResult

    @NativeCoroutines
    suspend fun deleteIfUnusedOneTime(attachmentIds: Set<Long>)

    @NativeCoroutines
    suspend fun cleanUp()
}
