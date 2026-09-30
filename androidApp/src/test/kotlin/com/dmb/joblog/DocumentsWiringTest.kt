package com.dmb.joblog

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DocumentsWiringTest {

    private fun read(relative: String): String = listOf(relative, "androidApp/$relative").map(::File).first { it.exists() }.readText()

    private val ui = "src/main/kotlin/com/dmb/joblog/ui"
    private val appRoot = read("$ui/AppRoot.kt")
    private val settings = read("$ui/settings/SettingsScreen.kt")
    private val documents = read("$ui/attachments/DocumentsScreen.kt")
    private val formSection = read("$ui/attachments/DocumentsFormSection.kt")
    private val import = read("$ui/attachments/AttachmentImport.kt")
    private val opening = read("$ui/attachments/AttachmentOpening.kt")
    private val preview = read("$ui/attachments/AttachmentPreviewDialog.kt")
    private val opener = read("$ui/attachments/AttachmentOpener.kt")
    private val form = read("$ui/joboffer/JobOfferFormSheet.kt")
    private val card = read("$ui/joboffer/JobOfferCard.kt")
    private val manifest = read("src/main/AndroidManifest.xml")
    private val paths = read("src/main/res/xml/attachment_paths.xml")

    @Test
    fun documentsScreen_isReachedFromSettings_andGoesBackToSettings() {
        assertTrue(settings.contains("content.documentsRowLabel"))
        assertTrue(settings.contains("onClick = onOpenDocuments"))
        assertTrue(appRoot.contains("onOpenDocuments = { overlay = OverlayScreen.DOCUMENTS }"))
        assertTrue(appRoot.contains("OverlayScreen.DOCUMENTS -> DocumentsScreen(onBack = { overlay = OverlayScreen.SETTINGS })"))
        assertTrue(appRoot.contains("OverlayScreen.DOCUMENTS, OverlayScreen.ABOUT, OverlayScreen.PRIVACY -> OverlayScreen.SETTINGS"), "retour système : Documents → Réglages")
    }

    @Test
    fun import_usesTheSystemPicker_copiesTheFile_andNeverKeepsTheOriginalUri() {
        assertTrue(import.contains("ActivityResultContracts.OpenDocument()"))
        assertTrue(import.contains("AttachmentFormat.mimeTypes"))
        assertTrue(import.contains("resolver.openInputStream(uri)"))
        assertTrue(import.contains("viewModel.importCopiedFile(temporaryFile.absolutePath"), "le ViewModel reçoit une copie locale, pas l'URI")
        assertTrue(import.contains("AttachmentRules.MAX_SIZE_BYTES"), "taille contrôlée avant et pendant la copie")
        listOf("takePersistableUriPermission", "uri.toString()").forEach { assertFalse(import.contains(it), "« $it » : l'URI d'origine ne doit pas être conservée") }
    }

    @Test
    fun libraryChoice_cannotAttachAFileMissingFromTheDevice() {
        assertTrue(formSection.contains("clickable(enabled = !document.isMissing) { onChosen(document) }"))
    }

    @Test
    fun manifest_declaresNoPermission_andAPrivateFileProvider() {
        assertFalse(manifest.contains("uses-permission"), "l'import ponctuel ne demande aucune permission")
        assertTrue(manifest.contains("androidx.core.content.FileProvider"))
        assertTrue(manifest.contains("android:authorities=\"\${applicationId}.fileprovider\""))
        val provider = manifest.substringAfter("androidx.core.content.FileProvider").substringBefore("</provider>")
        assertTrue(provider.contains("android:exported=\"false\""))
        assertTrue(provider.contains("android:grantUriPermissions=\"true\""))
    }

    @Test
    fun fileProvider_onlyExposesTheShareCopyDirectoryInTheCache() {
        assertTrue(paths.contains("<cache-path name=\"shared_attachments\" path=\"shared_attachments/\" />"))
        listOf("root-path", "files-path", "external-path", "external-files-path", "external-cache-path").forEach {
            assertFalse(paths.contains(it), "« $it » exposerait plus que la copie de partage")
        }
    }

    @Test
    fun openWith_sharesATemporaryCopyThroughTheFileProvider_andHandlesNoApp() {
        assertTrue(opening.contains("attachmentShareDirectory()"))
        assertTrue(opening.contains("FileProvider.getUriForFile(context, \"\${context.packageName}.fileprovider\""))
        assertTrue(opening.contains("Intent.FLAG_GRANT_READ_URI_PERMISSION"))
        assertTrue(opening.contains("catch (_: ActivityNotFoundException)"))
        assertTrue(opener.contains("R.string.preview_no_app"))
    }

    @Test
    fun preview_isInApp_forPdfAndImagesOnly() {
        assertTrue(opener.contains("if (attachment.format.isPreviewableInApp) previewing = attachment else openWith(attachment)"))
        assertTrue(preview.contains("PdfRenderer(descriptor)"))
        assertTrue(preview.contains("ParcelFileDescriptor.MODE_READ_ONLY"))
        assertTrue(preview.contains("R.string.preview_page_a11y"), "chaque page annoncée par TalkBack")
    }

    @Test
    fun form_keepsTheChosenDocuments_whenSaving() {
        assertTrue(form.contains("existingOffer?.cvAttachmentId"))
        assertTrue(form.contains("cvAttachmentId = cvAttachmentId,"))
        assertTrue(form.contains("coverLetterAttachmentId = coverLetterAttachmentId,"))
        assertTrue(form.contains("DocumentsFormSection("))
    }

    @Test
    fun ruleMessages_comeFromTheSharedRules() {
        assertTrue(documents.contains("AttachmentRules.stillInUseMessage("))
        assertTrue(documents.contains("AttachmentRules.missingFileMessage("))
        assertTrue(formSection.contains("AttachmentRules.missingFileMessage("))
        assertTrue(documents.contains("AttachmentRules.sizeLabel(") && formSection.contains("AttachmentRules.sizeLabel("))
    }

    @Test
    fun card_showsThePaperclipIndicator_onlyWhenADocumentIsAttached() {
        assertTrue(card.contains("offer.cvAttachmentId?.let { stringResource(R.string.card_documents_cv) }"))
        assertTrue(card.contains("offer.coverLetterAttachmentId?.let { stringResource(R.string.card_documents_letter) }"))
        assertTrue(card.contains("if (documentLabels.isNotEmpty())"))
        assertTrue(card.contains("\"📎 \""))
    }

    @Test
    fun everyIconButton_inTheDocumentsUi_hasAnAccessibilityLabel() {
        for ((name, source) in listOf("DocumentsScreen" to documents, "DocumentsFormSection" to formSection, "AttachmentPreviewDialog" to preview)) {
            val blocks = source.split("IconButton(").drop(1).map { it.substringBefore("\n            }").substringBefore("\n                }") }
            assertTrue(blocks.isNotEmpty(), "aucun IconButton trouvé dans $name")
            blocks.forEach { block ->
                assertTrue(
                    block.contains("contentDescription = stringResource(") || block.contains("contentDescription = content."),
                    "IconButton sans libellé dans $name : ${block.take(120)}",
                )
            }
        }
    }

    @Test
    fun deletingALibraryDocument_asksForConfirmation() {
        assertEquals(1, Regex("""viewModel\.deleteFromLibrary\(""").findAll(documents).count())
        assertTrue(documents.contains("R.string.documents_delete_confirm_title"))
    }

    @Test
    fun renameAndDeletionErrors_showAMessageInsteadOfCrashing() {
        assertEquals(2, Regex("""AttachmentRules\.actionFailedMessage\(language\)""").findAll(documents).count(), "renommage et suppression")
        assertEquals(2, Regex("""catch \(e: CancellationException\) \{\s*throw e""").findAll(documents).count(), "l'annulation reste une annulation")
    }
}
