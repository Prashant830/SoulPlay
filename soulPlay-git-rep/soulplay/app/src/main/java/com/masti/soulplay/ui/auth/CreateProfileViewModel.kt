package com.masti.soulplay.ui.auth

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.masti.soulplay.data.firebase.ProfilePhotoStorage
import com.masti.soulplay.data.firebase.profilePhotoUploadMessage
import kotlinx.coroutines.tasks.await

class CreateProfileViewModel(
    private val app: Application,
    private val auth: FirebaseAuth,
    private val database: FirebaseDatabase,
    private val storage: FirebaseStorage,
) : ViewModel() {

    /** When no photo is chosen, stored URL is empty and the UI should show the app mascot drawable. */
    suspend fun createProfile(
        username: String,
        gender: String,
        profilePictureUri: Uri?,
    ) {
        val uid = auth.currentUser?.uid ?: error("No user signed in")
        val photoUrl = if (profilePictureUri != null) {
            runCatching {
                ProfilePhotoStorage.uploadAvatarAndGetDownloadUrl(
                    storage = storage,
                    uid = uid,
                    uri = profilePictureUri,
                    contentResolver = app.contentResolver,
                )
            }.getOrElse { e ->
                error(e.profilePhotoUploadMessage())
            }
        } else {
            ""
        }
        val userRef = database.reference.child("users").child(uid)
        userRef.child("username").setValue(username).await()
        userRef.child("gender").setValue(gender).await()
        userRef.child("profilePictureUrl").setValue(photoUrl).await()
        userRef.child("profileCreated").setValue(true).await()
    }
}
