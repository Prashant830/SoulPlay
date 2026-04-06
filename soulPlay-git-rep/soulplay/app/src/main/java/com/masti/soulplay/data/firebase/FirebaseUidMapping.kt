package com.masti.soulplay.data.firebase

import java.security.MessageDigest

/**
 * Deterministic mapping so every device joins Agora with the same int uid for a given Firebase uid.
 * Agora expects a non-zero 32-bit signed uid.
 */
object FirebaseUidMapping {
    fun agoraUidFromFirebaseUid(firebaseUid: String): Int {
        val digest = MessageDigest.getInstance("MD5").digest(firebaseUid.toByteArray(Charsets.UTF_8))
        var v = 0
        for (i in 0 until 4) {
            v = (v shl 8) or (digest[i].toInt() and 0xff)
        }
        val positive = v and 0x7fffffff
        return if (positive == 0) 1 else positive
    }

    fun shortLabel(firebaseUid: String): String {
        if (firebaseUid.length <= 6) return firebaseUid
        return "…${firebaseUid.takeLast(5)}"
    }
}
