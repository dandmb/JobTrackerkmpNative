package com.dmb.jobtracker.ui.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dmb.jobtracker.presentation.onboarding.OnboardingContent
import kotlinx.coroutines.launch

// Icône de chaque page (même ordre que OnboardingContent.pages) : suivi, statuts, statistiques.
private val PageIcons: List<ImageVector> = listOf(Icons.Default.Work, Icons.Default.Flag, Icons.Default.BarChart)

/**
 * Onboarding en 3 pages (contenu et règles de navigation communs à iOS : sharedLogic `OnboardingContent`).
 * « Passer » et « Commencer » appellent tous deux [onFinished] ; « Suivant » fait défiler vers la page suivante.
 */
@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val pages = OnboardingContent.pages
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val current = pagerState.currentPage

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
    ) {
        // « Passer » : réserve la place sur la dernière page pour éviter un saut de mise en page
        Box(modifier = Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.CenterEnd) {
            if (OnboardingContent.showsSkip(current)) {
                TextButton(onClick = onFinished, modifier = Modifier.padding(end = 8.dp)) {
                    Text(OnboardingContent.SKIP_LABEL)
                }
            }
        }

        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { index ->
            OnboardingPageContent(
                icon = PageIcons[index],
                title = pages[index].title,
                description = pages[index].description,
            )
        }

        PageIndicators(
            count = pages.size,
            current = current,
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
        )

        Button(
            onClick = {
                if (OnboardingContent.isLastPage(current)) {
                    onFinished()
                } else {
                    scope.launch { pagerState.animateScrollToPage(OnboardingContent.nextPageIndex(current)) }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp),
        ) {
            Text(OnboardingContent.primaryButtonLabel(current))
        }
    }
}

@Composable
private fun OnboardingPageContent(icon: ImageVector, title: String, description: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,   // décorative : le titre porte le sens
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Spacer(modifier = Modifier.height(40.dp))
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PageIndicators(count: Int, current: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.semantics { contentDescription = "Page ${current + 1} sur $count" },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { index ->
            val selected = index == current
            val width = animateDpAsState(if (selected) 24.dp else 8.dp, label = "indicatorWidth")
            val color = animateColorAsState(
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                label = "indicatorColor",
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(width = width.value, height = 8.dp)
                    .clip(CircleShape)
                    .background(color.value),
            )
        }
    }
}
