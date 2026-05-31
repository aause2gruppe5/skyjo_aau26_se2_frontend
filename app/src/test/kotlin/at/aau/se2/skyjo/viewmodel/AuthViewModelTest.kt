package at.aau.se2.skyjo.viewmodel

import android.app.Application
import at.aau.se2.skyjo.InMemorySessionStore
import at.aau.se2.skyjo.model.auth.AuthResponse
import at.aau.se2.skyjo.model.auth.AuthUserDto
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthViewModelTest {

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
    fun `missing existing token finishes unauthenticated session check`() {
        val store = InMemorySessionStore()
        val viewModel = AuthViewModel(application, store, FakeAuthApi())

        assertFalse(viewModel.state.value.isCheckingSession)
        assertFalse(viewModel.state.value.isAuthenticated)
    }

    @Test
    fun `valid existing token authenticates user`() {
        val store = InMemorySessionStore("saved-token")
        val api = FakeAuthApi(meUser = AuthUserDto("user-1", "Alice"))
        val viewModel = AuthViewModel(application, store, api)

        assertTrue(viewModel.state.value.isAuthenticated)
        assertEquals("Alice", viewModel.state.value.username)
        assertEquals("user-1", viewModel.state.value.user?.userId)
    }

    @Test
    fun `invalid existing token clears local session`() {
        val store = InMemorySessionStore("bad-token")
        val viewModel = AuthViewModel(application, store, FakeAuthApi(meError = ApiException("expired", 401)))

        assertFalse(viewModel.state.value.isAuthenticated)
        assertNull(store.storedToken)
        assertEquals(1, store.clearCount)
    }

    @Test
    fun `invalid username is rejected before calling API`() {
        val api = FakeAuthApi()
        val viewModel = AuthViewModel(application, InMemorySessionStore(), api)

        viewModel.updateUsername("x")
        viewModel.updatePassword("SecurePass123")
        viewModel.submit()

        assertEquals(0, api.loginCalls)
        assertTrue(viewModel.state.value.errorMessage.orEmpty().contains("Username"))
    }

    @Test
    fun `short password is rejected before calling API`() {
        val api = FakeAuthApi()
        val viewModel = AuthViewModel(application, InMemorySessionStore(), api)

        viewModel.updateUsername("Alice_1")
        viewModel.updatePassword("short")
        viewModel.submit()

        assertEquals(0, api.loginCalls)
        assertTrue(viewModel.state.value.errorMessage.orEmpty().contains("Password"))
    }

    @Test
    fun `login success stores token and authenticated user`() {
        val store = InMemorySessionStore()
        val api = FakeAuthApi(authResponse = AuthResponse("token-1", AuthUserDto("user-1", "Alice")))
        val viewModel = AuthViewModel(application, store, api)

        viewModel.updateUsername(" Alice ")
        viewModel.updatePassword("SecurePass123")
        viewModel.submit()

        assertEquals(1, api.loginCalls)
        assertEquals("Alice", api.lastUsername)
        assertEquals("token-1", store.storedToken)
        assertTrue(viewModel.state.value.isAuthenticated)
        assertEquals("", viewModel.state.value.password)
    }

    @Test
    fun `register mode calls register endpoint`() {
        val api = FakeAuthApi(authResponse = AuthResponse("token-2", AuthUserDto("user-2", "Bob")))
        val viewModel = AuthViewModel(application, InMemorySessionStore(), api)

        viewModel.toggleMode()
        viewModel.updateUsername("Bob_1")
        viewModel.updatePassword("SecurePass123")
        viewModel.submit()

        assertEquals(1, api.registerCalls)
        assertEquals(0, api.loginCalls)
        assertTrue(viewModel.state.value.isAuthenticated)
    }

    @Test
    fun `login failure exposes readable API message`() {
        val api = FakeAuthApi(loginError = ApiException("Invalid username or password", 401))
        val viewModel = AuthViewModel(application, InMemorySessionStore(), api)

        viewModel.updateUsername("Alice")
        viewModel.updatePassword("SecurePass123")
        viewModel.submit()

        assertFalse(viewModel.state.value.isSubmitting)
        assertEquals("Invalid username or password", viewModel.state.value.errorMessage)
    }

    @Test
    fun `toggle mode clears password and update clears error`() {
        val viewModel = AuthViewModel(application, InMemorySessionStore(), FakeAuthApi())

        viewModel.updateUsername("x")
        viewModel.updatePassword("bad")
        viewModel.submit()
        assertTrue(viewModel.state.value.errorMessage != null)

        viewModel.updateUsername("Alice")
        assertNull(viewModel.state.value.errorMessage)
        viewModel.updatePassword("SecurePass123")
        viewModel.toggleMode()

        assertTrue(viewModel.state.value.isRegisterMode)
        assertEquals("", viewModel.state.value.password)
    }

    @Test
    fun `logout clears token even when API logout fails`() {
        val store = InMemorySessionStore("token")
        val api = FakeAuthApi(logoutError = ApiException("gone", 401))
        val viewModel = AuthViewModel(application, store, api)

        viewModel.logout()

        assertNull(store.storedToken)
        assertFalse(viewModel.state.value.isAuthenticated)
        assertFalse(viewModel.state.value.isCheckingSession)
    }

    private class FakeAuthApi(
        private val meUser: AuthUserDto = AuthUserDto("user", "Existing"),
        private val meError: Throwable? = null,
        private val authResponse: AuthResponse = AuthResponse("token", AuthUserDto("user", "Alice")),
        private val loginError: Throwable? = null,
        private val logoutError: Throwable? = null,
    ) : SkyjoApi {
        var loginCalls = 0
        var registerCalls = 0
        var lastUsername: String? = null

        override suspend fun me(): AuthUserDto {
            meError?.let { throw it }
            return meUser
        }

        override suspend fun login(username: String, password: String): AuthResponse {
            loginCalls++
            lastUsername = username
            loginError?.let { throw it }
            return authResponse
        }

        override suspend fun register(username: String, password: String): AuthResponse {
            registerCalls++
            lastUsername = username
            return authResponse
        }

        override suspend fun logout() {
            logoutError?.let { throw it }
        }
    }
}
