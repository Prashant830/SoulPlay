package com.souljoy.soulmasti.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.souljoy.soulmasti.data.billing.CoinPurchaseCoordinator
import com.souljoy.soulmasti.data.billing.PlayBillingRepository
import com.souljoy.soulmasti.data.firebase.FirebaseGameSessionRepository
import com.souljoy.soulmasti.data.firebase.FirebaseGiftRepository
import com.souljoy.soulmasti.data.firebase.FirestoreSocialRepository
import com.souljoy.soulmasti.data.firebase.FirebaseSocialVoiceRoomRepository
import com.souljoy.soulmasti.data.voice.AgoraVoiceRoomRepository
import com.souljoy.soulmasti.domain.repository.GameSessionRepository
import com.souljoy.soulmasti.domain.repository.GiftRepository
import com.souljoy.soulmasti.domain.repository.SocialRepository
import com.souljoy.soulmasti.domain.repository.SocialVoiceRoomRepository
import com.souljoy.soulmasti.domain.repository.VoiceRoomRepository
import com.souljoy.soulmasti.ui.auth.AuthGateViewModel
import com.souljoy.soulmasti.ui.auth.CreateProfileViewModel
import com.souljoy.soulmasti.ui.auth.LoginViewModel
import com.souljoy.soulmasti.ui.home.HomeViewModel
import com.souljoy.soulmasti.ui.chat.ChatThreadViewModel
import com.souljoy.soulmasti.ui.chat.ChatViewModel
import com.souljoy.soulmasti.ui.league.LeagueViewModel
import com.souljoy.soulmasti.ui.league.RewardInboxViewModel
import com.souljoy.soulmasti.ui.rewards.DailyRewardsViewModel
import com.souljoy.soulmasti.ui.settings.SettingsViewModel
import com.souljoy.soulmasti.ui.shop.GoldShopViewModel
import com.souljoy.soulmasti.ui.soulmasti.shopping.SoulMastiShoppingViewModel
import com.souljoy.soulmasti.ui.voice.game.VoiceRoomViewModel
import com.souljoy.soulmasti.ui.voice.social.SocialVoiceRoomsViewModel
import com.souljoy.soulmasti.ui.voice.social.SocialVoiceRoomViewModel
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
    single { PlayBillingRepository(androidApplication()) }
    single { CoinPurchaseCoordinator(get(), get()) }
    single<GiftRepository> { FirebaseGiftRepository(get(), get()) }
    single<SocialRepository> { FirestoreSocialRepository(get(), get()) }
    single<SocialVoiceRoomRepository> { FirebaseSocialVoiceRoomRepository(get(), get()) }
    single<VoiceRoomRepository> { AgoraVoiceRoomRepository(androidContext()) }
    viewModel { HomeViewModel(androidApplication(), get()) }
    viewModel { DailyRewardsViewModel(androidApplication(), get(), get()) }
    viewModel { GoldShopViewModel(get(), get(), get()) }
    viewModel { SoulMastiShoppingViewModel(get(), get()) }
    viewModel { AuthGateViewModel(get(), get()) }
    viewModel { LoginViewModel(get()) }
    viewModel { CreateProfileViewModel(androidApplication(), get(), get(), get()) }
    viewModel { SettingsViewModel(androidApplication(), get(), get(), get(), get(), get()) }
    viewModel { ChatViewModel(androidApplication(), get(), get(), get()) }
    viewModel { LeagueViewModel(androidApplication(), get(), get()) }
    viewModel { RewardInboxViewModel(androidApplication(), get(), get()) }
    viewModel { SocialVoiceRoomsViewModel(androidApplication(), get(), get(), get()) }
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
    viewModel { params ->
        SocialVoiceRoomViewModel(
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
