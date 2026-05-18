package at.aau.se2.skyjo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import at.aau.se2.skyjo.ui.navigation.AppNavHost
import at.aau.se2.skyjo.ui.theme.SkyjoTheme
import at.aau.se2.skyjo.viewmodel.AuthViewModel
import at.aau.se2.skyjo.viewmodel.FriendsViewModel
import at.aau.se2.skyjo.viewmodel.GameViewModel
import at.aau.se2.skyjo.viewmodel.LeaderboardViewModel
import androidx.activity.viewModels

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()
    private val gameViewModel: GameViewModel by viewModels()
    private val friendsViewModel: FriendsViewModel by viewModels()
    private val leaderboardViewModel: LeaderboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SkyjoTheme {
                val navController = rememberNavController()
                AppNavHost(
                    navController = navController,
                    authViewModel = authViewModel,
                    gameViewModel = gameViewModel,
                    friendsViewModel = friendsViewModel,
                    leaderboardViewModel = leaderboardViewModel,
                )
            }
        }
    }
}
