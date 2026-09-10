package com.senademirci.futbolyoklama.ui.roster

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.senademirci.futbolyoklama.data.model.Player
import com.senademirci.futbolyoklama.ui.components.EmptyState
import com.senademirci.futbolyoklama.ui.components.PlayerAvatar
import com.senademirci.futbolyoklama.ui.theme.RiskHigh
import com.senademirci.futbolyoklama.ui.theme.RiskMedium
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RosterScreen(
    onAddPlayer: () -> Unit,
    onEditPlayer: (String) -> Unit,
    onOpenPlayer: (String) -> Unit,
    onTakeAttendance: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenReport: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: RosterViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var menuForPlayer by remember { mutableStateOf<Player?>(null) }
    var confirmDelete by remember { mutableStateOf<Player?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = state.teamName.ifBlank { "Kadro" },
                            style = MaterialTheme.typography.titleLarge,
                        )
                        if (state.players.isNotEmpty()) {
                            Text(
                                text = "${state.players.size} öğrenci",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onOpenReport) {
                        Icon(Icons.Default.Assessment, contentDescription = "Devamsızlık raporu")
                    }
                    IconButton(onClick = onOpenHistory) {
                        Icon(Icons.Default.History, contentDescription = "Antrenman geçmişi")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Ayarlar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddPlayer) {
                Icon(Icons.Default.Add, contentDescription = "Öğrenci ekle")
            }
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {

            Button(
                onClick = onTakeAttendance,
                enabled = state.players.isNotEmpty() || state.query.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Text("Yoklama Al", style = MaterialTheme.typography.titleMedium)
            }

            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                placeholder = { Text("İsim veya forma numarası ara") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.query.isNotBlank()) {
                        IconButton(onClick = { viewModel.onQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Aramayı temizle")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )

            Spacer(Modifier.height(8.dp))

            when {
                state.isEmpty -> EmptyState(
                    emoji = "👥",
                    title = "Kadro boş",
                    message = "Sağ alttaki + düğmesiyle ilk öğrencini ekle.",
                    actionLabel = "Öğrenci ekle",
                    onAction = onAddPlayer,
                )

                state.noSearchResults -> EmptyState(
                    emoji = "🔍",
                    title = "Sonuç yok",
                    message = "\"${state.query}\" ile eşleşen öğrenci bulunamadı.",
                )

                else -> LazyColumn(
                    contentPadding = PaddingValues(bottom = 88.dp),
                ) {
                    items(state.players, key = { it.id }) { player ->
                        Box {
                            PlayerRow(
                                player = player,
                                onClick = { onOpenPlayer(player.id) },
                                onLongClick = { menuForPlayer = player },
                            )
                            DropdownMenu(
                                expanded = menuForPlayer?.id == player.id,
                                onDismissRequest = { menuForPlayer = null },
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Düzenle") },
                                    onClick = {
                                        menuForPlayer = null
                                        onEditPlayer(player.id)
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Kadrodan çıkar") },
                                    onClick = {
                                        menuForPlayer = null
                                        viewModel.archive(player.id)
                                    },
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Kalıcı sil",
                                            color = MaterialTheme.colorScheme.error,
                                        )
                                    },
                                    onClick = {
                                        menuForPlayer = null
                                        confirmDelete = player
                                    },
                                )
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    }
                }
            }
        }
    }

    confirmDelete?.let { player ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("${player.fullName} kalıcı olarak silinsin mi?") },
            text = {
                Text(
                    "Öğrenci ve tüm yoklama kayıtları silinecek. Bu işlem geri alınamaz.\n\n" +
                        "Geçmişi korumak istiyorsan \"Kadrodan çıkar\" seçeneğini kullan.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletePermanently(player.id)
                        confirmDelete = null
                    },
                ) {
                    Text("Kalıcı sil", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = null }) { Text("Vazgeç") }
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlayerRow(
    player: Player,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlayerAvatar(initials = player.initials, jerseyNumber = player.jerseyNumber)

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = player.fullName, style = MaterialTheme.typography.bodyLarge)
            val stats = player.stats
            Text(
                text = when {
                    stats.totalSessions == 0 -> "Henüz yoklama alınmadı"
                    stats.excused > 0 ->
                        "${stats.attendedCount}/${stats.totalSessions} antrenmana katıldı · " +
                            "${stats.excused} izinli"
                    else -> "${stats.attendedCount}/${stats.totalSessions} antrenmana katıldı"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (player.stats.absent > 0) {
            AbsenceBadge(percent = player.stats.absencePercent, absentCount = player.stats.absent)
        }
    }
}

@Composable
private fun AbsenceBadge(percent: Int, absentCount: Int) {
    val color = when {
        percent >= 30 -> RiskHigh
        percent >= 15 -> RiskMedium
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "%$percent",
                color = color,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "$absentCount yok",
                color = color,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}
