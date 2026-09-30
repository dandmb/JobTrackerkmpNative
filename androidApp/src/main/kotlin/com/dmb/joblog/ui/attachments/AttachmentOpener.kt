package com.dmb.joblog.ui.attachments

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.dmb.joblog.R
import com.dmb.joblog.domain.model.Attachment
import com.dmb.joblog.presentation.attachments.AttachmentsViewModel
import kotlinx.coroutines.launch

@Composable
fun rememberAttachmentOpener(viewModel: AttachmentsViewModel, onMessage: (String) -> Unit): (Attachment) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val noAppMessage = stringResource(R.string.preview_no_app)
    var previewing by remember { mutableStateOf<Attachment?>(null) }
    val openWith: (Attachment) -> Unit = { attachment ->
        scope.launch { if (!openWithAnotherApp(context, attachment, viewModel.pathOf(attachment))) onMessage(noAppMessage) }
    }
    previewing?.let { attachment ->
        AttachmentPreviewDialog(
            attachment = attachment,
            storedPath = viewModel.pathOf(attachment),
            onDismiss = { previewing = null },
            onOpenWith = { openWith(attachment) },
        )
    }
    return { attachment -> if (attachment.format.isPreviewableInApp) previewing = attachment else openWith(attachment) }
}
