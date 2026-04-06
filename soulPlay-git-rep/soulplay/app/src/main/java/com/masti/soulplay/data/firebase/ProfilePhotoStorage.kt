package com.masti.soulplay.data.firebase

import android.content.ContentResolver
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.tasks.await

object ProfilePhotoStorage {

    suspend fun uploadAvatarAndGetDownloadUrl(
        storage: FirebaseStorage,
        uid: String,
        uri: Uri,
        contentResolver: ContentResolver,
    ): String {
        val ref = storage.reference.child("profile_pictures").child(uid).child("avatar.jpg")
        val mime = contentResolver.getType(uri) ?: "image/jpeg"
        val metadata = StorageMetadata.Builder()
            .setContentType(mime)
            .build()
        ref.putFile(uri, metadata).await()
        return ref.downloadUrl.await().toString()
    }
}

fun Throwable.profilePhotoUploadMessage(): String {
    val storageEx = findStorageException(this)
    if (storageEx != null) {
        val code = storageEx.errorCode
        val hint = when (code) {
            StorageException.ERROR_UNKNOWN ->
                "Storage upload failed due to server rejection. Check Firebase Storage Rules first. " +
                    "If your rules deny all writes, update them to allow authenticated users in " +
                    "profile_pictures/{uid}/."
            StorageException.ERROR_NOT_AUTHORIZED ->
                "Storage blocked this upload. In Firebase Console open Storage → Rules and allow " +
                    "signed-in users to write files under profile_pictures/{userId}/ (match request.auth.uid)."
            StorageException.ERROR_NOT_AUTHENTICATED ->
                "You need to be signed in to upload a photo. Try signing in again."
            StorageException.ERROR_QUOTA_EXCEEDED ->
                "Storage quota exceeded. Check your Firebase plan or free up space."
            StorageException.ERROR_RETRY_LIMIT_EXCEEDED ->
                "Network error. Check your connection and try again."
            else -> null
        }
        val detail = storageEx.message?.takeIf { it.isNotBlank() }
        return buildString {
            if (hint != null) {
                append(hint)
            } else {
                append("Photo upload failed (code ").append(code).append(").")
            }
            if (detail != null && hint == null) {
                append(" ").append(detail)
            }
        }
    }
    return message?.takeIf { it.isNotBlank() } ?: "Photo upload failed. Please try again."
}

private fun findStorageException(t: Throwable): StorageException? {
    var c: Throwable? = t
    while (c != null) {
        if (c is StorageException) return c
        c = c.cause
    }
    return null
}
