package com.dmb.jobtracker.ui.joboffer

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.dmb.jobtracker.domain.model.JobOffer
import com.dmb.jobtracker.presentation.form.JobOfferFormDraft
import com.dmb.jobtracker.presentation.form.JobOfferFormLogic
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

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
    var appliedDate by remember { mutableStateOf(JobOfferFormLogic.initialAppliedDate(existingOffer, today)) }
    var interviewDate by remember { mutableStateOf(existingOffer?.interviewDate) }
    var resultDate by remember { mutableStateOf(existingOffer?.resultDate) }

    val initialSalary = remember { JobOfferFormLogic.parseSalaryFields(existingOffer?.salaryRange) }
    var salaryMin by remember { mutableStateOf(initialSalary.min) }
    var salaryMax by remember { mutableStateOf(initialSalary.max) }


    // Validation partagée (sharedLogic) : Enregistrer n'est actif que si `isValid` ; `errorMessage` (salaire max sans min) est affiché
    val validation = JobOfferFormLogic.validate(title, company, salaryMin, salaryMax)

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
                    onValueChange = { salaryMin = JobOfferFormLogic.nextSalaryInput(salaryMin, it) },
                    label = { Text("Salaire min (k€)") },
                    isError = validation.errorMessage != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f), singleLine = true
                )
                OutlinedTextField(
                    value = salaryMax,
                    onValueChange = { salaryMax = JobOfferFormLogic.nextSalaryInput(salaryMax, it) },
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
            validation.errorMessage?.let { message ->
                Text(
                    message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite }   // annoncé par TalkBack dès qu'il apparaît
                )
            }
            Button(
                onClick = {
                    onSave(
                        JobOfferFormLogic.toJobOffer(
                            JobOfferFormDraft(
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
                            ),
                            existingOffer,
                        )
                    )
                },
                enabled = validation.isValid,
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

    // Ouverture du sélecteur au TAP SUR LE CHAMP lui-même (et non via une couche transparente superposée) : une couche
    // décalée par `offset` gardait sa place dans la mise en page (56 dp d'espace en trop sous chaque champ de date) et
    // interceptait aussi les taps du bouton « Effacer », qui ouvrait le sélecteur au lieu d'effacer la date.
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
                TextButton(onClick = onClear) { Text("Effacer") }
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