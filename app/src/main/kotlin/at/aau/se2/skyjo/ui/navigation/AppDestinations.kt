package at.aau.se2.skyjo.ui.navigation

sealed class AppDestination(val route: String) {
    object Auth : AppDestination("auth")
    object Start : AppDestination("start")
    object Lobby : AppDestination("lobby")
    object Game : AppDestination("game")
    object Friends : AppDestination("friends")
    object Leaderboard : AppDestination("leaderboard")
    object Settings : AppDestination("settings")
    object Rules : AppDestination("rules")
    object GameOver : AppDestination("gameOver")

    companion object {
        private val all = listOf(
            Auth, Start, Lobby, Game, Friends, Leaderboard, Settings, Rules, GameOver,
        )

        fun fromRoute(route: String?): AppDestination? = all.find { it.route == route }
    }
}
