package com.dmb.jobtracker.ui.joboffer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dmb.jobtracker.presentation.joboffer.JobOfferListViewModel
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import com.dmb.jobtracker.domain.model.JobOffer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobOfferListScreen(
    viewModel: JobOfferListViewModel = koinInject()
) {
    val state by viewModel.state.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }
    var offerBeingEdited by remember { mutableStateOf<JobOffer?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var sortOption by remember { mutableStateOf(SortOption.DATE_DESC) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    // Offre qu'on vient de restaurer via « Annuler » : le LazyColumn garde en place le 1er élément visible quand
    // un élément est inséré au-dessus, donc une carte restaurée en tête de liste réapparaîtrait hors écran.
    var restoredOfferId by remember { mutableStateOf<Long?>(null) }
    val scope = rememberCoroutineScope()

    val visibleOffers = remember(state.offers, searchQuery, sortOption) {
        state.offers.searchedAndSorted(searchQuery, sortOption)
    }

    LaunchedEffect(restoredOfferId, state.offers, visibleOffers) {
        val id = restoredOfferId ?: return@LaunchedEffect
        when (val target = locateRestoredOffer(state.offers, visibleOffers, id)) {
            RestoredOfferTarget.AwaitingData -> return@LaunchedEffect   // ré-ajout pas encore reflété par le flow
            RestoredOfferTarget.HiddenBySearch -> Unit                  // filtrée par la recherche : rien à montrer
            is RestoredOfferTarget.InList -> {
                withFrameNanos { }                  // laisse le LazyColumn mesurer avec l'offre ré-insérée
                val layoutInfo = listState.layoutInfo
                val bounds = layoutInfo.visibleItemsInfo.firstOrNull { it.key == id }
                    ?.let { ItemBounds(it.offset, it.size) }
                if (!isItemFullyVisible(bounds, layoutInfo.viewportEndOffset)) listState.animateScrollToItem(target.index)
            }
        }
        // En dernier : restoredOfferId est une clé de cet effet, le remettre à null plus tôt l'annulerait
        restoredOfferId = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Candidatures") },
                actions = {
                    Box {
                        IconButton(onClick = { sortMenuExpanded = true }) {
                            Icon(Icons.Default.Sort, contentDescription = "Trier")
                        }
                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false }
                        ) {
                            SortOption.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label) },
                                    onClick = { sortOption = option; sortMenuExpanded = false }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSheet = true },
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            ) {
                // Icône (et non un « + » textuel) : TalkBack annonce « Ajouter une candidature » au lieu de « plus »
                Icon(Icons.Default.Add, contentDescription = "Ajouter une candidature")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.offers.isNotEmpty()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    JobOfferStatsCard(offers = state.offers)   // stats sur TOUTES les offres, pas filtrées

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Rechercher un poste ou une entreprise") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when {
                    state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    visibleOffers.isEmpty() && searchQuery.isNotBlank() -> {
                        Text(
                            "Aucun résultat pour « $searchQuery »",
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    state.offers.isEmpty() -> EmptyOffersMessage()
                    else -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            // Marge basse : le FAB (56 dp + 16 dp de marge) ne doit pas masquer la dernière carte
                            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 88.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(visibleOffers, key = { it.id }) { offer ->
                                SwipeableJobOfferItem(
                                    offer = offer,
                                    onDelete = {
                                        viewModel.onDeleteOffer(offer)
                                        scope.launch {
                                            // Suppression sans confirmation : on offre une annulation (guidelines M3)
                                            snackbarHostState.currentSnackbarData?.dismiss()
                                            val result = snackbarHostState.showSnackbar(
                                                message = "«${offer.title}» supprimée",
                                                actionLabel = "Annuler",
                                                duration = SnackbarDuration.Long
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                viewModel.onAddOffer(offer)   // même id : la candidature retrouve sa place
                                                restoredOfferId = offer.id
                                            }
                                        }
                                    },
                                    onStatusChanged = { newStatus ->
                                        viewModel.onStatusChanged(offer, newStatus)
                                    },
                                    onEdit = {
                                        offerBeingEdited = offer
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }


    if (showAddSheet) {
        JobOfferFormSheet(
            onDismiss = { showAddSheet = false },
            onSave = { offer ->
                viewModel.onAddOffer(offer)
                showAddSheet = false
            }
        )
    }
    offerBeingEdited?.let { offer ->
        JobOfferFormSheet(
            existingOffer = offer,
            onDismiss = { offerBeingEdited = null },
            onSave = { updated ->
                viewModel.onUpdateOffer(updated)
                offerBeingEdited = null
            }
        )
    }
}

@Composable
private fun EmptyOffersMessage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Aucune candidature", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Ajoute ta première candidature avec le bouton +",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}