package com.dmb.joblog.ui.joboffer

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dmb.joblog.R
import com.dmb.joblog.domain.model.ApplicationStatus
import com.dmb.joblog.ui.theme.LocalStatusPalette
import com.dmb.joblog.ui.theme.color

/**
 * Rangée de puces de filtre par statut, sous la barre de recherche : `FilterChip` M3 (état togglé, pas `AssistChip` qui
 * est pour une action) dans une `Row` à défilement horizontal (pas de retour à la ligne : à 5 statuts + « Tous », ça
 * déborde d'un petit écran ou en police agrandie).
 *
 * Sélection MULTIPLE : plusieurs statuts peuvent être actifs à la fois (union, voir `OfferListFilter`, sharedLogic).
 * « Tous » (= ensemble vide) désélectionne tout le reste ; sélectionner un statut désactive « Tous » puisqu'il n'est
 * représenté que par un ensemble vide, jamais mémorisé à part.
 */
@Composable
fun StatusFilterRow(
    selectedStatuses: Set<ApplicationStatus>,
    onSelectionChanged: (Set<ApplicationStatus>) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selectedStatuses.isEmpty(),
            onClick = { onSelectionChanged(emptySet()) },
            label = { Text(stringResource(R.string.filter_all)) },
        )
        ApplicationStatus.entries.forEach { status ->
            val selected = status in selectedStatuses
            FilterChip(
                selected = selected,
                onClick = {
                    onSelectionChanged(if (selected) selectedStatuses - status else selectedStatuses + status)
                },
                label = { Text(status.displayLabel()) },
                // Même couleur que le badge de statut de la carte de statistiques (StatusBadge) : teinte du statut sur
                // fond transparent (badgeTintAlpha), plutôt que la couleur secondaire par défaut de FilterChip.
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = status.color().copy(alpha = LocalStatusPalette.current.badgeTintAlpha),
                    selectedLabelColor = status.color(),
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected,
                    selectedBorderColor = status.color(),
                    selectedBorderWidth = 1.dp,
                ),
            )
        }
    }
}
