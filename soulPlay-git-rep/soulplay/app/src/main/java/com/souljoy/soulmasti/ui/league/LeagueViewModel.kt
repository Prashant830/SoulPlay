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
import java.util.Calendar
import java.util.TimeZone

data class LeagueConfig(
    val leagueName: String = "Maha Gifting League",
    val arenaTitle: String = "Global Arena",
    val prizePoolSoul: Long = 0L,
)

data class LeagueUserRow(
    val uid: String,
    val soul: Long,
    val profileTotalSoul: Long = 0L,
    val username: String = "",
    val photoUrl: String = "",
)

data class LeagueRoomRow(
    val roomId: String,
    val soul: Long,
    val roomName: String = "",
    val ownerPhotoUrl: String = "",
)

enum class LeaguePeriod { TODAY, WEEK, TOTAL }

class LeagueViewModel(
    application: Application,
    private val auth: FirebaseAuth,
    private val database: FirebaseDatabase,
) : AndroidViewModel(application) {

    private val _config = MutableStateFlow(LeagueConfig())
    val config: StateFlow<LeagueConfig> = _config.asStateFlow()

    private val _userToday = MutableStateFlow<List<LeagueUserRow>>(emptyList())
    val userToday: StateFlow<List<LeagueUserRow>> = _userToday.asStateFlow()
    private val _userWeek = MutableStateFlow<List<LeagueUserRow>>(emptyList())
    val userWeek: StateFlow<List<LeagueUserRow>> = _userWeek.asStateFlow()
    private val _userTotal = MutableStateFlow<List<LeagueUserRow>>(emptyList())
    val userTotal: StateFlow<List<LeagueUserRow>> = _userTotal.asStateFlow()

    private val _roomToday = MutableStateFlow<List<LeagueRoomRow>>(emptyList())
    val roomToday: StateFlow<List<LeagueRoomRow>> = _roomToday.asStateFlow()
    private val _roomWeek = MutableStateFlow<List<LeagueRoomRow>>(emptyList())
    val roomWeek: StateFlow<List<LeagueRoomRow>> = _roomWeek.asStateFlow()
    private val _roomTotal = MutableStateFlow<List<LeagueRoomRow>>(emptyList())
    val roomTotal: StateFlow<List<LeagueRoomRow>> = _roomTotal.asStateFlow()

    val myUid: String?
        get() = auth.currentUser?.uid

    private val listeners = mutableListOf<Pair<com.google.firebase.database.DatabaseReference, ValueEventListener>>()

    init {
        observeConfig()
        observeUserRankings()
        observeRoomRankings()
    }

    private fun observeConfig() {
        val ref = database.reference.child("leagueV1").child("config").child("current")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                _config.value = LeagueConfig(
                    leagueName = snapshot.child("leagueName").getValue(String::class.java).orEmpty()
                        .ifBlank { "Maha Gifting League" },
                    arenaTitle = snapshot.child("arenaTitle").getValue(String::class.java).orEmpty()
                        .ifBlank { "Global Arena" },
                    prizePoolSoul = parseLong(snapshot.child("prizePoolSoul").value) ?: 0L,
                )
            }
            override fun onCancelled(error: DatabaseError) = Unit
        }
        ref.addValueEventListener(listener)
        listeners += ref to listener
    }

    private fun observeUserRankings() {
        val dayKey = utcDayKey(System.currentTimeMillis()).toString()
        val weekKey = utcWeekKey(System.currentTimeMillis()).toString()
        val todayRef = database.reference.child("leaderboardsV1").child("profile").child("daily").child(dayKey)
        val weekRef = database.reference.child("leaderboardsV1").child("profile").child("weekly").child(weekKey)
        val totalRef = database.reference.child("leaderboardsV1").child("profile").child("total")

        attachUserListener(todayRef) { _userToday.value = it }
        attachUserListener(weekRef) { _userWeek.value = it }
        attachUserListener(totalRef) { _userTotal.value = it }
    }

    private fun observeRoomRankings() {
        val dayKey = utcDayKey(System.currentTimeMillis()).toString()
        val weekKey = utcWeekKey(System.currentTimeMillis()).toString()
        val todayRef = database.reference.child("leaderboardsV1").child("room").child("daily").child(dayKey)
        val weekRef = database.reference.child("leaderboardsV1").child("room").child("weekly").child(weekKey)
        val totalRef = database.reference.child("leaderboardsV1").child("room").child("total")

        attachRoomListener(todayRef) { _roomToday.value = it }
        attachRoomListener(weekRef) { _roomWeek.value = it }
        attachRoomListener(totalRef) { _roomTotal.value = it }
    }

    private fun attachUserListener(
        ref: com.google.firebase.database.DatabaseReference,
        sink: (List<LeagueUserRow>) -> Unit,
    ) {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val base = snapshot.children.mapNotNull { child ->
                    val uid = child.key?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    val soul = parseLong(child.child("soul").value) ?: 0L
                    LeagueUserRow(uid = uid, soul = soul)
                }.sortedByDescending { it.soul }
                viewModelScope.launch { sink(enrichUsers(base)) }
            }
            override fun onCancelled(error: DatabaseError) = Unit
        }
        ref.addValueEventListener(listener)
        listeners += ref to listener
    }

    private fun attachRoomListener(
        ref: com.google.firebase.database.DatabaseReference,
        sink: (List<LeagueRoomRow>) -> Unit,
    ) {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val base = snapshot.children.mapNotNull { child ->
                    val roomId = child.key?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    val soul = parseLong(child.child("soul").value) ?: 0L
                    LeagueRoomRow(roomId = roomId, soul = soul)
                }.sortedByDescending { it.soul }
                viewModelScope.launch { sink(enrichRooms(base)) }
            }
            override fun onCancelled(error: DatabaseError) = Unit
        }
        ref.addValueEventListener(listener)
        listeners += ref to listener
    }

    private suspend fun enrichUsers(rows: List<LeagueUserRow>): List<LeagueUserRow> {
        if (rows.isEmpty()) return rows
        data class ProfileMeta(
            val username: String,
            val photoUrl: String,
            val totalSoul: Long,
        )
        val profileMap = mutableMapOf<String, ProfileMeta>()
        rows.map { it.uid }.distinct().forEach { uid ->
            val snap = runCatching { database.reference.child("users").child(uid).get().await() }.getOrNull()
            if (snap != null) {
                val name = snap.child("username").getValue(String::class.java).orEmpty()
                val photo = snap.child("profilePictureUrl").getValue(String::class.java).orEmpty()
                val totalSoul = parseLong(snap.child("soul").value) ?: 0L
                profileMap[uid] = ProfileMeta(
                    username = name,
                    photoUrl = photo,
                    totalSoul = totalSoul,
                )
            }
        }
        return rows.map { row ->
            val meta = profileMap[row.uid]
            row.copy(
                username = meta?.username.orEmpty(),
                photoUrl = meta?.photoUrl.orEmpty(),
                profileTotalSoul = meta?.totalSoul ?: 0L,
            )
        }
    }

    private suspend fun enrichRooms(rows: List<LeagueRoomRow>): List<LeagueRoomRow> {
        if (rows.isEmpty()) return rows
        val roomMeta = mutableMapOf<String, Pair<String, String>>()
        rows.map { it.roomId }.distinct().forEach { roomId ->
            val snap = runCatching { database.reference.child("voiceRooms").child(roomId).get().await() }.getOrNull()
            val roomName = snap?.child("roomName")?.getValue(String::class.java).orEmpty().ifBlank { "Room ${roomId.takeLast(6)}" }
            val ownerUid = snap?.child("ownerUid")?.getValue(String::class.java).orEmpty()
            val ownerPhoto = if (ownerUid.isNotBlank()) {
                val ownerSnap = runCatching { database.reference.child("users").child(ownerUid).get().await() }.getOrNull()
                ownerSnap?.child("profilePictureUrl")?.getValue(String::class.java).orEmpty()
            } else {
                ""
            }
            roomMeta[roomId] = roomName to ownerPhoto
        }
        return rows.map {
            val meta = roomMeta[it.roomId]
            it.copy(
                roomName = meta?.first.orEmpty(),
                ownerPhotoUrl = meta?.second.orEmpty(),
            )
        }
    }

    override fun onCleared() {
        listeners.forEach { (ref, listener) -> ref.removeEventListener(listener) }
        listeners.clear()
        super.onCleared()
    }

    private fun parseLong(value: Any?): Long? = when (value) {
        null -> null
        is Long -> value
        is Int -> value.toLong()
        is Double -> value.toLong()
        is Float -> value.toLong()
        is String -> value.trim().toLongOrNull()
        is Number -> value.toLong()
        else -> null
    }

    private fun utcDayKey(timestampMs: Long): Long = timestampMs / 86_400_000L

    private fun utcWeekKey(timestampMs: Long): Long {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.minimalDaysInFirstWeek = 4
        cal.timeInMillis = timestampMs
        val year = cal.get(Calendar.YEAR).toLong()
        val weekOfYear = cal.get(Calendar.WEEK_OF_YEAR).toLong()
        return year * 100L + weekOfYear
    }
}
