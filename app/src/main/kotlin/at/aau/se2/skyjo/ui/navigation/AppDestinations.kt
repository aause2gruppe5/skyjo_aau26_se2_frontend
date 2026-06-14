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
        // Built on each call rather than cached in a companion `val`: the nested objects and this
        // companion form an initialization cycle, so an eagerly-initialized list can capture a
        // not-yet-initialized object as `null`. Deferring to call time guarantees fully-built objects.
        fun fromRoute(route: String?): AppDestination? = listOf(
            Auth, Start, Lobby, Game, Friends, Leaderboard, Settings, Rules, GameOver,
        ).find { it.route == route }
    }
}
