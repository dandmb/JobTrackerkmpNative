package com.dmb.joblog.ui.joboffer

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dmb.joblog.R
import com.dmb.joblog.domain.model.ApplicationStatus
import com.dmb.joblog.ui.theme.LocalStatusPalette
import com.dmb.joblog.ui.theme.color

@Composable
fun StatusFilterRow(
    selectedStatuses: Set<ApplicationStatus>,
    onSelectionChanged: (Set<ApplicationStatus>) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selectedStatuses.isEmpty(),
            onClick = { onSelectionChanged(emptySet()) },
            label = { Text(stringResource(R.string.filter_all)) },
        )
        ApplicationStatus.entries.forEach { status ->
            val selected = status in selectedStatuses
            FilterChip(
                selected = selected,
                onClick = {
                    onSelectionChanged(if (selected) selectedStatuses - status else selectedStatuses + status)
                },
                label = { Text(status.displayLabel()) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = status.color().copy(alpha = LocalStatusPalette.current.badgeTintAlpha),
                    selectedLabelColor = status.color(),
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected,
                    selectedBorderColor = status.color(),
                    selectedBorderWidth = 1.dp,
                ),
            )
        }
    }
}
