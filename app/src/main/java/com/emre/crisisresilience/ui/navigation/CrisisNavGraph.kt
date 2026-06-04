package com.emre.crisisresilience.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.emre.crisisresilience.ui.screens.chat.ChatScreen
import com.emre.crisisresilience.ui.screens.home.HomeScreen

// Güvenli tip (Type-safe) navigasyon için rotalar (KSP ile kotlinx-serialization da kullanılabilirdi, şimdilik basit Enum/Sealed class veya String kullanıyoruz)
object Routes {
    const val HOME = "home"
    const val CHAT = "chat"
}

@Composable
fun CrisisNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(navController = navController, startDestination = Routes.HOME) {
        
        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToChat = {
                    // Chat ekranına yönlendir
                    navController.navigate(Routes.CHAT) {
                        // Aynı sayfanın üst üste binmesini (stack) engellemek için
                        popUpTo(Routes.HOME) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }
        
        composable(Routes.CHAT) {
            ChatScreen()
        }
    }
}
