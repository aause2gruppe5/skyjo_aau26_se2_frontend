package at.aau.se2.skyjo.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppDestinationTest {

    @Test
    fun `auth destination has correct route`() {
        assertEquals("auth", AppDestination.Auth.route)
    }

    @Test
    fun `start destination has correct route`() {
        assertEquals("start", AppDestination.Start.route)
    }

    @Test
    fun `lobby destination has correct route`() {
        assertEquals("lobby", AppDestination.Lobby.route)
    }

    @Test
    fun `game destination has correct route`() {
        assertEquals("game", AppDestination.Game.route)
    }

    @Test
    fun `friends destination has correct route`() {
        assertEquals("friends", AppDestination.Friends.route)
    }

    @Test
    fun `leaderboard destination has correct route`() {
        assertEquals("leaderboard", AppDestination.Leaderboard.route)
    }

    @Test
    fun `settings destination has correct route`() {
        assertEquals("settings", AppDestination.Settings.route)
    }

    @Test
    fun `rules destination has correct route`() {
        assertEquals("rules", AppDestination.Rules.route)
    }

    @Test
    fun `gameOver destination has correct route`(){
        assertEquals("gameOver", AppDestination.GameOver.route)
    }

    @Test
    fun `all routes are non-empty strings`() {
        val destinations = listOf(
            AppDestination.Auth,
            AppDestination.Start,
            AppDestination.Lobby,
            AppDestination.Game,
            AppDestination.Friends,
            AppDestination.Leaderboard,
            AppDestination.Settings,
            AppDestination.Rules,
            AppDestination.GameOver,
        )
        destinations.forEach { dest ->
            assertTrue("route should not be empty for ${dest::class.simpleName}", dest.route.isNotBlank())
        }
    }

    @Test
    fun `all routes are unique`() {
        val routes = listOf(
            AppDestination.Auth.route,
            AppDestination.Start.route,
            AppDestination.Lobby.route,
            AppDestination.Game.route,
            AppDestination.Friends.route,
            AppDestination.Leaderboard.route,
            AppDestination.Settings.route,
            AppDestination.Rules.route,
            AppDestination.GameOver.route,
        )
        assertEquals("routes must be unique", routes.distinct().size, routes.size)
    }

}
