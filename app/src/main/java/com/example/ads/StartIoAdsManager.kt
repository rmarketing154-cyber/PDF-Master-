package com.example.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.startapp.sdk.adsbase.Ad
import com.startapp.sdk.adsbase.StartAppAd
import com.startapp.sdk.adsbase.StartAppSDK
import com.startapp.sdk.adsbase.adlisteners.AdEventListener
import com.startapp.sdk.adsbase.VideoListener

object StartIoAdsManager {
    private const val TAG = "StartIoAdsManager"
    private const val APP_ID = "206743399"
    private var isInitialized = false

    private var interstitialAd: StartAppAd? = null
    private var rewardedAd: StartAppAd? = null

    var isInterstitialLoaded = false
        private set

    var isRewardedLoaded = false
        private set

    fun initialize(context: Context) {
        if (isInitialized) return
        try {
            // Initialize SDK
            StartAppSDK.init(context, APP_ID, false)
            // Disable return ads (splash ads on returning to app) for better UX
            StartAppSDK.enableReturnAds(false)
            isInitialized = true
            Log.d(TAG, "Start.io SDK Initialized with App ID $APP_ID")

            // Preload Ads
            preloadInterstitial(context)
            preloadRewarded(context)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Start.io SDK", e)
        }
    }

    fun preloadInterstitial(context: Context) {
        try {
            val ad = StartAppAd(context)
            ad.loadAd(object : AdEventListener {
                override fun onReceiveAd(ad: Ad) {
                    isInterstitialLoaded = true
                    Log.d(TAG, "Interstitial Ad loaded successfully")
                }

                override fun onFailedToReceiveAd(ad: Ad?) {
                    isInterstitialLoaded = false
                    Log.e(TAG, "Failed to load Interstitial Ad: ${ad?.errorMessage}")
                }
            })
            interstitialAd = ad
        } catch (e: Exception) {
            Log.e(TAG, "Error preloading interstitial", e)
        }
    }

    fun preloadRewarded(context: Context) {
        try {
            val ad = StartAppAd(context)
            ad.loadAd(StartAppAd.AdMode.REWARDED_VIDEO, object : AdEventListener {
                override fun onReceiveAd(ad: Ad) {
                    isRewardedLoaded = true
                    Log.d(TAG, "Rewarded Ad loaded successfully")
                }

                override fun onFailedToReceiveAd(ad: Ad?) {
                    isRewardedLoaded = false
                    Log.e(TAG, "Failed to load Rewarded Ad: ${ad?.errorMessage}")
                }
            })
            rewardedAd = ad
        } catch (e: Exception) {
            Log.e(TAG, "Error preloading rewarded", e)
        }
    }

    fun showInterstitialAd(activity: Activity, onDismiss: () -> Unit) {
        try {
            val ad = interstitialAd
            if (ad != null && isInterstitialLoaded) {
                ad.showAd(null as com.startapp.sdk.adsbase.adlisteners.AdDisplayListener?)
                isInterstitialLoaded = false // Reset
                preloadInterstitial(activity) // Preload next
                onDismiss()
            } else {
                Log.d(TAG, "Interstitial not loaded. Executing callback directly.")
                preloadInterstitial(activity) // Retry preloading
                onDismiss()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing Interstitial", e)
            onDismiss()
        }
    }

    fun showRewardedAd(activity: Activity, onRewarded: () -> Unit, onFailed: () -> Unit) {
        try {
            val ad = rewardedAd
            if (ad != null && isRewardedLoaded) {
                ad.setVideoListener(object : VideoListener {
                    override fun onVideoCompleted() {
                        activity.runOnUiThread {
                            onRewarded()
                        }
                    }
                })
                ad.showAd(null as com.startapp.sdk.adsbase.adlisteners.AdDisplayListener?)
                isRewardedLoaded = false // Reset
                preloadRewarded(activity) // Preload next
            } else {
                Log.d(TAG, "Rewarded ad not loaded. Showing fallback dialog or failing gracefully.")
                preloadRewarded(activity) // Retry preloading
                onFailed()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing Rewarded Ad", e)
            onFailed()
        }
    }
}
