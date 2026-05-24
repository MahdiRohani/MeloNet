package com.example.melonet.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute

@Composable
fun MelonetMainScreen() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { MelonetBottomNavigation(navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = SplashRoute,
            modifier = Modifier.padding(innerPadding)
        ) {


            composable<SplashRoute> {
                SplashScreen(onSplashFinished = {
                    navController.navigate(HomeRoute) {
                        popUpTo(SplashRoute) { inclusive = true }
                    }
                })
            }


            composable<HomeRoute> { DummyScreen("تب خانه") }
            composable<SearchRoute> { DummyScreen("تب جستجو") }
            composable<DownloadsRoute> { DummyScreen("تب دانلودها") }
            composable<PlaylistsRoute> { DummyScreen("تب پلی‌لیست‌ها") }
            composable<ProfileRoute> { DummyScreen("تب پروفایل من (Harmoniq Me)") }


            composable<PlayerRoute> { backStackEntry ->
                val args = backStackEntry.toRoute<PlayerRoute>()
                DummyScreen("صفحه پلیر - شناسه آهنگ: ${args.songId}")
            }

            composable<ChatRoute> { backStackEntry ->
                val args = backStackEntry.toRoute<ChatRoute>()
                DummyScreen("صفحه چت - شناسه کاربر: ${args.userId}")
            }
        }
    }
}


@Composable
fun DummyScreen(title: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = title)
    }
}