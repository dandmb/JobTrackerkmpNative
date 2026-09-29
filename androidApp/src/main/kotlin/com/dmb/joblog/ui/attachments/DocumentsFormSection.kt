package com.dmb.joblog.ui.attachments

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.dmb.joblog.R
import com.dmb.joblog.domain.attachment.AttachmentRules
import com.dmb.joblog.domain.model.Attachment
import com.dmb.joblog.domain.model.AttachmentKind
import com.dmb.joblog.domain.repository.AttachmentImportResult
import com.dmb.joblog.i18n.AppLanguage
import com.dmb.joblog.presentation.attachments.AttachmentsViewModel
import com.dmb.joblog.ui.i18n.rememberAppLanguage
import kotlinx.coroutines.launch

@Composable
fun DocumentsFormSection(
    cvAttachmentId: Long?,
    coverLetterAttachmentId: Long?,
    onCvChanged: (Long?) -> Unit,
    onCoverLetterChanged: (Long?) -> Unit,
    viewModel: AttachmentsViewModel,
) {
    val state by viewModel.state.collectAsState()
    val language = rememberAppLanguage()
    var message by remember { mutableStateOf<String?>(null) }
    val open = rememberAttachmentOpener(viewModel) { message = it }

    Text(stringResource(R.string.form_section_documents), style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(8.dp))
    DocumentSlot(
        kind = AttachmentKind.CV,
        label = stringResource(R.string.form_document_cv),
        attachment = state.byId(cvAttachmentId),
        library = state.library(AttachmentKind.CV),
        language = language,
        viewModel = viewModel,
        onChanged = onCvChanged,
        onOpen = open,
        onMessage = { message = it },
    )
    Spacer(modifier = Modifier.height(8.dp))
    DocumentSlot(
        kind = AttachmentKind.COVER_LETTER,
        label = stringResource(R.string.form_document_letter),
        attachment = state.byId(coverLetterAttachmentId),
        library = state.library(AttachmentKind.COVER_LETTER),
        language = language,
        viewModel = viewModel,
        onChanged = onCoverLetterChanged,
        onOpen = open,
        onMessage = { message = it },
    )
    message?.let {
        Text(
            it,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp).semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

@Composable
private fun DocumentSlot(
    kind: AttachmentKind,
    label: String,
    attachment: Attachment?,
    library: List<Attachment>,
    language: AppLanguage,
    viewModel: AttachmentsViewModel,
    onChanged: (Long?) -> Unit,
    onOpen: (Attachment) -> Unit,
    onMessage: (String?) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var choicesExpanded by remember { mutableStateOf(false) }
    var optionsExpanded by remember { mutableStateOf(false) }
    var choosingFromLibrary by remember { mutableStateOf(false) }
    var importInLibrary by remember { mutableStateOf(true) }
    var importing by remember { mutableStateOf(false) }
    val importingLabel = stringResource(R.string.documents_importing)

    val picker = rememberAttachmentPicker { uri ->
        scope.launch {
            importing = true
            onMessage(null)
            when (val result = importPickedFile(context, uri, kind, importInLibrary, viewModel, language)) {
                is AttachmentImportResult.Imported -> onChanged(result.attachment.id)
                is AttachmentImportResult.Rejected -> onMessage(result.message)
            }
            importing = false
        }
    }
    val startImport: (Boolean) -> Unit = { inLibrary ->
        importInLibrary = inLibrary
        picker.launch(attachmentPickerMimeTypes)
    }

    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        ListItem(
            overlineContent = { Text(label) },
            headlineContent = { Text(attachment?.displayName ?: stringResource(R.string.form_document_none)) },
            supportingContent = attachment?.let { document ->
                {
                    if (document.isMissing) {
                        Text(AttachmentRules.missingFileMessage(language), color = MaterialTheme.colorScheme.error)
                    } else {
                        val details = "${document.format.name} · ${AttachmentRules.sizeLabel(document.sizeBytes, language)}"
                        Text(if (document.inLibrary) details else "$details · ${stringResource(R.string.form_document_one_time)}")
                    }
                }
            },
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (attachment == null) {
                        Box {
                            val addDescription = stringResource(R.string.form_document_add_a11y, label)
                            TextButton(
                                onClick = { choicesExpanded = true },
                                modifier = Modifier.semantics { this.contentDescription = addDescription },
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Text(stringResource(R.string.form_document_add), modifier = Modifier.padding(start = 4.dp))
                            }
                            ImportChoicesMenu(choicesExpanded, { choicesExpanded = false }, { choosingFromLibrary = true }, startImport)
                        }
                    } else {
                        if (!attachment.isMissing) {
                            IconButton(onClick = { onOpen(attachment) }) {
                                Icon(
                                    Icons.Outlined.Visibility,
                                    contentDescription = stringResource(
                                        if (attachment.format.isPreviewableInApp) R.string.documents_open else R.string.preview_open_with,
                                    ) + ", " + attachment.displayName,
                                )
                            }
                        }
                        Box {
                            IconButton(onClick = { optionsExpanded = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.form_document_options_a11y, label))
                            }
                            DropdownMenu(expanded = optionsExpanded, onDismissRequest = { optionsExpanded = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.form_document_replace)) },
                                    onClick = { optionsExpanded = false; choicesExpanded = true },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.form_document_remove)) },
                                    onClick = { optionsExpanded = false; onMessage(null); onChanged(null) },
                                )
                            }
                            ImportChoicesMenu(choicesExpanded, { choicesExpanded = false }, { choosingFromLibrary = true }, startImport)
                        }
                    }
                }
            },
        )
        if (importing) LinearProgressIndicator(modifier = Modifier.fillMaxWidth().semantics { contentDescription = importingLabel })
    }

    if (choosingFromLibrary) {
        LibraryChoiceDialog(
            title = label,
            documents = library,
            language = language,
            onDismiss = { choosingFromLibrary = false },
            onChosen = { chosen ->
                choosingFromLibrary = false
                onMessage(null)
                onChanged(chosen.id)
            },
        )
    }
}

@Composable
private fun ImportChoicesMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onChooseFromLibrary: () -> Unit,
    onImport: (inLibrary: Boolean) -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(text = { Text(stringResource(R.string.form_document_choose_library)) }, onClick = { onDismiss(); onChooseFromLibrary() })
        DropdownMenuItem(text = { Text(stringResource(R.string.form_document_import_library)) }, onClick = { onDismiss(); onImport(true) })
        DropdownMenuItem(text = { Text(stringResource(R.string.form_document_import_once)) }, onClick = { onDismiss(); onImport(false) })
    }
}

@Composable
private fun LibraryChoiceDialog(
    title: String,
    documents: List<Attachment>,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onChosen: (Attachment) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            if (documents.isEmpty()) {
                Text(stringResource(R.string.form_document_library_empty))
            } else {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    documents.forEach { document ->
                        ListItem(
                            headlineContent = { Text(document.displayName) },
                            supportingContent = {
                                Text(
                                    if (document.isMissing) stringResource(R.string.documents_missing)
                                    else "${document.format.name} · ${AttachmentRules.sizeLabel(document.sizeBytes, language)}",
                                )
                            },
                            modifier = Modifier.fillMaxWidth().clickable(enabled = !document.isMissing) { onChosen(document) },
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
