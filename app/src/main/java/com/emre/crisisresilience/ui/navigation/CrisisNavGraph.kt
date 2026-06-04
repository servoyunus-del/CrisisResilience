package com.emre.crisisresilience.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.emre.crisisresilience.ui.screens.chat.ChatScreen
import com.emre.crisisresilience.ui.screens.home.HomeScreen
import com.emre.crisisresilience.ui.screens.sos.SosScreen

// Güvenli tip (Type-safe) navigasyon için rotalar (KSP ile kotlinx-serialization da kullanılabilirdi, şimdilik basit Enum/Sealed class veya String kullanıyoruz)
object Routes {
    const val HOME = "home"
    const val CHAT = "chat"
    const val SOS = "sos"
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
                        popUpTo(Routes.HOME) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onNavigateToSos = {
                    navController.navigate(Routes.SOS)
                }
            )
        }
        
        composable(Routes.CHAT) {
            ChatScreen(navController = navController)
        }
        
        composable(Routes.SOS) {
            SosScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
