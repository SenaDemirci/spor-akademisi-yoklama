package com.senademirci.futbolyoklama.data.repository.memory

import com.senademirci.futbolyoklama.data.model.Coach
import com.senademirci.futbolyoklama.data.repository.AuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class InMemoryAuthRepository(private val store: InMemoryStore) : AuthRepository {

    private val _currentCoach = MutableStateFlow<Coach?>(null)
    override val currentCoach: StateFlow<Coach?> = _currentCoach.asStateFlow()

    override val isRestoring: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()

    override suspend fun signIn(email: String, password: String): Result<Coach> {
        delay(300) // ağ gecikmesini taklit et, yükleniyor göstergeleri test edilebilsin
        val entry = store.coaches.value.values
            .firstOrNull { it.first.email.equals(email.trim(), ignoreCase = true) }
            ?: return Result.failure(IllegalStateException("Bu e-posta ile kayıtlı koç bulunamadı."))
        if (entry.second != password) {
            return Result.failure(IllegalStateException("Şifre hatalı."))
        }
        _currentCoach.value = entry.first
        return Result.success(entry.first)
    }

    override suspend fun signUp(name: String, email: String, password: String): Result<Coach> {
        delay(300)
        val normalized = email.trim().lowercase()
        if (store.coaches.value.values.any { it.first.email.lowercase() == normalized }) {
            return Result.failure(IllegalStateException("Bu e-posta zaten kayıtlı."))
        }
        val uid = store.newId("coach")
        val coach = Coach(uid = uid, name = name.trim(), email = normalized)

        store.coaches.update { it + (uid to (coach to password)) }
        // Geliştirme sürümüne özel örnek veri; Firestore'a geçince kaldırılacak
        DemoDataSeeder.seed(store, uid)
        _currentCoach.value = coach
        return Result.success(coach)
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> {
        delay(200)
        return Result.success(Unit)
    }

    override suspend fun signOut() {
        _currentCoach.value = null
    }
}
