package com.souljoy.soulmasti.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.ServerValue
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import com.souljoy.soulmasti.domain.gift.GiftCatalog
import com.souljoy.soulmasti.domain.gift.GiftEvent
import com.souljoy.soulmasti.domain.gift.GiftSendContext
import com.souljoy.soulmasti.domain.repository.GiftRepository
import com.souljoy.soulmasti.domain.repository.GiftSendResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import java.util.concurrent.ThreadLocalRandom

class FirebaseGiftRepository(
    private val auth: FirebaseAuth,
    private val database: FirebaseDatabase,
) : GiftRepository {

    override suspend fun sendGift(
        context: GiftSendContext,
        giftId: String,
        recipientUserId: String?,
        selectedCount: Int,
    ): Result<GiftSendResult> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Not signed in"))

        val unitPrice = resolvePriceCoins(giftId)
            ?: return Result.failure(IllegalArgumentException("Unknown gift: $giftId"))
        val safeSelectedCount = selectedCount.coerceAtLeast(1)
        val totalPrice = unitPrice * safeSelectedCount.toLong()

        val balanceRef = database.reference.child("users").child(uid).child("totalWinnings")
        val eventsRef = database.reference
            .child("giftEvents")
            .child(context.rtdbSegment)
            .child(context.scopeId)

        return runCatching {
            val newBalance = deductCoins(balanceRef, totalPrice)

            val receiverCoins = if (!recipientUserId.isNullOrBlank()) {
                computeRandomPortion(max = totalPrice)
            } else {
                0L
            }
            val receiverSoul = if (!recipientUserId.isNullOrBlank()) {
                computeSoulReward(totalPrice)
            } else {
                0L
            }

            if (!recipientUserId.isNullOrBlank() && receiverCoins > 0L) {
                addReceivedGiftStats(
                    recipientUid = recipientUserId,
                    fromUserId = uid,
                    giftId = giftId,
                    coins = receiverCoins,
                    soul = receiverSoul,
                    selectedCount = safeSelectedCount,
                )
            }

            val newEventRef = eventsRef.push()
            val eventId = newEventRef.key ?: error("push() key")
            val payload = mutableMapOf<String, Any?>(
                "fromUserId" to uid,
                "giftId" to giftId,
                "selectedCount" to safeSelectedCount,
                "coins" to totalPrice,
                "receiverCoins" to receiverCoins,
                "receiverSoul" to receiverSoul,
                "createdAt" to ServerValue.TIMESTAMP,
            )
            if (!recipientUserId.isNullOrBlank()) {
                payload["toUserId"] = recipientUserId
            }
            try {
                newEventRef.setValue(payload).await()
            } catch (e: Exception) {
                addCoins(balanceRef, totalPrice)
                throw e
            }
            GiftSendResult(
                newBalance = newBalance,
                eventId = eventId,
                receiverCoins = receiverCoins,
                receiverSoul = receiverSoul,
                selectedCount = safeSelectedCount,
            )
        }
    }

    private suspend fun resolvePriceCoins(giftId: String): Long? {
        val remote = runCatching {
            database.reference.child("giftCatalog").child(giftId).get().await()
        }.getOrNull()
        if (remote != null && remote.exists()) {
            val p = parseLong(remote.child("price").value)
            if (p != null && p > 0) return p
        }
        return GiftCatalog.priceCoinsOrNull(giftId)
    }

    /**
     * Random reward helper tuned to reduce predictable values:
     * - For gifts above 15k total, receiver always gets at least 5k, plus random from the remaining range.
     * - Otherwise, receiver gets a fully random amount in [1, max].
     */
    private fun computeRandomPortion(max: Long): Long {
        if (max <= 0L) return 0L
        val base = max.coerceAtLeast(1L)
        val random = ThreadLocalRandom.current()
        return if (base > 15_000L) {
            val floor = 5_000L
            val remainder = (base - floor).coerceAtLeast(0L)
            if (remainder == 0L) floor else floor + random.nextLong(remainder + 1L)
        } else {
            random.nextLong(base) + 1L
        }
    }

    private fun computeSoulReward(giftGoldValue: Long): Long {
        if (giftGoldValue <= 0L) return 0L
        return giftGoldValue / 10L
    }

    /** Coins is the *rewarded* amount for receiver (already randomized). */
    private suspend fun addReceivedGiftStats(
        recipientUid: String,
        fromUserId: String,
        giftId: String,
        coins: Long,
        soul: Long,
        selectedCount: Int,
    ) {
        val userRef = database.reference.child("users").child(recipientUid)
        val coinsRef = userRef.child("giftReceivedCoins")
        val countRef = userRef.child("giftReceivedCount")
        val winningsRef = userRef.child("totalWinnings")
        val soulRef = userRef.child("soul")

        // Aggregate total coins from gifts for profile.
        suspendCancellableCoroutine<Unit> { cont ->
            coinsRef.runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val v = parseLong(currentData.value) ?: 0L
                    currentData.value = v + coins
                    return Transaction.success(currentData)
                }

                override fun onComplete(
                    error: DatabaseError?,
                    committed: Boolean,
                    currentData: DataSnapshot?,
                ) {
                    if (error != null || !committed) {
                        cont.resume(Unit)
                    } else {
                        cont.resume(Unit)
                    }
                }
            })
        }

        // Count of gifts received.
        suspendCancellableCoroutine<Unit> { cont ->
            countRef.runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val v = parseLong(currentData.value) ?: 0L
                    currentData.value = v + selectedCount.toLong()
                    return Transaction.success(currentData)
                }

                override fun onComplete(
                    error: DatabaseError?,
                    committed: Boolean,
                    currentData: DataSnapshot?,
                ) {
                    cont.resume(Unit)
                }
            })
        }

        // Also credit rewarded coins into receiver's total winnings.
        addCoins(winningsRef, coins)
        if (soul > 0L) {
            addCoins(soulRef, soul)
        }
        // Append a history row under users/{uid}/giftsReceived for profile gift wall.
        val historyRef = userRef.child("giftsReceived").push()
        val now = System.currentTimeMillis()
        val historyPayload = mapOf(
            "coins" to coins,
            "soul" to soul,
            "selectedCount" to selectedCount,
            "createdAt" to now,
            "fromUserId" to fromUserId,
            "giftId" to giftId,
        )
        historyRef.setValue(historyPayload).await()
    }

    private suspend fun deductCoins(balanceRef: com.google.firebase.database.DatabaseReference, amount: Long): Long =
        suspendCancellableCoroutine { cont ->
            balanceRef.runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val v = parseLong(currentData.value) ?: 0L
                    if (v < amount) {
                        return Transaction.abort()
                    }
                    currentData.value = v - amount
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
                                IllegalStateException("Not enough coins for this gift."),
                            )
                        else -> cont.resume(parseLong(currentData?.value) ?: 0L)
                    }
                }
            })
        }

    private suspend fun addCoins(balanceRef: com.google.firebase.database.DatabaseReference, amount: Long): Long =
        suspendCancellableCoroutine { cont ->
            balanceRef.runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val v = parseLong(currentData.value) ?: 0L
                    currentData.value = v + amount
                    return Transaction.success(currentData)
                }

                override fun onComplete(
                    error: DatabaseError?,
                    committed: Boolean,
                    currentData: DataSnapshot?,
                ) {
                    when {
                        error != null -> cont.resumeWithException(error.toException())
                        !committed -> cont.resumeWithException(IllegalStateException("Refund failed"))
                        else -> cont.resume(parseLong(currentData?.value) ?: 0L)
                    }
                }
            })
        }

    override fun observeGiftEvents(context: GiftSendContext): Flow<GiftEvent> = callbackFlow {
        val ref = database.reference
            .child("giftEvents")
            .child(context.rtdbSegment)
            .child(context.scopeId)
        var previousKeys = emptySet<String>()
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentKeys = snapshot.children.mapNotNull { it.key }.toSet()
                val added = currentKeys - previousKeys
                previousKeys = currentKeys
                for (key in added) {
                    val child = snapshot.child(key)
                    val ev = parseGiftEvent(context, child) ?: continue
                    trySend(ev)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    private fun parseGiftEvent(context: GiftSendContext, snapshot: DataSnapshot): GiftEvent? {
        val id = snapshot.key ?: return null
        val from = snapshot.child("fromUserId").getValue(String::class.java) ?: return null
        val giftId = snapshot.child("giftId").getValue(String::class.java) ?: return null
        val selectedCount = parseLong(snapshot.child("selectedCount").value)?.toInt()?.coerceAtLeast(1) ?: 1
        val coins = parseLong(snapshot.child("coins").value) ?: return null
        val to = snapshot.child("toUserId").getValue(String::class.java)?.takeIf { it.isNotBlank() }
        val createdAt = parseLong(snapshot.child("createdAt").value)
        val receiverCoins = parseLong(snapshot.child("receiverCoins").value) ?: 0L
        val receiverSoul = parseLong(snapshot.child("receiverSoul").value) ?: 0L
        return GiftEvent(
            eventId = id,
            context = context,
            fromUserId = from,
            toUserId = to,
            giftId = giftId,
            selectedCount = selectedCount,
            coins = coins,
            receiverCoins = receiverCoins,
            receiverSoul = receiverSoul,
            createdAt = createdAt,
        )
    }

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
