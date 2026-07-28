package com.example.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.startapp.sdk.adsbase.Ad
import com.startapp.sdk.adsbase.StartAppAd
import com.startapp.sdk.adsbase.StartAppSDK
import com.startapp.sdk.adsbase.adlisteners.AdEventListener
import com.startapp.sdk.adsbase.VideoListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
            // Enable test ads for consistent, high-fill ad loading during testing and review
            StartAppSDK.setTestAdsEnabled(true)
            // Disable return ads (splash ads on returning to app) for better UX
            StartAppSDK.enableReturnAds(false)
            isInitialized = true
            Log.d(TAG, "Start.io SDK Initialized with App ID $APP_ID (Test Ads Enabled)")

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
            if (isRewardedLoaded) {
                showLoadedRewarded(activity, onRewarded, onFailed)
            } else {
                Log.d(TAG, "Rewarded ad not loaded yet. Requesting load and waiting...")
                preloadRewarded(activity)
                
                // Smart-wait coroutine loop (up to 3 seconds, checking every 500ms)
                CoroutineScope(Dispatchers.Main).launch {
                    val toast = android.widget.Toast.makeText(activity, "Loading Video Ad...", android.widget.Toast.LENGTH_SHORT)
                    toast.show()
                    
                    var elapsed = 0
                    val timeout = 3000
                    val checkInterval = 500
                    
                    while (!isRewardedLoaded && elapsed < timeout) {
                        delay(checkInterval.toLong())
                        elapsed += checkInterval
                    }
                    
                    toast.cancel()
                    if (isRewardedLoaded) {
                        showLoadedRewarded(activity, onRewarded, onFailed)
                    } else {
                        Log.w(TAG, "Rewarded ad failed to load within timeout. Executing failure callback.")
                        onFailed()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking/showing Rewarded Ad", e)
            onFailed()
        }
    }

    private fun showLoadedRewarded(activity: Activity, onRewarded: () -> Unit, onFailed: () -> Unit) {
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
                onFailed()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing loaded rewarded ad", e)
            onFailed()
        }
    }
}
