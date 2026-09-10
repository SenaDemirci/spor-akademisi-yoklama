package com.senademirci.futbolyoklama.data.repository

import com.senademirci.futbolyoklama.data.model.Coach
import com.senademirci.futbolyoklama.data.model.Team
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Koç kimlik doğrulaması. Öğrencilerin hesabı yoktur; uygulamaya yalnızca koçlar girer.
 */
interface AuthRepository {
    /** Oturum açmış koç, yoksa null. Uygulama açılışında Firebase'den geri yüklenir. */
    val currentCoach: StateFlow<Coach?>

    /** İlk yükleme bitene kadar true — açılışta giriş ekranının yanıp sönmesini engeller. */
    val isRestoring: StateFlow<Boolean>

    suspend fun signIn(email: String, password: String): Result<Coach>

    suspend fun signUp(
        name: String,
        email: String,
        password: String,
        teamName: String,
    ): Result<Coach>

    suspend fun sendPasswordReset(email: String): Result<Unit>

    /** Oturum açmış koçun takımı, canlı. */
    fun observeTeam(): Flow<Team?>

    suspend fun updateTeamName(name: String): Result<Unit>

    suspend fun signOut()
}
