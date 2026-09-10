package com.senademirci.futbolyoklama.data.repository.memory

import com.senademirci.futbolyoklama.data.model.AttendanceRecord
import com.senademirci.futbolyoklama.data.model.Coach
import com.senademirci.futbolyoklama.data.model.Player
import com.senademirci.futbolyoklama.data.model.Team
import com.senademirci.futbolyoklama.data.model.TrainingSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.random.Random

/**
 * Firebase yapılandırması gelene kadar uygulamayı uçtan uca çalıştırabilmek için
 * bellek içi veri deposu. Koleksiyonlar Firestore modeliyle birebir aynı.
 *
 * Veriler süreç kapanınca kaybolur — sadece geliştirme/doğrulama amaçlıdır.
 */
class InMemoryStore {
    /** uid -> (koç, şifre). Şifre sadece bellek içi giriş taklidi için tutulur. */
    val coaches = MutableStateFlow<Map<String, Pair<Coach, String>>>(emptyMap())
    val teams = MutableStateFlow<List<Team>>(emptyList())
    val players = MutableStateFlow<List<Player>>(emptyList())
    val sessions = MutableStateFlow<List<TrainingSession>>(emptyList())
    val records = MutableStateFlow<List<AttendanceRecord>>(emptyList())

    fun newId(prefix: String): String =
        "$prefix-${Random.nextLong(1_000_000_000L, 9_999_999_999L)}"
}
