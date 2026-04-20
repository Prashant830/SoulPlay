package com.souljoy.soulmasti.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.ServerValue
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import com.souljoy.soulmasti.domain.model.SeatRole
import com.souljoy.soulmasti.domain.model.SocialSeatInvite
import com.souljoy.soulmasti.domain.model.SocialVoiceChatMessage
import com.souljoy.soulmasti.domain.model.SocialVoiceRoomSnapshot
import com.souljoy.soulmasti.domain.model.SocialVoiceSeat
import com.souljoy.soulmasti.domain.model.VoiceEntitlementState
import com.souljoy.soulmasti.domain.repository.SocialVoiceRoomRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirebaseSocialVoiceRoomRepository(
    private val auth: FirebaseAuth,
    private val database: FirebaseDatabase,
) : SocialVoiceRoomRepository {

    override fun observeMyEntitlement(): Flow<VoiceEntitlementState> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            trySend(VoiceEntitlementState(hasPermanentRoom = false, vipExpiresAt = null))
            awaitClose { }
            return@callbackFlow
        }
        val ref = database.reference.child("users").child(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val owned = snapshot.child("voiceRoomOwned").getValue(Boolean::class.java) == true ||
                    snapshot.child("voiceRoomPurchaseAt").exists()
                val vipExpiresAt = parseLong(snapshot.child("vipExpiresAt").value)
                trySend(VoiceEntitlementState(hasPermanentRoom = owned, vipExpiresAt = vipExpiresAt))
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override suspend fun purchaseOwnRoom(costCoins: Long): Result<Unit> = runCatching {
        val uid = requireUid()
        if (costCoins <= 0L) error("Invalid room purchase cost")
        deductCoins(uid, costCoins)
        val now = System.currentTimeMillis()
        val userRef = database.reference.child("users").child(uid)
        userRef.child("voiceRoomOwned").setValue(true).await()
        userRef.child("voiceRoomPurchaseAt").setValue(now).await()
        ensureRoomStructure(uid)
    }

    override suspend fun purchaseVipMonthly(costCoins: Long): Result<Unit> = runCatching {
        val uid = requireUid()
        if (costCoins <= 0L) error("Invalid VIP cost")
        deductCoins(uid, costCoins)
        val userRef = database.reference.child("users").child(uid)
        val now = System.currentTimeMillis()
        val current = parseLong(userRef.child("vipExpiresAt").get().await().value) ?: 0L
        val base = if (current > now) current else now
        val extended = base + 30L * 24L * 60L * 60L * 1000L
        userRef.child("vipExpiresAt").setValue(extended).await()
    }

    override fun observeRoom(roomId: String): Flow<SocialVoiceRoomSnapshot?> = callbackFlow {
        if (roomId.isBlank()) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }
        val ref = database.reference.child("voiceRooms").child(roomId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(parseRoom(roomId, snapshot))
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override suspend fun ensureOwnRoomCreated(): Result<String> = runCatching {
        val uid = requireUid()
        ensureRoomStructure(uid)
        uid
    }

    override suspend fun enterFriendRoom(roomId: String, friendEntryCostCoins: Long): Result<Unit> = runCatching {
        val uid = requireUid()
        if (roomId.isBlank()) error("Invalid room id")
        val roomSnap = database.reference.child("voiceRooms").child(roomId).get().await()
        if (!roomSnap.exists()) error("Room not found")

        val userRef = database.reference.child("users").child(uid)
        val vipExpiresAt = parseLong(userRef.child("vipExpiresAt").get().await().value) ?: 0L
        val vipActive = vipExpiresAt > System.currentTimeMillis()
        if (!vipActive && friendEntryCostCoins > 0L) {
            deductCoins(uid, friendEntryCostCoins)
        }
    }

    override suspend fun setRoomPresence(roomId: String, online: Boolean): Result<Unit> = runCatching {
        val uid = requireUid()
        if (roomId.isBlank()) return@runCatching
        val roomRef = database.reference.child("voiceRooms").child(roomId)
        val ref = roomRef.child("presence").child(uid)
        if (online) {
            ref.setValue(true).await()
            ref.onDisconnect().removeValue()
            val ownerUid = roomRef.child("ownerUid").get().await().getValue(String::class.java).orEmpty()
            if (ownerUid == uid) {
                roomRef.child("collapsed").setValue(false).await()
                // Owner rejoined: seat 1 should be owned again.
                roomRef.child("seats").child("1").child("uid").setValue(uid).await()
                roomRef.child("seats").child("1").child("muted").setValue(false).await()
            }
        } else {
            ref.removeValue().await()
        }
    }

    override suspend fun takeSeat(roomId: String, seatNo: Int): Result<Unit> = runCatching {
        val uid = requireUid()
        requireValidSeat(seatNo)
        val roomRef = database.reference.child("voiceRooms").child(roomId)
        val roomSnap = roomRef.get().await()
        val ownerUid = roomSnap.child("ownerUid").getValue(String::class.java).orEmpty()
        val adminUids = readAdminUids(roomSnap)
        if (uid == ownerUid && seatNo != 1) error("Owner seat is fixed")
        if (seatNo == 1 && uid != ownerUid) error("Owner seat only")
        val seatsRef = roomRef.child("seats")

        suspendCancellableCoroutine<Unit> { cont ->
            seatsRef.runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val targetSeat = currentData.child(seatNo.toString())
                    val locked = targetSeat.child("locked").value as? Boolean ?: false
                    val currentUid = targetSeat.child("uid").value as? String
                    if (locked && uid != ownerUid && !adminUids.contains(uid)) return Transaction.abort()
                    if (!currentUid.isNullOrBlank() && currentUid != uid) return Transaction.abort()

                    // If guest chooses another seat, move them and clear previous seat.
                    for (i in 1..10) {
                        if (i == 1) continue
                        val seat = currentData.child(i.toString())
                        val seatUid = seat.child("uid").value as? String
                        if (seatUid == uid) {
                            seat.child("uid").value = ""
                            seat.child("muted").value = false
                            seat.child("updatedAt").value = ServerValue.TIMESTAMP
                        }
                    }
                    targetSeat.child("uid").value = uid
                    if (targetSeat.child("muted").value == null) {
                        targetSeat.child("muted").value = false
                    }
                    targetSeat.child("updatedAt").value = ServerValue.TIMESTAMP
                    return Transaction.success(currentData)
                }

                override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                    when {
                        error != null -> cont.resumeWithException(error.toException())
                        !committed -> cont.resumeWithException(IllegalStateException("Seat unavailable"))
                        else -> cont.resume(Unit)
                    }
                }
            })
        }
    }

    override suspend fun leaveSeat(roomId: String, seatNo: Int): Result<Unit> = runCatching {
        val uid = requireUid()
        requireValidSeat(seatNo)
        val roomRef = database.reference.child("voiceRooms").child(roomId)
        val roomSnap = roomRef.get().await()
        val ownerUid = roomSnap.child("ownerUid").getValue(String::class.java).orEmpty()
        if (seatNo == 1 || uid == ownerUid) error("Owner seat cannot be vacated")
        val seatRef = database.reference.child("voiceRooms").child(roomId).child("seats").child(seatNo.toString())
        suspendCancellableCoroutine<Unit> { cont ->
            seatRef.runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val currentUid = currentData.child("uid").value as? String
                    if (currentUid.isNullOrBlank() || currentUid != uid) return Transaction.abort()
                    currentData.child("uid").value = ""
                    currentData.child("muted").value = false
                    currentData.child("updatedAt").value = ServerValue.TIMESTAMP
                    return Transaction.success(currentData)
                }

                override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                    when {
                        error != null -> cont.resumeWithException(error.toException())
                        !committed -> cont.resumeWithException(IllegalStateException("Cannot leave this seat"))
                        else -> cont.resume(Unit)
                    }
                }
            })
        }
    }

    override suspend fun setSeatLocked(roomId: String, seatNo: Int, locked: Boolean): Result<Unit> = runCatching {
        val uid = requireUid()
        requireValidSeat(seatNo)
        if (seatNo == 1) error("Owner seat cannot be locked")
        val roomRef = database.reference.child("voiceRooms").child(roomId)
        val roomSnap = roomRef.get().await()
        val ownerUid = roomSnap.child("ownerUid").getValue(String::class.java).orEmpty()
        val adminUids = readAdminUids(roomSnap)
        if (uid != ownerUid && !adminUids.contains(uid)) error("Only owner/admin can lock seats")
        val seatRef = roomRef.child("seats").child(seatNo.toString())
        if (locked) {
            // Closing an occupied seat should force-unseat current user so owner/admin action always works.
            seatRef.child("uid").setValue("").await()
            seatRef.child("muted").setValue(false).await()
        }
        seatRef.child("locked").setValue(locked).await()
    }

    override suspend fun setSeatMuted(roomId: String, seatNo: Int, muted: Boolean): Result<Unit> = runCatching {
        val uid = requireUid()
        requireValidSeat(seatNo)
        val roomRef = database.reference.child("voiceRooms").child(roomId)
        val roomSnap = roomRef.get().await()
        val ownerUid = roomSnap.child("ownerUid").getValue(String::class.java).orEmpty()
        val adminUids = readAdminUids(roomSnap)
        val seatSnap = roomSnap.child("seats").child(seatNo.toString())
        val occupantUid = seatSnap.child("uid").getValue(String::class.java).orEmpty()
        val canManage = uid == ownerUid || adminUids.contains(uid) || uid == occupantUid
        if (!canManage) error("You cannot mute this seat")
        roomRef.child("seats").child(seatNo.toString()).child("muted").setValue(muted).await()
    }

    override suspend fun removeSeatOccupant(roomId: String, seatNo: Int): Result<Unit> = runCatching {
        val uid = requireUid()
        requireValidSeat(seatNo)
        if (seatNo == 1) error("Owner seat cannot be removed")
        val roomRef = database.reference.child("voiceRooms").child(roomId)
        val roomSnap = roomRef.get().await()
        val ownerUid = roomSnap.child("ownerUid").getValue(String::class.java).orEmpty()
        val adminUids = readAdminUids(roomSnap)
        if (uid != ownerUid && !adminUids.contains(uid)) error("Only owner/admin can remove")
        val seatRef = roomRef.child("seats").child(seatNo.toString())
        seatRef.child("uid").setValue("").await()
        seatRef.child("muted").setValue(false).await()
    }

    override suspend fun sendSeatInvite(roomId: String, toUid: String, seatNo: Int): Result<Unit> = runCatching {
        val uid = requireUid()
        requireValidSeat(seatNo)
        if (toUid.isBlank()) error("Invalid user")
        val roomRef = database.reference.child("voiceRooms").child(roomId)
        val roomSnap = roomRef.get().await()
        val ownerUid = roomSnap.child("ownerUid").getValue(String::class.java).orEmpty()
        val adminUids = readAdminUids(roomSnap)
        if (uid != ownerUid && !adminUids.contains(uid)) error("Only owner/admin can invite")
        val inviteRef = roomRef.child("seatInvites").push()
        inviteRef.setValue(
            mapOf(
                "roomId" to roomId,
                "fromUid" to uid,
                "toUid" to toUid,
                "seatNo" to seatNo,
                "status" to "pending",
                "createdAt" to ServerValue.TIMESTAMP,
            ),
        ).await()
    }

    override suspend fun respondSeatInvite(roomId: String, inviteId: String, accept: Boolean): Result<Unit> = runCatching {
        val uid = requireUid()
        val inviteRef = database.reference.child("voiceRooms").child(roomId).child("seatInvites").child(inviteId)
        val snap = inviteRef.get().await()
        if (!snap.exists()) error("Invite not found")
        val toUid = snap.child("toUid").getValue(String::class.java).orEmpty()
        val seatNo = parseLong(snap.child("seatNo").value)?.toInt() ?: error("Invalid seat in invite")
        if (toUid != uid) error("Not your invite")
        val status = snap.child("status").getValue(String::class.java).orEmpty()
        if (status != "pending") return@runCatching
        if (accept) {
            takeSeat(roomId, seatNo).getOrThrow()
            inviteRef.child("status").setValue("accepted").await()
        } else {
            inviteRef.child("status").setValue("declined").await()
        }
    }

    override fun observeSeatInvites(roomId: String): Flow<List<SocialSeatInvite>> = callbackFlow {
        if (roomId.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val ref = database.reference.child("voiceRooms").child(roomId).child("seatInvites")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val invites = snapshot.children.mapNotNull { child ->
                    val id = child.key ?: return@mapNotNull null
                    val fromUid = child.child("fromUid").getValue(String::class.java) ?: return@mapNotNull null
                    val toUid = child.child("toUid").getValue(String::class.java) ?: return@mapNotNull null
                    val seatNo = parseLong(child.child("seatNo").value)?.toInt() ?: return@mapNotNull null
                    val status = child.child("status").getValue(String::class.java).orEmpty().ifBlank { "pending" }
                    SocialSeatInvite(
                        inviteId = id,
                        roomId = roomId,
                        fromUid = fromUid,
                        toUid = toUid,
                        seatNo = seatNo,
                        status = status,
                        createdAt = parseLong(child.child("createdAt").value),
                    )
                }.sortedByDescending { it.createdAt ?: 0L }
                trySend(invites)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override suspend fun dismissMyPendingSeatInvites(roomId: String): Result<Unit> = runCatching {
        val uid = requireUid()
        if (roomId.isBlank()) return@runCatching
        val ref = database.reference.child("voiceRooms").child(roomId).child("seatInvites")
        val snap = ref.get().await()
        for (child in snap.children) {
            val toUid = child.child("toUid").getValue(String::class.java).orEmpty()
            val status = child.child("status").getValue(String::class.java).orEmpty()
            if (toUid == uid && status == "pending") {
                child.ref.child("status").setValue("dismissed").await()
            }
        }
    }

    override fun observeMyKickSignal(roomId: String): Flow<Long?> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (roomId.isBlank() || uid.isNullOrBlank()) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }
        val ref = database.reference.child("voiceRooms").child(roomId).child("kickedUsers").child(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(parseLong(snapshot.value))
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override fun observeMyChatVisibleAfter(roomId: String): Flow<Long?> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (roomId.isBlank() || uid.isNullOrBlank()) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }
        val ref = database.reference.child("users").child(uid).child("voiceRoomChatVisibleAfter").child(roomId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(parseLong(snapshot.value))
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override suspend fun markMyChatCleared(roomId: String): Result<Unit> = runCatching {
        val uid = requireUid()
        if (roomId.isBlank()) return@runCatching
        database.reference
            .child("users")
            .child(uid)
            .child("voiceRoomChatVisibleAfter")
            .child(roomId)
            .setValue(ServerValue.TIMESTAMP)
            .await()
    }

    override suspend fun updateRoomName(roomId: String, roomName: String): Result<Unit> = runCatching {
        val uid = requireUid()
        val trimmed = roomName.trim().take(40)
        if (roomId.isBlank() || trimmed.isBlank()) error("Invalid room name")
        val roomRef = database.reference.child("voiceRooms").child(roomId)
        val roomSnap = roomRef.get().await()
        if (!roomSnap.exists()) error("Room not found")
        val isInRoomPresence = roomSnap.child("presence").hasChild(uid)
        if (!isInRoomPresence) error("Only room members can rename")
        roomRef.child("roomName").setValue(trimmed).await()
    }

    override suspend fun collapseRoomIfOwnerLeft(roomId: String): Result<Unit> = runCatching {
        val uid = requireUid()
        if (roomId.isBlank()) return@runCatching
        val roomRef = database.reference.child("voiceRooms").child(roomId)
        val roomSnap = roomRef.get().await()
        val ownerUid = roomSnap.child("ownerUid").getValue(String::class.java).orEmpty()
        if (ownerUid != uid) return@runCatching

        // Owner left: collapse room to lobby mode.
        roomRef.child("collapsed").setValue(true).await()
        roomRef.child("chat").removeValue().await()
        roomRef.child("seatInvites").removeValue().await()
        roomRef.child("seats").get().await().children.forEach { child ->
            val key = child.key ?: return@forEach
            roomRef.child("seats").child(key).child("uid").setValue("").await()
            roomRef.child("seats").child(key).child("muted").setValue(false).await()
        }
    }

    override fun observeRoomChat(roomId: String): Flow<List<SocialVoiceChatMessage>> = callbackFlow {
        if (roomId.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val ref = database.reference.child("voiceRooms").child(roomId).child("chat")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val msgs = snapshot.children.mapNotNull { c ->
                    val id = c.key ?: return@mapNotNull null
                    val fromUid = c.child("fromUid").getValue(String::class.java) ?: return@mapNotNull null
                    val fromName = c.child("fromName").getValue(String::class.java).orEmpty().ifBlank {
                        FirebaseUidMapping.shortLabel(fromUid)
                    }
                    val text = c.child("text").getValue(String::class.java)?.trim().orEmpty()
                    if (text.isBlank()) return@mapNotNull null
                    SocialVoiceChatMessage(
                        id = id,
                        fromUid = fromUid,
                        fromName = fromName,
                        text = text,
                        createdAt = parseLong(c.child("createdAt").value),
                    )
                }.sortedBy { it.createdAt ?: 0L }
                trySend(msgs)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override suspend fun sendRoomChatMessage(roomId: String, text: String): Result<Unit> = runCatching {
        val uid = requireUid()
        val trimmed = text.trim()
        if (trimmed.isBlank()) return@runCatching
        val userSnap = database.reference.child("users").child(uid).child("username").get().await()
        val fromName = userSnap.getValue(String::class.java)?.trim().orEmpty().ifBlank {
            FirebaseUidMapping.shortLabel(uid)
        }
        database.reference.child("voiceRooms").child(roomId).child("chat").push().setValue(
            mapOf(
                "fromUid" to uid,
                "fromName" to fromName,
                "text" to trimmed,
                "createdAt" to ServerValue.TIMESTAMP,
            ),
        ).await()
    }

    private fun parseRoom(roomId: String, snap: DataSnapshot): SocialVoiceRoomSnapshot? {
        if (!snap.exists()) return null
        val ownerUid = snap.child("ownerUid").getValue(String::class.java)?.takeIf { it.isNotBlank() } ?: roomId
        val roomName = snap.child("roomName").getValue(String::class.java)?.trim().orEmpty().ifBlank {
            "Room ${roomId.takeLast(6)}"
        }
        val adminUids = readAdminUids(snap)
        val onlineUids = snap.child("presence").children.mapNotNull { it.key }.toSet()
        val ownerOnline = onlineUids.contains(ownerUid)
        val collapsed = snap.child("collapsed").getValue(Boolean::class.java) == true || !ownerOnline
        val onlineCount = snap.child("presence").childrenCount.toInt()
        val seats = (1..10).map { no ->
            val seat = snap.child("seats").child(no.toString())
            val uid = seat.child("uid").getValue(String::class.java)?.takeIf { it.isNotBlank() }
            val locked = seat.child("locked").getValue(Boolean::class.java) == true
            val muted = seat.child("muted").getValue(Boolean::class.java) == true
            val role = when (no) {
                1 -> SeatRole.OWNER
                else -> if (uid != null && adminUids.contains(uid)) SeatRole.ADMIN else SeatRole.GUEST
            }
            SocialVoiceSeat(
                seatNo = no,
                occupantUid = uid,
                occupantName = uid?.let { FirebaseUidMapping.shortLabel(it) },
                locked = locked,
                muted = muted,
                role = role,
            )
        }
        return SocialVoiceRoomSnapshot(
            roomId = roomId,
            roomName = roomName,
            ownerUid = ownerUid,
            ownerOnline = ownerOnline,
            collapsed = collapsed,
            adminUids = adminUids,
            onlineUids = onlineUids,
            seats = seats,
            onlineCount = onlineCount,
        )
    }

    private fun requireUid(): String =
        auth.currentUser?.uid?.takeIf { it.isNotBlank() } ?: error("Not signed in")

    private suspend fun ensureRoomStructure(ownerUid: String): DataSnapshot {
        val roomRef = database.reference.child("voiceRooms").child(ownerUid)
        val existing = roomRef.get().await()
        if (existing.exists()) return existing
        val seats = mutableMapOf<String, Any>()
        for (i in 1..10) {
            seats[i.toString()] = mapOf(
                "uid" to if (i == 1) ownerUid else "",
                "locked" to false,
                "muted" to false,
            )
        }
        roomRef.setValue(
            mapOf(
                "ownerUid" to ownerUid,
                "roomName" to "My Voice Room",
                "collapsed" to false,
                "admins" to emptyMap<String, Boolean>(),
                "permanent" to true,
                "createdAt" to ServerValue.TIMESTAMP,
                "seats" to seats,
            ),
        ).await()
        return roomRef.get().await()
    }

    private fun requireValidSeat(seatNo: Int) {
        if (seatNo !in 1..10) error("Invalid seat")
    }

    private fun readAdminUids(snapshot: DataSnapshot): Set<String> =
        snapshot.child("admins").children.mapNotNull { c ->
            val uid = c.key ?: return@mapNotNull null
            val enabled = c.getValue(Boolean::class.java) == true
            uid.takeIf { enabled }
        }.toSet()

    private suspend fun deductCoins(uid: String, amount: Long): Long {
        val ref = database.reference.child("users").child(uid).child("totalWinnings")
        return suspendCancellableCoroutine { cont ->
            ref.runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val v = parseLong(currentData.value) ?: 0L
                    if (v < amount) return Transaction.abort()
                    currentData.value = v - amount
                    return Transaction.success(currentData)
                }

                override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                    when {
                        error != null -> cont.resumeWithException(error.toException())
                        !committed -> cont.resumeWithException(IllegalStateException("Not enough coins"))
                        else -> cont.resume(parseLong(currentData?.value) ?: 0L)
                    }
                }
            })
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
        (value as? java.lang.Number)?.let { return it.longValue() }
        return null
    }
}

