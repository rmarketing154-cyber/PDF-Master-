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

    /**
     * Plays a standard light keyboard/button click sound effect using the system AudioManager.
     * This respects the user's system sound settings and volume.
     */
    fun playClick(context: Context) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.playSoundEffect(AudioManager.FX_KEY_CLICK, 0.6f)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play click sound effect", e)
        }
    }

    /**
     * Plays a pleasant success confirmation tone using ToneGenerator.
     * Generates a high-pitched double chirp (pleasant confirmation).
     */
    fun playSuccess(context: Context) {
        try {
            CoroutineScope(Dispatchers.IO).launch {
                var toneGen: ToneGenerator? = null
                try {
                    toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 75)
                    // High pitch pleasant beep 1
                    toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 120)
                    delay(150)
                    // High pitch pleasant beep 2
                    toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 80)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in success tone thread", e)
                } finally {
                    delay(200)
                    toneGen?.release()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play success sound", e)
        }
    }

    /**
     * Plays an error alert buzzer sound using ToneGenerator.
     * Generates a deeper double buzz signaling an unsuccessful action.
     */
    fun playError(context: Context) {
        try {
            CoroutineScope(Dispatchers.IO).launch {
                var toneGen: ToneGenerator? = null
                try {
                    toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 80)
                    // Low error buzz
                    toneGen.startTone(ToneGenerator.TONE_PROP_NACK, 250)
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
        try {
            CoroutineScope(Dispatchers.IO).launch {
                var toneGen: ToneGenerator? = null
                try {
                    toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 85)
                    // Chime note 1 (medium high)
                    toneGen.startTone(ToneGenerator.TONE_DTMF_5, 100)
                    delay(120)
                    // Chime note 2 (higher)
                    toneGen.startTone(ToneGenerator.TONE_DTMF_9, 100)
                    delay(120)
                    // Chime note 3 (highest, triumphant)
                    toneGen.startTone(ToneGenerator.TONE_DTMF_D, 200)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in celebration tone thread", e)
                } finally {
                    delay(300)
                    toneGen?.release()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play reward unlocked celebration", e)
        }
    }
}
