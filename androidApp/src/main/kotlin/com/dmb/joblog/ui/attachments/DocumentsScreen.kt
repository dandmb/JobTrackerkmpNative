package com.dmb.joblog.ui.attachments

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.dmb.joblog.R
import com.dmb.joblog.domain.attachment.AttachmentRules
import com.dmb.joblog.domain.model.Attachment
import com.dmb.joblog.domain.model.AttachmentKind
import com.dmb.joblog.domain.repository.AttachmentDeletionResult
import com.dmb.joblog.domain.repository.AttachmentImportResult
import com.dmb.joblog.i18n.AppLanguage
import com.dmb.joblog.presentation.attachments.AttachmentsViewModel
import com.dmb.joblog.presentation.settings.SettingsContent
import com.dmb.joblog.ui.i18n.rememberAppLanguage
import com.dmb.joblog.ui.theme.StatusBarIconsForPrimaryTopBar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsScreen(
    onBack: () -> Unit,
    viewModel: AttachmentsViewModel = koinInject(),
) {
    DisposableEffect(viewModel) { onDispose { viewModel.onCleared() } }
    val state by viewModel.state.collectAsState()
    val language = rememberAppLanguage()
    val content = SettingsContent.of(language)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val showMessage: (String) -> Unit = { message -> scope.launch { snackbarHostState.showSnackbar(message) } }
    val open = rememberAttachmentOpener(viewModel, showMessage)

    var importing by remember { mutableStateOf(false) }
    val importingLabel = stringResource(R.string.documents_importing)
    var pendingKind by rememberSaveable { mutableStateOf<AttachmentKind?>(null) }
    var renaming by remember { mutableStateOf<Attachment?>(null) }
    var deleting by remember { mutableStateOf<Attachment?>(null) }

    val picker = rememberAttachmentPicker { uri ->
        val kind = pendingKind ?: return@rememberAttachmentPicker
        pendingKind = null
        scope.launch {
            importing = true
            val result = importPickedFile(context, uri, kind, inLibrary = true, viewModel = viewModel, language = language)
            importing = false
            if (result is AttachmentImportResult.Rejected) showMessage(result.message)
        }
    }
    val addDocument: (AttachmentKind) -> Unit = { kind ->
        pendingKind = kind
        picker.launch(attachmentPickerMimeTypes)
    }

    StatusBarIconsForPrimaryTopBar()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(content.documentsRowLabel) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = content.backLabel)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (importing) LinearProgressIndicator(modifier = Modifier.fillMaxWidth().semantics { contentDescription = importingLabel })
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                documentSection(
                    title = R.string.documents_section_cv,
                    addLabel = R.string.documents_add_cv,
                    emptyLabel = R.string.documents_empty_cv,
                    documents = state.library(AttachmentKind.CV),
                    language = language,
                    onAdd = { addDocument(AttachmentKind.CV) },
                    onOpen = open,
                    onRename = { renaming = it },
                    onDelete = { deleting = it },
                )
                documentSection(
                    title = R.string.documents_section_letters,
                    addLabel = R.string.documents_add_letter,
                    emptyLabel = R.string.documents_empty_letters,
                    documents = state.library(AttachmentKind.COVER_LETTER),
                    language = language,
                    onAdd = { addDocument(AttachmentKind.COVER_LETTER) },
                    onOpen = open,
                    onRename = { renaming = it },
                    onDelete = { deleting = it },
                )
                if (state.librarySizeBytes > 0) {
                    item(key = "total") {
                        Text(
                            stringResource(R.string.documents_total_size, AttachmentRules.sizeLabel(state.librarySizeBytes, language)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }
        }
    }

    renaming?.let { attachment ->
        RenameDialog(
            attachment = attachment,
            onDismiss = { renaming = null },
            onConfirm = { name ->
                renaming = null
                scope.launch {
                    try {
                        viewModel.rename(attachment.id, name)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        showMessage(AttachmentRules.actionFailedMessage(language))
                    }
                }
            },
        )
    }
    deleting?.let { attachment ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.documents_delete_confirm_title)) },
            text = { Text(stringResource(R.string.documents_delete_confirm_message, attachment.displayName)) },
            confirmButton = {
                TextButton(onClick = {
                    deleting = null
                    scope.launch {
                        try {
                            val result = viewModel.deleteFromLibrary(attachment.id)
                            if (result is AttachmentDeletionResult.StillInUse) showMessage(AttachmentRules.stillInUseMessage(result.usageCount, language))
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Exception) {
                            showMessage(AttachmentRules.actionFailedMessage(language))
                        }
                    }
                }) { Text(stringResource(R.string.delete_action)) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

private fun LazyListScope.documentSection(
    title: Int,
    addLabel: Int,
    emptyLabel: Int,
    documents: List<Attachment>,
    language: AppLanguage,
    onAdd: () -> Unit,
    onOpen: (Attachment) -> Unit,
    onRename: (Attachment) -> Unit,
    onDelete: (Attachment) -> Unit,
) {
    item(key = "header-$title") {
        ListItem(
            headlineContent = { Text(stringResource(title), style = MaterialTheme.typography.titleMedium) },
            trailingContent = {
                TextButton(onClick = onAdd) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text(stringResource(addLabel), modifier = Modifier.padding(start = 4.dp))
                }
            },
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
        )
    }
    if (documents.isEmpty()) {
        item(key = "empty-$title") {
            Text(
                stringResource(emptyLabel),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
    items(documents, key = { it.id }) { document ->
        DocumentRow(document, language, onOpen, onRename, onDelete)
    }
}

@Composable
private fun DocumentRow(
    document: Attachment,
    language: AppLanguage,
    onOpen: (Attachment) -> Unit,
    onRename: (Attachment) -> Unit,
    onDelete: (Attachment) -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val usage = if (document.usageCount == 0) stringResource(R.string.documents_unused)
    else stringResource(if (document.usageCount > 1) R.string.documents_used_by_other else R.string.documents_used_by_one, document.usageCount)
    ListItem(
        headlineContent = { Text(document.displayName) },
        supportingContent = {
            if (document.isMissing) {
                Text(AttachmentRules.missingFileMessage(language), color = MaterialTheme.colorScheme.error)
            } else {
                Text("${document.format.name} · ${AttachmentRules.sizeLabel(document.sizeBytes, language)} · $usage")
            }
        },
        leadingContent = {
            if (document.isMissing) Icon(Icons.Outlined.ErrorOutline, contentDescription = stringResource(R.string.documents_missing), tint = MaterialTheme.colorScheme.error)
            else Icon(Icons.Outlined.Description, contentDescription = null)
        },
        trailingContent = {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.documents_actions_a11y, document.displayName))
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(if (document.format.isPreviewableInApp) R.string.documents_open else R.string.preview_open_with)) },
                    enabled = !document.isMissing,
                    onClick = { menuExpanded = false; onOpen(document) },
                )
                DropdownMenuItem(text = { Text(stringResource(R.string.documents_rename)) }, onClick = { menuExpanded = false; onRename(document) })
                DropdownMenuItem(text = { Text(stringResource(R.string.delete_action)) }, onClick = { menuExpanded = false; onDelete(document) })
            }
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
        modifier = Modifier.fillMaxWidth().clickable(enabled = !document.isMissing) { onOpen(document) },
    )
}

@Composable
private fun RenameDialog(attachment: Attachment, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf(attachment.displayName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.documents_rename_title)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.documents_name_label)) },
                singleLine = true,
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) { Text(stringResource(R.string.ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
