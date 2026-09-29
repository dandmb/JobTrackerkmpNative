package com.dmb.joblog.domain.attachment

import com.dmb.joblog.domain.model.AttachmentFormat
import com.dmb.joblog.domain.model.AttachmentKind
import com.dmb.joblog.i18n.AppLanguage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AttachmentRulesTest {

    private val pdf = byteArrayOf(0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x37)
    private val png = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
    private val jpg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 0, 0x10, 0x4A, 0x46)
    private val ole = byteArrayOf(0xD0.toByte(), 0xCF.toByte(), 0x11, 0xE0.toByte(), 0xA1.toByte(), 0xB1.toByte(), 0x1A, 0xE1.toByte())
    private val zip = byteArrayOf(0x50, 0x4B, 0x03, 0x04, 0x14, 0, 0x06, 0)

    private fun validate(name: String?, mime: String?, header: ByteArray, size: Long = 1_000) =
        AttachmentRules.validate(name, mime, size, header, AppLanguage.EN)

    @Test
    fun maximumSize_isTenMebibytes() {
        assertEquals(10L * 1024 * 1024, AttachmentRules.MAX_SIZE_BYTES)
    }

    @Test
    fun validFiles_areRecognisedByTheirSignature() {
        assertEquals(AttachmentValidation.Valid(AttachmentFormat.PDF), validate("cv.pdf", "application/pdf", pdf))
        assertEquals(AttachmentValidation.Valid(AttachmentFormat.PNG), validate("scan.png", null, png))
        assertEquals(AttachmentValidation.Valid(AttachmentFormat.JPG), validate("photo.jpeg", "image/jpeg", jpg))
        assertEquals(AttachmentValidation.Valid(AttachmentFormat.DOC), validate("lettre.doc", null, ole))
        assertEquals(AttachmentValidation.Valid(AttachmentFormat.DOCX), validate(null, AttachmentFormat.DOCX.mimeType, zip))
    }

    @Test
    fun signatureWins_overAMisleadingExtension() {
        assertEquals(AttachmentValidation.Valid(AttachmentFormat.PDF), validate("cv.png", null, pdf))
    }

    @Test
    fun renamedOrUnsupportedFiles_areRejected() {
        assertIs<AttachmentValidation.Invalid>(validate("fake.pdf", "application/pdf", "hello!!!".encodeToByteArray()))
        assertIs<AttachmentValidation.Invalid>(validate("archive.zip", "application/zip", zip))
        assertIs<AttachmentValidation.Invalid>(validate("sheet.xls", "application/vnd.ms-excel", ole))
        assertIs<AttachmentValidation.Invalid>(validate("cv.pdf", "application/pdf", pdf, size = 0))
    }

    @Test
    fun sizeLimit_isInclusive() {
        assertIs<AttachmentValidation.Valid>(validate("cv.pdf", null, pdf, size = AttachmentRules.MAX_SIZE_BYTES))
        val tooBig = validate("cv.pdf", null, pdf, size = AttachmentRules.MAX_SIZE_BYTES + 1)
        assertEquals(AttachmentValidation.Invalid("This file is 10 MB: the maximum size is 10 MB."), tooBig)
    }

    @Test
    fun tooLargeMessage_givesTheActualSizeInEachLanguage() {
        val size = (14.2 * 1024 * 1024).toLong()
        assertEquals("Ce fichier fait 14,2 Mo : la taille maximale est de 10 Mo.", AttachmentRules.tooLargeMessage(size, AppLanguage.FR))
        assertEquals("This file is 14.2 MB: the maximum size is 10 MB.", AttachmentRules.tooLargeMessage(size, AppLanguage.EN))
    }

    @Test
    fun sizeLabel_usesKilobytesBelowOneMebibyte() {
        assertEquals("1 Ko", AttachmentRules.sizeLabel(10, AppLanguage.FR))
        assertEquals("250 KB", AttachmentRules.sizeLabel(250 * 1024, AppLanguage.EN))
        assertEquals("1,5 Mo", AttachmentRules.sizeLabel(1536 * 1024, AppLanguage.FR))
    }

    @Test
    fun defaultDisplayName_isTheFileNameWithoutExtension_orTheKindLabel() {
        assertEquals("CV Data - FR", AttachmentRules.defaultDisplayName("CV Data - FR.pdf", AttachmentKind.CV, AppLanguage.FR))
        assertEquals("Lettre de motivation", AttachmentRules.defaultDisplayName("  ", AttachmentKind.COVER_LETTER, AppLanguage.FR))
        assertEquals("Cover letter", AttachmentRules.defaultDisplayName(null, AttachmentKind.COVER_LETTER, AppLanguage.EN))
    }

    @Test
    fun declaredFormat_readsExtensionThenMimeType() {
        assertEquals(AttachmentFormat.DOCX, AttachmentRules.declaredFormat("Lettre.DOCX", null))
        assertEquals(AttachmentFormat.PDF, AttachmentRules.declaredFormat("no-extension", "application/pdf"))
        assertNull(AttachmentRules.declaredFormat("notes.txt", "text/plain"))
    }

    @Test
    fun messages_existInBothLanguages_andDiffer() {
        for (message in listOf<(AppLanguage) -> String>(
            AttachmentRules::unsupportedFormatMessage,
            AttachmentRules::missingFileMessage,
            AttachmentRules::importFailedMessage,
            { AttachmentRules.stillInUseMessage(2, it) },
        )) {
            assertTrue(message(AppLanguage.FR) != message(AppLanguage.EN))
        }
        assertEquals("Utilisé par 1 candidature : retire-le d'abord de cette candidature.", AttachmentRules.stillInUseMessage(1, AppLanguage.FR))
        assertEquals("Used by 3 applications: remove it from those applications first.", AttachmentRules.stillInUseMessage(3, AppLanguage.EN))
    }
}
