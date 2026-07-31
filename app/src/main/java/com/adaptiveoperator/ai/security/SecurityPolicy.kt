package com.adaptiveoperator.ai.security

import com.adaptiveoperator.ai.ai.tools.RiskLevel
import com.adaptiveoperator.ai.ai.tools.ToolCall
import com.adaptiveoperator.ai.ai.tools.ToolNames
import com.adaptiveoperator.ai.memory.db.dao.PreferenceDao
import javax.inject.Inject
import javax.inject.Singleton

enum class ConfirmationMode { ASK_FOR_RISKY, ASK_FOR_EVERY_ACTION, AUTONOMOUS_LOW_RISK }

data class PolicyDecision(
    val allowed: Boolean,
    val requiresConfirmation: Boolean,
    val riskLevel: RiskLevel,
    val reason: String? = null
)

/**
 * Section 43 (Operator Confirmation) + Section 21 (every tool call passes a policy
 * check before execution). This is intentionally the *only* place risk is decided --
 * ToolRegistry calls into this before every single execute(), no exceptions, so an
 * app-blocklist or confirmation-mode change takes effect immediately everywhere.
 */
@Singleton
class SecurityPolicy @Inject constructor(
    private val blocklistManager: BlocklistManager,
    private val preferenceDao: PreferenceDao
) {
    private val lowRiskTools = setOf(
        ToolNames.SCREENSHOT, ToolNames.GET_SCREEN_STATE, ToolNames.FIND_ELEMENT,
        ToolNames.FIND_TEXT, ToolNames.WAIT, ToolNames.SCROLL, ToolNames.PRESS_BACK,
        ToolNames.OPEN_APP
    )

    private val confirmRequiredTools = setOf(
        ToolNames.CLOSE_APP, ToolNames.CLEAR_TEXT, ToolNames.TYPE_TEXT,
        ToolNames.TAP_ELEMENT, ToolNames.TAP_COORDINATE, ToolNames.LONG_PRESS, ToolNames.SWIPE
    )

    suspend fun currentMode(): ConfirmationMode {
        val stored = preferenceDao.get(PREF_CONFIRMATION_MODE)?.value
        return ConfirmationMode.entries.find { it.name == stored } ?: ConfirmationMode.ASK_FOR_RISKY
    }

    suspend fun setMode(mode: ConfirmationMode) {
        preferenceDao.set(
            com.adaptiveoperator.ai.memory.db.entity.PreferenceEntity(
                key = PREF_CONFIRMATION_MODE, value = mode.name, updatedAtEpochMs = System.currentTimeMillis()
            )
        )
    }

    /**
     * [foregroundPackage] is the package the accessibility tree currently belongs to --
     * blocklist checks apply regardless of which tool is being called, since the risk
     * is "operator is inside a banking app at all", not the specific action.
     */
    suspend fun evaluate(call: ToolCall, foregroundPackage: String?): PolicyDecision {
        if (foregroundPackage != null && blocklistManager.isBlocked(foregroundPackage)) {
            return PolicyDecision(
                allowed = false,
                requiresConfirmation = false,
                riskLevel = RiskLevel.BLOCKED,
                reason = "$foregroundPackage is on the blocked-apps list"
            )
        }

        val risk = classify(call.tool)
        val mode = currentMode()

        val needsConfirmation = when (mode) {
            ConfirmationMode.ASK_FOR_EVERY_ACTION -> true
            ConfirmationMode.ASK_FOR_RISKY -> risk != RiskLevel.LOW
            ConfirmationMode.AUTONOMOUS_LOW_RISK -> false
        }

        return PolicyDecision(allowed = true, requiresConfirmation = needsConfirmation, riskLevel = risk)
    }

    private fun classify(toolName: String): RiskLevel = when (toolName) {
        in lowRiskTools -> RiskLevel.LOW
        in confirmRequiredTools -> RiskLevel.CONFIRM_REQUIRED
        else -> RiskLevel.CONFIRM_REQUIRED // unknown tools default to the safer path
    }

    companion object {
        private const val PREF_CONFIRMATION_MODE = "security.confirmation_mode"
    }
}
