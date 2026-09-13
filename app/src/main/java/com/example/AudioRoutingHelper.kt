package com.example

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AudioRoutingHelper(private val context: Context) {
    private val TAG = "AudioRoutingHelper"
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _isSpeakerForceEnabled = MutableStateFlow(false)
    val isSpeakerForceEnabled: StateFlow<Boolean> = _isSpeakerForceEnabled.asStateFlow()

    private val _isHeadphoneConnected = MutableStateFlow(false)
    val isHeadphoneConnected: StateFlow<Boolean> = _isHeadphoneConnected.asStateFlow()

    private val _useCallStream = MutableStateFlow(true)
    val useCallStream: StateFlow<Boolean> = _useCallStream.asStateFlow()

    private var silentTrack: AudioTrack? = null

    // Highly reactive hardware listener that automatically handles physical plugs/unplugs
    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            Log.d(TAG, "Audio devices added event received")
            updateHeadphoneConnectionStatus()
            if (_isSpeakerForceEnabled.value) {
                applySpeakerRoutingInternal()
            }
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            Log.d(TAG, "Audio devices removed event received")
            updateHeadphoneConnectionStatus()
            if (_isSpeakerForceEnabled.value) {
                applySpeakerRoutingInternal()
            }
        }
    }

    init {
        updateHeadphoneConnectionStatus()
    }

    fun startTracking() {
        Log.d(TAG, "Registering AudioDeviceCallback for hardware tracking")
        try {
            audioManager.registerAudioDeviceCallback(audioDeviceCallback, mainHandler)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register AudioDeviceCallback", e)
        }
        updateHeadphoneConnectionStatus()
    }

    fun stopTracking() {
        Log.d(TAG, "Unregistering AudioDeviceCallback")
        stopSilentAudioLoop()
        try {
            audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unregister AudioDeviceCallback", e)
        }
    }

    fun setUseCallStream(enable: Boolean) {
        _useCallStream.value = enable
        if (_isSpeakerForceEnabled.value) {
            applySpeakerRoutingInternal()
        }
    }

    fun updateHeadphoneConnectionStatus() {
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        var connected = false
        for (device in devices) {
            val type = device.type
            if (type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                type == AudioDeviceInfo.TYPE_USB_HEADSET) {
                connected = true
                Log.d(TAG, "Headphones/Headset active: ${device.productName} (Type ID: $type)")
                break
            }
        }
        _isHeadphoneConnected.value = connected
    }

    fun forceSpeaker(enable: Boolean) {
        Log.d(TAG, "Routing trigger: forceSpeaker($enable)")
        if (enable) {
            _isSpeakerForceEnabled.value = true
            applySpeakerRoutingInternal()
            startSilentAudioLoop()
        } else {
            _isSpeakerForceEnabled.value = false
            stopSilentAudioLoop()
            applyHeadphoneRouting()
        }
    }

    fun resetToDefault() {
        Log.d(TAG, "Routing trigger: resetToDefault()")
        _isSpeakerForceEnabled.value = false
        stopSilentAudioLoop()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                audioManager.clearCommunicationDevice()
                audioManager.isSpeakerphoneOn = false
                audioManager.mode = AudioManager.MODE_NORMAL
                Log.d(TAG, "API 31+: Cleared communication device, speakerphone = false, mode set to MODE_NORMAL")
            } catch (e: Exception) {
                Log.e(TAG, "API 31+: Error in clearCommunicationDevice", e)
            }
        } else {
            try {
                audioManager.isSpeakerphoneOn = false
                audioManager.mode = AudioManager.MODE_NORMAL
                Log.d(TAG, "Legacy: Set isSpeakerphoneOn = false, mode set to MODE_NORMAL")
            } catch (e: Exception) {
                Log.e(TAG, "Legacy: Error in legacy resetToDefault", e)
            }
        }
    }

    private fun startSilentAudioLoop() {
        try {
            stopSilentAudioLoop()
            Log.d(TAG, "Starting ultra-low power silent loop to hold OS communication channel active...")
            val sampleRate = 8000
            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val buffer = ShortArray(minBufferSize) // Automatically filled with 0s (silence)

            silentTrack = AudioTrack(
                AudioManager.STREAM_VOICE_CALL,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                buffer.size * 2,
                AudioTrack.MODE_STATIC
            )

            silentTrack?.let { track ->
                track.write(buffer, 0, buffer.size)
                track.setLoopPoints(0, buffer.size, -1) // Loop infinitely
                track.play()
                Log.d(TAG, "Silent loop started successfully.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start silent loop", e)
        }
    }

    private fun stopSilentAudioLoop() {
        try {
            silentTrack?.let { track ->
                if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    track.stop()
                }
                track.release()
                Log.d(TAG, "Silent loop stopped and released.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop silent loop", e)
        } finally {
            silentTrack = null
        }
    }

    private fun applySpeakerRoutingInternal() {
        val targetMode = AudioManager.MODE_IN_COMMUNICATION

        try {
            if (audioManager.mode != targetMode) {
                audioManager.mode = targetMode
                Log.d(TAG, "Set audioManager.mode to MODE_IN_COMMUNICATION")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error asserting mode", e)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val devices = audioManager.availableCommunicationDevices
                val speakerDevice = devices.find { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
                if (speakerDevice != null) {
                    val success = audioManager.setCommunicationDevice(speakerDevice)
                    Log.d(TAG, "API 31+ routing set: setCommunicationDevice success = $success")
                } else {
                    Log.w(TAG, "API 31+: Built-in speaker not found in communication devices list")
                    fallbackLegacySpeakerRouting()
                }

                if (!audioManager.isSpeakerphoneOn) {
                    audioManager.isSpeakerphoneOn = true
                    Log.d(TAG, "Set speakerphone parameter to true")
                }
            } catch (e: Exception) {
                Log.e(TAG, "API 31+ routing exception, falling back", e)
                fallbackLegacySpeakerRouting()
            }
        } else {
            fallbackLegacySpeakerRouting()
        }
    }

    private fun fallbackLegacySpeakerRouting() {
        try {
            audioManager.isSpeakerphoneOn = true
            Log.d(TAG, "Legacy Routing: Set isSpeakerphoneOn = true")
        } catch (e: Exception) {
            Log.e(TAG, "Legacy Routing: Exception setting speakerphone on", e)
        }
    }

    private fun applyHeadphoneRouting() {
        _isSpeakerForceEnabled.value = false
        stopSilentAudioLoop()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val devices = audioManager.availableCommunicationDevices
                val headsetDevice = devices.find {
                    it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                    it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                    it.type == AudioDeviceInfo.TYPE_USB_HEADSET
                }
                if (headsetDevice != null) {
                    audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                    val success = audioManager.setCommunicationDevice(headsetDevice)
                    audioManager.isSpeakerphoneOn = false
                    Log.d(TAG, "API 31+: Set communication device to headset success = $success, speakerphone = false")
                } else {
                    Log.w(TAG, "API 31+: No headset found in available devices, clearing communication device")
                    audioManager.clearCommunicationDevice()
                    audioManager.isSpeakerphoneOn = false
                    audioManager.mode = AudioManager.MODE_NORMAL
                }
            } catch (e: Exception) {
                Log.e(TAG, "API 31+: Exception setting communication device to headset", e)
                audioManager.mode = AudioManager.MODE_NORMAL
                audioManager.isSpeakerphoneOn = false
            }
        } else {
            try {
                audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                audioManager.isSpeakerphoneOn = false
                Log.d(TAG, "Legacy Routing: Set MODE_IN_COMMUNICATION and isSpeakerphoneOn = false")
            } catch (e: Exception) {
                Log.e(TAG, "Legacy Routing: Exception setting speakerphone off", e)
            }
        }
    }
}
