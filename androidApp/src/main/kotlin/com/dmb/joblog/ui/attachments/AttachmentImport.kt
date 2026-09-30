package com.dmb.joblog.ui.attachments

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import com.dmb.joblog.domain.attachment.AttachmentRules
import com.dmb.joblog.domain.model.AttachmentFormat
import com.dmb.joblog.domain.model.AttachmentKind
import com.dmb.joblog.domain.repository.AttachmentImportResult
import com.dmb.joblog.i18n.AppLanguage
import com.dmb.joblog.presentation.attachments.AttachmentsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

val attachmentPickerMimeTypes: Array<String> = AttachmentFormat.mimeTypes.toTypedArray()

@Composable
fun rememberAttachmentPicker(onPicked: (Uri) -> Unit): ManagedActivityResultLauncher<Array<String>, Uri?> =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) onPicked(uri) }

suspend fun importPickedFile(
    context: Context,
    uri: Uri,
    kind: AttachmentKind,
    inLibrary: Boolean,
    viewModel: AttachmentsViewModel,
    language: AppLanguage,
): AttachmentImportResult = withContext(Dispatchers.IO) {
    val resolver = context.contentResolver
    var name: String? = null
    var declaredSize: Long? = null
    resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (nameIndex >= 0 && !cursor.isNull(nameIndex)) name = cursor.getString(nameIndex)
            if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) declaredSize = cursor.getLong(sizeIndex)
        }
    }
    declaredSize?.let { size ->
        if (size > AttachmentRules.MAX_SIZE_BYTES) return@withContext AttachmentImportResult.Rejected(AttachmentRules.tooLargeMessage(size, language))
    }
    val temporaryFile = File(viewModel.newTemporaryPath())
    val copiedSize = try {
        resolver.openInputStream(uri)?.use { input -> temporaryFile.outputStream().use { output -> copyUpToLimit(input, output) } }
    } catch (_: IOException) {
        null
    } catch (_: SecurityException) {
        null
    }
    when {
        copiedSize == null -> {
            temporaryFile.delete()
            AttachmentImportResult.Rejected(AttachmentRules.importFailedMessage(language))
        }
        copiedSize > AttachmentRules.MAX_SIZE_BYTES -> {
            temporaryFile.delete()
            AttachmentImportResult.Rejected(AttachmentRules.tooLargeMessage(copiedSize, language))
        }
        else -> viewModel.importCopiedFile(temporaryFile.absolutePath, name, resolver.getType(uri), kind, inLibrary, language)
    }
}

private fun copyUpToLimit(input: InputStream, output: OutputStream): Long {
    val buffer = ByteArray(64 * 1024)
    var total = 0L
    while (true) {
        val read = input.read(buffer)
        if (read < 0) return total
        if (total + read <= AttachmentRules.MAX_SIZE_BYTES) output.write(buffer, 0, read)
        total += read
    }
}
