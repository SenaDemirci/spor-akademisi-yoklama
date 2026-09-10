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
    NavHost(navController = navController, startDestination = Route.Roster) {

        composable<Route.Roster> {
            RosterScreen(
                onAddPlayer = { navController.navigate(Route.PlayerEdit()) },
                onEditPlayer = { navController.navigate(Route.PlayerEdit(it)) },
                onOpenPlayer = { navController.navigate(Route.PlayerDetail(it)) },
                onTakeAttendance = { navController.navigate(Route.TakeAttendance()) },
                onOpenHistory = { navController.navigate(Route.SessionHistory) },
                onOpenReport = { navController.navigate(Route.AbsenceReport) },
                onOpenSettings = { navController.navigate(Route.Settings) },
            )
        }

        composable<Route.PlayerEdit> { backStackEntry ->
            PlayerEditScreen(
                playerId = backStackEntry.toRoute<Route.PlayerEdit>().playerId,
                onBack = { navController.popBackStack() },
            )
        }

        composable<Route.PlayerDetail> { backStackEntry ->
            PlayerDetailScreen(
                playerId = backStackEntry.toRoute<Route.PlayerDetail>().playerId,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(Route.PlayerEdit(it)) },
            )
        }

        composable<Route.TakeAttendance> { backStackEntry ->
            TakeAttendanceScreen(
                sessionId = backStackEntry.toRoute<Route.TakeAttendance>().sessionId,
                onBack = { navController.popBackStack() },
            )
        }

        composable<Route.SessionHistory> {
            SessionHistoryScreen(
                onBack = { navController.popBackStack() },
                onOpenSession = { navController.navigate(Route.TakeAttendance(it)) },
            )
        }

        composable<Route.AbsenceReport> {
            AbsenceReportScreen(
                onBack = { navController.popBackStack() },
                onOpenPlayer = { navController.navigate(Route.PlayerDetail(it)) },
            )
        }

        composable<Route.Settings> {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
