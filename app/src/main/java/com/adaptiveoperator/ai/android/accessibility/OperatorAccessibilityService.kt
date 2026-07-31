package com.adaptiveoperator.ai.android.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

enum class AccessibilityMode { SAFE_READ_ONLY, OPERATOR }

/**
 * Section 18: the only class in the app allowed to touch AccessibilityNodeInfo or
 * dispatch a gesture. Everything else (ToolRegistry, AgentOrchestrator) goes through
 * the small set of suspend functions below, which is deliberate -- it keeps every
 * mutating action funneled through one place that Safe Mode can gate.
 *
 * Registered as a bound Android service (see AndroidManifest); the OS creates exactly
 * one instance, so other classes reach it through [instance] rather than DI. This is
 * one of the few spots in the codebase Hilt intentionally doesn't manage.
 */
class OperatorAccessibilityService : AccessibilityService() {

    private val _currentPackage = MutableStateFlow<String?>(null)
    val currentPackage: StateFlow<String?> = _currentPackage.asStateFlow()

    private val _mode = MutableStateFlow(AccessibilityMode.SAFE_READ_ONLY)
    val mode: StateFlow<AccessibilityMode> = _mode.asStateFlow()

    fun setMode(newMode: AccessibilityMode) {
        _mode.value = newMode
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event?.packageName?.let { _currentPackage.value = it.toString() }
    }

    override fun onInterrupt() { /* no-op: nothing queued that needs teardown */ }

    override fun onDestroy() {
        super.onDestroy()
        if (instance === this) instance = null
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        if (instance === this) instance = null
        return super.onUnbind(intent)
    }

    // ---- Reading the screen (allowed in both Safe Mode and Operator Mode) ----

    fun captureTree(): AccessibilityTree? {
        val root = rootInActiveWindow ?: return null
        val pkg = root.packageName?.toString() ?: _currentPackage.value ?: "unknown"
        return AccessibilityTreeParser.parse(root, pkg, screenTitle = null)
    }

    private fun findNode(index: Int): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null
        var found: AccessibilityNodeInfo? = null
        var counter = 0
        fun walk(node: AccessibilityNodeInfo) {
            if (found != null) return
            val meaningful = node.isClickable || node.isScrollable || node.isEditable ||
                !node.text.isNullOrBlank() || !node.contentDescription.isNullOrBlank()
            if (meaningful) {
                if (counter == index) { found = node; return }
                counter++
            }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { walk(it) }
                if (found != null) return
            }
        }
        walk(root)
        return found
    }

    // ---- Acting on the screen (Operator Mode only) ----

    private fun requireOperatorMode(): Boolean = _mode.value == AccessibilityMode.OPERATOR

    fun tapElement(index: Int): Boolean {
        if (!requireOperatorMode()) return false
        val node = findNode(index) ?: return false
        return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    suspend fun tapCoordinate(x: Int, y: Int): Boolean {
        if (!requireOperatorMode()) return false
        val path = Path().apply { moveTo(x.toFloat(), y.toFloat()) }
        return dispatchGestureAwait(GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 60))
            .build())
    }

    suspend fun longPress(x: Int, y: Int): Boolean {
        if (!requireOperatorMode()) return false
        val path = Path().apply { moveTo(x.toFloat(), y.toFloat()) }
        return dispatchGestureAwait(GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 600))
            .build())
    }

    suspend fun swipe(x1: Int, y1: Int, x2: Int, y2: Int, durationMs: Long = 300): Boolean {
        if (!requireOperatorMode()) return false
        val path = Path().apply { moveTo(x1.toFloat(), y1.toFloat()); lineTo(x2.toFloat(), y2.toFloat()) }
        return dispatchGestureAwait(GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, durationMs))
            .build())
    }

    fun scroll(index: Int, forward: Boolean): Boolean {
        if (!requireOperatorMode()) return false
        val node = findNode(index) ?: rootInActiveWindow ?: return false
        val action = if (forward) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
            else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        return node.performAction(action)
    }

    fun setText(index: Int, text: String): Boolean {
        if (!requireOperatorMode()) return false
        val node = findNode(index) ?: return false
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    fun clearText(index: Int): Boolean = setText(index, "")

    fun pressBack(): Boolean {
        if (!requireOperatorMode()) return false
        return performGlobalAction(GLOBAL_ACTION_BACK)
    }

    fun pressHome(): Boolean {
        if (!requireOperatorMode()) return false
        return performGlobalAction(GLOBAL_ACTION_HOME)
    }

    private suspend fun dispatchGestureAwait(gesture: GestureDescription): Boolean =
        suspendCancellableCoroutine { cont ->
            val callback = object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    if (cont.isActive) cont.resume(true)
                }
                override fun onCancelled(gestureDescription: GestureDescription?) {
                    if (cont.isActive) cont.resume(false)
                }
            }
            val dispatched = dispatchGesture(gesture, callback, null)
            if (!dispatched && cont.isActive) cont.resume(false)
        }

    companion object {
        var instance: OperatorAccessibilityService? = null
            private set
    }
}
