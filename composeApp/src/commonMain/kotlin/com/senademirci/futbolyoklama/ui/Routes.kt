package com.senademirci.futbolyoklama.ui

import kotlinx.serialization.Serializable

/**
 * Uygulamadaki tüm ekran adresleri. Tip güvenli navigasyon için serializable.
 *
 * Hiyerarşi: Anasayfa (kulüpler) -> Kulüp (takımlar) -> Takım (kadro).
 * Takıma bağlı ekranlar teamId taşır.
 */
sealed interface Route {
    @Serializable data object Login : Route
    @Serializable data object SignUp : Route

    @Serializable data object Home : Route
    @Serializable data class ClubDetail(val clubId: String) : Route

    @Serializable data class Roster(val teamId: String) : Route
    @Serializable data class PlayerEdit(val teamId: String, val playerId: String? = null) : Route
    @Serializable data class PlayerDetail(val playerId: String) : Route

    @Serializable data class TakeAttendance(val teamId: String, val sessionId: String? = null) : Route
    @Serializable data class SessionHistory(val teamId: String) : Route
    @Serializable data class AbsenceReport(val teamId: String) : Route

    @Serializable data class Dues(val teamId: String) : Route

    @Serializable data object Settings : Route
}
