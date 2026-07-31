package com.ai.operator.service.floating

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Binder
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.ai.operator.core.agent.AgentState
import com.ai.operator.feature.floating.FloatingBubbleComponent
import com.ai.operator.feature.floating.FloatingPanelComponent

class FloatingOperatorService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private var windowManager: WindowManager? = null
    private var composeBubbleView: ComposeView? = null
    private var composePanelView: ComposeView? = null

    private val agentState = mutableStateOf(AgentState.IDLE)
    private val currentApp = mutableStateOf("Home Screen")

    private val binder = LocalBinder()

    // Architecture-specific requirements to host Compose in a Service
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val viewModelStore = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = viewModelStore
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    inner class LocalBinder : Binder() {
        fun getService(): FloatingOperatorService = this@FloatingOperatorService
    }

    override fun onCreate() {
        super.onCreate()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        savedStateRegistryController.performRestore(null)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    fun showFloatingBubble() {
        if (composeBubbleView != null) return

        composeBubbleView = ComposeView(this).apply {
            setContent {
                FloatingBubbleComponent(
                    agentState = agentState.value,
                    onClick = { toggleFloatingPanel() },
                    onDrag = { _, _ -> /* Snap or offset updates handled in Component */ }
                )
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 100
        }

        composeBubbleView?.let { view ->
            view.setViewTreeLifecycleOwner(this)
            view.setViewTreeViewModelStoreOwner(this)
            view.setViewTreeSavedStateRegistryOwner(this)
            windowManager?.addView(view, params)
        }

        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
    }

    fun updateAgentState(state: AgentState) {
        agentState.value = state
    }

    fun updateCurrentApp(appName: String) {
        currentApp.value = appName
    }

    private fun toggleFloatingPanel() {
        if (composePanelView != null) {
            hideFloatingPanel()
        } else {
            showFloatingPanel()
        }
    }

    private fun showFloatingPanel() {
        composePanelView = ComposeView(this).apply {
            setContent {
                FloatingPanelComponent(
                    agentState = agentState.value,
                    currentApp = currentApp.value,
                    onClose = { hideFloatingPanel() },
                    onSpeak = { /* Trigger TTS or Speech recognition */ },
                    onAnalyzeScreen = { /* Trigger vision logic */ },
                    onStartOperator = { updateAgentState(AgentState.THINKING) },
                    onStopOperator = { updateAgentState(AgentState.STOPPED) }
                )
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        composePanelView?.let { view ->
            view.setViewTreeLifecycleOwner(this)
            view.setViewTreeViewModelStoreOwner(this)
            view.setViewTreeSavedStateRegistryOwner(this)
            windowManager?.addView(view, params)
        }
    }

    private fun hideFloatingPanel() {
        composePanelView?.let {
            windowManager?.removeView(it)
            composePanelView = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        composeBubbleView?.let {
            windowManager?.removeView(it)
            composeBubbleView = null
        }
        hideFloatingPanel()
    }
}
