package com.senademirci.futbolyoklama

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.senademirci.futbolyoklama.ui.Route
import com.senademirci.futbolyoklama.ui.theme.FutbolYoklamaTheme
import com.senademirci.futbolyoklama.ui.PlaceholderScreen

@Composable
fun App() {
    FutbolYoklamaTheme {
        val navController = rememberNavController()

        NavHost(
            navController = navController,
            startDestination = Route.Roster,
        ) {
            composable<Route.Roster> {
                PlaceholderScreen(
                    title = "Kadro",
                    message = "Faz 1: iskelet ayakta. Öğrenci listesi Faz 3'te gelecek.",
                )
            }
        }
    }
}
