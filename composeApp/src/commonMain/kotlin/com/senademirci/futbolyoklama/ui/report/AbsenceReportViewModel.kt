package com.senademirci.futbolyoklama.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.senademirci.futbolyoklama.data.model.AttendanceRecord
import com.senademirci.futbolyoklama.data.model.AttendanceStatus
import com.senademirci.futbolyoklama.data.model.Player
import com.senademirci.futbolyoklama.data.repository.AttendanceRepository
import com.senademirci.futbolyoklama.data.repository.AuthRepository
import com.senademirci.futbolyoklama.data.repository.PlayerRepository
import com.senademirci.futbolyoklama.util.DateUtil
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.math.roundToInt

enum class ReportRange(val label: String) {
    LAST_MONTH("Son 1 ay"),
    LAST_3_MONTHS("Son 3 ay"),
    SEASON("Tüm sezon");

    /** Aralığın başlangıç tarihi (yyyy-MM-dd). */
    fun fromDate(): String = when (this) {
        LAST_MONTH -> DateUtil.monthsAgo(1)
        LAST_3_MONTHS -> DateUtil.monthsAgo(3)
        SEASON -> "0000-01-01"
    }
}

/** Bir öğrencinin seçili tarih aralığındaki devam durumu. */
data class AbsenceSummary(
    val player: Player,
    val totalSessions: Int,
    val present: Int,
    val absent: Int,
    val excused: Int,
    val late: Int,
) {
    /** İzinli olunan antrenmanlar paydadan düşülür. */
    private val accountable: Int get() = totalSessions - excused

    val absenceRate: Float get() = if (accountable <= 0) 0f else absent.toFloat() / accountable
    val absencePercent: Int get() = (absenceRate * 100).roundToInt()
    val attended: Int get() = present + late
}

data class AbsenceReportUiState(
    val range: ReportRange = ReportRange.LAST_MONTH,
    val summaries: List<AbsenceSummary> = emptyList(),
    val todayAbsentees: List<AbsenceTodayItem> = emptyList(),
    val sessionCountInRange: Int = 0,
    val isLoading: Boolean = true,
) {
    /** En az bir kez gelmemiş olanlar — asıl ilgilenilen liste. */
    val withAbsences: List<AbsenceSummary> get() = summaries.filter { it.absent > 0 }
    val hasAnyData: Boolean get() = sessionCountInRange > 0
}

data class AbsenceTodayItem(
    val player: Player,
    val status: AttendanceStatus,
    val note: String,
)

@OptIn(ExperimentalCoroutinesApi::class)
class AbsenceReportViewModel(
    private val authRepository: AuthRepository,
    private val playerRepository: PlayerRepository,
    private val attendanceRepository: AttendanceRepository,
) : ViewModel() {

    private val range = MutableStateFlow(ReportRange.LAST_MONTH)

    private val recordsAndPlayers = combine(
        authRepository.currentCoach,
        range,
    ) { coach, r -> coach to r }
        .flatMapLatest { (coach, r) ->
            if (coach == null) {
                flowOf(Triple(emptyList<AttendanceRecord>(), emptyList<Player>(), r))
            } else {
                combine(
                    attendanceRepository.observeRecordsInRange(
                        teamId = coach.teamId,
                        fromDate = r.fromDate(),
                        toDate = DateUtil.today(),
                    ),
                    playerRepository.observeAll(coach.teamId),
                ) { records, players -> Triple(records, players, r) }
            }
        }

    val state: StateFlow<AbsenceReportUiState> = recordsAndPlayers
        .map { (records, players, r) -> buildState(records, players, r) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AbsenceReportUiState())

    private fun buildState(
        records: List<AttendanceRecord>,
        players: List<Player>,
        r: ReportRange,
    ): AbsenceReportUiState {
        val byPlayer = records.groupBy { it.playerId }
        val playersById = players.associateBy { it.id }

        val summaries = players.mapNotNull { player ->
            val recs = byPlayer[player.id].orEmpty()
            // Bu aralıkta hiç kaydı olmayan öğrenci raporda yer almaz
            if (recs.isEmpty()) return@mapNotNull null
            AbsenceSummary(
                player = player,
                totalSessions = recs.size,
                present = recs.count { it.status == AttendanceStatus.PRESENT },
                absent = recs.count { it.status == AttendanceStatus.ABSENT },
                excused = recs.count { it.status == AttendanceStatus.EXCUSED },
                late = recs.count { it.status == AttendanceStatus.LATE },
            )
        }.sortedWith(
            compareByDescending<AbsenceSummary> { it.absenceRate }
                .thenByDescending { it.absent }
                .thenBy { it.player.lastName.lowercase() },
        )

        val today = DateUtil.today()
        // Geç gelen antrenmana katılmıştır — "gelmeyenler" listesinde yeri yok.
        val todayAbsentees = records
            .filter {
                it.date == today &&
                    (it.status == AttendanceStatus.ABSENT || it.status == AttendanceStatus.EXCUSED)
            }
            .mapNotNull { rec ->
                playersById[rec.playerId]?.let {
                    AbsenceTodayItem(player = it, status = rec.status, note = rec.note)
                }
            }
            .sortedWith(
                compareBy<AbsenceTodayItem> { it.status != AttendanceStatus.ABSENT }
                    .thenBy { it.player.lastName.lowercase() },
            )

        return AbsenceReportUiState(
            range = r,
            summaries = summaries,
            todayAbsentees = todayAbsentees,
            sessionCountInRange = records.map { it.sessionId }.distinct().size,
            isLoading = false,
        )
    }

    fun setRange(value: ReportRange) {
        range.value = value
    }
}
