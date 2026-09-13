package com.example

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class AudioTileService : TileService() {
    private val TAG = "AudioTileService"
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var routingHelper: AudioRoutingHelper

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "TileService onCreate()")
        routingHelper = AudioRoutingManager.getHelper(this)
    }

    override fun onStartListening() {
        super.onStartListening()
        Log.d(TAG, "TileService onStartListening() - active listening")
        
        // Listen to state changes to update the tile dynamically if changed from elsewhere
        routingHelper.isSpeakerForceEnabled.onEach { isForced ->
            updateTileState(isForced)
        }.launchIn(serviceScope)
    }

    override fun onStopListening() {
        Log.d(TAG, "TileService onStopListening()")
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        val helper = routingHelper
        val isCurrentlyForced = helper.isSpeakerForceEnabled.value
        val newForceState = !isCurrentlyForced
        Log.d(TAG, "TileService clicked. Toggling force speaker from $isCurrentlyForced to $newForceState")

        if (newForceState) {
            // Force speaker on, and make sure background service is running to persist it
            helper.forceSpeaker(true)
            AudioForegroundService.startService(this)
        } else {
            // Restore default, and stop service if appropriate
            helper.resetToDefault()
        }
        updateTileState(newForceState)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        Log.d(TAG, "TileService onDestroy()")
        super.onDestroy()
    }

    private fun updateTileState(isForced: Boolean) {
        val tile = qsTile ?: return
        tile.state = if (isForced) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (isForced) "Speaker Forced" else "System Default"
        }
        
        tile.updateTile()
        Log.d(TAG, "Tile updated: state=${tile.state}")
    }
}
