package com.souljoy.soulmasti.ui.rewards

import android.app.Activity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.souljoy.soulmasti.BuildConfig

object AdMobRewardedAds {
    fun loadAndShow(
        activity: Activity,
        onEarnedReward: (RewardItem) -> Unit,
        onDone: () -> Unit,
        onFailed: (String) -> Unit,
    ) {
        if (BuildConfig.ADMOB_REWARDED_AD_UNIT_ID.isBlank()) {
            onFailed("Missing ADMOB_REWARDED_UNIT_ID in local.properties")
            onDone()
            return
        }
        val request = AdRequest.Builder().build()
        RewardedAd.load(
            activity,
            BuildConfig.ADMOB_REWARDED_AD_UNIT_ID,
            request,
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    onFailed(error.message.ifBlank { "Failed to load ad" })
                    onDone()
                }

                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    rewardedAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            onDone()
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                            onFailed(adError.message.ifBlank { "Failed to show ad" })
                            onDone()
                        }
                    }
                    rewardedAd.show(activity) { rewardItem ->
                        onEarnedReward(rewardItem)
                    }
                }
            },
        )
    }
}

