package at.aau.se2.skyjo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import at.aau.se2.skyjo.ui.navigation.AppNavHost
import at.aau.se2.skyjo.ui.theme.SkyjoTheme
import at.aau.se2.skyjo.viewmodel.GameViewModel
import androidx.activity.viewModels

class MainActivity : ComponentActivity() {

    private val gameViewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SkyjoTheme {
                val navController = rememberNavController()
                AppNavHost(
                    navController = navController,
                    gameViewModel = gameViewModel
                )
            }
        }
    }
}
