package at.aau.se2.skyjo.viewmodel

import android.app.Application
import at.aau.se2.skyjo.model.stats.LeaderboardEntryDto
import at.aau.se2.skyjo.network.ApiException
import at.aau.se2.skyjo.network.SkyjoApi
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

class LeaderboardViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val application = mockk<Application>(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `refresh loads leaderboard entries`() {
        val entries = listOf(LeaderboardEntryDto(1, "user-1", "Alice", 5.5, 3, 4, 2, 22))
        val viewModel = LeaderboardViewModel(application, FakeLeaderboardApi(entries = entries))

        viewModel.refresh()

        assertEquals(entries, viewModel.state.value.entries)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `refresh failure stores error`() {
        val viewModel = LeaderboardViewModel(application, FakeLeaderboardApi(error = ApiException("down")))

        viewModel.refresh()

        assertEquals("down", viewModel.state.value.errorMessage)
        assertFalse(viewModel.state.value.isLoading)
    }

    private class FakeLeaderboardApi(
        private val entries: List<LeaderboardEntryDto> = emptyList(),
        private val error: Throwable? = null,
    ) : SkyjoApi {
        override suspend fun leaderboard(limit: Int): List<LeaderboardEntryDto> {
            error?.let { throw it }
            return entries
        }
    }
}
