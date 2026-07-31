package com.adaptiveoperator.ai.agent

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Section 23: on failure, observe the new state and ask Gemma for an alternative --
 * this class only enforces the retry *ceiling*; AgentOrchestrator is what actually
 * re-plans between attempts using a fresh AgentContext (the whole point of retrying
 * is that the second attempt sees a different screen than the first).
 */
@Singleton
class RetryEngine @Inject constructor() {
    private val attemptsByStep = mutableMapOf<Int, Int>()

    fun shouldRetry(stepIndex: Int, maxRetries: Int = MAX_RETRIES): Boolean {
        val attempts = attemptsByStep.getOrDefault(stepIndex, 0)
        return attempts < maxRetries
    }

    fun recordAttempt(stepIndex: Int) {
        attemptsByStep[stepIndex] = attemptsByStep.getOrDefault(stepIndex, 0) + 1
    }

    fun reset(stepIndex: Int) {
        attemptsByStep.remove(stepIndex)
    }

    fun resetAll() = attemptsByStep.clear()

    companion object {
        const val MAX_RETRIES = 3 // Section 23
    }
}
