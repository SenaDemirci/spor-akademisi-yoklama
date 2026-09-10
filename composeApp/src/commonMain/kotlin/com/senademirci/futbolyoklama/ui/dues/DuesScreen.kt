package com.senademirci.futbolyoklama.ui.dues

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.senademirci.futbolyoklama.ui.components.EmptyState
import com.senademirci.futbolyoklama.ui.components.PlayerAvatar
import com.senademirci.futbolyoklama.ui.theme.RiskHigh
import com.senademirci.futbolyoklama.ui.theme.StatusPresent
import com.senademirci.futbolyoklama.util.DateUtil
import com.senademirci.futbolyoklama.util.formatMoney
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuesScreen(
    teamId: String,
    onBack: () -> Unit,
    viewModel: DuesViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showAmountDialog by remember { mutableStateOf(false) }

    LaunchedEffect(teamId) { viewModel.load(teamId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Aidat bilgileri")
                        Text(
                            text = state.team?.name.orEmpty(),
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
                    IconButton(onClick = { showAmountDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Aylık aidat tutarını değiştir")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {

            MonthSelector(
                period = state.period,
                canGoPrevious = state.canGoPrevious,
                canGoNext = state.canGoNext,
                onPrevious = viewModel::previousPeriod,
                onNext = viewModel::nextPeriod,
            )

            if (state.rows.isEmpty() && !state.isLoading) {
                EmptyState(
                    emoji = "💳",
                    title = "Kadroda futbolcu yok",
                    message = "Aidat takibi için önce takıma futbolcu ekle.",
                )
                return@Column
            }

            SummaryBar(
                paid = state.paidCount,
                total = state.playerCount,
                collected = state.collected,
                pending = state.pending,
                monthly = state.monthlyAmount,
            )

            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                items(state.rows, key = { it.player.id }) { row ->
                    DuesRow(row = row, onToggle = { viewModel.togglePaid(row) })
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                }
            }
        }
    }

    if (showAmountDialog) {
        AmountDialog(
            current = state.monthlyAmount,
            onDismiss = { showAmountDialog = false },
            onConfirm = {
                viewModel.updateMonthlyAmount(it)
                showAmountDialog = false
            },
        )
    }
}

@Composable
private fun MonthSelector(
    period: String,
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onPrevious, enabled = canGoPrevious) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Önceki ay")
        }
        Text(
            text = DateUtil.periodLabel(period),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        IconButton(onClick = onNext, enabled = canGoNext) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Sonraki ay")
        }
    }
}

@Composable
private fun SummaryBar(paid: Int, total: Int, collected: Int, pending: Int, monthly: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = "$total futbolcudan $paid tanesi ödedi",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.padding(top = 6.dp))
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Column {
                Text(
                    text = "Tahsil edilen",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formatMoney(collected),
                    color = StatusPresent,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Column {
                Text(
                    text = "Bekleyen",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formatMoney(pending),
                    color = if (pending > 0) RiskHigh else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Aylık aidat",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formatMoney(monthly),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
private fun DuesRow(row: PlayerDuesRow, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlayerAvatar(
            initials = row.player.initials,
            jerseyNumber = row.player.jerseyNumber,
        )
        Spacer(Modifier.width(14.dp))

        Column(Modifier.weight(1f)) {
            Text(text = row.player.fullName, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = when {
                    row.isPaid && row.paidDate != null ->
                        "${DateUtil.formatShort(row.paidDate)} tarihinde ödendi"
                    row.isPaid -> "Ödendi"
                    else -> "Bu ay ödenmedi"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (row.hasDebt) {
                Text(
                    text = "${row.unpaidMonths} ay borç · ${formatMoney(row.debtAmount)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = RiskHigh,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        PaidToggle(isPaid = row.isPaid, onToggle = onToggle)
    }
}

@Composable
private fun PaidToggle(isPaid: Boolean, onToggle: () -> Unit) {
    val color = if (isPaid) StatusPresent else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.12f))
            .clickable(onClick = onToggle)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (isPaid) Icons.Default.CheckCircle
            else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = color,
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = if (isPaid) "Ödendi" else "Ödenmedi",
            color = color,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun AmountDialog(current: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var text by remember { mutableStateOf(current.toString()) }
    val parsed = text.toIntOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Aylık aidat tutarı") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { if (it.length <= 7 && it.all { c -> c.isDigit() }) text = it },
                    label = { Text("Tutar (₺)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                Text(
                    text = "Değişiklik bundan sonraki ödemelere uygulanır; " +
                        "geçmişte kaydedilmiş ödemeler olduğu gibi kalır.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { parsed?.let(onConfirm) }, enabled = parsed != null) {
                Text("Kaydet")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } },
    )
}
