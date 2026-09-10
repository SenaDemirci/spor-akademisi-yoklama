package com.senademirci.futbolyoklama.ui.roster

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.senademirci.futbolyoklama.ui.components.PlayerAvatar
import com.senademirci.futbolyoklama.ui.components.StatusBadge
import com.senademirci.futbolyoklama.ui.theme.RiskHigh
import com.senademirci.futbolyoklama.ui.theme.RiskMedium
import com.senademirci.futbolyoklama.ui.theme.StatusAbsent
import com.senademirci.futbolyoklama.ui.theme.StatusExcused
import com.senademirci.futbolyoklama.ui.theme.StatusLate
import com.senademirci.futbolyoklama.ui.theme.StatusPresent
import com.senademirci.futbolyoklama.util.DateUtil
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerDetailScreen(
    playerId: String,
    onBack: () -> Unit,
    onEdit: (teamId: String, playerId: String) -> Unit,
    viewModel: PlayerDetailViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(playerId) { viewModel.load(playerId) }

    val player = state.player

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(player?.fullName ?: "Öğrenci") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    if (player != null) {
                        IconButton(onClick = { onEdit(player.teamId, player.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Düzenle")
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        if (player == null) return@Scaffold

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PlayerAvatar(
                    initials = player.initials,
                    jerseyNumber = player.jerseyNumber,
                    size = 64.dp,
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(player.fullName, style = MaterialTheme.typography.headlineSmall)
                    val age = DateUtil.ageFrom(player.birthDate)
                    val details = buildList {
                        player.jerseyNumber?.let { add("Forma $it") }
                        age?.let { add("$it yaşında") }
                        if (!player.isActive) add("Kadro dışı")
                    }
                    if (details.isNotEmpty()) {
                        Text(
                            text = details.joinToString(" · "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (player.parentPhone != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Veli: ${player.parentPhone}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Spacer(Modifier.height(20.dp))

            val stats = player.stats
            if (stats.totalSessions == 0) {
                Text(
                    text = "Bu öğrenci için henüz yoklama alınmadı.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                val riskColor = when {
                    stats.absencePercent >= 30 -> RiskHigh
                    stats.absencePercent >= 15 -> RiskMedium
                    else -> MaterialTheme.colorScheme.primary
                }
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = riskColor.copy(alpha = 0.10f),
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = "Devamsızlık oranı  %${stats.absencePercent}",
                            style = MaterialTheme.typography.titleMedium,
                            color = riskColor,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "${stats.totalSessions} antrenmanın ${stats.attendedCount} tanesine katıldı",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                            StatCell("Var", stats.present, StatusPresent)
                            StatCell("Yok", stats.absent, StatusAbsent)
                            StatCell("İzinli", stats.excused, StatusExcused)
                            StatCell("Geç", stats.late, StatusLate)
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                Text("Son antrenmanlar", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))

                state.recentRecords.forEach { record ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        StatusBadge(record.status)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = DateUtil.formatLong(record.date),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = record.status.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                }
            }

            Spacer(Modifier.height(28.dp))

            OutlinedButton(
                onClick = { if (player.isActive) viewModel.archive() else viewModel.restore() },
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                Text(if (player.isActive) "Kadrodan çıkar" else "Kadroya geri al")
            }
            Text(
                text = "Kadrodan çıkarılan futbolcunun geçmiş yoklamaları korunur.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun StatCell(label: String, value: Int, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value.toString(),
            color = color,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
