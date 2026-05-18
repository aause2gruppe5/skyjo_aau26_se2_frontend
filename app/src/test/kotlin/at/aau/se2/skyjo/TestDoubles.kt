package at.aau.se2.skyjo

import at.aau.se2.skyjo.session.SessionStore

class InMemorySessionStore(initialToken: String? = null) : SessionStore {
    var storedToken: String? = initialToken
    var clearCount = 0

    override fun saveToken(token: String) {
        this.storedToken = token
    }

    override fun getToken(): String? = storedToken

    override fun clearToken() {
        storedToken = null
        clearCount++
    }
}
