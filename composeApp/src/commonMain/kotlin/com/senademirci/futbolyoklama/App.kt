package com.senademirci.futbolyoklama

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.senademirci.futbolyoklama.data.repository.AuthRepository
import com.senademirci.futbolyoklama.di.appModule
import com.senademirci.futbolyoklama.ui.Route
import com.senademirci.futbolyoklama.ui.attendance.SessionHistoryScreen
import com.senademirci.futbolyoklama.ui.attendance.TakeAttendanceScreen
import com.senademirci.futbolyoklama.ui.auth.LoginScreen
import com.senademirci.futbolyoklama.ui.auth.SignUpScreen
import com.senademirci.futbolyoklama.ui.club.ClubDetailScreen
import com.senademirci.futbolyoklama.ui.dues.DuesScreen
import com.senademirci.futbolyoklama.ui.home.HomeScreen
import com.senademirci.futbolyoklama.ui.report.AbsenceReportScreen
import com.senademirci.futbolyoklama.ui.roster.PlayerDetailScreen
import com.senademirci.futbolyoklama.ui.roster.PlayerEditScreen
import com.senademirci.futbolyoklama.ui.roster.RosterScreen
import com.senademirci.futbolyoklama.ui.settings.SettingsScreen
import com.senademirci.futbolyoklama.ui.theme.FutbolYoklamaTheme
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject

@Composable
fun App() {
    KoinApplication(application = { modules(appModule) }) {
        FutbolYoklamaTheme {
            val authRepository = koinInject<AuthRepository>()
            val coach by authRepository.currentCoach.collectAsStateWithLifecycle()
            val isRestoring by authRepository.isRestoring.collectAsStateWithLifecycle()

            when {
                isRestoring -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                // Oturum açık değilken ayrı bir NavHost kullanılır; böylece çıkış yapıldığında
                // giriş yapılmış ekranlar geri yığınında kalmaz.
                coach == null -> AuthNavHost()
                else -> MainNavHost()
            }
        }
    }
}

@Composable
private fun AuthNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Route.Login) {
        composable<Route.Login> {
            LoginScreen(onNavigateToSignUp = { navController.navigate(Route.SignUp) })
        }
        composable<Route.SignUp> {
            SignUpScreen(onBack = { navController.popBackStack() })
        }
    }
}

@Composable
private fun MainNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Route.Home) {

        composable<Route.Home> {
            HomeScreen(
                onOpenClub = { navController.navigate(Route.ClubDetail(it)) },
                onOpenSettings = { navController.navigate(Route.Settings) },
            )
        }

        composable<Route.ClubDetail> { entry ->
            ClubDetailScreen(
                clubId = entry.toRoute<Route.ClubDetail>().clubId,
                onBack = { navController.popBackStack() },
                onOpenTeam = { navController.navigate(Route.Roster(it)) },
            )
        }

        composable<Route.Roster> { entry ->
            val teamId = entry.toRoute<Route.Roster>().teamId
            RosterScreen(
                teamId = teamId,
                onBack = { navController.popBackStack() },
                onAddPlayer = { navController.navigate(Route.PlayerEdit(teamId)) },
                onEditPlayer = { navController.navigate(Route.PlayerEdit(teamId, it)) },
                onOpenPlayer = { navController.navigate(Route.PlayerDetail(it)) },
                onTakeAttendance = { navController.navigate(Route.TakeAttendance(teamId)) },
                onOpenDues = { navController.navigate(Route.Dues(teamId)) },
                onOpenHistory = { navController.navigate(Route.SessionHistory(teamId)) },
                onOpenReport = { navController.navigate(Route.AbsenceReport(teamId)) },
            )
        }

        composable<Route.PlayerEdit> { entry ->
            val route = entry.toRoute<Route.PlayerEdit>()
            PlayerEditScreen(
                teamId = route.teamId,
                playerId = route.playerId,
                onBack = { navController.popBackStack() },
            )
        }

        composable<Route.PlayerDetail> { entry ->
            PlayerDetailScreen(
                playerId = entry.toRoute<Route.PlayerDetail>().playerId,
                onBack = { navController.popBackStack() },
                onEdit = { teamId, playerId ->
                    navController.navigate(Route.PlayerEdit(teamId, playerId))
                },
            )
        }

        composable<Route.TakeAttendance> { entry ->
            val route = entry.toRoute<Route.TakeAttendance>()
            TakeAttendanceScreen(
                teamId = route.teamId,
                sessionId = route.sessionId,
                onBack = { navController.popBackStack() },
            )
        }

        composable<Route.SessionHistory> { entry ->
            val teamId = entry.toRoute<Route.SessionHistory>().teamId
            SessionHistoryScreen(
                teamId = teamId,
                onBack = { navController.popBackStack() },
                onOpenSession = { navController.navigate(Route.TakeAttendance(teamId, it)) },
            )
        }

        composable<Route.AbsenceReport> { entry ->
            AbsenceReportScreen(
                teamId = entry.toRoute<Route.AbsenceReport>().teamId,
                onBack = { navController.popBackStack() },
                onOpenPlayer = { navController.navigate(Route.PlayerDetail(it)) },
            )
        }

        composable<Route.Dues> { entry ->
            DuesScreen(
                teamId = entry.toRoute<Route.Dues>().teamId,
                onBack = { navController.popBackStack() },
            )
        }

        composable<Route.Settings> {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
