package at.aau.se2.skyjo.ui.navigation

sealed class AppDestination(val route: String) {
    object Start : AppDestination("start")
    object Lobby : AppDestination("lobby")
    object Game : AppDestination("game")
    object Friends : AppDestination("friends")
    object Settings : AppDestination("settings")
}
