package com.adaptiveoperator.ai.android.capture

import android.content.Context
import android.graphics.Bitmap
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.util.DisplayMetrics
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Section 16: capture-on-demand, never a continuous stream.
 *   action -> wait -> capture -> analyze
 * instead of a fixed-rate frame loop. [captureOnce] tears its VirtualDisplay/ImageReader
 * down immediately after grabbing one frame, so there is no background capture running
 * between agent steps -- this is what Section 44's "Screen Capture: Available" (rather
 * than "Active") status in the Security Center is describing.
 *
 * [projection] must come from an Activity's ACTION_SCREEN_CAPTURE result; this class
 * only knows how to use a projection token once the user has already granted it for
 * this session (Android re-prompts every session by design -- there is no way around
 * that at the platform level, and V1 should not try to work around it).
 */
@Singleton
class ScreenCaptureManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var projection: MediaProjection? = null

    fun attachProjection(mediaProjection: MediaProjection) {
        projection = mediaProjection
    }

    fun releaseProjection() {
        projection?.stop()
        projection = null
    }

    val isAvailable: Boolean get() = projection != null

    suspend fun captureOnce(): Bitmap? {
        val proj = projection ?: return null
        val metrics = DisplayMetrics().also {
            (context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager)
                .defaultDisplay.getRealMetrics(it)
        }
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        val imageReader = ImageReader.newInstance(width, height, android.graphics.PixelFormat.RGBA_8888, 2)
        var virtualDisplay: VirtualDisplay? = null

        return try {
            suspendCancellableCoroutine { cont ->
                imageReader.setOnImageAvailableListener({ reader ->
                    val image: Image? = reader.acquireLatestImage()
                    val bitmap = image?.let { toBitmap(it, width, height) }
                    image?.close()
                    if (cont.isActive) cont.resume(bitmap)
                }, null)

                virtualDisplay = proj.createVirtualDisplay(
                    "adaptive-operator-capture",
                    width, height, density,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    imageReader.surface, null, null
                )

                cont.invokeOnCancellation {
                    virtualDisplay?.release()
                    imageReader.close()
                }
            }
        } finally {
            virtualDisplay?.release()
            imageReader.close()
        }
    }

    private fun toBitmap(image: Image, width: Int, height: Int): Bitmap {
        val plane = image.planes[0]
        val rowPadding = plane.rowStride - plane.pixelStride * width
        val bitmap = Bitmap.createBitmap(
            width + rowPadding / plane.pixelStride, height, Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(plane.buffer)
        return if (rowPadding == 0) bitmap else Bitmap.createBitmap(bitmap, 0, 0, width, height)
    }
}
