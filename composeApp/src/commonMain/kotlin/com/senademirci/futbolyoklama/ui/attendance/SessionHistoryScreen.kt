package com.senademirci.futbolyoklama.ui.attendance

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.senademirci.futbolyoklama.data.model.TrainingSession
import com.senademirci.futbolyoklama.ui.components.EmptyState
import com.senademirci.futbolyoklama.ui.theme.RiskHigh
import com.senademirci.futbolyoklama.ui.theme.RiskMedium
import com.senademirci.futbolyoklama.util.DateUtil
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionHistoryScreen(
    teamId: String,
    onBack: () -> Unit,
    onOpenSession: (String) -> Unit,
    viewModel: SessionHistoryViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(teamId) { viewModel.load(teamId) }
    var menuFor by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf<TrainingSession?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Antrenman geçmişi") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            if (state.sessions.isEmpty() && !state.isLoading) {
                EmptyState(
                    emoji = "📋",
                    title = "Henüz yoklama alınmadı",
                    message = "İlk antrenmanın yoklamasını aldığında burada listelenecek.",
                )
                return@Column
            }

            LazyColumn {
                items(state.sessions, key = { it.id }) { session ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenSession(session.id) }
                            .padding(start = 16.dp, top = 12.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = DateUtil.formatLong(session.date),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                text = buildString {
                                    append("${session.attendedCount}/${session.playerCount} katıldı")
                                    if (session.absentCount > 0) append(" · ${session.absentCount} yok")
                                    if (session.excusedCount > 0) append(" · ${session.excusedCount} izinli")
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (session.note.isNotBlank()) {
                                Text(
                                    text = session.note,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        val absentRatio = if (session.playerCount == 0) 0f
                        else session.absentCount.toFloat() / session.playerCount
                        val color = when {
                            absentRatio >= 0.3f -> RiskHigh
                            absentRatio >= 0.15f -> RiskMedium
                            else -> MaterialTheme.colorScheme.primary
                        }
                        Text(
                            text = "${session.attendedCount}/${session.playerCount}",
                            color = color,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(Modifier.width(4.dp))

                        Box {
                            IconButton(onClick = { menuFor = session.id }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Seçenekler")
                            }
                            DropdownMenu(
                                expanded = menuFor == session.id,
                                onDismissRequest = { menuFor = null },
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Düzenle") },
                                    onClick = {
                                        menuFor = null
                                        onOpenSession(session.id)
                                    },
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text("Sil", color = MaterialTheme.colorScheme.error)
                                    },
                                    onClick = {
                                        menuFor = null
                                        confirmDelete = session
                                    },
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                }
            }
        }
    }

    confirmDelete?.let { session ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Yoklama silinsin mi?") },
            text = {
                Text(
                    "${DateUtil.formatLong(session.date)} tarihli antrenmanın tüm kayıtları " +
                        "silinecek ve öğrenci istatistikleri buna göre güncellenecek.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSession(session.id)
                        confirmDelete = null
                    },
                ) { Text("Sil", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = null }) { Text("Vazgeç") }
            },
        )
    }
}
