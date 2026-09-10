package com.senademirci.futbolyoklama.di

import com.senademirci.futbolyoklama.data.repository.AttendanceRepository
import com.senademirci.futbolyoklama.data.repository.AuthRepository
import com.senademirci.futbolyoklama.data.repository.ClubRepository
import com.senademirci.futbolyoklama.data.repository.DuesRepository
import com.senademirci.futbolyoklama.data.repository.PlayerRepository
import com.senademirci.futbolyoklama.data.repository.memory.InMemoryAttendanceRepository
import com.senademirci.futbolyoklama.data.repository.memory.InMemoryAuthRepository
import com.senademirci.futbolyoklama.data.repository.memory.InMemoryClubRepository
import com.senademirci.futbolyoklama.data.repository.memory.InMemoryDuesRepository
import com.senademirci.futbolyoklama.data.repository.memory.InMemoryPlayerRepository
import com.senademirci.futbolyoklama.data.repository.memory.InMemoryStore
import com.senademirci.futbolyoklama.ui.attendance.SessionHistoryViewModel
import com.senademirci.futbolyoklama.ui.attendance.TakeAttendanceViewModel
import com.senademirci.futbolyoklama.ui.auth.LoginViewModel
import com.senademirci.futbolyoklama.ui.auth.SignUpViewModel
import com.senademirci.futbolyoklama.ui.club.ClubDetailViewModel
import com.senademirci.futbolyoklama.ui.dues.DuesViewModel
import com.senademirci.futbolyoklama.ui.home.HomeViewModel
import com.senademirci.futbolyoklama.ui.report.AbsenceReportViewModel
import com.senademirci.futbolyoklama.ui.roster.PlayerDetailViewModel
import com.senademirci.futbolyoklama.ui.roster.PlayerEditViewModel
import com.senademirci.futbolyoklama.ui.roster.RosterViewModel
import com.senademirci.futbolyoklama.ui.settings.SettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Şu an repository'ler bellek içi uygulamalara bağlı. Firebase yapılandırması
 * (google-services.json) geldiğinde yalnızca bu beş satır Firestore sürümleriyle
 * değiştirilecek — ekranlarda ve ViewModel'lerde hiçbir değişiklik gerekmeyecek.
 */
val appModule = module {
    single { InMemoryStore() }

    single<AuthRepository> { InMemoryAuthRepository(get()) }
    single<ClubRepository> { InMemoryClubRepository(get(), get()) }
    single<PlayerRepository> { InMemoryPlayerRepository(get(), get()) }
    single<AttendanceRepository> { InMemoryAttendanceRepository(get()) }
    single<DuesRepository> { InMemoryDuesRepository(get()) }

    viewModel { LoginViewModel(get()) }
    viewModel { SignUpViewModel(get()) }
    viewModel { HomeViewModel(get(), get(), get()) }
    viewModel { ClubDetailViewModel(get(), get()) }
    viewModel { RosterViewModel(get(), get()) }
    viewModel { PlayerEditViewModel(get(), get()) }
    viewModel { PlayerDetailViewModel(get(), get()) }
    viewModel { TakeAttendanceViewModel(get(), get()) }
    viewModel { SessionHistoryViewModel(get()) }
    viewModel { AbsenceReportViewModel(get(), get()) }
    viewModel { DuesViewModel(get(), get(), get()) }
    viewModel { SettingsViewModel(get()) }
}
