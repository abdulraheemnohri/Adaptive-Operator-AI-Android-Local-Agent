package com.adaptiveoperator.ai.android.accessibility

/**
 * Section 19: Gemma reasons over this structured tree instead of raw pixels wherever
 * possible. Kept flat (no nested view hierarchy) and pruned to interactive/labeled
 * nodes -- a raw AccessibilityNodeInfo dump can run into the thousands of nodes for a
 * single screen, most of which are meaningless containers with no text or action.
 */
data class UiElement(
    val index: Int,
    val text: String?,
    val contentDescription: String?,
    val role: String,          // button, text, image, edit_text, checkbox, etc.
    val clickable: Boolean,
    val enabled: Boolean,
    val scrollable: Boolean,
    val focused: Boolean,
    val bounds: Bounds
)

data class Bounds(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val centerX: Int get() = (left + right) / 2
    val centerY: Int get() = (top + bottom) / 2
}

data class AccessibilityTree(
    val packageName: String,
    val screenTitle: String?,
    val elements: List<UiElement>
)
