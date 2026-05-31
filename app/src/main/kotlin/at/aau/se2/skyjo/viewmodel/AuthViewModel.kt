package at.aau.se2.skyjo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import at.aau.se2.skyjo.model.auth.AuthUserDto
import at.aau.se2.skyjo.network.SkyjoApi
import at.aau.se2.skyjo.network.SkyjoApiClient
import at.aau.se2.skyjo.session.EncryptedSessionStore
import at.aau.se2.skyjo.session.SessionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val isCheckingSession: Boolean = true,
    val isAuthenticated: Boolean = false,
    val isRegisterMode: Boolean = false,
    val username: String = "",
    val password: String = "",
    val user: AuthUserDto? = null,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
)

class AuthViewModel(
    application: Application,
    private val sessionStore: SessionStore,
    private val apiClient: SkyjoApi,
) : AndroidViewModel(application) {

    constructor(application: Application) : this(application, EncryptedSessionStore(application))

    private constructor(
        application: Application,
        sessionStore: SessionStore,
    ) : this(application, sessionStore, SkyjoApiClient(sessionStore))

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    init {
        checkExistingSession()
    }

    fun updateUsername(value: String) {
        _state.update { it.copy(username = value, errorMessage = null) }
    }

    fun updatePassword(value: String) {
        _state.update { it.copy(password = value, errorMessage = null) }
    }

    fun toggleMode() {
        _state.update {
            it.copy(
                isRegisterMode = !it.isRegisterMode,
                password = "",
                errorMessage = null,
            )
        }
    }

    fun submit() {
        val snapshot = _state.value
        val username = snapshot.username.trim()
        val password = snapshot.password
        if (!USERNAME_PATTERN.matches(username)) {
            _state.update { it.copy(errorMessage = "Username: 3-20 characters, letters, numbers, and _ only") }
            return
        }
        if (password.length < 8) {
            _state.update { it.copy(errorMessage = "Password must be at least 8 characters long") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, errorMessage = null) }
            runCatching {
                if (snapshot.isRegisterMode) {
                    apiClient.register(username, password)
                } else {
                    apiClient.login(username, password)
                }
            }.onSuccess { response ->
                sessionStore.saveToken(response.token)
                _state.update {
                    it.copy(
                        isAuthenticated = true,
                        user = response.user,
                        password = "",
                        isSubmitting = false,
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = error.readableMessage("Login failed"),
                    )
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            runCatching { apiClient.logout() }
            sessionStore.clearToken()
            _state.value = AuthUiState(isCheckingSession = false)
        }
    }

    private fun checkExistingSession() {
        viewModelScope.launch {
            val token = sessionStore.getToken()
            if (token.isNullOrBlank()) {
                _state.update { it.copy(isCheckingSession = false) }
                return@launch
            }

            runCatching { apiClient.me() }
                .onSuccess { user ->
                    _state.update {
                        it.copy(
                            isCheckingSession = false,
                            isAuthenticated = true,
                            user = user,
                            username = user.username,
                        )
                    }
                }
                .onFailure {
                    sessionStore.clearToken()
                    _state.update { state -> state.copy(isCheckingSession = false, isAuthenticated = false) }
                }
        }
    }

    private fun Throwable.readableMessage(fallback: String): String = message ?: fallback

    private companion object {
        val USERNAME_PATTERN = Regex("^[A-Za-z0-9_]{3,20}$")
    }
}
