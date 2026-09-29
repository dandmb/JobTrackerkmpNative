package com.dmb.joblog.ui.joboffer

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.dmb.joblog.domain.model.JobOffer
import com.dmb.joblog.presentation.form.JobOfferFormDraft
import com.dmb.joblog.presentation.attachments.AttachmentsViewModel
import com.dmb.joblog.presentation.form.JobOfferFormLogic
import com.dmb.joblog.ui.attachments.DocumentsFormSection
import com.dmb.joblog.ui.util.LocalDateSaver
import com.dmb.joblog.ui.util.OptionalLocalDateSaver
import org.koin.compose.koinInject
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.res.stringResource
import com.dmb.joblog.R
import com.dmb.joblog.ui.i18n.rememberAppLanguage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobOfferFormSheet(
    existingOffer: JobOffer? = null,
    onDismiss: () -> Unit,
    onSave: (JobOffer) -> Unit
) {
    val isEditing = existingOffer != null
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

    var title by rememberSaveable { mutableStateOf(existingOffer?.title ?: "") }
    var company by rememberSaveable { mutableStateOf(existingOffer?.company ?: "") }
    var url by rememberSaveable { mutableStateOf(existingOffer?.url ?: "") }
    var location by rememberSaveable { mutableStateOf(existingOffer?.location ?: "") }
    var source by rememberSaveable { mutableStateOf(existingOffer?.source ?: "") }
    var notes by rememberSaveable { mutableStateOf(existingOffer?.notes ?: "") }
    var appliedDate by rememberSaveable(stateSaver = LocalDateSaver) { mutableStateOf(JobOfferFormLogic.initialAppliedDate(existingOffer, today)) }
    var interviewDate by rememberSaveable(stateSaver = OptionalLocalDateSaver) { mutableStateOf(existingOffer?.interviewDate) }
    var resultDate by rememberSaveable(stateSaver = OptionalLocalDateSaver) { mutableStateOf(existingOffer?.resultDate) }

    val initialSalary = remember { JobOfferFormLogic.parseSalaryFields(existingOffer?.salaryRange) }
    var salaryMin by rememberSaveable { mutableStateOf(initialSalary.min) }
    var salaryMax by rememberSaveable { mutableStateOf(initialSalary.max) }

    val validation = JobOfferFormLogic.validate(title, company, salaryMin, salaryMax, rememberAppLanguage())
    var cvAttachmentId by rememberSaveable { mutableStateOf(existingOffer?.cvAttachmentId) }
    var coverLetterAttachmentId by rememberSaveable { mutableStateOf(existingOffer?.coverLetterAttachmentId) }
    val attachmentsViewModel: AttachmentsViewModel = koinInject()
    DisposableEffect(attachmentsViewModel) { onDispose { attachmentsViewModel.onCleared() } }

    val draft = JobOfferFormDraft(
        title = title,
        company = company,
        url = url,
        location = location,
        source = source,
        salaryMin = salaryMin,
        salaryMax = salaryMax,
        appliedDate = appliedDate,
        interviewDate = interviewDate,
        resultDate = resultDate,
        notes = notes,
        cvAttachmentId = cvAttachmentId,
        coverLetterAttachmentId = coverLetterAttachmentId,
    )
    val hasUnsavedChanges = JobOfferFormLogic.hasUnsavedChanges(draft, existingOffer, today)
    val currentHasUnsavedChanges by rememberUpdatedState(hasUnsavedChanges)
    var showDiscardConfirmation by rememberSaveable { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { value ->
            (value != SheetValue.Hidden || !currentHasUnsavedChanges).also { allowed -> if (!allowed) showDiscardConfirmation = true }
        },
    )

    if (showDiscardConfirmation) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirmation = false },
            title = { Text(stringResource(R.string.form_discard_title)) },
            text = { Text(stringResource(R.string.form_discard_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardConfirmation = false
                    onDismiss()
                }) { Text(stringResource(R.string.form_discard_confirm), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirmation = false }) { Text(stringResource(R.string.form_discard_cancel)) }
            },
        )
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                stringResource(if (isEditing) R.string.form_title_edit else R.string.form_title_new),
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = title, onValueChange = { title = it },
                label = { Text(stringResource(R.string.form_field_title)) },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = company, onValueChange = { company = it },
                label = { Text(stringResource(R.string.form_field_company)) },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = location, onValueChange = { location = it },
                label = { Text(stringResource(R.string.form_field_location)) },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = source, onValueChange = { source = it },
                label = { Text(stringResource(R.string.form_field_source)) },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = salaryMin,
                    onValueChange = { salaryMin = JobOfferFormLogic.nextSalaryInput(salaryMin, it) },
                    label = { Text(stringResource(R.string.form_field_salary_min)) },
                    isError = validation.errorMessage != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f), singleLine = true
                )
                OutlinedTextField(
                    value = salaryMax,
                    onValueChange = { salaryMax = JobOfferFormLogic.nextSalaryInput(salaryMax, it) },
                    label = { Text(stringResource(R.string.form_field_salary_max)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f), singleLine = true
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = url, onValueChange = { url = it },
                label = { Text(stringResource(R.string.form_field_url)) },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(R.string.form_section_dates), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            DatePickerField(
                label = stringResource(R.string.form_date_applied),
                date = appliedDate,
                onDateSelected = { appliedDate = it }
            )
            Spacer(modifier = Modifier.height(8.dp))
            DatePickerField(
                label = stringResource(R.string.form_date_interview),
                date = interviewDate,
                onDateSelected = { interviewDate = it },
                onClear = { interviewDate = null }
            )
            Spacer(modifier = Modifier.height(8.dp))
            DatePickerField(
                label = stringResource(R.string.form_date_result),
                date = resultDate,
                onDateSelected = { resultDate = it },
                onClear = { resultDate = null }
            )

            Spacer(modifier = Modifier.height(16.dp))
            DocumentsFormSection(
                cvAttachmentId = cvAttachmentId,
                coverLetterAttachmentId = coverLetterAttachmentId,
                onCvChanged = { cvAttachmentId = it },
                onCoverLetterChanged = { coverLetterAttachmentId = it },
                viewModel = attachmentsViewModel,
            )

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = notes, onValueChange = { notes = it },
                label = { Text(stringResource(R.string.form_field_notes)) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                minLines = 3
            )

            Spacer(modifier = Modifier.height(16.dp))
            validation.errorMessage?.let { message ->
                Text(
                    message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite }
                )
            }
            Button(
                onClick = {
                    onSave(JobOfferFormLogic.toJobOffer(draft, existingOffer))
                },
                enabled = validation.isValid,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(if (isEditing) R.string.form_save else R.string.form_add))
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerField(
    label: String,
    date: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    onClear: (() -> Unit)? = null
) {
    var showPicker by remember { mutableStateOf(false) }

    // Sélecteur ouvert par le tap sur le champ lui-même : une couche transparente superposée (offset) ajoutait de
    // l'espace en trop sous le champ et interceptait aussi les taps du bouton « Effacer ».
    val interactionSource = remember { MutableInteractionSource() }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { if (it is PressInteraction.Release) showPicker = true }
    }

    OutlinedTextField(
        value = date?.toString() ?: "",
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        interactionSource = interactionSource,
        trailingIcon = {
            if (date != null && onClear != null) {
                TextButton(onClick = onClear) { Text(stringResource(R.string.form_clear)) }
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            disabledTextColor = MaterialTheme.colorScheme.onSurface
        )
    )

    if (showPicker) {
        val initialMillis = date?.toDatePickerMillis()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        onDateSelected(datePickerMillisToLocalDate(millis))
                    }
                    showPicker = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text(stringResource(R.string.cancel)) }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}