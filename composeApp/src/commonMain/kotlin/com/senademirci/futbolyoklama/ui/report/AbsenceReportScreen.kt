package com.senademirci.futbolyoklama.ui.report

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.senademirci.futbolyoklama.ui.components.PlayerAvatar
import com.senademirci.futbolyoklama.ui.components.StatusBadge
import com.senademirci.futbolyoklama.ui.components.EmptyState
import com.senademirci.futbolyoklama.ui.theme.RiskHigh
import com.senademirci.futbolyoklama.ui.theme.RiskMedium
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AbsenceReportScreen(
    teamId: String,
    onBack: () -> Unit,
    onOpenPlayer: (String) -> Unit,
    viewModel: AbsenceReportViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(teamId) { viewModel.load(teamId) }
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Devamsızlık") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {

            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Gelmeyenler") },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Bugün") },
                )
            }

            if (selectedTab == 0) {
                RangeFilter(
                    selected = state.range,
                    onSelect = viewModel::setRange,
                )

                when {
                    !state.hasAnyData -> EmptyState(
                        emoji = "📊",
                        title = "Bu aralıkta veri yok",
                        message = "Seçili tarih aralığında hiç yoklama alınmamış.",
                    )

                    state.withAbsences.isEmpty() -> EmptyState(
                        emoji = "🎉",
                        title = "Devamsızlık yok",
                        message = "${state.sessionCountInRange} antrenmanda kimse eksik kalmamış.",
                    )

                    else -> {
                        Text(
                            text = "${state.sessionCountInRange} antrenman · " +
                                "${state.withAbsences.size} futbolcunun devamsızlığı var",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                        LazyColumn {
                            items(state.withAbsences, key = { it.player.id }) { summary ->
                                AbsenceRow(summary, onClick = { onOpenPlayer(summary.player.id) })
                                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                            }
                        }
                    }
                }
            } else {
                if (state.todayAbsentees.isEmpty()) {
                    EmptyState(
                        emoji = "✅",
                        title = "Bugün eksik yok",
                        message = "Bugünkü antrenmanda gelmeyen ya da izinli futbolcu görünmüyor.",
                    )
                } else {
                    LazyColumn {
                        items(state.todayAbsentees, key = { it.player.id }) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenPlayer(item.player.id) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                StatusBadge(item.status)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = item.player.fullName,
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                    Text(
                                        text = item.player.parentPhone
                                            ?.let { "Veli: $it" }
                                            ?: item.status.label,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RangeFilter(selected: ReportRange, onSelect: (ReportRange) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ReportRange.entries.forEach { range ->
            FilterChip(
                selected = range == selected,
                onClick = { onSelect(range) },
                label = { Text(range.label) },
            )
        }
    }
}

@Composable
private fun AbsenceRow(summary: AbsenceSummary, onClick: () -> Unit) {
    val color = when {
        summary.absencePercent >= 30 -> RiskHigh
        summary.absencePercent >= 15 -> RiskMedium
        else -> MaterialTheme.colorScheme.primary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlayerAvatar(
            initials = summary.player.initials,
            jerseyNumber = summary.player.jerseyNumber,
        )
        Spacer(Modifier.width(14.dp))

        Column(Modifier.weight(1f)) {
            Text(text = summary.player.fullName, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "${summary.absent} kez gelmedi · ${summary.totalSessions} antrenmanda",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.padding(top = 4.dp))
            LinearProgressIndicator(
                progress = { summary.absenceRate.coerceIn(0f, 1f) },
                color = color,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.width(12.dp))

        Card(colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f))) {
            Text(
                text = "%${summary.absencePercent}",
                color = color,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
    }
}
