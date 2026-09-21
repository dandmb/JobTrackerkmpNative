package com.dmb.jobtracker.ui.joboffer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dmb.jobtracker.presentation.about.AboutContent
import com.dmb.jobtracker.ui.theme.StatusBarIconsForPrimaryTopBar
import com.dmb.jobtracker.presentation.joboffer.JobOfferListViewModel
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import com.dmb.jobtracker.domain.model.JobOffer

/** Nombre d'items placés AVANT les candidatures dans la LazyColumn (la carte de statistiques) : décale les index de défilement. */
private const val STATS_ITEM_COUNT = 1

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobOfferListScreen(
    viewModel: JobOfferListViewModel = koinInject(),
    onOpenAbout: () -> Unit = {},
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
                if (!isItemFullyVisible(bounds, layoutInfo.viewportEndOffset)) listState.animateScrollToItem(target.index + STATS_ITEM_COUNT)
            }
        }
        // En dernier : restoredOfferId est une clé de cet effet, le remettre à null plus tôt l'annulerait
        restoredOfferId = null
    }

    // Sans candidature, le champ de recherche est masqué : on efface la requête pour qu'elle ne filtre pas en silence les
    // futures candidatures (et ne masque pas l'état « aucune candidature » derrière « Aucun résultat pour… »).
    LaunchedEffect(state.offers.isEmpty()) { if (state.offers.isEmpty()) searchQuery = "" }

    // Le menu de tri disparaît avec la dernière candidature : on referme son état pour qu'il ne se rouvre pas tout seul
    // quand une candidature sera de nouveau ajoutée.
    LaunchedEffect(state.offers.isEmpty()) { if (state.offers.isEmpty()) sortMenuExpanded = false }

    StatusBarIconsForPrimaryTopBar()   // icônes de la barre d'état lisibles sur la barre teal (surtout en thème sombre)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Candidatures") },
                actions = {
                    IconButton(onClick = onOpenAbout) {
                        Icon(Icons.Outlined.Info, contentDescription = AboutContent.ENTRY_POINT_LABEL)
                    }
                    // Trier n'a de sens que s'il existe au moins une candidature (state.offers = TOUTES les offres).
                    // On teste donc `state.offers`, PAS `visibleOffers` : une recherche sans résultat alors que des
                    // candidatures existent laisse l'action de tri visible.
                    if (state.offers.isNotEmpty()) {
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
            // Seule la recherche reste épinglée en haut (comme sur iOS : barre de recherche sous le titre). La carte de
            // statistiques défile AVEC la liste : épinglée, elle occupait jusqu'à ~60 % de l'écran en police agrandie / petit écran.
            if (state.offers.isNotEmpty()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Rechercher un poste ou une entreprise") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp)
                )
            }

            // fillMaxWidth OBLIGATOIRE : dans une Column, un enfant à `weight(1f)` reçoit toute la hauteur restante mais sa
            // LARGEUR reste celle de son contenu (wrap). Sans elle, `Modifier.align(Alignment.Center)` des enfants ne centre que
            // dans un Box aussi large que le texte, donc collé au bord gauche (« Aucun résultat pour… », indicateur de chargement).
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when {
                    state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    state.offers.isEmpty() -> EmptyOffersMessage()
                    else -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            // Marge basse : le FAB (56 dp + 16 dp de marge) ne doit pas masquer la dernière carte
                            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 88.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Item 0 : statistiques sur TOUTES les offres (pas filtrées), défile avec la liste
                            item(key = "stats") { JobOfferStatsCard(offers = state.offers) }
                            if (visibleOffers.isEmpty()) {
                                // Recherche active sans résultat alors que des candidatures existent
                                item(key = "no-results") {
                                    Text(
                                        "Aucun résultat pour « $searchQuery »",
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
                                        scope.launch {
                                            // Suppression sans confirmation : on offre une annulation (guidelines M3)
                                            snackbarHostState.currentSnackbarData?.dismiss()
                                            val result = snackbarHostState.showSnackbar(
                                                message = "«${offer.title}» supprimée",
                                                actionLabel = "Annuler",
                                                duration = SnackbarDuration.Long
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                viewModel.onRestoreOffer(offer)   // mêmes valeurs qu'avant, createdAt compris : retrouve SA place dans la liste
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
        // Plateau/boîte vide : l'icône conventionnelle de l'état vide (équivalent SF Symbols : `tray`).
        // Décorative (le titre dit déjà « Aucune candidature ») : pas de contentDescription, donc ignorée par TalkBack
        Icon(
            Icons.Outlined.Inbox,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        // textAlign Center : sans lui, un texte qui passe sur 2 lignes (police agrandie, petit écran) s'aligne à gauche
        // à l'intérieur de sa colonne centrée (comme le fait `.multilineTextAlignment(.center)` sur iOS)
        Text("Aucune candidature", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Ajoute ta première candidature avec le bouton +",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}