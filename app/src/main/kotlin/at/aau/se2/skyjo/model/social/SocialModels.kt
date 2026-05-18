package at.aau.se2.skyjo.model.social

import kotlinx.serialization.Serializable

@Serializable
enum class FriendRequestStatus {
    PENDING,
    ACCEPTED,
    DECLINED,
}

@Serializable
enum class RelationshipStatus {
    NONE,
    FRIENDS,
    INCOMING_REQUEST,
    OUTGOING_REQUEST,
}

@Serializable
data class SocialUserDto(
    val userId: String,
    val username: String,
    val relationshipStatus: RelationshipStatus = RelationshipStatus.NONE,
)

@Serializable
data class FriendDto(
    val userId: String,
    val username: String,
    val online: Boolean,
    val currentLobbyId: String? = null,
)

@Serializable
data class FriendRequestDto(
    val requestId: String,
    val from: SocialUserDto,
    val to: SocialUserDto,
    val status: FriendRequestStatus,
    val createdAt: Long,
    val respondedAt: Long? = null,
)

@Serializable
data class FriendRequestsResponse(
    val incoming: List<FriendRequestDto>,
    val outgoing: List<FriendRequestDto>,
)

@Serializable
data class SendFriendRequestRequest(val toUserId: String)

@Serializable
enum class LobbyInviteStatus {
    PENDING,
    ACCEPTED,
    DECLINED,
}

@Serializable
data class LobbyInviteRequest(val toUserId: String)

@Serializable
data class LobbyInviteDto(
    val inviteId: String,
    val lobbyId: String,
    val joinCode: String,
    val from: SocialUserDto,
    val to: SocialUserDto,
    val status: LobbyInviteStatus,
    val createdAt: Long,
    val respondedAt: Long? = null,
)
