package com.masti.soulplay.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import com.masti.soulplay.domain.model.GameHistoryEntry
import com.masti.soulplay.domain.model.GamePhase
import com.masti.soulplay.domain.model.GameRoomSnapshot
import com.masti.soulplay.domain.model.MatchOutcome
import com.masti.soulplay.domain.RoomJoinEconomy
import com.masti.soulplay.domain.model.PlayerInRoom
import com.masti.soulplay.domain.repository.GameSessionRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirebaseGameSessionRepository(
    private val auth: FirebaseAuth,
    private val database: FirebaseDatabase,
) : GameSessionRepository {

    private val _firebaseUid = MutableStateFlow<String?>(
        auth.currentUser?.uid
    )
    override val firebaseUid: StateFlow<String?> = _firebaseUid.asStateFlow()

    override suspend fun ensureSignedInAndPresence() {
        val uid: String = auth.currentUser?.uid
            ?: throw IllegalStateException("User not signed in. Login with Google first.")
        _firebaseUid.value = uid
        try {
            val userRef = database.reference.child("users").child(uid)
            userRef.child("online").setValue(true).await()
            userRef.child("online").onDisconnect().setValue(false)
        } catch (_: Exception) {
            // Presence write can fail due to RTDB rules; matchmaking / Agora still rely on Firebase uid.
        }
    }

    override suspend fun addToWaitingQueue() {
        val uid = requireUid()
        database.reference
            .child("matchmaking")
            .child("waitingPlayers")
            .child(uid)
            .setValue(mapOf("online" to true))
            .await()
    }

    override suspend fun removeFromWaitingQueue() {
        val uid = effectiveUid() ?: return
        database.reference
            .child("matchmaking")
            .child("waitingPlayers")
            .child(uid)
            .removeValue()
            .await()
    }

    override suspend fun awaitMatchedRoom(): String = suspendCancellableCoroutine { cont ->
        val uid = effectiveUid()
        if (uid == null) {
            cont.resumeWithException(IllegalStateException("No player id — call ensureSignedInAndPresence first"))
            return@suspendCancellableCoroutine
        }
        val roomsRef = database.reference.child("rooms")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val match = findBestRoomForPlayer(snapshot, uid)
                if (match != null && cont.isActive) {
                    roomsRef.removeEventListener(this)
                    cont.resume(match)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                if (cont.isActive) {
                    roomsRef.removeEventListener(this)
                    cont.resumeWithException(error.toException())
                }
            }
        }
        roomsRef.addValueEventListener(listener)
        cont.invokeOnCancellation { roomsRef.removeEventListener(listener) }
    }

    override fun observeRoom(roomId: String): Flow<GameRoomSnapshot?> = callbackFlow {
        val ref = database.reference.child("rooms").child(roomId)
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

    override suspend fun setSelectedSuspect(roomId: String, suspectFirebaseUid: String) {
        database.reference
            .child("rooms")
            .child(roomId)
            .child("gameState")
            .child("selectedSuspect")
            .setValue(suspectFirebaseUid)
            .await()
    }

    override suspend fun setLocalPlayerOnline(online: Boolean) {
        val uid = effectiveUid() ?: return
        runCatching {
            database.reference.child("users").child(uid).child("online").setValue(online).await()
        }
    }

    override suspend fun getCoinBalance(): Long {
        val uid = requireUid()
        val snap = database.reference.child("users").child(uid).child("totalWinnings").get().await()
        return parseLong(snap.value) ?: 0L
    }

    override suspend fun deductJoinRoomFee(): Long {
        val uid = requireUid()
        val fee = RoomJoinEconomy.JOIN_ROOM_FEE_COINS
        val ref = database.reference.child("users").child(uid).child("totalWinnings")
        return suspendCancellableCoroutine { cont ->
            ref.runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val v = parseLong(currentData.value) ?: 0L
                    if (v < fee) {
                        return Transaction.abort()
                    }
                    currentData.value = v - fee
                    return Transaction.success(currentData)
                }

                override fun onComplete(
                    error: DatabaseError?,
                    committed: Boolean,
                    currentData: DataSnapshot?,
                ) {
                    when {
                        error != null -> cont.resumeWithException(error.toException())
                        !committed ->
                            cont.resumeWithException(
                                IllegalStateException(
                                    "Not enough coins. Joining a room costs $fee coins. Top up by playing and winning."
                                )
                            )
                        else -> cont.resume(parseLong(currentData?.value) ?: 0L)
                    }
                }
            })
        }
    }

    override fun observeUserTotalWinnings(): Flow<Long?> =
        firebaseUid.flatMapLatest { uid ->
            if (uid == null) return@flatMapLatest flowOf(null)
            callbackFlow {
                val ref = database.reference.child("users").child(uid).child("totalWinnings")
                val listener = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        trySend(parseLong(snapshot.value) ?: 0L)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        close(error.toException())
                    }
                }
                ref.addValueEventListener(listener)
                awaitClose { ref.removeEventListener(listener) }
            }
        }

    override fun observeUserGameHistory(): Flow<List<GameHistoryEntry>> =
        firebaseUid.flatMapLatest { uid ->
            if (uid == null) return@flatMapLatest flowOf(emptyList())
            callbackFlow {
                val ref = database.reference.child("rooms")
                val listener = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val list = mutableListOf<GameHistoryEntry>()
                        for (child in snapshot.children) {
                            val roomId = child.key ?: continue
                            if (!child.child("players").hasChild(uid)) continue
                            val room = parseRoom(roomId, child) ?: continue
                            if (room.status != "ENDED") continue
                            list.add(gameHistoryEntryFromRoom(room, uid))
                        }
                        list.sortByDescending { it.endedAt }
                        trySend(list)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        close(error.toException())
                    }
                }
                ref.addValueEventListener(listener)
                awaitClose { ref.removeEventListener(listener) }
            }
        }

    private fun gameHistoryEntryFromRoom(room: GameRoomSnapshot, myUid: String): GameHistoryEntry {
        val endedAt = room.timerEndAt ?: room.createdAt
        return GameHistoryEntry(
            id = room.roomId,
            roomId = room.roomId,
            endedAt = endedAt,
            userId = myUid,
            role = room.roles[myUid],
            gamePoints = room.scores[myUid],
            outcome = deriveMatchOutcome(room, myUid),
            result = room.endReason,
            summary = room.message,
            scoresByUser = room.scores,
        )
    }

    /** 0 points → lost; any other score → win. Missing score → unknown. */
    private fun deriveMatchOutcome(room: GameRoomSnapshot, myUid: String): MatchOutcome {
        val pts = room.scores[myUid] ?: return MatchOutcome.UNKNOWN
        return if (pts == 0L) MatchOutcome.LOST else MatchOutcome.WIN
    }

    private fun requireUid(): String =
        effectiveUid() ?: error("No player id — call ensureSignedInAndPresence first")

    private fun effectiveUid(): String? =
        _firebaseUid.value ?: auth.currentUser?.uid

    /**
     * Picks the newest room where this user is a player and the match is still active.
     * Finished rooms ([ENDED]) are ignored so "Find match" waits for a new [PLAYING] room
     * instead of reusing a previous game.
     */
    private fun findBestRoomForPlayer(roomsSnapshot: DataSnapshot, myUid: String): String? {
        var bestId: String? = null
        var bestCreatedAt = Long.MIN_VALUE
        for (child in roomsSnapshot.children) {
            val key = child.key ?: continue
            val players = child.child("players")
            if (!players.hasChild(myUid)) continue
            val status = child.child("status").getValue(String::class.java) ?: continue
            if (status != "PLAYING") continue
            val createdAt = parseLong(child.child("createdAt").value) ?: 0L
            if (createdAt >= bestCreatedAt) {
                bestCreatedAt = createdAt
                bestId = key
            }
        }
        return bestId
    }

    private fun parseRoom(roomId: String, snap: DataSnapshot): GameRoomSnapshot? {
        if (!snap.exists()) return null
        val status = snap.child("status").getValue(String::class.java) ?: return null
        val createdAt = parseLong(snap.child("createdAt").value) ?: 0L
        val message = snap.child("message").getValue(String::class.java)
        val endReason = snap.child("endReason").getValue(String::class.java)
        val players = mutableMapOf<String, PlayerInRoom>()
        val playersSnap = snap.child("players")
        for (p in playersSnap.children) {
            val id = p.key ?: continue
            val online = p.child("online").getValue(Boolean::class.java) ?: false
            players[id] = PlayerInRoom(online = online)
        }
        val gs = snap.child("gameState")
        val phase = GamePhase.fromRaw(gs.child("phase").getValue(String::class.java))
        val roles = readStringMap(gs.child("roles"))
        val scores = readLongMap(gs.child("scores"))
        val selectedSuspect = gs.child("selectedSuspect").getValue(String::class.java).orEmpty()
        val timerEndAt = parseLong(gs.child("timerEndAt").value)
        val winnerUid = gs.child("winner").getValue(String::class.java)
            ?: gs.child("winnerUid").getValue(String::class.java)
        return GameRoomSnapshot(
            roomId = roomId,
            status = status,
            createdAt = createdAt,
            message = message,
            endReason = endReason,
            players = players,
            phase = phase,
            roles = roles,
            scores = scores,
            selectedSuspect = selectedSuspect,
            timerEndAt = timerEndAt,
            winnerUid = winnerUid,
        )
    }

    private fun readStringMap(snap: DataSnapshot): Map<String, String> {
        val out = mutableMapOf<String, String>()
        for (c in snap.children) {
            val k = c.key ?: continue
            val v = c.getValue(String::class.java) ?: continue
            out[k] = v
        }
        return out
    }

    private fun readLongMap(snap: DataSnapshot): Map<String, Long> {
        val out = mutableMapOf<String, Long>()
        for (c in snap.children) {
            val k = c.key ?: continue
            val v = parseLong(c.value) ?: continue
            out[k] = v
        }
        return out
    }

    /**
     * RTDB returns mixed numeric types. [Transaction] [MutableData] often exposes **Java** boxed
     * numbers (`java.lang.Integer`, `java.lang.Long`, `java.lang.Double`), which do **not** match
     * Kotlin's `is Number` — that caused join-fee transactions to read 0 and abort after a match.
     */
    private fun parseLong(value: Any?): Long? {
        if (value == null) return null
        if (value is String) return value.trim().toLongOrNull()
        if (value is Long) return value
        if (value is Int) return value.toLong()
        if (value is Double) return value.toLong()
        if (value is Float) return value.toLong()
        if (value is kotlin.Number) return value.toLong()
        (value as? java.lang.Number)?.let { return it.longValue() }
        return null
    }

}
