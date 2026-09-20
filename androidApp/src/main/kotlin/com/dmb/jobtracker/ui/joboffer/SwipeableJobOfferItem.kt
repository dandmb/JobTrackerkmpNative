package com.dmb.jobtracker.ui.joboffer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.dmb.jobtracker.domain.model.ApplicationStatus
import com.dmb.jobtracker.domain.model.JobOffer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableJobOfferItem(
    offer: JobOffer,
    onDelete: () -> Unit,
    onStatusChanged: (ApplicationStatus) -> Unit,
    onEdit: () -> Unit
) {
    // Pourquoi `remember` et pas `rememberSwipeToDismissBoxState` : ce dernier est un `rememberSaveable`.
    // Dans un LazyColumn à clés (`key = { it.id }`), l'état sauvegardé d'un item retiré de la liste est
    // restauré quand un item de MÊME clé réapparaît. Or une suppression confirmée laisse l'état sur
    // `EndToStart` : si l'offre revient (« Annuler » du snackbar, ré-ajout avec le même id), la carte
    // restait décalée hors écran avec le fond d'erreur du swipe. Un état non sauvegardable repart toujours
    // de `Settled` à chaque nouvelle composition de l'item, quel que soit le scénario de retour.
    val density = LocalDensity.current
    val positionalThreshold = SwipeToDismissBoxDefaults.positionalThreshold
    val currentOnDelete by rememberUpdatedState(onDelete)
    val dismissState = remember {
        SwipeToDismissBoxState(
            initialValue = SwipeToDismissBoxValue.Settled,
            density = density,
            confirmValueChange = { value ->
                if (value == SwipeToDismissBoxValue.EndToStart) {
                    currentOnDelete()
                    true
                } else {
                    false
                }
            },
            positionalThreshold = positionalThreshold
        )
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    "Supprimer",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    ) {
        JobOfferCard(offer = offer, onStatusChanged = onStatusChanged, onEditClick = onEdit)
    }
}
