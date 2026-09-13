package com.example

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class AudioForegroundService : Service() {

    private val TAG = "AudioForegroundService"
    private val NOTIFICATION_ID = 8881
    private val CHANNEL_ID = "audio_routing_channel"

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var routingHelper: AudioRoutingHelper

    companion object {
        const val ACTION_START = "com.example.action.START_SERVICE"
        const val ACTION_STOP = "com.example.action.STOP_SERVICE"
        const val ACTION_FORCE_SPEAKER = "com.example.action.FORCE_SPEAKER"
        const val ACTION_RESET_DEFAULT = "com.example.action.RESET_DEFAULT"
        const val ACTION_FORCE_HEADPHONE = "com.example.action.FORCE_HEADPHONE"

        fun startService(context: Context) {
            val intent = Intent(context, AudioForegroundService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, AudioForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate() - Starting Service")
        AudioRoutingManager.setServiceRunning(true)
        routingHelper = AudioRoutingManager.getHelper(this)

        createNotificationChannel()

        // Observe routing and hardware state changes to dynamically update notification
        routingHelper.isSpeakerForceEnabled.onEach {
            Log.d(TAG, "Speaker force state changed: $it, updating notification")
            updateNotification()
        }.launchIn(serviceScope)

        routingHelper.isHeadphoneConnected.onEach {
            Log.d(TAG, "Headphone hardware connection changed: $it, updating notification")
            updateNotification()
        }.launchIn(serviceScope)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d(TAG, "onStartCommand() - Action: $action")

        when (action) {
            ACTION_START -> {
                startServiceForeground()
            }
            ACTION_FORCE_SPEAKER -> {
                routingHelper.forceSpeaker(true)
            }
            ACTION_FORCE_HEADPHONE -> {
                routingHelper.forceSpeaker(false)
            }
            ACTION_RESET_DEFAULT -> {
                routingHelper.resetToDefault()
            }
            ACTION_STOP -> {
                Log.d(TAG, "Stopping service via ACTION_STOP")
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy() - Cleaning up resources")
        serviceScope.cancel()
        AudioRoutingManager.setServiceRunning(false)
        super.onDestroy()
    }

    private fun startServiceForeground() {
        val notification = buildNotification()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
                Log.d(TAG, "startForeground success (API 29+ mediaPlayback type)")
            } else {
                startForeground(NOTIFICATION_ID, notification)
                Log.d(TAG, "startForeground success (legacy)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in startForeground", e)
        }
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val mainActivityIntent = Intent(this, MainActivity::class.java)
        val mainPendingIntent = PendingIntent.getActivity(
            this,
            0,
            mainActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Force Speaker action button
        val speakerIntent = Intent(this, AudioForegroundService::class.java).apply {
            action = ACTION_FORCE_SPEAKER
        }
        val speakerPendingIntent = PendingIntent.getService(
            this,
            1,
            speakerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Reset to Default action button
        val resetIntent = Intent(this, AudioForegroundService::class.java).apply {
            action = ACTION_RESET_DEFAULT
        }
        val resetPendingIntent = PendingIntent.getService(
            this,
            2,
            resetIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isSpeakerForced = routingHelper.isSpeakerForceEnabled.value
        val isHeadphonePlugged = routingHelper.isHeadphoneConnected.value

        val statusText = when {
            isSpeakerForced -> "Status: Speaker Forced (JACK BYPASSED)"
            isHeadphonePlugged -> "Status: Earphone Mode Active"
            else -> "Status: System Default (Normal)"
        }

        val actionText = if (isSpeakerForced) {
            "Audio routed to built-in Speaker"
        } else {
            "Audio automatically routed by hardware"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Speaker Enabler & Jack Bypass")
            .setContentText(statusText)
            .setSubText(actionText)
            .setSmallIcon(R.drawable.ic_speaker)
            .setContentIntent(mainPendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(
                NotificationCompat.Action.Builder(
                    R.drawable.ic_speaker,
                    "Force Speaker",
                    speakerPendingIntent
                ).build()
            )
            .addAction(
                NotificationCompat.Action.Builder(
                    R.drawable.ic_headset,
                    "Reset to Default",
                    resetPendingIntent
                ).build()
            )
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Audio Routing Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ensures background persistence of loudspeaker audio routing override."
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created")
        }
    }
}
