package at.aau.se2.skyjo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.compose.rememberNavController
import at.aau.se2.skyjo.audio.AudioController
import at.aau.se2.skyjo.audio.LocalAudio
import at.aau.se2.skyjo.haptic.HapticController
import at.aau.se2.skyjo.haptic.LocalHaptic
import at.aau.se2.skyjo.network.PresenceHeartbeat
import at.aau.se2.skyjo.network.SkyjoApiClient
import at.aau.se2.skyjo.session.EncryptedSessionStore
import at.aau.se2.skyjo.settings.SettingsRepository
import at.aau.se2.skyjo.ui.navigation.AppNavHost
import at.aau.se2.skyjo.ui.theme.SkyjoTheme
import at.aau.se2.skyjo.viewmodel.AuthViewModel
import at.aau.se2.skyjo.viewmodel.FriendsViewModel
import at.aau.se2.skyjo.viewmodel.GameViewModel
import at.aau.se2.skyjo.viewmodel.LeaderboardViewModel
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()
    private val gameViewModel: GameViewModel by viewModels()
    private val friendsViewModel: FriendsViewModel by viewModels()
    private val leaderboardViewModel: LeaderboardViewModel by viewModels()

    private lateinit var settings: SettingsRepository
    private lateinit var audioController: AudioController
    private lateinit var hapticController: HapticController
    private lateinit var presenceHeartbeat: PresenceHeartbeat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = SettingsRepository.getInstance(this)
        audioController = AudioController(this, settings)
        hapticController = HapticController(this, settings)
        presenceHeartbeat = PresenceHeartbeat(
            api = SkyjoApiClient(EncryptedSessionStore(this)),
            isAuthenticated = { authViewModel.state.value.isAuthenticated },
            beforeHeartbeat = { gameViewModel.ensureInviteSubscription() },
        )

        setContent {
            SkyjoTheme {
                CompositionLocalProvider(
                    LocalAudio provides audioController,
                    LocalHaptic provides hapticController,
                ) {
                    val navController = rememberNavController()
                    AppNavHost(
                        navController = navController,
                        authViewModel = authViewModel,
                        gameViewModel = gameViewModel,
                        friendsViewModel = friendsViewModel,
                        leaderboardViewModel = leaderboardViewModel,
                        settings = settings,
                        audioController = audioController,
                    )
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        audioController.pause()
        presenceHeartbeat.stop()
    }

    override fun onResume() {
        super.onResume()
        audioController.resume()
        presenceHeartbeat.start(lifecycleScope)
    }

    override fun onDestroy() {
        audioController.release()
        super.onDestroy()
    }
}
