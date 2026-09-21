package com.dmb.joblog.ui.joboffer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dmb.joblog.domain.model.ApplicationStatus
import com.dmb.joblog.domain.model.JobOffer
import com.dmb.joblog.ui.theme.LocalStatusPalette
import com.dmb.joblog.ui.theme.color

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.dmb.joblog.R

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JobOfferStatsCard(offers: List<JobOffer>) {
    val total = offers.size
    val counts = ApplicationStatus.entries.associateWith { status ->
        offers.count { it.status == status }
    }.filterValues { it > 0 }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Total + libellé lus ensemble (« 3 applications tracked » / « 3 candidatures suivies »)
            Column(modifier = Modifier.semantics(mergeDescendants = true) {}) {
                Text(
                    "$total",
                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = 34.sp, fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    stringResource(if (total > 1) R.string.stats_tracked_other else R.string.stats_tracked_one),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }

            if (counts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                // FlowRow : avec 4-5 statuts, une Row simple déborde de l'écran (360 dp) ou à grande taille de police
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    counts.forEach { (status, count) ->
                        StatusBadge(status = status, count = count)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: ApplicationStatus, count: Int) {
    Box(
        modifier = Modifier
            .background(status.color().copy(alpha = LocalStatusPalette.current.badgeTintAlpha), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            status.shortLabel(count),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = status.color()
        )
    }
}
