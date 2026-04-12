package com.souljoy.soulmasti.data.billing

import java.security.MessageDigest

internal object PurchaseTokenHasher {
    fun sha256Hex(token: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(token.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { b -> "%02x".format(b) }
    }
}
