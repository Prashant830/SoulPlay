package com.souljoy.soulmasti.ui.navigation

object SoulplayDestinations {
    const val AuthGate = "auth_gate"
    const val Login = "login"
    const val CreateProfile = "create_profile"

    const val Home = "home"
    const val GoldShop = "gold_shop"
    const val VoiceRoom = "voice_room"
    const val VoiceRoomWithId = "voice_room/{roomId}"

    fun voiceRoom(roomId: String) = "voice_room/$roomId"

    const val Settings = "settings"

    const val Chat = "chat"
    const val ChatThread = "chat_thread/{peerUid}"

    fun chatThread(peerUid: String) = "chat_thread/$peerUid"
}
