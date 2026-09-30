package com.dmb.joblog.domain.attachment

import com.dmb.joblog.domain.model.AttachmentFormat
import com.dmb.joblog.domain.model.AttachmentKind
import com.dmb.joblog.i18n.AppLanguage
import com.dmb.joblog.i18n.pick
import kotlin.math.roundToLong

sealed class AttachmentValidation {
    data class Valid(val format: AttachmentFormat) : AttachmentValidation()
    data class Invalid(val message: String) : AttachmentValidation()
}

object AttachmentRules {
    const val MAX_SIZE_BYTES: Long = 10L * 1024 * 1024
    const val HEADER_BYTE_COUNT = 8

    fun declaredFormat(fileName: String?, mimeType: String?): AttachmentFormat? {
        val extension = fileName?.substringAfterLast('.', "")?.lowercase().orEmpty()
        val byExtension = when (extension) {
            "jpeg" -> AttachmentFormat.JPG
            else -> AttachmentFormat.entries.firstOrNull { it.extension == extension }
        }
        val byMime = AttachmentFormat.entries.firstOrNull { it.mimeType.equals(mimeType?.trim(), ignoreCase = true) }
        return byExtension ?: byMime
    }

    fun detectedFormat(header: ByteArray, declared: AttachmentFormat?): AttachmentFormat? {
        fun startsWith(vararg bytes: Int) = header.size >= bytes.size && bytes.indices.all { header[it] == bytes[it].toByte() }
        return when {
            startsWith(0x25, 0x50, 0x44, 0x46) -> AttachmentFormat.PDF
            startsWith(0x89, 0x50, 0x4E, 0x47) -> AttachmentFormat.PNG
            startsWith(0xFF, 0xD8, 0xFF) -> AttachmentFormat.JPG
            startsWith(0xD0, 0xCF, 0x11, 0xE0) && declared == AttachmentFormat.DOC -> AttachmentFormat.DOC
            startsWith(0x50, 0x4B, 0x03, 0x04) && declared == AttachmentFormat.DOCX -> AttachmentFormat.DOCX
            else -> null
        }
    }

    fun validate(
        fileName: String?,
        mimeType: String?,
        sizeBytes: Long,
        header: ByteArray,
        language: AppLanguage,
    ): AttachmentValidation {
        if (sizeBytes > MAX_SIZE_BYTES) return AttachmentValidation.Invalid(tooLargeMessage(sizeBytes, language))
        val declared = declaredFormat(fileName, mimeType)
        val detected = if (sizeBytes <= 0) null else detectedFormat(header, declared)
        return if (detected == null) AttachmentValidation.Invalid(unsupportedFormatMessage(language)) else AttachmentValidation.Valid(detected)
    }

    fun defaultDisplayName(fileName: String?, kind: AttachmentKind, language: AppLanguage): String {
        val base = fileName?.substringAfterLast('/')?.let { if ('.' in it) it.substringBeforeLast('.') else it }?.trim().orEmpty()
        return base.ifEmpty { kindLabel(kind, language) }
    }

    fun kindLabel(kind: AttachmentKind, language: AppLanguage): String = when (kind) {
        AttachmentKind.CV -> language.pick(en = "CV", fr = "CV")
        AttachmentKind.COVER_LETTER -> language.pick(en = "Cover letter", fr = "Lettre de motivation")
    }

    fun sizeLabel(sizeBytes: Long, language: AppLanguage): String {
        val kilo = 1024.0
        val (value, unit) = when {
            sizeBytes >= kilo * kilo -> sizeBytes / (kilo * kilo) to language.pick(en = "MB", fr = "Mo")
            else -> maxOf(1.0, sizeBytes / kilo) to language.pick(en = "KB", fr = "Ko")
        }
        val tenths = (value * 10).roundToLong()
        val text = if (tenths % 10 == 0L) "${tenths / 10}" else "${tenths / 10}${language.pick(en = ".", fr = ",")}${tenths % 10}"
        return "$text $unit"
    }

    fun tooLargeMessage(sizeBytes: Long, language: AppLanguage): String = language.pick(
        en = "This file is ${sizeLabel(sizeBytes, language)}: the maximum size is ${sizeLabel(MAX_SIZE_BYTES, language)}.",
        fr = "Ce fichier fait ${sizeLabel(sizeBytes, language)} : la taille maximale est de ${sizeLabel(MAX_SIZE_BYTES, language)}.",
    )

    fun unsupportedFormatMessage(language: AppLanguage): String = language.pick(
        en = "This file can't be added: only PDF, Word (DOC, DOCX) and image (JPG, PNG) files are accepted.",
        fr = "Ce fichier ne peut pas être ajouté : seuls les fichiers PDF, Word (DOC, DOCX) et image (JPG, PNG) sont acceptés.",
    )

    fun stillInUseMessage(usageCount: Int, language: AppLanguage): String = if (usageCount > 1) {
        language.pick(
            en = "Used by $usageCount applications: remove it from those applications first.",
            fr = "Utilisé par $usageCount candidatures : retire-le d'abord de ces candidatures.",
        )
    } else {
        language.pick(
            en = "Used by 1 application: remove it from that application first.",
            fr = "Utilisé par 1 candidature : retire-le d'abord de cette candidature.",
        )
    }

    fun missingFileMessage(language: AppLanguage): String = language.pick(
        en = "File not found on this device (it is not included in backups). You can remove it or import it again.",
        fr = "Fichier introuvable sur cet appareil (il n'est pas inclus dans les sauvegardes). Tu peux le retirer ou l'importer à nouveau.",
    )

    fun importFailedMessage(language: AppLanguage): String = language.pick(
        en = "The file couldn't be copied into the app. Please try again.",
        fr = "Le fichier n'a pas pu être copié dans l'application. Réessaie.",
    )

    fun actionFailedMessage(language: AppLanguage): String = language.pick(
        en = "This action couldn't be completed. Please try again.",
        fr = "Cette action n'a pas pu aboutir. Réessaie.",
    )
}
