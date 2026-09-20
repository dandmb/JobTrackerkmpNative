package com.dmb.jobtracker.ui.joboffer

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dmb.jobtracker.domain.model.ApplicationStatus
import com.dmb.jobtracker.domain.model.JobOffer
import com.dmb.jobtracker.ui.theme.color
import com.dmb.jobtracker.ui.util.capitalizeFirst
import com.dmb.jobtracker.ui.util.toTitleCase
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char

// kotlinx-datetime n'a pas de noms de mois français intégrés : mêmes abréviations que iOS (fr_FR, « d MMM »).
private val FrenchMonthsAbbreviated = MonthNames(
    "janv.", "févr.", "mars", "avr.", "mai", "juin",
    "juil.", "août", "sept.", "oct.", "nov.", "déc."
)

private val dateFormat = LocalDate.Format {
    dayOfMonth(Padding.NONE)
    char(' ')
    monthName(FrenchMonthsAbbreviated)
}

@Composable
fun JobOfferCard(
    offer: JobOffer,
    onStatusChanged: (ApplicationStatus) -> Unit,
    onEditClick: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        offer.title.toTitleCase(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.15.sp
                        )
                    )
                    Text(
                        offer.company.capitalizeFirst(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onEditClick) {
                    Icon(Icons.Default.Edit, contentDescription = "Modifier la candidature ${offer.title}")
                }
            }

            val location = offer.location
            if (location != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        location,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Statut (cliquable)
            Box {
                AssistChip(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.semantics { contentDescription = "Statut : ${offer.status.displayLabel()}" },
                    label = { Text(offer.status.displayLabel()) },
                    trailingIcon = {
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(labelColor = offer.status.color())
                )
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    ApplicationStatus.entries.forEach { status ->
                        DropdownMenuItem(
                            text = { Text(status.displayLabel()) },
                            onClick = {
                                onStatusChanged(status)
                                menuExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            // Timeline des dates
            DateRow(label = "Postulé", date = offer.appliedDate)
            offer.interviewDate?.let { DateRow(label = "Entretien", date = it) }
            offer.resultDate?.let { DateRow(label = "Résultat", date = it) }

            if (offer.salaryRange != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "💰 ${offer.salaryRange}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun DateRow(label: String, date: LocalDate) {
    Row(
        // Libellé + valeur lus d'un bloc par TalkBack (« Postulé, 5 sept. »)
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 11.sp,
                letterSpacing = 0.5.sp,
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            date.format(dateFormat),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}

private fun ApplicationStatus.displayLabel(): String = when (this) {
    ApplicationStatus.PENDING -> "En attente"
    ApplicationStatus.APPLIED -> "Postulé"
    ApplicationStatus.INTERVIEW -> "Entretien"
    ApplicationStatus.REJECTED -> "Refusé"
    ApplicationStatus.ACCEPTED -> "Accepté"
}