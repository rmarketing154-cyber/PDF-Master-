package com.example.ui.components

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object SoundHelper {
    private const val TAG = "SoundHelper"
    
    var isSoundEnabled = true

    /**
     * Plays a standard light keyboard/button click sound effect using the system AudioManager.
     * This respects the user's system sound settings and volume.
     */
    fun playClick(context: Context) {
        if (!isSoundEnabled) return
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.playSoundEffect(AudioManager.FX_KEY_CLICK, 0.6f)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play click sound effect", e)
        }
    }

    /**
     * Plays a pleasant success confirmation tone using ToneGenerator.
     * Generates a premium high-quality ascending triple-chirp chime.
     */
    fun playSuccess(context: Context) {
        if (!isSoundEnabled) return
        try {
            CoroutineScope(Dispatchers.IO).launch {
                var toneGen: ToneGenerator? = null
                try {
                    toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 90)
                    // Beautiful custom chime: brief soft pip followed by crisp ACK confirmation
                    toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 50)
                    delay(70)
                    toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 120)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in success tone thread", e)
                } finally {
                    delay(250)
                    toneGen?.release()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play success sound", e)
        }
    }

    /**
     * Plays an error alert buzzer sound using ToneGenerator.
     * Generates a softer, dual-tone warning signal (not overly harsh, but clear).
     */
    fun playError(context: Context) {
        if (!isSoundEnabled) return
        try {
            CoroutineScope(Dispatchers.IO).launch {
                var toneGen: ToneGenerator? = null
                try {
                    toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 80)
                    // Non-intrusive but clear system warning tone
                    toneGen.startTone(ToneGenerator.TONE_PROP_NACK, 220)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in error tone thread", e)
                } finally {
                    delay(300)
                    toneGen?.release()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play error sound", e)
        }
    }

    /**
     * Plays an ascending celebration chime sequence for premium rewards unlocked.
     */
    fun playRewardUnlocked(context: Context) {
        if (!isSoundEnabled) return
        try {
            CoroutineScope(Dispatchers.IO).launch {
                var toneGen: ToneGenerator? = null
                try {
                    toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 95)
                    // Premium sparkling digital arpeggio
                    toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 40)
                    delay(60)
                    toneGen.startTone(ToneGenerator.TONE_PROP_PROMPT, 60)
                    delay(80)
                    toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 180)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in celebration tone thread", e)
                } finally {
                    delay(400)
                    toneGen?.release()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play reward unlocked celebration", e)
        }
    }
}
