package com.dmb.jobtracker.ui.joboffer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dmb.jobtracker.domain.model.ApplicationStatus
import com.dmb.jobtracker.domain.model.JobOffer
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.todayIn
import kotlinx.datetime.toLocalDateTime

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobOfferFormSheet(
    existingOffer: JobOffer? = null,   // null = mode ajout, sinon mode édition
    onDismiss: () -> Unit,
    onSave: (JobOffer) -> Unit
) {
    val isEditing = existingOffer != null
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

    var title by remember { mutableStateOf(existingOffer?.title ?: "") }
    var company by remember { mutableStateOf(existingOffer?.company ?: "") }
    var url by remember { mutableStateOf(existingOffer?.url ?: "") }
    var location by remember { mutableStateOf(existingOffer?.location ?: "") }
    var source by remember { mutableStateOf(existingOffer?.source ?: "") }
    var notes by remember { mutableStateOf(existingOffer?.notes ?: "") }
    var appliedDate by remember { mutableStateOf(existingOffer?.appliedDate ?: today) }
    var interviewDate by remember { mutableStateOf(existingOffer?.interviewDate) }
    var resultDate by remember { mutableStateOf(existingOffer?.resultDate) }

    var salaryMin by remember { mutableStateOf(existingOffer?.salaryRange?.substringBefore("-")?.trim()?.filter { it.isDigit() } ?: "") }
    var salaryMax by remember { mutableStateOf(existingOffer?.salaryRange?.substringAfter("-", "")?.trim()?.filter { it.isDigit() } ?: "") }


    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
                if (isEditing) "Modifier la candidature" else "Nouvelle candidature",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = title, onValueChange = { title = it },
                label = { Text("Titre du poste") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = company, onValueChange = { company = it },
                label = { Text("Entreprise") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = location, onValueChange = { location = it },
                label = { Text("Localisation (ville, remote...)") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = source, onValueChange = { source = it },
                label = { Text("Source (LinkedIn, cooptation...)") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = salaryMin,
                    onValueChange = { if (it.length <= 4) salaryMin = it.filter { c -> c.isDigit() } },
                    label = { Text("Salaire min (k€)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f), singleLine = true
                )
                OutlinedTextField(
                    value = salaryMax,
                    onValueChange = { if (it.length <= 4) salaryMax = it.filter { c -> c.isDigit() } },
                    label = { Text("Salaire max (k€)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f), singleLine = true
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = url, onValueChange = { url = it },
                label = { Text("Lien de l'annonce") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text("Dates", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            DatePickerField(
                label = "Date de candidature",
                date = appliedDate,
                onDateSelected = { appliedDate = it }
            )
            Spacer(modifier = Modifier.height(8.dp))
            DatePickerField(
                label = "Date d'entretien (optionnel)",
                date = interviewDate,
                onDateSelected = { interviewDate = it },
                onClear = { interviewDate = null }
            )
            Spacer(modifier = Modifier.height(8.dp))
            DatePickerField(
                label = "Date de résultat (optionnel)",
                date = resultDate,
                onDateSelected = { resultDate = it },
                onClear = { resultDate = null }
            )

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = notes, onValueChange = { notes = it },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                minLines = 3
            )

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    onSave(
                        JobOffer(
                            id = existingOffer?.id ?: 0,
                            title = title,
                            company = company,
                            url = url.ifBlank { null },
                            location = location.ifBlank { null },
                            source = source.ifBlank { null },
                            salaryRange = when {
                                salaryMin.isNotBlank() && salaryMax.isNotBlank() -> "${salaryMin}k - ${salaryMax}k"
                                salaryMin.isNotBlank() -> "${salaryMin}k+"
                                else -> null
                            },
                            appliedDate = appliedDate,
                            interviewDate = interviewDate,
                            resultDate = resultDate,
                            status = existingOffer?.status ?: ApplicationStatus.APPLIED,
                            notes = notes.ifBlank { null }
                        )
                    )
                },
                enabled = title.isNotBlank() && company.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isEditing) "Enregistrer" else "Ajouter")
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

    OutlinedTextField(
        value = date?.toString() ?: "",
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        trailingIcon = {
            if (date != null && onClear != null) {
                TextButton(onClick = onClear) { Text("Effacer") }
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            disabledTextColor = MaterialTheme.colorScheme.onSurface
        )
    )
    // Overlay invisible cliquable pour ouvrir le picker (readOnly bloque la saisie clavier mais pas le clic)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .offset(y = (-56).dp)
            .clickable { showPicker = true }
    )

    if (showPicker) {
        val initialMillis = date
            ?.atStartOfDayIn(TimeZone.UTC)
            ?.toEpochMilliseconds()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val localDate = Instant.fromEpochMilliseconds(millis)
                            .toLocalDateTime(TimeZone.UTC).date
                        onDateSelected(localDate)
                    }
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Annuler") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}