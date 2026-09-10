package com.senademirci.futbolyoklama.ui

import kotlinx.serialization.Serializable

/** Uygulamadaki tüm ekran adresleri. Tip güvenli navigasyon için serializable. */
sealed interface Route {
    @Serializable data object Login : Route
    @Serializable data object SignUp : Route
    @Serializable data object Roster : Route
    @Serializable data class PlayerEdit(val playerId: String? = null) : Route
    @Serializable data class PlayerDetail(val playerId: String) : Route
    @Serializable data class TakeAttendance(val sessionId: String? = null) : Route
    @Serializable data object SessionHistory : Route
    @Serializable data object AbsenceReport : Route
    @Serializable data object Settings : Route
}
