package com.imagepuzzles.app.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.puzzleimagegame.ui.screens.playscreens.PlayScreen
import com.github.skgmn.composetooltip.sample.ExamplePopup
import com.imagepuzzles.app.navigation.Screen

@Composable
fun App() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Splash.route) {
        composable(Screen.Splash.route) {
            SplashScreen(navController)
        }
        composable(Screen.Home.route) {
            HomeScreen(navController)
        }
        composable(Screen.Game.route) {
            ExamplePopup()
        }

    }
}