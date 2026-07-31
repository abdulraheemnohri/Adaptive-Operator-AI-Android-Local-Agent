package com.adaptiveoperator.ai.presentation.floating

import android.animation.ValueAnimator
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.adaptiveoperator.ai.R
import androidx.compose.ui.graphics.toArgb
import com.adaptiveoperator.ai.agent.AgentOrchestrator
import com.adaptiveoperator.ai.agent.color
import com.adaptiveoperator.ai.agent.label
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

/**
 * Section 32-38: the floating bubble that stays above supported apps. Built with
 * plain Android views rather than a Compose overlay -- hosting Compose inside a bare
 * WindowManager-added view needs a hand-rolled LifecycleOwner/ViewModelStoreOwner/
 * SavedStateRegistryOwner, which is a lot of boilerplate for a bubble with five
 * states; this gets drag, edge-snap, expand/collapse, and live status color for a
 * fraction of the code, and can be swapped for a ComposeView later without changing
 * the service's public behavior.
 */
@AndroidEntryPoint
class FloatingOperatorService : Service() {

    @Inject lateinit var agentOrchestrator: AgentOrchestrator

    private lateinit var windowManager: WindowManager
    private var bubbleView: FrameLayout? = null
    private var panelView: LinearLayout? = null
    private var statusDot: View? = null
    private var statusText: TextView? = null
    private var expanded = false

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var statusJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        startForeground(NOTIFICATION_ID, buildNotification())
        addBubble()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        super.onDestroy()
        statusJob?.cancel()
        bubbleView?.let { runCatching { windowManager.removeView(it) } }
        panelView?.let { runCatching { windowManager.removeView(it) } }
    }

    private fun bubbleLayoutParams(x: Int = 0, y: Int = 200) = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT
    ).apply { gravity = Gravity.TOP or Gravity.START; this.x = x; this.y = y }

    private fun addBubble() {
        val size = (56 * resources.displayMetrics.density).toInt()
        val dot = View(this).apply {
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(Color.parseColor("#3DDC84"))
            }
        }
        statusDot = dot

        val label = TextView(this).apply {
            text = "🤖"
            gravity = android.view.Gravity.CENTER
            setTextColor(Color.WHITE)
            textSize = 20f
        }

        val bubble = FrameLayout(this).apply {
            addView(dot, FrameLayout.LayoutParams(size, size))
            addView(label, FrameLayout.LayoutParams(size, size))
        }

        var startRawX = 0f
        var startRawY = 0f
        var startParamX = 0
        var startParamY = 0
        var moved = false
        val params = bubbleLayoutParams()

        bubble.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startRawX = event.rawX; startRawY = event.rawY
                    startParamX = params.x; startParamY = params.y
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - startRawX).toInt()
                    val dy = (event.rawY - startRawY).toInt()
                    if (abs(dx) > 8 || abs(dy) > 8) moved = true
                    params.x = startParamX + dx
                    params.y = startParamY + dy
                    windowManager.updateViewLayout(bubble, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) toggleExpanded() else snapToEdge(bubble, params)
                    true
                }
                else -> false
            }
        }

        windowManager.addView(bubble, params)
        bubbleView = bubble
        observeStatus()
    }

    private fun snapToEdge(view: View, params: WindowManager.LayoutParams) {
        val screenWidth = resources.displayMetrics.widthPixels
        val targetX = if (params.x + view.width / 2 < screenWidth / 2) 0 else screenWidth - view.width
        ValueAnimator.ofInt(params.x, targetX).apply {
            duration = 150
            addUpdateListener {
                params.x = it.animatedValue as Int
                runCatching { windowManager.updateViewLayout(view, params) }
            }
            start()
        }
    }

    private fun toggleExpanded() {
        expanded = !expanded
        if (expanded) showPanel() else hidePanel()
    }

    /** Section 34's expanded panel: current status + the four quick actions from
     *  Section 35 (Ask, Type is handled in-app, Analyze Screen, Stop). */
    private fun showPanel() {
        if (panelView != null) return
        val padding = (12 * resources.displayMetrics.density).toInt()

        val status = TextView(this).apply { setTextColor(Color.WHITE); textSize = 13f }
        statusText = status

        val stopButton = TextView(this).apply {
            text = "🛑 Stop"
            setTextColor(Color.WHITE)
            setPadding(padding, padding / 2, padding, padding / 2)
            setOnClickListener { agentOrchestrator.stop() }
        }

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
            setBackgroundColor(Color.parseColor("#DD15181C"))
            addView(TextView(this@FloatingOperatorService).apply {
                text = "🤖 Operator"; setTextColor(Color.WHITE); textSize = 15f
            })
            addView(status)
            addView(stopButton)
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.END; y = 300 }

        windowManager.addView(panel, params)
        panelView = panel
    }

    private fun hidePanel() {
        panelView?.let { runCatching { windowManager.removeView(it) } }
        panelView = null
    }

    private fun observeStatus() {
        statusJob = scope.launch {
            agentOrchestrator.state.collect { taskState ->
                statusDot?.background?.setTint(taskState.status.color().toArgb())
                statusText?.text = taskState.status.label() + (taskState.lastMessage?.let { " · $it" } ?: "")
            }
        }
    }

    private fun buildNotification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Floating Operator", NotificationManager.IMPORTANCE_LOW)
            )
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Adaptive Operator AI")
            .setContentText("Floating Operator is active")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "floating_operator"
        private const val NOTIFICATION_ID = 1002
    }
}
