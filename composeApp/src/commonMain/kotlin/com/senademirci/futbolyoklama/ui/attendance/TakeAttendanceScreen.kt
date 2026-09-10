package com.senademirci.futbolyoklama.ui.attendance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.senademirci.futbolyoklama.ui.components.AttendanceStatusSelector
import com.senademirci.futbolyoklama.ui.components.EmptyState
import com.senademirci.futbolyoklama.ui.components.PlayerAvatar
import com.senademirci.futbolyoklama.ui.theme.StatusAbsent
import com.senademirci.futbolyoklama.ui.theme.StatusExcused
import com.senademirci.futbolyoklama.ui.theme.StatusLate
import com.senademirci.futbolyoklama.ui.theme.StatusPresent
import com.senademirci.futbolyoklama.util.DateUtil
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TakeAttendanceScreen(
    sessionId: String?,
    onBack: () -> Unit,
    viewModel: TakeAttendanceViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(sessionId) { viewModel.load(sessionId) }
    LaunchedEffect(state.isSaved) { if (state.isSaved) onBack() }
    LaunchedEffect(state.error) { state.error?.let { snackbarHostState.showSnackbar(it) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(if (state.isEditing) "Yoklamayı düzenle" else "Yoklama Al")
                        Text(
                            text = DateUtil.formatLong(state.date),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = "Tarihi değiştir",
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                    TextButton(onClick = viewModel::markAllPresent) {
                        Text("Tümü Var", color = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (state.players.isNotEmpty()) {
                Button(
                    onClick = viewModel::save,
                    enabled = state.canSubmit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(54.dp),
                ) {
                    if (state.isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text("Yoklamayı kaydet", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {

            if (state.players.isEmpty() && !state.isLoading) {
                EmptyState(
                    emoji = "👥",
                    title = "Kadroda öğrenci yok",
                    message = "Yoklama alabilmek için önce kadroya öğrenci ekle.",
                )
                return@Column
            }

            SummaryBar(
                present = state.presentCount,
                absent = state.absentCount,
                excused = state.excusedCount,
                late = state.lateCount,
            )

            OutlinedTextField(
                value = state.note,
                onValueChange = viewModel::onNoteChange,
                label = { Text("Antrenman notu") },
                placeholder = { Text("İsteğe bağlı — örn. hazırlık maçı") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )

            LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                items(state.players, key = { it.id }) { player ->
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PlayerAvatar(
                                initials = player.initials,
                                jerseyNumber = player.jerseyNumber,
                                size = 36.dp,
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = player.fullName,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        AttendanceStatusSelector(
                            selected = state.statuses[player.id]
                                ?: com.senademirci.futbolyoklama.data.model.AttendanceStatus.PRESENT,
                            onSelect = { viewModel.setStatus(player.id, it) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                }
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = DateUtil.isoToEpochMillis(state.date),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis
                            ?.let { viewModel.onDateChange(DateUtil.epochMillisToIso(it)) }
                        showDatePicker = false
                    },
                ) { Text("Seç") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Vazgeç") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (state.existingSessionIdForDate != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissExistingSessionWarning,
            title = { Text("Bu tarihte zaten yoklama var") },
            text = {
                Text(
                    "${DateUtil.formatLong(state.date)} için daha önce yoklama alınmış. " +
                        "Mevcut kaydı açıp düzenleyebilirsin.",
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::switchToExistingSession) {
                    Text("Mevcut kaydı aç")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissExistingSessionWarning) {
                    Text("Yeni kayıt oluştur")
                }
            },
        )
    }
}

/** Üstte canlı sayaç — koç kaydetmeden önce dağılımı görsün. */
@Composable
private fun SummaryBar(present: Int, absent: Int, excused: Int, late: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        SummaryItem("Var", present, StatusPresent)
        SummaryItem("Yok", absent, StatusAbsent)
        SummaryItem("İzinli", excused, StatusExcused)
        SummaryItem("Geç", late, StatusLate)
    }
}

@Composable
private fun SummaryItem(label: String, count: Int, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
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
