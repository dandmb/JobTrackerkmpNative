package com.dmb.joblog.presentation.attachments

import com.dmb.joblog.domain.model.Attachment
import com.dmb.joblog.domain.model.AttachmentKind
import com.dmb.joblog.domain.repository.AttachmentDeletionResult
import com.dmb.joblog.domain.repository.AttachmentImportResult
import com.dmb.joblog.domain.repository.AttachmentRepository
import com.dmb.joblog.i18n.AppLanguage
import com.rickclephas.kmp.nativecoroutines.NativeCoroutines
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

data class AttachmentsState(
    val attachments: List<Attachment> = emptyList(),
    val isLoading: Boolean = true,
) {
    fun library(kind: AttachmentKind): List<Attachment> = attachments.filter { it.inLibrary && it.kind == kind }

    fun byId(id: Long?): Attachment? = id?.let { wanted -> attachments.firstOrNull { it.id == wanted } }

    val librarySizeBytes: Long get() = attachments.filter { it.inLibrary && !it.isMissing }.sumOf { it.sizeBytes }
}

class AttachmentsViewModel internal constructor(
    private val repository: AttachmentRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _state = MutableStateFlow(AttachmentsState())

    @NativeCoroutinesState
    val state: StateFlow<AttachmentsState> = _state.asStateFlow()

    init {
        repository.getAll()
            .onEach { _state.value = AttachmentsState(attachments = it, isLoading = false) }
            .launchIn(scope)
    }

    fun newTemporaryPath(): String = repository.newTemporaryPath()

    fun pathOf(attachment: Attachment): String = repository.pathOf(attachment)

    @NativeCoroutines
    suspend fun importCopiedFile(
        temporaryPath: String,
        originalFileName: String?,
        mimeType: String?,
        kind: AttachmentKind,
        inLibrary: Boolean,
        language: AppLanguage,
    ): AttachmentImportResult = repository.importCopiedFile(temporaryPath, originalFileName, mimeType, kind, inLibrary, language)

    @NativeCoroutines
    suspend fun rename(attachmentId: Long, displayName: String): Unit = repository.rename(attachmentId, displayName)

    @NativeCoroutines
    suspend fun deleteFromLibrary(attachmentId: Long): AttachmentDeletionResult = repository.deleteFromLibrary(attachmentId)

    fun onCleared() {
        scope.cancel()
    }
}
