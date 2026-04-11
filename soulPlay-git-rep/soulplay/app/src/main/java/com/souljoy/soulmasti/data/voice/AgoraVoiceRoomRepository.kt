package com.souljoy.soulmasti.data.voice

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.souljoy.soulmasti.BuildConfig
import com.souljoy.soulmasti.domain.model.VoiceConnectionState
import com.souljoy.soulmasti.domain.repository.VoiceRoomRepository
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.LinkedHashSet

class AgoraVoiceRoomRepository(
    context: Context
) : VoiceRoomRepository {

    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private var rtcEngine: RtcEngine? = null

    private val uidSet = LinkedHashSet<Int>()

    private val _participants = MutableStateFlow<List<Int>>(emptyList())
    override val participants: StateFlow<List<Int>> = _participants.asStateFlow()

    private val _connectionState = MutableStateFlow<VoiceConnectionState>(VoiceConnectionState.Idle)
    override val connectionState: StateFlow<VoiceConnectionState> = _connectionState.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    override val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _audioLevelsByUid = MutableStateFlow<Map<Int, Float>>(emptyMap())
    override val audioLevelsByUid: StateFlow<Map<Int, Float>> = _audioLevelsByUid.asStateFlow()

    private var localUid: Int? = null

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block()
        else main.post(block)
    }

    private val rtcEventHandler = object : IRtcEngineEventHandler() {
        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            runOnMain {
                localUid = uid
                uidSet.add(uid)
                _participants.value = uidSet.toList()
                _connectionState.value = VoiceConnectionState.InRoom(channel.orEmpty())
            }
            // Re-apply after join so remote speakers are included in volume callbacks (see Agora docs).
            rtcEngine?.muteAllRemoteAudioStreams(false)
            rtcEngine?.enableAudioVolumeIndication(200, 10, true)
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            runOnMain {
                uidSet.add(uid)
                _participants.value = uidSet.toList()
            }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            runOnMain {
                uidSet.remove(uid)
                _participants.value = uidSet.toList()
            }
        }

        override fun onError(err: Int) {
            runOnMain {
                _connectionState.value = VoiceConnectionState.Error("Agora error $err")
            }
        }

        override fun onAudioVolumeIndication(speakers: Array<out AudioVolumeInfo>?, totalVolume: Int) {
            if (speakers == null) return
            runOnMain {
                val prev = _audioLevelsByUid.value
                val next = HashMap<Int, Float>()
                for ((k, v) in prev) {
                    if (isLocalUidMuted(k)) continue
                    val decayed = v * LEVEL_DECAY
                    if (decayed > MIN_LEVEL_TO_KEEP) next[k] = decayed
                }
                for (info in speakers) {
                    val uidKey = resolveSpeakingUid(info.uid) ?: continue
                    if (isLocalUidMuted(uidKey)) continue
                    val raw = (info.volume / 255f).coerceIn(0f, 1f)
                    val hasVoice = info.volume > 0 || info.vad != 0
                    val boosted = when {
                        !hasVoice -> 0f
                        info.vad != 0 && info.volume > 0 -> maxOf(raw, REMOTE_SPEAK_FLOOR)
                        info.volume > 0 -> maxOf(raw, 0.04f)
                        info.vad != 0 -> REMOTE_SPEAK_FLOOR
                        else -> 0f
                    }
                    if (boosted > 0f) {
                        next[uidKey] = maxOf(next[uidKey] ?: 0f, boosted)
                    }
                }
                stripLocalLevelIfMuted(next)
                _audioLevelsByUid.value = next
            }
        }

        /**
         * Fires for whoever Agora considers the active speaker (local or remote).
         * Remote volume arrays are sometimes sparse; this keeps other users' waves visible.
         */
        override fun onActiveSpeaker(uid: Int) {
            runOnMain {
                val uidKey = resolveSpeakingUid(uid) ?: return@runOnMain
                if (isLocalUidMuted(uidKey)) return@runOnMain
                val merged = _audioLevelsByUid.value.toMutableMap()
                merged[uidKey] = maxOf(merged[uidKey] ?: 0f, ACTIVE_SPEAKER_BOOST)
                stripLocalLevelIfMuted(merged)
                _audioLevelsByUid.value = merged
            }
        }
    }

    private fun resolveSpeakingUid(agoraUid: Int): Int? {
        val uidKey = when {
            agoraUid == 0 && localUid != null -> localUid!!
            else -> agoraUid
        }
        return if (uidKey == 0) null else uidKey
    }

    /** Mic is muted: Agora may still report local volume/VAD — never show waves for self. */
    private fun isLocalUidMuted(uid: Int): Boolean =
        _isMuted.value && localUid != null && uid == localUid

    private fun stripLocalLevelIfMuted(levels: MutableMap<Int, Float>) {
        if (_isMuted.value) localUid?.let { levels.remove(it) }
    }

    companion object {
        private const val LEVEL_DECAY = 0.88f
        private const val MIN_LEVEL_TO_KEEP = 0.02f
        private const val REMOTE_SPEAK_FLOOR = 0.14f
        private const val ACTIVE_SPEAKER_BOOST = 0.42f
    }

    override fun join(channelName: String, token: String?, uid: Int) {
        if (BuildConfig.AGORA_APP_ID.isBlank()) {
            _connectionState.value = VoiceConnectionState.Error("Set AGORA_APP_ID in local.properties")
            return
        }
        _connectionState.value = VoiceConnectionState.Connecting
        try {
            ensureEngineCreated()
            val options = ChannelMediaOptions().apply {
                clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
                channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
                publishMicrophoneTrack = true
                autoSubscribeAudio = true
            }
            rtcEngine?.joinChannel(token, channelName, uid, options)
        } catch (e: Exception) {
            _connectionState.value = VoiceConnectionState.Error(e.message ?: "Join failed")
        }
    }

    private fun ensureEngineCreated() {
        if (rtcEngine != null) return
        val config = RtcEngineConfig().apply {
            mContext = appContext
            mAppId = BuildConfig.AGORA_APP_ID
            mEventHandler = rtcEventHandler
        }
        rtcEngine = RtcEngine.create(config)
        rtcEngine?.setAudioProfile(Constants.AUDIO_PROFILE_SPEECH_STANDARD)
        rtcEngine?.setAudioScenario(Constants.AUDIO_SCENARIO_CHATROOM)
        rtcEngine?.enableAudio()
        rtcEngine?.enableLocalAudio(true)
        rtcEngine?.muteLocalAudioStream(false)
        rtcEngine?.muteAllRemoteAudioStreams(false)
        rtcEngine?.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION)
        rtcEngine?.setClientRole(Constants.CLIENT_ROLE_BROADCASTER)
        rtcEngine?.enableAudioVolumeIndication(200, 10, true)
    }

    override fun leave() {
        rtcEngine?.leaveChannel()
        runOnMain {
            localUid = null
            _audioLevelsByUid.value = emptyMap()
            uidSet.clear()
            _participants.value = emptyList()
            _connectionState.value = VoiceConnectionState.Idle
            _isMuted.value = false
            rtcEngine?.let {
                RtcEngine.destroy()
                rtcEngine = null
            }
        }
    }

    override fun toggleMute() {
        val newMuted = !_isMuted.value
        _isMuted.value = newMuted
        rtcEngine?.muteLocalAudioStream(newMuted)
        if (newMuted) {
            val local = localUid ?: return
            val cleared = _audioLevelsByUid.value.toMutableMap()
            cleared.remove(local)
            _audioLevelsByUid.value = cleared
        }
    }

    override fun release() {
        rtcEngine?.leaveChannel()
        runOnMain {
            localUid = null
            _audioLevelsByUid.value = emptyMap()
            uidSet.clear()
            _participants.value = emptyList()
            _connectionState.value = VoiceConnectionState.Idle
            _isMuted.value = false
        }
        rtcEngine?.let {
            RtcEngine.destroy()
            rtcEngine = null
        }
    }
}
