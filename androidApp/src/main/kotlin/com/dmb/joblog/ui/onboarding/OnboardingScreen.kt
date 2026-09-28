package com.dmb.joblog.ui.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dmb.joblog.presentation.onboarding.OnboardingContent
import kotlinx.coroutines.launch
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.dmb.joblog.R
import com.dmb.joblog.ui.i18n.rememberAppLanguage

// ⚠️ Ces images devront être régénérées si l'UI des écrans montrés (carte, statuts, carte de stats) change.
private val PageImages: List<Int> = listOf(R.drawable.onboarding_1, R.drawable.onboarding_2, R.drawable.onboarding_3)

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val language = rememberAppLanguage()
    val content = remember(language) { OnboardingContent.of(language) }
    val pages = content.pages
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val current = pagerState.currentPage

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.CenterEnd) {
            if (content.showsSkip(current)) {
                TextButton(onClick = onFinished, modifier = Modifier.padding(end = 8.dp)) {
                    Text(content.skipLabel)
                }
            }
        }

        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { index ->
            OnboardingPageContent(
                imageRes = PageImages[index],
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
                if (content.isLastPage(current)) {
                    onFinished()
                } else {
                    scope.launch { pagerState.animateScrollToPage(content.nextPageIndex(current)) }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp),
        ) {
            Text(content.primaryButtonLabel(current))
        }
    }
}

@Composable
private fun OnboardingPageContent(imageRes: Int, title: String, description: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val painter = painterResource(imageRes)
        Image(
            painter,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .weight(1f, fill = false)
                .widthIn(max = 320.dp)
                .aspectRatio(painter.intrinsicSize.width / painter.intrinsicSize.height)
                .shadow(elevation = 6.dp, shape = RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp)),
        )
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
    val pageLabel = stringResource(R.string.onboarding_page_indicator, current + 1, count)
    Row(
        modifier = modifier.semantics { contentDescription = pageLabel },
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
