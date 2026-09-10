package com.senademirci.futbolyoklama.ui.dues

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.senademirci.futbolyoklama.data.model.DuesRecord
import com.senademirci.futbolyoklama.data.model.Player
import com.senademirci.futbolyoklama.data.model.Team
import com.senademirci.futbolyoklama.data.repository.ClubRepository
import com.senademirci.futbolyoklama.data.repository.DuesRepository
import com.senademirci.futbolyoklama.data.repository.PlayerRepository
import com.senademirci.futbolyoklama.util.DateUtil
import com.senademirci.futbolyoklama.util.turkishSortKey
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Bir öğrencinin seçili aydaki aidat durumu ve toplam borcu. */
data class PlayerDuesRow(
    val player: Player,
    val isPaid: Boolean,
    val paidDate: String?,
    val amount: Int,
    /** Aidat başlangıcından bu aya kadar ödenmemiş ay sayısı (seçili aydan bağımsız). */
    val unpaidMonths: Int,
    val debtAmount: Int,
) {
    val hasDebt: Boolean get() = unpaidMonths > 0
}

data class DuesUiState(
    val team: Team? = null,
    val period: String = DateUtil.currentPeriod(),
    val rows: List<PlayerDuesRow> = emptyList(),
    val isLoading: Boolean = true,
) {
    val monthlyAmount: Int get() = team?.monthlyDuesAmount ?: 0
    val paidCount: Int get() = rows.count { it.isPaid }
    val playerCount: Int get() = rows.size
    val collected: Int get() = rows.filter { it.isPaid }.sumOf { it.amount }
    val pending: Int get() = rows.filter { !it.isPaid }.sumOf { monthlyAmount }
    val debtorCount: Int get() = rows.count { it.hasDebt }

    /**
     * Yalnızca aidat takibinin yapıldığı aylar gezilebilir: başlangıç ayından
     * içinde bulunulan aya kadar. Dışarıdaki aylarda kimsenin borcu yoktur, o
     * ayları göstermek yanıltıcı olur.
     */
    val canGoNext: Boolean get() = period < DateUtil.currentPeriod()
    val canGoPrevious: Boolean
        get() = team?.duesStartPeriod?.let { it.isNotBlank() && period > it } ?: false
}

@OptIn(ExperimentalCoroutinesApi::class)
class DuesViewModel(
    private val clubRepository: ClubRepository,
    private val playerRepository: PlayerRepository,
    private val duesRepository: DuesRepository,
) : ViewModel() {

    private val teamId = MutableStateFlow<String?>(null)
    private val period = MutableStateFlow(DateUtil.currentPeriod())

    val state: StateFlow<DuesUiState> = teamId
        .flatMapLatest { id ->
            if (id == null) flowOf(DuesUiState())
            else combine(
                clubRepository.observeTeam(id),
                playerRepository.observeRoster(id),
                duesRepository.observeDuesForTeam(id),
                period,
            ) { team, players, dues, selectedPeriod ->
                buildState(team, players, dues, selectedPeriod)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DuesUiState())

    private fun buildState(
        team: Team?,
        players: List<Player>,
        dues: List<DuesRecord>,
        selectedPeriod: String,
    ): DuesUiState {
        val monthly = team?.monthlyDuesAmount ?: 0
        val start = team?.duesStartPeriod.orEmpty().ifBlank { DateUtil.currentPeriod() }
        // Borç yalnızca geçmiş ve içinde bulunulan aylar için hesaplanır
        val trackedMonths = DateUtil.periodsBetween(start, DateUtil.currentPeriod())

        val paidByPlayer = dues.filter { it.isPaid }.groupBy { it.playerId }

        val rows = players.map { player ->
            val paidPeriods = paidByPlayer[player.id].orEmpty().associateBy { it.period }
            val thisMonth = paidPeriods[selectedPeriod]
            val unpaid = trackedMonths.count { it !in paidPeriods }

            PlayerDuesRow(
                player = player,
                isPaid = thisMonth != null,
                paidDate = thisMonth?.paidDate,
                amount = thisMonth?.amount ?: monthly,
                unpaidMonths = unpaid,
                debtAmount = unpaid * monthly,
            )
        }.sortedWith(
            // Borcu olanlar üstte; koç önce onları görsün
            compareByDescending<PlayerDuesRow> { it.unpaidMonths }
                .thenBy { turkishSortKey(it.player.lastName) },
        )

        return DuesUiState(team = team, period = selectedPeriod, rows = rows, isLoading = false)
    }

    fun load(id: String) {
        teamId.value = id
    }

    fun previousPeriod() {
        if (state.value.canGoPrevious) period.value = DateUtil.shiftPeriod(period.value, -1)
    }

    fun nextPeriod() {
        if (state.value.canGoNext) period.value = DateUtil.shiftPeriod(period.value, 1)
    }

    fun togglePaid(row: PlayerDuesRow) {
        val amount = state.value.monthlyAmount
        viewModelScope.launch {
            duesRepository.setPaid(
                playerId = row.player.id,
                period = state.value.period,
                isPaid = !row.isPaid,
                amount = amount,
            )
        }
    }

    fun updateMonthlyAmount(amount: Int) {
        val id = teamId.value ?: return
        if (amount < 0) return
        viewModelScope.launch { clubRepository.updateMonthlyDues(id, amount) }
    }
}
