package com.dmb.joblog.ui.joboffer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dmb.joblog.presentation.settings.SettingsContent
import com.dmb.joblog.ui.theme.StatusBarIconsForPrimaryTopBar
import com.dmb.joblog.ui.util.StatusSetSaver
import com.dmb.joblog.presentation.joboffer.JobOfferListViewModel
import com.dmb.joblog.presentation.joboffer.SortOption
import com.dmb.joblog.presentation.joboffer.filteredForDisplay
import org.koin.compose.koinInject

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import com.dmb.joblog.domain.model.ApplicationStatus
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import com.dmb.joblog.R
import com.dmb.joblog.ui.i18n.rememberAppLanguage

private const val STATS_ITEM_COUNT = 1

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobOfferListScreen(
    viewModel: JobOfferListViewModel = koinInject(),
    onOpenSettings: () -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    var showAddSheet by rememberSaveable { mutableStateOf(false) }
    var editedOfferId by rememberSaveable { mutableStateOf<Long?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var sortOption by rememberSaveable { mutableStateOf(SortOption.DATE_DESC) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var selectedStatuses by rememberSaveable(stateSaver = StatusSetSaver) { mutableStateOf<Set<ApplicationStatus>>(emptySet()) }
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    var restoredOfferId by remember { mutableStateOf<Long?>(null) }
    var pendingUndoOfferId by rememberSaveable { mutableStateOf<Long?>(null) }
    var pendingUndoTitle by rememberSaveable { mutableStateOf("") }
    val resources = LocalResources.current
    val language = rememberAppLanguage()

    val visibleOffers = remember(state.offers, searchQuery, sortOption, selectedStatuses) {
        state.offers.filteredForDisplay(searchQuery, sortOption, selectedStatuses)
    }

    LaunchedEffect(restoredOfferId, state.offers, visibleOffers) {
        val id = restoredOfferId ?: return@LaunchedEffect
        when (val target = locateRestoredOffer(state.offers, visibleOffers, id)) {
            RestoredOfferTarget.AwaitingData -> return@LaunchedEffect
            RestoredOfferTarget.HiddenBySearch -> Unit
            is RestoredOfferTarget.InList -> {
                withFrameNanos { }
                val layoutInfo = listState.layoutInfo
                val bounds = layoutInfo.visibleItemsInfo.firstOrNull { it.key == id }
                    ?.let { ItemBounds(it.offset, it.size) }
                if (!isItemFullyVisible(bounds, layoutInfo.viewportEndOffset)) listState.animateScrollToItem(target.index + STATS_ITEM_COUNT)
            }
        }
        restoredOfferId = null
    }

    LaunchedEffect(pendingUndoOfferId) {
        val id = pendingUndoOfferId ?: return@LaunchedEffect
        snackbarHostState.currentSnackbarData?.dismiss()
        val result = snackbarHostState.showSnackbar(
            message = resources.getString(R.string.list_deleted_snackbar, pendingUndoTitle),
            actionLabel = resources.getString(R.string.undo),
            duration = SnackbarDuration.Long
        )
        if (result == SnackbarResult.ActionPerformed) {
            viewModel.onRestoreDeletedOffer(id)
            restoredOfferId = id
        }
        pendingUndoOfferId = null
    }

    LaunchedEffect(state.offers.isEmpty()) {
        if (state.offers.isEmpty()) {
            searchQuery = ""
            selectedStatuses = emptySet()
        }
    }

    LaunchedEffect(state.offers.isEmpty()) { if (state.offers.isEmpty()) sortMenuExpanded = false }

    StatusBarIconsForPrimaryTopBar()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.list_title)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = SettingsContent.of(language).entryPointLabel)
                    }
                    if (state.offers.isNotEmpty()) {
                        Box {
                            IconButton(onClick = { sortMenuExpanded = true }) {
                                Icon(Icons.Default.Sort, contentDescription = stringResource(R.string.list_sort))
                            }
                            DropdownMenu(
                                expanded = sortMenuExpanded,
                                onDismissRequest = { sortMenuExpanded = false }
                            ) {
                                SortOption.entries.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(stringResource(option.labelRes())) },
                                        onClick = { sortOption = option; sortMenuExpanded = false }
                                    )
                                }
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
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.list_add))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.offers.isNotEmpty()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(R.string.list_search_placeholder)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.list_search_clear))
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp)
                )
                StatusFilterRow(
                    selectedStatuses = selectedStatuses,
                    onSelectionChanged = { selectedStatuses = it },
                )
            }

            // fillMaxWidth nécessaire : sans elle, un enfant `weight(1f)` d'une Column garde la largeur de son
            // contenu, et `Alignment.Center` ne centre alors que dans une boîte aussi large que le texte.
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when {
                    state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    state.offers.isEmpty() -> EmptyOffersMessage()
                    else -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 88.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item(key = "stats") { JobOfferStatsCard(offers = state.offers) }
                            if (visibleOffers.isEmpty()) {
                                item(key = "no-results") {
                                    Text(
                                        if (selectedStatuses.isNotEmpty()) stringResource(R.string.list_no_results_filter)
                                        else stringResource(R.string.list_no_results, searchQuery),
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            items(visibleOffers, key = { it.id }) { offer ->
                                SwipeableJobOfferItem(
                                    offer = offer,
                                    onDelete = {
                                        viewModel.onDeleteOffer(offer)
                                        pendingUndoTitle = offer.title
                                        pendingUndoOfferId = offer.id
                                    },
                                    onStatusChanged = { newStatus ->
                                        viewModel.onStatusChanged(offer, newStatus)
                                    },
                                    onEdit = {
                                        editedOfferId = offer.id
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
    editedOfferId?.let { id -> state.offers.firstOrNull { it.id == id } }?.let { offer ->
        JobOfferFormSheet(
            existingOffer = offer,
            onDismiss = { editedOfferId = null },
            onSave = { updated ->
                viewModel.onUpdateOffer(updated)
                editedOfferId = null
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
        Icon(
            Icons.Outlined.Inbox,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(stringResource(R.string.list_empty_title), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            stringResource(R.string.list_empty_subtitle),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}