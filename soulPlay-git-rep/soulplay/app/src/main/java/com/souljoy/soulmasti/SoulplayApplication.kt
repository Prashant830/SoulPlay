package com.souljoy.soulmasti

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.souljoy.soulmasti.BuildConfig
import com.souljoy.soulmasti.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class SoulplayApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val testDeviceIds = BuildConfig.ADMOB_TEST_DEVICE_IDS
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (testDeviceIds.isNotEmpty()) {
            MobileAds.setRequestConfiguration(
                RequestConfiguration.Builder()
                    .setTestDeviceIds(testDeviceIds)
                    .build(),
            )
        }
        MobileAds.initialize(this)
        startKoin {
            androidContext(this@SoulplayApplication)
            modules(appModule)
        }
    }
}
