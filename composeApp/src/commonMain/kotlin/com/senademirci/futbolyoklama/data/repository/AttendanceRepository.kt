package com.senademirci.futbolyoklama.data.repository

import com.senademirci.futbolyoklama.data.model.AttendanceRecord
import com.senademirci.futbolyoklama.data.model.AttendanceStatus
import com.senademirci.futbolyoklama.data.model.TrainingSession
import kotlinx.coroutines.flow.Flow

interface AttendanceRepository {
    /** Antrenman geçmişi, tarihe göre yeniden eskiye. */
    fun observeSessions(teamId: String): Flow<List<TrainingSession>>

    fun observeSession(sessionId: String): Flow<TrainingSession?>

    fun observeRecordsForSession(sessionId: String): Flow<List<AttendanceRecord>>

    /** Rapor ekranı için tarih aralığındaki tüm kayıtlar (sınırlar dahil). */
    fun observeRecordsInRange(teamId: String, fromDate: String, toDate: String): Flow<List<AttendanceRecord>>

    /** Bir öğrencinin son kayıtları, yeniden eskiye. */
    fun observeRecordsForPlayer(playerId: String, limit: Int): Flow<List<AttendanceRecord>>

    /** O tarihte zaten yoklama alınmış mı? Aynı güne ikinci kayıt açılmasını engellemek için. */
    suspend fun findSessionByDate(teamId: String, date: String): TrainingSession?

    /**
     * Antrenmanı ve tüm öğrenci kayıtlarını tek seferde yazar, öğrenci sayaçlarını
     * günceller. Ya hepsi yazılır ya hiçbiri.
     *
     * @param sessionId null ise yeni antrenman oluşturulur, doluysa mevcut olan güncellenir.
     */
    suspend fun saveAttendance(
        sessionId: String?,
        teamId: String,
        date: String,
        note: String,
        statuses: Map<String, AttendanceStatus>,
        playerNames: Map<String, String>,
    ): Result<String>

    suspend fun deleteSession(sessionId: String): Result<Unit>
}
