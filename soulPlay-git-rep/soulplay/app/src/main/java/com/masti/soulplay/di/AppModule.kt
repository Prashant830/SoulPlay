package com.masti.soulplay.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.masti.soulplay.data.firebase.FirebaseGameSessionRepository
import com.masti.soulplay.data.firebase.FirebaseGiftRepository
import com.masti.soulplay.data.firebase.FirestoreSocialRepository
import com.masti.soulplay.data.voice.AgoraVoiceRoomRepository
import com.masti.soulplay.domain.repository.GameSessionRepository
import com.masti.soulplay.domain.repository.GiftRepository
import com.masti.soulplay.domain.repository.SocialRepository
import com.masti.soulplay.domain.repository.VoiceRoomRepository
import com.masti.soulplay.ui.auth.AuthGateViewModel
import com.masti.soulplay.ui.auth.CreateProfileViewModel
import com.masti.soulplay.ui.auth.LoginViewModel
import com.masti.soulplay.ui.home.HomeViewModel
import com.masti.soulplay.ui.chat.ChatThreadViewModel
import com.masti.soulplay.ui.chat.ChatViewModel
import com.masti.soulplay.ui.settings.SettingsViewModel
import com.masti.soulplay.ui.voiceroom.VoiceRoomViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { FirebaseAuth.getInstance() }
    single { FirebaseDatabase.getInstance() }
    single { FirebaseFirestore.getInstance() }
    single { FirebaseStorage.getInstance() }
    single<GameSessionRepository> { FirebaseGameSessionRepository(get(), get()) }
    single<GiftRepository> { FirebaseGiftRepository(get(), get()) }
    single<SocialRepository> { FirestoreSocialRepository(get(), get()) }
    single<VoiceRoomRepository> { AgoraVoiceRoomRepository(androidContext()) }
    viewModel { HomeViewModel(androidApplication(), get()) }
    viewModel { AuthGateViewModel(get(), get()) }
    viewModel { LoginViewModel(get()) }
    viewModel { CreateProfileViewModel(androidApplication(), get(), get(), get()) }
    viewModel { SettingsViewModel(androidApplication(), get(), get(), get(), get(), get()) }
    viewModel { ChatViewModel(androidApplication(), get(), get()) }
    viewModel { params ->
        ChatThreadViewModel(
            androidApplication(),
            get(),
            get(),
            get(),
            get(),
            peerUid = params.get(),
        )
    }
    viewModel { params ->
        VoiceRoomViewModel(
            androidApplication(),
            get(),
            get(),
            get(),
            get(),
            get(),
            roomId = params.get(),
        )
    }
}
