package com.addie.sundayleague.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.addie.sundayleague.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MatchTimerService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob())
    private var tickJob: Job? = null
    private var endTimeMillis: Long = 0L
    private var mediaPlayer: MediaPlayer? = null
    private var lastKnownMinutes = 10

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val matchId = intent.getStringExtra(EXTRA_MATCH_ID) ?: return START_NOT_STICKY
                val minutes = intent.getIntExtra(EXTRA_MINUTES, 10)
                startCountdown(matchId, minutes)
            }
            ACTION_PAUSE -> pauseCountdown()
            ACTION_RESUME -> resumeCountdown()
            ACTION_RESET -> {
                val minutes = intent.getIntExtra(EXTRA_MINUTES, lastKnownMinutes)
                resetCountdown(minutes)
            }
            ACTION_STOP -> stopServiceCompletely()
        }
        return START_STICKY
    }

    private fun startCountdown(matchId: String, minutes: Int) {
        lastKnownMinutes = minutes
        val seconds = minutes * 60
        _activeMatchId.value = matchId
        _remainingSeconds.value = seconds
        _timerFinished.value = false
        endTimeMillis = System.currentTimeMillis() + seconds * 1000L
        _isRunning.value = true
        startForeground(NOTIFICATION_ID, buildRunningNotification())
        launchTicker()
    }

    private fun resumeCountdown() {
        if (_isRunning.value) return
        endTimeMillis = System.currentTimeMillis() + _remainingSeconds.value * 1000L
        _isRunning.value = true
        startForeground(NOTIFICATION_ID, buildRunningNotification())
        launchTicker()
    }

    private fun pauseCountdown() {
        _isRunning.value = false
        tickJob?.cancel()
        updateNotification(buildPausedNotification(_remainingSeconds.value))
    }

    private fun resetCountdown(minutes: Int) {
        lastKnownMinutes = minutes
        tickJob?.cancel()
        _isRunning.value = false
        _remainingSeconds.value = minutes * 60
        _timerFinished.value = false
        stopAlarmSound()
        updateNotification(buildPausedNotification(minutes * 60))
    }

    private fun launchTicker() {
        tickJob?.cancel()
        tickJob = serviceScope.launch {
            while (_isRunning.value) {
                val remainingMs = endTimeMillis - System.currentTimeMillis()
                val remainingSec = (remainingMs / 1000L).toInt().coerceAtLeast(0)
                _remainingSeconds.value = remainingSec
                if (remainingSec <= 0) {
                    onCountdownFinished()
                    break
                }
                delay(250)
            }
        }
    }

    private fun onCountdownFinished() {
        _isRunning.value = false
        _timerFinished.value = true
        playAlarmSound()
        updateNotification(buildFinishedNotification())
    }

    private fun playAlarmSound() {
        try {
            val uri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(this@MatchTimerService, uri)
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            // No accessible alarm sound on this device — fail quietly
        }
    }

    private fun stopAlarmSound() {
        mediaPlayer?.let {
            try {
                if (it.isPlaying) it.stop()
                it.release()
            } catch (e: Exception) { /* already released */ }
        }
        mediaPlayer = null
    }

    private fun stopServiceCompletely() {
        tickJob?.cancel()
        stopAlarmSound()
        _isRunning.value = false
        _activeMatchId.value = null
        _timerFinished.value = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        stopAlarmSound()
        super.onDestroy()
    }

    // --- Notifications ---

    private fun buildRunningNotification(): Notification {
        ensureChannel()
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Match in progress")
            .setContentText("Tap to return to the live match")
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setWhen(endTimeMillis)
            .setContentIntent(openAppIntent())
            .build()
    }

    private fun buildPausedNotification(seconds: Int): Notification {
        ensureChannel()
        val minutes = seconds / 60
        val secs = seconds % 60
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Match paused")
            .setContentText("%02d:%02d remaining".format(minutes, secs))
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setUsesChronometer(false)
            .setContentIntent(openAppIntent())
            .build()
    }

    private fun buildFinishedNotification(): Notification {
        ensureChannel()
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Time's up!")
            .setContentText("Tap to open the app and finish the match")
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setOngoing(true)
            .setOnlyAlertOnce(false)
            .setUsesChronometer(false)
            .setContentIntent(openAppIntent())
            .build()
    }

    private fun updateNotification(notification: Notification) {
        getSystemService(NotificationManager::class.java)?.notify(NOTIFICATION_ID, notification)
    }

    private fun openAppIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Match Timer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows the live countdown for the current match"
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "match_timer_channel"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.addie.sundayleague.action.START"
        const val ACTION_PAUSE = "com.addie.sundayleague.action.PAUSE"
        const val ACTION_RESUME = "com.addie.sundayleague.action.RESUME"
        const val ACTION_RESET = "com.addie.sundayleague.action.RESET"
        const val ACTION_STOP = "com.addie.sundayleague.action.STOP"

        const val EXTRA_MATCH_ID = "extra_match_id"
        const val EXTRA_MINUTES = "extra_minutes"

        private val _remainingSeconds = MutableStateFlow(0)
        val remainingSeconds: StateFlow<Int> = _remainingSeconds

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning

        private val _activeMatchId = MutableStateFlow<String?>(null)
        val activeMatchId: StateFlow<String?> = _activeMatchId

        private val _timerFinished = MutableStateFlow(false)
        val timerFinished: StateFlow<Boolean> = _timerFinished

        fun start(context: Context, matchId: String, minutes: Int) {
            val intent = Intent(context, MatchTimerService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_MATCH_ID, matchId)
                putExtra(EXTRA_MINUTES, minutes)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun pause(context: Context) {
            context.startService(Intent(context, MatchTimerService::class.java).apply {
                action = ACTION_PAUSE
            })
        }

        fun resume(context: Context) {
            context.startService(Intent(context, MatchTimerService::class.java).apply {
                action = ACTION_RESUME
            })
        }

        fun reset(context: Context, minutes: Int) {
            context.startService(Intent(context, MatchTimerService::class.java).apply {
                action = ACTION_RESET
                putExtra(EXTRA_MINUTES, minutes)
            })
        }

        fun stop(context: Context) {
            context.startService(Intent(context, MatchTimerService::class.java).apply {
                action = ACTION_STOP
            })
        }
    }
}