package com.example

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AudioRoutingManager {
    private const val TAG = "AudioRoutingManager"
    private var helper: AudioRoutingHelper? = null

    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    fun setServiceRunning(running: Boolean) {
        Log.d(TAG, "Service running state updated to: $running")
        _isServiceRunning.value = running
    }

    fun getHelper(context: Context): AudioRoutingHelper {
        return helper ?: synchronized(this) {
            helper ?: AudioRoutingHelper(context.applicationContext).also {
                helper = it
                it.startTracking()
                Log.d(TAG, "AudioRoutingHelper initialized and started tracking")
            }
        }
    }
}
