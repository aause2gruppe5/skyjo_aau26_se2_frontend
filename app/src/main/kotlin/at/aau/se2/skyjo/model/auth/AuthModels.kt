package at.aau.se2.skyjo.model.auth

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(val username: String, val password: String)

@Serializable
data class LoginRequest(val username: String, val password: String)

@Serializable
data class AuthResponse(val token: String, val user: AuthUserDto)

@Serializable
data class AuthUserDto(val userId: String, val username: String)

@Serializable
data class WsTicketResponse(val ticket: String, val expiresAt: Long)

@Serializable
data class ErrorResponse(val message: String)
