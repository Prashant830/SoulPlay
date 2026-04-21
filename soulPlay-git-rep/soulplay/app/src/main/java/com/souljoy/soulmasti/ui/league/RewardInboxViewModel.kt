package com.souljoy.soulmasti.ui.league

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class RewardInboxItem(
    val rewardId: String,
    val title: String,
    val periodType: String,
    val rank: Int,
    val sourceSoul: Long,
    val coinReward: Long,
    val soulReward: Long,
    val createdAt: Long,
    val read: Boolean,
)

class RewardInboxViewModel(
    application: Application,
    private val auth: FirebaseAuth,
    private val database: FirebaseDatabase,
) : AndroidViewModel(application) {

    private val _items = MutableStateFlow<List<RewardInboxItem>>(emptyList())
    val items: StateFlow<List<RewardInboxItem>> = _items.asStateFlow()

    private var listener: ValueEventListener? = null

    init {
        observeInbox()
    }

    private fun observeInbox() {
        val uid = auth.currentUser?.uid ?: return
        val ref = database.reference.child("users").child(uid).child("rewardInbox")
        val l = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                _items.value = snapshot.children.mapNotNull { child ->
                    val rewardId = child.key ?: return@mapNotNull null
                    RewardInboxItem(
                        rewardId = rewardId,
                        title = child.child("title").getValue(String::class.java).orEmpty().ifBlank { "Reward" },
                        periodType = child.child("periodType").getValue(String::class.java).orEmpty(),
                        rank = (child.child("rank").getValue(Long::class.java) ?: 0L).toInt(),
                        sourceSoul = child.child("sourceSoul").getValue(Long::class.java) ?: 0L,
                        coinReward = child.child("coinReward").getValue(Long::class.java) ?: 0L,
                        soulReward = child.child("soulReward").getValue(Long::class.java) ?: 0L,
                        createdAt = child.child("createdAt").getValue(Long::class.java) ?: 0L,
                        read = child.child("read").getValue(Boolean::class.java) == true,
                    )
                }.sortedByDescending { it.createdAt }
            }

            override fun onCancelled(error: DatabaseError) = Unit
        }
        ref.addValueEventListener(l)
        listener = l
    }

    fun markRead(rewardId: String) {
        val uid = auth.currentUser?.uid ?: return
        if (rewardId.isBlank()) return
        viewModelScope.launch {
            runCatching {
                database.reference.child("users").child(uid).child("rewardInbox").child(rewardId).child("read")
                    .setValue(true).await()
                database.reference.child("users").child(uid).child("judgeBotMessages").child(rewardId).child("read")
                    .setValue(true).await()
            }
        }
    }

    override fun onCleared() {
        val uid = auth.currentUser?.uid
        val l = listener
        if (uid != null && l != null) {
            database.reference.child("users").child(uid).child("rewardInbox").removeEventListener(l)
        }
        super.onCleared()
    }
}
