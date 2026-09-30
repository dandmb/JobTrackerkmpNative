package com.dmb.joblog.ui.attachments

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.createBitmap
import com.dmb.joblog.R
import com.dmb.joblog.domain.model.Attachment
import com.dmb.joblog.domain.model.AttachmentFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

private const val MAX_BITMAP_EDGE = 2048

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentPreviewDialog(
    attachment: Attachment,
    storedPath: String,
    onDismiss: () -> Unit,
    onOpenWith: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(attachment.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.preview_close))
                        }
                    },
                    actions = { TextButton(onClick = onOpenWith) { Text(stringResource(R.string.preview_open_with)) } },
                )
            },
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.surfaceVariant)) {
                when (attachment.format) {
                    AttachmentFormat.PDF -> PdfPages(storedPath)
                    AttachmentFormat.JPG, AttachmentFormat.PNG -> ImagePreview(storedPath, attachment.displayName)
                    AttachmentFormat.DOC, AttachmentFormat.DOCX -> PreviewFailed()
                }
            }
        }
    }
}

@Composable
private fun PreviewFailed() {
    Text(
        stringResource(R.string.preview_failed),
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(32.dp),
    )
}

private class PdfDocument(path: String) {
    private val descriptor = ParcelFileDescriptor.open(File(path), ParcelFileDescriptor.MODE_READ_ONLY)
    private val renderer = PdfRenderer(descriptor)
    private val mutex = Mutex()
    val pageCount: Int get() = renderer.pageCount

    fun aspectRatio(index: Int): Float = renderer.openPage(index).use { it.width.toFloat() / it.height }

    suspend fun render(index: Int, widthPx: Int): ImageBitmap = mutex.withLock {
        withContext(Dispatchers.IO) {
            renderer.openPage(index).use { page ->
                val width = widthPx.coerceIn(1, MAX_BITMAP_EDGE)
                val height = (width.toFloat() * page.height / page.width).toInt().coerceIn(1, MAX_BITMAP_EDGE * 2)
                val bitmap = createBitmap(width, height)
                bitmap.eraseColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmap.asImageBitmap()
            }
        }
    }

    fun close() {
        renderer.close()
        descriptor.close()
    }
}

@Composable
private fun PdfPages(storedPath: String) {
    val document = remember(storedPath) { runCatching { PdfDocument(storedPath) }.getOrNull() }
    DisposableEffect(document) { onDispose { document?.close() } }
    if (document == null || document.pageCount == 0) {
        PreviewFailed()
        return
    }
    val ratios = remember(document) { (0 until document.pageCount).map { document.aspectRatio(it) } }
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val widthPx = with(LocalDensity.current) { maxWidth.roundToPx() }
        LazyColumn(
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(document.pageCount) { index ->
                val pageLabel = stringResource(R.string.preview_page_a11y, index + 1, document.pageCount)
                val bitmap by produceState<ImageBitmap?>(null, document, index, widthPx) { value = document.render(index, widthPx) }
                Box(
                    modifier = Modifier.fillMaxWidth().aspectRatio(ratios[index]).background(ComposeColor.White)
                        .semantics { contentDescription = pageLabel },
                    contentAlignment = Alignment.Center,
                ) {
                    val page = bitmap
                    if (page == null) CircularProgressIndicator() else Image(page, contentDescription = null, modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
private fun ImagePreview(storedPath: String, description: String) {
    val image by produceState<Result<ImageBitmap>?>(null, storedPath) {
        value = withContext(Dispatchers.IO) { runCatching { decodeSampled(storedPath).asImageBitmap() } }
    }
    when (val result = image) {
        null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        else -> result.fold(
            onSuccess = { Image(it, contentDescription = description, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize()) },
            onFailure = { PreviewFailed() },
        )
    }
}

private fun decodeSampled(path: String): Bitmap {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    var sample = 1
    while (bounds.outWidth / sample > MAX_BITMAP_EDGE || bounds.outHeight / sample > MAX_BITMAP_EDGE) sample *= 2
    return checkNotNull(BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample }))
}
