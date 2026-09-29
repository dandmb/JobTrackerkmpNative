package com.dmb.joblog.domain.model

enum class AttachmentKind { CV, COVER_LETTER }

enum class AttachmentFormat(val extension: String, val mimeType: String) {
    PDF("pdf", "application/pdf"),
    DOC("doc", "application/msword"),
    DOCX("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
    JPG("jpg", "image/jpeg"),
    PNG("png", "image/png");

    val isPreviewableInApp: Boolean get() = this == PDF || this == JPG || this == PNG

    companion object {
        val mimeTypes: List<String> = entries.map { it.mimeType }
    }
}

data class Attachment(
    val id: Long,
    val displayName: String,
    val kind: AttachmentKind,
    val format: AttachmentFormat,
    val sizeBytes: Long,
    val addedAtEpochMillis: Long,
    val inLibrary: Boolean,
    val storageName: String,
    val usageCount: Int = 0,
    val isMissing: Boolean = false,
)
