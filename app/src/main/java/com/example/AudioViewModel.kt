package com.example

import android.app.Application
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AudioViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "AudioViewModel"
    private val context: Context = application.applicationContext
    private val routingHelper = AudioRoutingManager.getHelper(context)

    // Exposed states from AudioRoutingHelper
    val isSpeakerForceEnabled: StateFlow<Boolean> = routingHelper.isSpeakerForceEnabled
    val isHeadphoneConnected: StateFlow<Boolean> = routingHelper.isHeadphoneConnected
    val isServiceRunning: StateFlow<Boolean> = AudioRoutingManager.isServiceRunning
    val useCallStream: StateFlow<Boolean> = routingHelper.useCallStream

    // Tone generator state
    private val _isTestSoundPlaying = MutableStateFlow(false)
    val isTestSoundPlaying: StateFlow<Boolean> = _isTestSoundPlaying.asStateFlow()

    private var toneGenerator: ToneGenerator? = null
    private var toneThread: Thread? = null
    private var isPlayingActive = false

    init {
        Log.d(TAG, "AudioViewModel initialized")
    }

    fun toggleSpeakerForce() {
        val nextState = !isSpeakerForceEnabled.value
        Log.d(TAG, "UI Action - toggleSpeakerForce from ${isSpeakerForceEnabled.value} to $nextState")
        routingHelper.forceSpeaker(nextState)

        // Automatically start background protection service
        if (nextState && !isServiceRunning.value) {
            Log.d(TAG, "Forced Speaker enabled: automatically starting persistence service")
            AudioForegroundService.startService(context)
        }
    }

    fun toggleRoutingStream() {
        val nextState = !useCallStream.value
        Log.d(TAG, "UI Action - toggleRoutingStream to: $nextState")
        routingHelper.setUseCallStream(nextState)
    }

    fun resetToDefault() {
        Log.d(TAG, "UI Action - resetToDefault()")
        routingHelper.resetToDefault()
        if (isServiceRunning.value) {
            Log.d(TAG, "Resetting routing: stopping background service")
            AudioForegroundService.stopService(context)
        }
    }

    fun toggleForegroundService() {
        val running = isServiceRunning.value
        Log.d(TAG, "UI Action - toggleForegroundService. Current: $running")
        if (running) {
            AudioForegroundService.stopService(context)
        } else {
            AudioForegroundService.startService(context)
        }
    }

    fun playTestSound() {
        Log.d(TAG, "UI Action - playTestSound()")
        if (_isTestSoundPlaying.value) {
            stopTestSound()
            return
        }

        try {
            stopTestSound()
            _isTestSoundPlaying.value = true
            isPlayingActive = true

            // Initialize ToneGenerator on STREAM_VOICE_CALL stream for forced speaker compliance
            val streamType = if (isSpeakerForceEnabled.value) {
                AudioManager.STREAM_VOICE_CALL
            } else {
                AudioManager.STREAM_MUSIC
            }

            toneGenerator = ToneGenerator(streamType, 100) // Max Volume (100)

            toneThread = Thread {
                try {
                    Log.d(TAG, "ToneGenerator starting 3-second diagnostic pulse...")
                    
                    // Generate a high-priority PIP tone (3 seconds duration loop)
                    toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 3000)
                    
                    // Keep state active for the duration of the tone
                    var elapsed = 0
                    while (isPlayingActive && elapsed < 3000) {
                        Thread.sleep(100)
                        elapsed += 100
                    }
                } catch (e: InterruptedException) {
                    Log.d(TAG, "ToneThread interrupted")
                } catch (e: Exception) {
                    Log.e(TAG, "Exception in ToneThread run", e)
                } finally {
                    _isTestSoundPlaying.value = false
                    cleanupTone()
                }
            }.apply { start() }

        } catch (e: Exception) {
            Log.e(TAG, "Exception playing test sound tone", e)
            _isTestSoundPlaying.value = false
            cleanupTone()
        }
    }

    fun stopTestSound() {
        Log.d(TAG, "Stopping test sound")
        isPlayingActive = false
        _isTestSoundPlaying.value = false
        try {
            toneGenerator?.stopTone()
        } catch (e: Exception) {
            Log.e(TAG, "Exception while stopping tone", e)
        } finally {
            cleanupTone()
        }
    }

    private fun cleanupTone() {
        toneThread?.interrupt()
        toneThread = null
        try {
            toneGenerator?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Exception releasing ToneGenerator", e)
        }
        toneGenerator = null
        Log.d(TAG, "ToneGenerator resources fully cleaned up")
    }

    override fun onCleared() {
        Log.d(TAG, "onCleared() - cleaning up ViewModel resources")
        cleanupTone()
        super.onCleared()
    }
}
