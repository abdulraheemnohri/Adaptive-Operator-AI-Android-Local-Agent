package com.adaptiveoperator.ai.android.capture

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.adaptiveoperator.ai.R

/**
 * Android requires an active foreground service of type `mediaProjection` for the
 * whole time a MediaProjection session is alive (Section 16). This service's only job
 * is satisfying that requirement with a visible, honest notification -- it does not
 * itself own the projection or perform capture; ScreenCaptureManager does that once
 * this service confirms it's running. Started only right before a capture and stopped
 * immediately after (or when Emergency Stop trips), matching the "capture-on-demand"
 * principle in Section 16.
 */
class ScreenCaptureService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        return START_NOT_STICKY
    }

    private fun buildNotification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Operator screen capture", NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Shown only while the operator is reading the screen to plan its next step." }
            manager.createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Adaptive Operator AI")
            .setContentText("Reading the screen to decide the next action")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "screen_capture"
        private const val NOTIFICATION_ID = 1001
    }
}
