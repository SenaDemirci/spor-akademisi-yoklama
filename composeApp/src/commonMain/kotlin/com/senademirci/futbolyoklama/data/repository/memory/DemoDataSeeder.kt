package com.senademirci.futbolyoklama.data.repository.memory

import com.senademirci.futbolyoklama.data.model.Club
import com.senademirci.futbolyoklama.data.model.Player
import com.senademirci.futbolyoklama.data.model.Team
import com.senademirci.futbolyoklama.util.DateUtil

/**
 * Yeni açılan koç hesabına örnek kulüp/takım/öğrenci verisi yerleştirir.
 *
 * SADECE bellek içi geliştirme sürümüne özeldir. Firestore'a geçildiğinde bu sınıf
 * kullanılmaz — gerçek koç boş bir anasayfayla başlar ve kulüplerini kendi ekler.
 */
object DemoDataSeeder {

    private val clubNames = listOf("A Kulübü", "B Kulübü", "C Kulübü")
    private val ageGroups = (8..16).map { "U$it" }

    /** A Kulübü / U8 takımına konan örnek futbolcular. */
    private val sampleU8Players = listOf(
        Triple("Arda", "Yücel", 7),
        Triple("Bora", "Çetin", 10),
        Triple("Kaan", "Aydın", 4),
        Triple("Mert", "Doğan", 9),
        Triple("Tuna", "Keskin", 11),
    )

    fun seed(store: InMemoryStore, ownerUid: String) {
        val season = "2026-2027"
        val startPeriod = DateUtil.currentPeriod()

        val newClubs = mutableListOf<Club>()
        val newTeams = mutableListOf<Team>()
        val newPlayers = mutableListOf<Player>()

        clubNames.forEach { clubName ->
            val clubId = store.newId("club")
            newClubs += Club(id = clubId, ownerUid = ownerUid, name = clubName)

            ageGroups.forEach { age ->
                val teamId = store.newId("team")
                newTeams += Team(
                    id = teamId,
                    ownerUid = ownerUid,
                    clubId = clubId,
                    name = age,
                    season = season,
                    monthlyDuesAmount = 1500,
                    duesStartPeriod = startPeriod,
                )

                // Örnek futbolcular yalnızca A Kulübü'nün U8 takımında
                if (clubName == clubNames.first() && age == "U8") {
                    sampleU8Players.forEach { (ad, soyad, forma) ->
                        newPlayers += Player(
                            id = store.newId("player"),
                            ownerUid = ownerUid,
                            clubId = clubId,
                            teamId = teamId,
                            firstName = ad,
                            lastName = soyad,
                            jerseyNumber = forma,
                        )
                    }
                }
            }
        }

        store.clubs.value = store.clubs.value + newClubs
        store.teams.value = store.teams.value + newTeams
        store.players.value = store.players.value + newPlayers
    }
}
