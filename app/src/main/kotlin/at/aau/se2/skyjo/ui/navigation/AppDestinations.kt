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
}
