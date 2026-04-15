package com.souljoy.soulmasti.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.souljoy.soulmasti.data.firebase.FirebaseUidMapping
import com.souljoy.soulmasti.domain.gift.GiftCatalog
import com.souljoy.soulmasti.domain.gift.GiftSendContext
import com.souljoy.soulmasti.domain.model.ChatMessage
import com.souljoy.soulmasti.domain.repository.GiftRepository
import com.souljoy.soulmasti.domain.repository.SocialRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ChatThreadViewModel(
    application: Application,
    private val social: SocialRepository,
    private val gifts: GiftRepository,
    private val database: FirebaseDatabase,
    private val auth: FirebaseAuth,
    val peerUid: String,
) : AndroidViewModel(application) {

    val myUid: String? get() = auth.currentUser?.uid

    val messages: StateFlow<List<ChatMessage>> =
        social.observeChatMessages(peerUid).stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList(),
        )

    private val _peerDisplayName = MutableStateFlow(FirebaseUidMapping.shortLabel(peerUid))
    val peerDisplayName: StateFlow<String> = _peerDisplayName.asStateFlow()

    private val _peerPhotoUrl = MutableStateFlow<String?>(null)
    val peerPhotoUrl: StateFlow<String?> = _peerPhotoUrl.asStateFlow()

    private val _sendError = MutableStateFlow<String?>(null)
    val sendError: StateFlow<String?> = _sendError.asStateFlow()
    private val _myCoins = MutableStateFlow<Long?>(null)
    val myCoins: StateFlow<Long?> = _myCoins.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching {
                val snap = database.reference.child("users").child(peerUid).get().await()
                val name = snap.child("username").getValue(String::class.java)?.trim().orEmpty()
                val photo = snap.child("profilePictureUrl").getValue(String::class.java)?.trim().orEmpty()
                if (name.isNotBlank()) _peerDisplayName.value = name
                _peerPhotoUrl.value = photo.takeIf { it.isNotBlank() }
            }
        }
        viewModelScope.launch {
            social.markChatAsRead(peerUid)
        }
        viewModelScope.launch {
            val uid = auth.currentUser?.uid ?: return@launch
            val snap = runCatching { database.reference.child("users").child(uid).child("totalWinnings").get().await() }.getOrNull()
            _myCoins.value = parseLong(snap?.value) ?: 0L
        }
    }

    fun dismissSendError() {
        _sendError.value = null
    }

    fun send(text: String) {
        viewModelScope.launch {
            social.sendChatMessage(peerUid, text).onFailure { e ->
                _sendError.value = e.message ?: "Could not send"
            }
        }
    }

    suspend fun sendGift(giftId: String, selectedCount: Int): Result<Unit> {
        val me = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val chatId = directChatId(me, peerUid)
        val giftLabel = GiftCatalog.displayLabel(giftId)
        val safeSelectedCount = selectedCount.coerceAtLeast(1)
        val giftResult = gifts.sendGift(
            context = GiftSendContext.Chat(chatId),
            giftId = giftId,
            recipientUserId = peerUid,
            selectedCount = safeSelectedCount,
        )
        if (giftResult.isFailure) return Result.failure(giftResult.exceptionOrNull() ?: IllegalStateException("Gift failed"))

        val receiverCoins = giftResult.getOrNull()?.receiverCoins ?: 0L
        val receiverSoul = giftResult.getOrNull()?.receiverSoul ?: 0L
        val text = buildGiftMessageText(
            giftId = giftId,
            giftLabel = giftLabel,
            giftCoins = (GiftCatalog.priceCoinsOrNull(giftId) ?: 0L) * safeSelectedCount.toLong(),
            receiverCoins = receiverCoins,
            receiverSoul = receiverSoul,
            selectedCount = safeSelectedCount,
        )
        val msgResult = social.sendChatMessage(peerUid, text)
        if (msgResult.isFailure) {
            return Result.failure(msgResult.exceptionOrNull() ?: IllegalStateException("Could not write gift message"))
        }
        return Result.success(Unit)
    }
}

private fun parseLong(value: Any?): Long? {
    if (value == null) return null
    if (value is String) return value.trim().toLongOrNull()
    if (value is Long) return value
    if (value is Int) return value.toLong()
    if (value is Double) return value.toLong()
    if (value is Float) return value.toLong()
    if (value is Number) return value.toLong()
    return null
}

private fun directChatId(a: String, b: String): String {
    val sorted = listOf(a, b).sorted()
    return "${sorted[0]}_${sorted[1]}"
}

private fun buildGiftMessageText(
    giftId: String,
    giftLabel: String,
    giftCoins: Long,
    receiverCoins: Long,
    receiverSoul: Long,
    selectedCount: Int,
): String = "GIFT|$giftId|$giftLabel|$giftCoins|$receiverCoins|$receiverSoul|$selectedCount"
