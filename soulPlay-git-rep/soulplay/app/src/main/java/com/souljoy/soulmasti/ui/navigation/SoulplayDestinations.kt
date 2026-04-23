package com.souljoy.soulmasti.ui.navigation

object SoulplayDestinations {
    const val AuthGate = "auth_gate"
    const val Login = "login"
    const val CreateProfile = "create_profile"

    const val Home = "home"
    const val GoldShop = "gold_shop"
    const val VoiceRoom = "voice_room"
    const val VoiceRoomWithId = "voice_room/{roomId}"
    const val SocialVoiceHub = "social_voice_hub"
    const val SocialVoiceRoomWithId = "social_voice_room/{roomId}"

    fun voiceRoom(roomId: String) = "voice_room/$roomId"
    fun socialVoiceRoom(roomId: String) = "social_voice_room/$roomId"

    const val Settings = "settings"
    const val DailyRewards = "daily_rewards"
    const val League = "league"
    const val RewardInbox = "reward_inbox"
    const val UserProfile = "user_profile/{uid}"

    const val Chat = "chat"
    const val ChatThread = "chat_thread/{peerUid}"

    fun chatThread(peerUid: String) = "chat_thread/$peerUid"
    fun userProfile(uid: String) = "user_profile/$uid"
}
