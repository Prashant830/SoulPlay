package com.masti.soulplay.domain.model

sealed interface VoiceConnectionState {
    data object Idle : VoiceConnectionState
    data object Connecting : VoiceConnectionState
    data class InRoom(val channelId: String) : VoiceConnectionState
    data class Error(val message: String) : VoiceConnectionState
}
