package com.dmb.jobtracker.ui.joboffer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
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
    val scope = rememberCoroutineScope()

    val visibleOffers = remember(state.offers, searchQuery, sortOption) {
        state.offers
            .filter {
                searchQuery.isBlank() ||
                        it.title.contains(searchQuery, ignoreCase = true) ||
                        it.company.contains(searchQuery, ignoreCase = true)
            }
            .let { list ->
                when (sortOption) {
                    SortOption.DATE_DESC -> list.sortedByDescending { it.appliedDate }
                    SortOption.DATE_ASC -> list.sortedBy { it.appliedDate }
                    SortOption.ALPHA_ASC -> list.sortedBy { it.title.lowercase() }
                    SortOption.ALPHA_DESC -> list.sortedByDescending { it.title.lowercase() }
                }
            }
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
                Text("+", style = MaterialTheme.typography.headlineSmall)
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
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(visibleOffers, key = { it.id }) { offer ->
                                SwipeableJobOfferItem(
                                    offer = offer,
                                    onDelete = {
                                        viewModel.onDeleteOffer(offer)
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                "«${offer.title}» supprimée"
                                            )
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