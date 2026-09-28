package com.dmb.joblog.ui.joboffer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.dmb.joblog.domain.model.ApplicationStatus
import com.dmb.joblog.domain.model.JobOffer
import androidx.compose.ui.res.stringResource
import com.dmb.joblog.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableJobOfferItem(
    offer: JobOffer,
    onDelete: () -> Unit,
    onStatusChanged: (ApplicationStatus) -> Unit,
    onEdit: () -> Unit
) {
    val density = LocalDensity.current
    val positionalThreshold = SwipeToDismissBoxDefaults.positionalThreshold
    val currentOnDelete by rememberUpdatedState(onDelete)
    // `remember`, pas `rememberSwipeToDismissBoxState` (rememberSaveable) : dans une LazyColumn à clés, l'état swipe
    // d'un item supprimé était restauré si un item de même id réapparaissait (« Annuler »), le laissant décalé hors écran.
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
                    stringResource(R.string.delete_action),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    ) {
        JobOfferCard(offer = offer, onStatusChanged = onStatusChanged, onEditClick = onEdit)
    }
}
