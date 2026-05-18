package at.aau.se2.skyjo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import at.aau.se2.skyjo.model.social.FriendDto
import at.aau.se2.skyjo.model.social.FriendRequestDto
import at.aau.se2.skyjo.model.social.LobbyInviteDto
import at.aau.se2.skyjo.model.social.SocialUserDto
import at.aau.se2.skyjo.network.SkyjoApi
import at.aau.se2.skyjo.network.SkyjoApiClient
import at.aau.se2.skyjo.session.EncryptedSessionStore
import at.aau.se2.skyjo.session.SessionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FriendsUiState(
    val friends: List<FriendDto> = emptyList(),
    val incomingRequests: List<FriendRequestDto> = emptyList(),
    val outgoingRequests: List<FriendRequestDto> = emptyList(),
    val lobbyInvites: List<LobbyInviteDto> = emptyList(),
    val searchResults: List<SocialUserDto> = emptyList(),
    val query: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

class FriendsViewModel(
    application: Application,
    private val apiClient: SkyjoApi,
) : AndroidViewModel(application) {

    constructor(application: Application) : this(application, EncryptedSessionStore(application))

    private constructor(
        application: Application,
        sessionStore: SessionStore,
    ) : this(application, SkyjoApiClient(sessionStore))

    private val _state = MutableStateFlow(FriendsUiState())
    val state: StateFlow<FriendsUiState> = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                val friends = apiClient.friends()
                val requests = apiClient.friendRequests()
                val invites = apiClient.lobbyInvites()
                _state.update {
                    it.copy(
                        friends = friends,
                        incomingRequests = requests.incoming,
                        outgoingRequests = requests.outgoing,
                        lobbyInvites = invites,
                        isLoading = false,
                    )
                }
            }.onFailure { error ->
                _state.update { it.copy(isLoading = false, errorMessage = error.message) }
            }
        }
    }

    fun updateSearch(query: String) {
        _state.update { it.copy(query = query) }
        viewModelScope.launch {
            if (query.trim().length < 2) {
                _state.update { it.copy(searchResults = emptyList()) }
                return@launch
            }
            runCatching { apiClient.searchUsers(query) }
                .onSuccess { results -> _state.update { it.copy(searchResults = results) } }
        }
    }

    fun sendFriendRequest(userId: String) {
        viewModelScope.launch {
            runCatching { apiClient.sendFriendRequest(userId) }
                .onSuccess { refresh() }
                .onFailure { error -> _state.update { it.copy(errorMessage = error.message) } }
        }
    }

    fun acceptRequest(requestId: String) {
        viewModelScope.launch {
            runCatching { apiClient.acceptFriendRequest(requestId) }
                .onSuccess { refresh() }
                .onFailure { error -> _state.update { it.copy(errorMessage = error.message) } }
        }
    }

    fun declineRequest(requestId: String) {
        viewModelScope.launch {
            runCatching { apiClient.declineFriendRequest(requestId) }
                .onSuccess { refresh() }
                .onFailure { error -> _state.update { it.copy(errorMessage = error.message) } }
        }
    }

    fun inviteFriend(lobbyId: String?, friendUserId: String) {
        val activeLobbyId = lobbyId ?: return
        viewModelScope.launch {
            runCatching { apiClient.sendLobbyInvite(activeLobbyId, friendUserId) }
                .onFailure { error -> _state.update { it.copy(errorMessage = error.message) } }
        }
    }

    fun addLobbyInvite(invite: LobbyInviteDto) {
        _state.update { state ->
            state.copy(lobbyInvites = (state.lobbyInvites.filterNot { it.inviteId == invite.inviteId } + invite))
        }
    }

    fun removeLobbyInvite(inviteId: String) {
        _state.update { state ->
            state.copy(lobbyInvites = state.lobbyInvites.filterNot { it.inviteId == inviteId })
        }
    }

    fun declineLobbyInvite(inviteId: String) {
        viewModelScope.launch {
            runCatching { apiClient.declineLobbyInvite(inviteId) }
                .onSuccess { removeLobbyInvite(inviteId) }
                .onFailure { error -> _state.update { it.copy(errorMessage = error.message) } }
        }
    }
}
