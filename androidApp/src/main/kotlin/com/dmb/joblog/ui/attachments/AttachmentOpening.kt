package com.dmb.joblog.ui.attachments

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.dmb.joblog.data.files.attachmentShareDirectory
import com.dmb.joblog.domain.model.Attachment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

fun shareFileName(attachment: Attachment): String {
    val base = attachment.displayName.replace(Regex("""[\\/:*?"<>|\p{Cntrl}]"""), "_").trim().ifEmpty { "document" }.take(80)
    return "$base.${attachment.format.extension}"
}

suspend fun openWithAnotherApp(context: Context, attachment: Attachment, storedPath: String): Boolean {
    val shared = withContext(Dispatchers.IO) {
        val directory = attachmentShareDirectory()
        directory.deleteRecursively()
        directory.mkdirs()
        File(storedPath).copyTo(File(directory, shareFileName(attachment)), overwrite = true)
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", shared)
    val intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(uri, attachment.format.mimeType)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    return try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        shared.delete()
        false
    }
}
