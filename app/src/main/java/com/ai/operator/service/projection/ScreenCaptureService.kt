package com.ai.operator.service.projection

import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.media.projection.MediaProjection
import android.os.Binder
import android.os.IBinder

class ScreenCaptureService : Service() {

    private var mediaProjection: MediaProjection? = null
    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): ScreenCaptureService = this@ScreenCaptureService
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    fun setMediaProjection(projection: MediaProjection?) {
        this.mediaProjection = projection
    }

    /**
     * Captured screens are preprocessed inside this on-demand pipeline as defined in DESIGN.md.
     * Conserves device battery and RAM by avoiding continuous 30fps screen captures.
     */
    fun captureAndPreprocessFrame(
        rawBitmap: Bitmap?,
        targetWidth: Int = 512,
        targetHeight: Int = 512,
        cropSystemBars: Boolean = true
    ): Bitmap? {
        if (rawBitmap == null) return null

        try {
            val width = rawBitmap.width
            val height = rawBitmap.height

            // Crop system bars (typically top 5% and bottom 10% of display height)
            val cropRect = if (cropSystemBars) {
                val topCrop = (height * 0.05).toInt()
                val bottomCrop = (height * 0.90).toInt()
                Rect(0, topCrop, width, bottomCrop)
            } else {
                Rect(0, 0, width, height)
            }

            // Create cropped bitmap
            val croppedBitmap = Bitmap.createBitmap(
                rawBitmap,
                cropRect.left,
                cropRect.top,
                cropRect.width(),
                cropRect.height()
            )

            // Scaled/resized downsample to match visual token resolution limits
            val finalBitmap = Bitmap.createScaledBitmap(croppedBitmap, targetWidth, targetHeight, true)

            // Aggressive memory recycling post-tokenization
            if (croppedBitmap != rawBitmap) {
                croppedBitmap.recycle()
            }
            rawBitmap.recycle() // Recycle the raw heavy capture instantly

            return finalBitmap
        } catch (e: Exception) {
            return null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaProjection?.stop()
        mediaProjection = null
    }
}
