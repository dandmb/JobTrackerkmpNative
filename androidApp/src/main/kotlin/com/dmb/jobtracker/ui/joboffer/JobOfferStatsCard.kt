package com.dmb.jobtracker.ui.joboffer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dmb.jobtracker.domain.model.ApplicationStatus
import com.dmb.jobtracker.domain.model.JobOffer
import com.dmb.jobtracker.ui.theme.color

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

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
            Text(
                "$total",
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 34.sp, fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                if (total > 1) "candidatures suivies" else "candidature suivie",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )

            if (counts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
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
            .background(status.color().copy(alpha = 0.16f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            "$count ${status.shortLabel()}",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = status.color()
        )
    }
}

private fun ApplicationStatus.shortLabel(): String = when (this) {
    ApplicationStatus.PENDING -> "attente"
    ApplicationStatus.APPLIED -> "postulé"
    ApplicationStatus.INTERVIEW -> "entretien"
    ApplicationStatus.REJECTED -> "refusé"
    ApplicationStatus.ACCEPTED -> "accepté"
}