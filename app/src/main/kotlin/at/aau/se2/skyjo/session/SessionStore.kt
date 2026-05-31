package at.aau.se2.skyjo.session

interface SessionStore {
    fun saveToken(token: String)
    fun getToken(): String?
    fun clearToken()
}
