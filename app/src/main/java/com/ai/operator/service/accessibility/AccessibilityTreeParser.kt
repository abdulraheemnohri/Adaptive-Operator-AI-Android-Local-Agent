package com.ai.operator.service.accessibility

import android.view.accessibility.AccessibilityNodeInfo
import org.json.JSONArray
import org.json.JSONObject

object AccessibilityTreeParser {

    fun parseTreeToJson(rootNode: AccessibilityNodeInfo?): String {
        if (rootNode == null) return "{}"
        val jsonObject = JSONObject()
        try {
            jsonObject.put("package", rootNode.packageName?.toString() ?: "unknown")
            jsonObject.put("elements", parseNodeChildren(rootNode))
        } catch (e: Exception) {
            jsonObject.put("error", e.message)
        }
        return jsonObject.toString()
    }

    private fun parseNodeChildren(node: AccessibilityNodeInfo): JSONArray {
        val array = JSONArray()
        parseNodeRecursive(node, array)
        return array
    }

    private fun parseNodeRecursive(node: AccessibilityNodeInfo, array: JSONArray) {
        val element = nodeToSummary(node)
        if (element != null) {
            array.put(element)
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                parseNodeRecursive(child, array)
                child.recycle()
            }
        }
    }

    private fun nodeToSummary(node: AccessibilityNodeInfo): JSONObject? {
        val text = node.text?.toString() ?: node.contentDescription?.toString()
        if (text.isNullOrBlank() && !node.isClickable) {
            return null
        }

        val obj = JSONObject()
        obj.put("text", text ?: "")
        obj.put("role", node.className?.toString() ?: "view")
        obj.put("clickable", node.isClickable)
        obj.put("scrollable", node.isScrollable)
        obj.put("enabled", node.isEnabled)
        obj.put("focused", node.isFocused)

        val bounds = android.graphics.Rect()
        node.getBoundsInScreen(bounds)
        val boundsObj = JSONObject()
        boundsObj.put("left", bounds.left)
        boundsObj.put("top", bounds.top)
        boundsObj.put("right", bounds.right)
        boundsObj.put("bottom", bounds.bottom)
        obj.put("bounds", boundsObj)

        return obj
    }
}
