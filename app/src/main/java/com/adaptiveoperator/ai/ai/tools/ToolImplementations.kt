package com.adaptiveoperator.ai.ai.tools

import com.adaptiveoperator.ai.android.accessibility.OperatorAccessibilityService
import com.adaptiveoperator.ai.android.capture.ScreenCaptureManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Each Tool below does exactly one thing and returns a ToolResult -- no tool ever
 * consults SecurityPolicy itself (Section 21's flow puts the policy check *before*
 * execution, in ToolRegistry). That keeps these implementations simple and makes the
 * policy layer the single place risk decisions live.
 */

class OpenAppTool @Inject constructor(
    @ApplicationContext private val context: android.content.Context
) : Tool {
    override val name = ToolNames.OPEN_APP
    override val description = "Launch an installed app by name or package."
    override val riskLevel = RiskLevel.LOW
    override val argumentSchema = mapOf("appName" to "string")

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val appName = arguments["appName"] as? String
            ?: return ToolResult.Failure("Missing 'appName' argument", retryable = false)

        val pm = context.packageManager
        val match = pm.getInstalledApplications(android.content.pm.PackageManager.GET_META_DATA)
            .firstOrNull { pm.getApplicationLabel(it).toString().equals(appName, ignoreCase = true) }
            ?: return ToolResult.Failure("No installed app matches '$appName'", retryable = false)

        val launchIntent = pm.getLaunchIntentForPackage(match.packageName)
            ?: return ToolResult.Failure("'$appName' has no launchable activity", retryable = false)

        context.startActivity(launchIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
        return ToolResult.Success(mapOf("packageName" to match.packageName), "Opened $appName")
    }
}

class GetScreenStateTool @Inject constructor() : Tool {
    override val name = ToolNames.GET_SCREEN_STATE
    override val description = "Read the current accessibility tree (Section 19)."
    override val riskLevel = RiskLevel.LOW
    override val argumentSchema = emptyMap<String, String>()

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val service = OperatorAccessibilityService.instance
            ?: return ToolResult.Failure("Accessibility service is not connected", retryable = false)
        val tree = service.captureTree()
            ?: return ToolResult.Failure("No active window to read", retryable = true)
        return ToolResult.Success(
            data = mapOf("tree" to tree),
            summary = "${tree.elements.size} elements on ${tree.packageName}"
        )
    }
}

class FindElementTool @Inject constructor() : Tool {
    override val name = ToolNames.FIND_ELEMENT
    override val description = "Find an element index by visible text or content description."
    override val riskLevel = RiskLevel.LOW
    override val argumentSchema = mapOf("query" to "string")

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val query = (arguments["query"] as? String)?.lowercase()
            ?: return ToolResult.Failure("Missing 'query' argument", retryable = false)
        val service = OperatorAccessibilityService.instance
            ?: return ToolResult.Failure("Accessibility service is not connected", retryable = false)
        val tree = service.captureTree() ?: return ToolResult.Failure("No active window", retryable = true)

        val match = tree.elements.firstOrNull {
            it.text?.lowercase()?.contains(query) == true || it.contentDescription?.lowercase()?.contains(query) == true
        } ?: return ToolResult.Failure("No element matching '$query'", retryable = false)

        return ToolResult.Success(mapOf("index" to match.index), "Found '$query' at element ${match.index}")
    }
}

class FindTextTool @Inject constructor() : Tool {
    override val name = ToolNames.FIND_TEXT
    override val description = "Check whether text is present anywhere on screen."
    override val riskLevel = RiskLevel.LOW
    override val argumentSchema = mapOf("text" to "string")

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val text = (arguments["text"] as? String)?.lowercase()
            ?: return ToolResult.Failure("Missing 'text' argument", retryable = false)
        val service = OperatorAccessibilityService.instance
            ?: return ToolResult.Failure("Accessibility service is not connected", retryable = false)
        val tree = service.captureTree() ?: return ToolResult.Failure("No active window", retryable = true)
        val found = tree.elements.any { it.text?.lowercase()?.contains(text) == true }
        return ToolResult.Success(mapOf("found" to found), if (found) "Found '$text'" else "'$text' not present")
    }
}

class TapElementTool @Inject constructor() : Tool {
    override val name = ToolNames.TAP_ELEMENT
    override val description = "Tap an element by the index returned from find_element / get_screen_state."
    override val riskLevel = RiskLevel.CONFIRM_REQUIRED
    override val argumentSchema = mapOf("index" to "int")

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val index = (arguments["index"] as? Number)?.toInt()
            ?: return ToolResult.Failure("Missing 'index' argument", retryable = false)
        val service = OperatorAccessibilityService.instance
            ?: return ToolResult.Failure("Accessibility service is not connected", retryable = false)
        return if (service.tapElement(index)) ToolResult.Success(summary = "Tapped element $index")
            else ToolResult.Failure("Tap failed -- element $index not found or not operator mode", retryable = true)
    }
}

class TapCoordinateTool @Inject constructor() : Tool {
    override val name = ToolNames.TAP_COORDINATE
    override val description = "Tap raw screen coordinates -- fallback for when no matching element exists."
    override val riskLevel = RiskLevel.CONFIRM_REQUIRED
    override val argumentSchema = mapOf("x" to "int", "y" to "int")

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val x = (arguments["x"] as? Number)?.toInt() ?: return ToolResult.Failure("Missing 'x'", retryable = false)
        val y = (arguments["y"] as? Number)?.toInt() ?: return ToolResult.Failure("Missing 'y'", retryable = false)
        val service = OperatorAccessibilityService.instance
            ?: return ToolResult.Failure("Accessibility service is not connected", retryable = false)
        return if (service.tapCoordinate(x, y)) ToolResult.Success(summary = "Tapped ($x, $y)")
            else ToolResult.Failure("Gesture dispatch failed", retryable = true)
    }
}

class LongPressTool @Inject constructor() : Tool {
    override val name = ToolNames.LONG_PRESS
    override val riskLevel = RiskLevel.CONFIRM_REQUIRED
    override val description = "Long-press raw screen coordinates."
    override val argumentSchema = mapOf("x" to "int", "y" to "int")

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val x = (arguments["x"] as? Number)?.toInt() ?: return ToolResult.Failure("Missing 'x'", retryable = false)
        val y = (arguments["y"] as? Number)?.toInt() ?: return ToolResult.Failure("Missing 'y'", retryable = false)
        val service = OperatorAccessibilityService.instance
            ?: return ToolResult.Failure("Accessibility service is not connected", retryable = false)
        return if (service.longPress(x, y)) ToolResult.Success(summary = "Long-pressed ($x, $y)")
            else ToolResult.Failure("Gesture dispatch failed", retryable = true)
    }
}

class SwipeTool @Inject constructor() : Tool {
    override val name = ToolNames.SWIPE
    override val riskLevel = RiskLevel.CONFIRM_REQUIRED
    override val description = "Swipe from one point to another."
    override val argumentSchema = mapOf("x1" to "int", "y1" to "int", "x2" to "int", "y2" to "int")

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        fun intArg(key: String) = (arguments[key] as? Number)?.toInt()
        val x1 = intArg("x1") ?: return ToolResult.Failure("Missing 'x1'", retryable = false)
        val y1 = intArg("y1") ?: return ToolResult.Failure("Missing 'y1'", retryable = false)
        val x2 = intArg("x2") ?: return ToolResult.Failure("Missing 'x2'", retryable = false)
        val y2 = intArg("y2") ?: return ToolResult.Failure("Missing 'y2'", retryable = false)
        val service = OperatorAccessibilityService.instance
            ?: return ToolResult.Failure("Accessibility service is not connected", retryable = false)
        return if (service.swipe(x1, y1, x2, y2)) ToolResult.Success(summary = "Swiped ($x1,$y1) -> ($x2,$y2)")
            else ToolResult.Failure("Gesture dispatch failed", retryable = true)
    }
}

class ScrollTool @Inject constructor() : Tool {
    override val name = ToolNames.SCROLL
    override val riskLevel = RiskLevel.LOW
    override val description = "Scroll a scrollable element forward or backward."
    override val argumentSchema = mapOf("index" to "int", "forward" to "boolean")

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val index = (arguments["index"] as? Number)?.toInt() ?: -1
        val forward = arguments["forward"] as? Boolean ?: true
        val service = OperatorAccessibilityService.instance
            ?: return ToolResult.Failure("Accessibility service is not connected", retryable = false)
        return if (service.scroll(index, forward)) ToolResult.Success(summary = "Scrolled ${if (forward) "forward" else "backward"}")
            else ToolResult.Failure("Scroll failed", retryable = true)
    }
}

class TypeTextTool @Inject constructor() : Tool {
    override val name = ToolNames.TYPE_TEXT
    override val riskLevel = RiskLevel.CONFIRM_REQUIRED
    override val description = "Set text on the focused/target input field."
    override val argumentSchema = mapOf("index" to "int", "text" to "string")

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val index = (arguments["index"] as? Number)?.toInt()
            ?: return ToolResult.Failure("Missing 'index' argument", retryable = false)
        val text = arguments["text"] as? String
            ?: return ToolResult.Failure("Missing 'text' argument", retryable = false)
        val service = OperatorAccessibilityService.instance
            ?: return ToolResult.Failure("Accessibility service is not connected", retryable = false)
        return if (service.setText(index, text)) ToolResult.Success(summary = "Typed into element $index")
            else ToolResult.Failure("Could not set text on element $index", retryable = true)
    }
}

class ClearTextTool @Inject constructor() : Tool {
    override val name = ToolNames.CLEAR_TEXT
    override val riskLevel = RiskLevel.CONFIRM_REQUIRED
    override val description = "Clear text from an input field."
    override val argumentSchema = mapOf("index" to "int")

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val index = (arguments["index"] as? Number)?.toInt()
            ?: return ToolResult.Failure("Missing 'index' argument", retryable = false)
        val service = OperatorAccessibilityService.instance
            ?: return ToolResult.Failure("Accessibility service is not connected", retryable = false)
        return if (service.clearText(index)) ToolResult.Success(summary = "Cleared element $index")
            else ToolResult.Failure("Could not clear element $index", retryable = true)
    }
}

class PressBackTool @Inject constructor() : Tool {
    override val name = ToolNames.PRESS_BACK
    override val riskLevel = RiskLevel.LOW
    override val description = "Press the system Back button."
    override val argumentSchema = emptyMap<String, String>()
    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val service = OperatorAccessibilityService.instance
            ?: return ToolResult.Failure("Accessibility service is not connected", retryable = false)
        return if (service.pressBack()) ToolResult.Success(summary = "Pressed Back")
            else ToolResult.Failure("Back action failed", retryable = true)
    }
}

class PressHomeTool @Inject constructor() : Tool {
    override val name = ToolNames.PRESS_HOME
    override val riskLevel = RiskLevel.CONFIRM_REQUIRED // leaves the current task's app entirely
    override val description = "Press the system Home button."
    override val argumentSchema = emptyMap<String, String>()
    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val service = OperatorAccessibilityService.instance
            ?: return ToolResult.Failure("Accessibility service is not connected", retryable = false)
        return if (service.pressHome()) ToolResult.Success(summary = "Pressed Home")
            else ToolResult.Failure("Home action failed", retryable = true)
    }
}

class WaitTool @Inject constructor() : Tool {
    override val name = ToolNames.WAIT
    override val riskLevel = RiskLevel.LOW
    override val description = "Pause briefly for a UI transition/animation to settle before observing again."
    override val argumentSchema = mapOf("milliseconds" to "int")
    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val ms = ((arguments["milliseconds"] as? Number)?.toLong() ?: 500L).coerceIn(0, 5000)
        delay(ms)
        return ToolResult.Success(summary = "Waited ${ms}ms")
    }
}

@Singleton
class ScreenshotTool @Inject constructor(
    private val screenCaptureManager: ScreenCaptureManager
) : Tool {
    override val name = ToolNames.SCREENSHOT
    override val riskLevel = RiskLevel.LOW
    override val description = "Capture one frame for visual reasoning the accessibility tree can't express (Section 17)."
    override val argumentSchema = emptyMap<String, String>()

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        if (!screenCaptureManager.isAvailable) {
            return ToolResult.Failure("Screen capture permission not granted for this session", retryable = false)
        }
        val bitmap = screenCaptureManager.captureOnce()
            ?: return ToolResult.Failure("Capture returned no frame", retryable = true)
        return ToolResult.Success(mapOf("bitmap" to bitmap), "Captured 1 frame")
    }
}
