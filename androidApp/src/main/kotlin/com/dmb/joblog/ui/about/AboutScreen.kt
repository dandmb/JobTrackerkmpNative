package com.dmb.joblog.ui.about

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.dmb.joblog.R
import com.dmb.joblog.presentation.about.AboutContent
import com.dmb.joblog.ui.theme.Teal40
import com.dmb.joblog.presentation.about.AboutSection
import com.dmb.joblog.presentation.about.AboutViewModel
import com.dmb.joblog.presentation.about.ConfirmationTexts
import com.dmb.joblog.presentation.about.DeleteAllStep
import com.dmb.joblog.ui.theme.StatusBarIconsForPrimaryTopBar
import org.koin.compose.koinInject
import com.dmb.joblog.ui.i18n.rememberAppLanguage

/**
 * Écran « À propos », en **Material 3 Expressive** (limité à cet écran : le reste de l'app garde son thème).
 * Tout le texte vient de `AboutContent` (sharedLogic) : cet écran ne fait que le mettre en forme.
 * La suppression totale passe par la double confirmation portée par `AboutViewModel`.
 *
 * Composants / principes Expressive utilisés (voir rapport) : `MaterialExpressiveTheme` + `MotionScheme.expressive()`
 * (ressorts), `LargeFlexibleTopAppBar` (grand titre qui se réduit au défilement), `Button(shapes = ButtonDefaults.shapes())`
 * (forme qui se transforme à l'appui) à hauteur `MediumContainerHeight`, `LoadingIndicator` (forme qui se déforme, pendant la
 * suppression), `MaterialShapes.Cookie9Sided` (pastille d'en-tête, logo JobLog), cartes à grands arrondis (`shapes.extraLarge`), espacements
 * en multiples de 8 dp.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    viewModel: AboutViewModel = koinInject(),
) {
    DisposableEffect(viewModel) { onDispose { viewModel.onCleared() } }
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val language = rememberAppLanguage()
    val content = remember(language) { AboutContent.of(language) }
    val versionLabel = appVersionLabel(context, content)
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    StatusBarIconsForPrimaryTopBar()   // icônes de la barre d'état lisibles sur la barre teal (surtout en thème sombre)

    // Thème Expressive local à l'écran : couleurs, formes et typographie de l'app conservées, motion en ressorts expressifs.
    MaterialExpressiveTheme(
        colorScheme = MaterialTheme.colorScheme,
        motionScheme = MotionScheme.expressive(),
        shapes = MaterialTheme.shapes,
        typography = MaterialTheme.typography,
    ) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                LargeFlexibleTopAppBar(
                    title = { Text(content.screenTitle) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = content.backLabel)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        scrolledContainerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                AppHeader(content, versionLabel)
                content.sections.forEach { SectionCard(it) }
                DeleteAllBlock(
                    content = content,
                    isDeleting = state.isDeleting,
                    dataDeleted = state.dataDeleted,
                    deletionFailed = state.deletionFailed,
                    onDeleteRequested = viewModel::onDeleteAllRequested,
                )
                ContactBlock(context, content)
            }
        }

        when (state.deleteStep) {
            DeleteAllStep.IDLE -> Unit
            DeleteAllStep.FIRST_CONFIRMATION -> ConfirmationDialog(
                texts = content.firstConfirmation,
                isFinal = false,
                onConfirm = viewModel::onDeleteAllFirstConfirmed,
                onCancel = viewModel::onDeleteAllCancelled,
            )
            DeleteAllStep.FINAL_CONFIRMATION -> ConfirmationDialog(
                texts = content.finalConfirmation,
                isFinal = true,
                onConfirm = viewModel::onDeleteAllFinalConfirmed,
                onCancel = viewModel::onDeleteAllCancelled,
            )
        }
    }
}

/** En-tête : pastille « cookie » Expressive (forme polygonale) + nom, accroche et version. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AppHeader(content: AboutContent, versionLabel: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(
            modifier = Modifier
                .size(72.dp)
                // Teal de MARQUE fixe (et non `primary`, qui devient un teal clair en thème sombre) : le logo blanc/corail
                // garde le même contraste dans les deux thèmes, comme l'icône de l'app.
                .background(Teal40, MaterialShapes.Cookie9Sided.toShape()),
            contentAlignment = Alignment.Center,
        ) {
            Image(painterResource(R.drawable.ic_joblog_mark), contentDescription = null, modifier = Modifier.size(44.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(AboutContent.APP_NAME, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
            Text(content.tagline, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(versionLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.semantics { heading() },
    )
}

/** Une section = une carte à grands arrondis (surface tonale), titre en `titleLarge`, corps en `bodyLarge`. */
@Composable
private fun SectionCard(section: AboutSection) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionTitle(section.title)
            section.intro?.let { Text(it, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface) }
            section.bullets.forEach { Bullet(it) }
            section.note?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
private fun Bullet(text: String) {
    Row {
        Text("•", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

/** Zone d'alerte : fond `errorContainer`, bouton `error` Expressive — clairement distincte du reste de l'écran. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DeleteAllBlock(
    content: AboutContent,
    isDeleting: Boolean,
    dataDeleted: Boolean,
    deletionFailed: Boolean,
    onDeleteRequested: () -> Unit,
) {
    val height = ButtonDefaults.MediumContainerHeight
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                content.deleteAllExplanation,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Button(
                onClick = onDeleteRequested,
                enabled = !isDeleting,
                shapes = ButtonDefaults.shapes(),   // forme qui se transforme à l'appui
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
                contentPadding = ButtonDefaults.contentPaddingFor(height),
                modifier = Modifier.fillMaxWidth().heightIn(min = height),
            ) {
                if (isDeleting) {
                    LoadingIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onError)
                    Spacer(Modifier.width(8.dp))
                    Text(content.deleteAllInProgressLabel, style = ButtonDefaults.textStyleFor(height))
                } else {
                    Text(content.deleteAllLabel, style = ButtonDefaults.textStyleFor(height))
                }
            }
            val message = if (deletionFailed) content.deleteAllFailedMessage else content.deleteAllSuccessMessage.takeIf { dataDeleted }
            message?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
        }
    }
}

/** Lien de contact : ouvre l'application de messagerie (lien mailto + objet pré-rempli). Sans application : l'adresse s'affiche en clair. */
@Composable
private fun ContactBlock(context: Context, content: AboutContent) {
    var noMailApp by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionTitle(content.contactTitle)
        Text(content.contactIntro, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
        Surface(
            onClick = { noMailApp = !openMailApp(context) },
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .semantics(mergeDescendants = true) { role = Role.Button },
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Icon(Icons.Outlined.Email, contentDescription = null, modifier = Modifier.size(24.dp))
                Column {
                    Text(content.contactLabel, style = MaterialTheme.typography.titleMedium)
                    Text(
                        AboutContent.CONTACT_EMAIL,
                        style = MaterialTheme.typography.bodyMedium.copy(textDecoration = TextDecoration.Underline),
                    )
                }
            }
        }
        Text(content.contactNote, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (noMailApp) {
            Text(
                content.contactNoMailAppMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

/**
 * Dialogue de confirmation Expressive : grands arrondis, icône, hiérarchie des actions (annuler = discret, confirmer = plein).
 * À l'étape finale le bouton de confirmation est un bouton `error` plein ; à la première étape c'est un bouton texte en `error`.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ConfirmationDialog(texts: ConfirmationTexts, isFinal: Boolean, onConfirm: () -> Unit, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        shape = MaterialTheme.shapes.extraLarge,
        icon = { Icon(Icons.Outlined.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = { Text(texts.title) },
        text = { Text(texts.message) },
        confirmButton = {
            if (isFinal) {
                Button(
                    onClick = onConfirm,
                    shapes = ButtonDefaults.shapes(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) { Text(texts.confirmLabel) }
            } else {
                TextButton(
                    onClick = onConfirm,
                    shapes = ButtonDefaults.shapes(),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(texts.confirmLabel) }
            }
        },
        dismissButton = { TextButton(onClick = onCancel, shapes = ButtonDefaults.shapes()) { Text(texts.cancelLabel) } },
    )
}
