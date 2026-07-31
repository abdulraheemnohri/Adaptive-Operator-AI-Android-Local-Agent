package com.adaptiveoperator.ai.ai.tools

/**
 * One entry in the ToolRegistry (Section 20). Gemma can only ever call tools that
 * implement this interface and are registered -- there is no free-form shell/JS/eval
 * escape hatch anywhere in this architecture.
 */
interface Tool {
    val name: String
    val description: String
    val riskLevel: RiskLevel

    /** JSON-schema-ish description of expected arguments, used both to prompt Gemma
     *  and to validate incoming tool calls before execution. */
    val argumentSchema: Map<String, String>

    suspend fun execute(arguments: Map<String, Any?>): ToolResult
}

/** The full V1 tool surface from Section 20. */
object ToolNames {
    const val OPEN_APP = "open_app"
    const val CLOSE_APP = "close_app"
    const val TAP_ELEMENT = "tap_element"
    const val TAP_COORDINATE = "tap_coordinate"
    const val LONG_PRESS = "long_press"
    const val SWIPE = "swipe"
    const val SCROLL = "scroll"
    const val TYPE_TEXT = "type_text"
    const val CLEAR_TEXT = "clear_text"
    const val PRESS_BACK = "press_back"
    const val PRESS_HOME = "press_home"
    const val FIND_TEXT = "find_text"
    const val FIND_ELEMENT = "find_element"
    const val SCREENSHOT = "screenshot"
    const val WAIT = "wait"
    const val GET_SCREEN_STATE = "get_screen_state"
}
