package com.souljoy.soulmasti.domain.model

data class SocialVoiceSeat(
    val seatNo: Int,
    val occupantUid: String?,
    val occupantName: String?,
    val locked: Boolean,
    val muted: Boolean,
    val role: SeatRole = SeatRole.GUEST,
)

enum class SeatRole {
    OWNER,
    ADMIN,
    GUEST,
}

data class SocialVoiceRoomSnapshot(
    val roomId: String,
    val roomName: String,
    val ownerUid: String,
    val ownerOnline: Boolean,
    val collapsed: Boolean,
    val adminUids: Set<String>,
    val onlineUids: Set<String>,
    val seats: List<SocialVoiceSeat>,
    val onlineCount: Int,
)

data class SocialVoiceChatMessage(
    val id: String,
    val fromUid: String,
    val fromName: String,
    val text: String,
    val createdAt: Long?,
)

data class VoiceEntitlementState(
    val hasPermanentRoom: Boolean,
    val vipExpiresAt: Long?,
) {
    val vipActive: Boolean
        get() = (vipExpiresAt ?: 0L) > System.currentTimeMillis()
}

data class SocialSeatInvite(
    val inviteId: String,
    val roomId: String,
    val fromUid: String,
    val toUid: String,
    val seatNo: Int,
    val status: String,
    val createdAt: Long?,
)

