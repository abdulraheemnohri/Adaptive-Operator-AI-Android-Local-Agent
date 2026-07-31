package com.adaptiveoperator.ai.android.accessibility

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo

object AccessibilityTreeParser {

    /**
     * Walks the node tree depth-first, keeping only nodes that are meaningfully
     * interactive or carry text/content-description a model could reason about.
     * Every kept node gets a stable [UiElement.index] used by `find_element` /
     * `tap_element` tool calls so Gemma can refer back to "element 7" instead of
     * re-deriving coordinates itself.
     */
    fun parse(rootNode: AccessibilityNodeInfo, packageName: String, screenTitle: String?): AccessibilityTree {
        val elements = mutableListOf<UiElement>()
        visit(rootNode, elements)
        return AccessibilityTree(packageName = packageName, screenTitle = screenTitle, elements = elements)
    }

    private fun visit(node: AccessibilityNodeInfo, out: MutableList<UiElement>) {
        val isMeaningful = node.isClickable || node.isScrollable || node.isEditable ||
            !node.text.isNullOrBlank() || !node.contentDescription.isNullOrBlank()

        if (isMeaningful) {
            val bounds = Rect().also { node.getBoundsInScreen(it) }
            out += UiElement(
                index = out.size,
                text = node.text?.toString(),
                contentDescription = node.contentDescription?.toString(),
                role = roleOf(node),
                clickable = node.isClickable,
                enabled = node.isEnabled,
                scrollable = node.isScrollable,
                focused = node.isFocused,
                bounds = Bounds(bounds.left, bounds.top, bounds.right, bounds.bottom)
            )
        }

        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                visit(child, out)
                child.recycle()
            }
        }
    }

    private fun roleOf(node: AccessibilityNodeInfo): String = when {
        node.isEditable -> "edit_text"
        node.className?.contains("Button", ignoreCase = true) == true -> "button"
        node.className?.contains("CheckBox", ignoreCase = true) == true -> "checkbox"
        node.className?.contains("Image", ignoreCase = true) == true -> "image"
        node.isClickable -> "clickable"
        else -> "text"
    }

    fun activeWindowRoot(windows: List<AccessibilityWindowInfo>): AccessibilityNodeInfo? =
        windows.firstOrNull { it.isActive }?.root
}
