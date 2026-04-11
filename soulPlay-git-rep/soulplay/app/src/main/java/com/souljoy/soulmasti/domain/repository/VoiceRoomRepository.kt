package com.souljoy.soulmasti.domain.repository

import com.souljoy.soulmasti.domain.model.VoiceConnectionState
import kotlinx.coroutines.flow.StateFlow

interface VoiceRoomRepository {
    val participants: StateFlow<List<Int>>
    val connectionState: StateFlow<VoiceConnectionState>
    val isMuted: StateFlow<Boolean>
    /** Normalized 0–1 mic level per Agora UID (for speaking UI). */
    val audioLevelsByUid: StateFlow<Map<Int, Float>>

    fun join(channelName: String, token: String?, uid: Int)
    fun leave()
    fun toggleMute()
    fun release()
}
