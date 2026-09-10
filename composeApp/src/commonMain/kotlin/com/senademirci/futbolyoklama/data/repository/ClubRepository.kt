package com.senademirci.futbolyoklama.data.repository

import com.senademirci.futbolyoklama.data.model.Club
import com.senademirci.futbolyoklama.data.model.Team
import kotlinx.coroutines.flow.Flow

interface ClubRepository {
    /** Oturum açmış koçun kulüpleri. */
    fun observeClubs(): Flow<List<Club>>

    fun observeClub(clubId: String): Flow<Club?>

    /** Bir kulübün takımları, yaş grubuna göre sıralı (U8, U9, ... U16). */
    fun observeTeams(clubId: String): Flow<List<Team>>

    fun observeTeam(teamId: String): Flow<Team?>

    /** Koçun tüm takımları — anasayfadaki kulüp özetleri için. */
    fun observeAllTeams(): Flow<List<Team>>

    suspend fun upsertClub(club: Club): Result<String>

    suspend fun upsertTeam(team: Team): Result<String>

    /** Aidat tutarını değiştirir; geçmiş aylardaki ödenmiş kayıtlara dokunmaz. */
    suspend fun updateMonthlyDues(teamId: String, amount: Int): Result<Unit>
}
