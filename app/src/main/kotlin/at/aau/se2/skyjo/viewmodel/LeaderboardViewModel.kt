package at.aau.se2.skyjo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import at.aau.se2.skyjo.model.stats.LeaderboardEntryDto
import at.aau.se2.skyjo.network.SkyjoApi
import at.aau.se2.skyjo.network.SkyjoApiClient
import at.aau.se2.skyjo.session.EncryptedSessionStore
import at.aau.se2.skyjo.session.SessionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LeaderboardUiState(
    val entries: List<LeaderboardEntryDto> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

class LeaderboardViewModel(
    application: Application,
    private val apiClient: SkyjoApi,
) : AndroidViewModel(application) {

    constructor(application: Application) : this(application, EncryptedSessionStore(application))

    private constructor(
        application: Application,
        sessionStore: SessionStore,
    ) : this(application, SkyjoApiClient(sessionStore))

    private val _state = MutableStateFlow(LeaderboardUiState())
    val state: StateFlow<LeaderboardUiState> = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching { apiClient.leaderboard() }
                .onSuccess { entries -> _state.update { it.copy(entries = entries, isLoading = false) } }
                .onFailure { error -> _state.update { it.copy(isLoading = false, errorMessage = error.message) } }
        }
    }
}
