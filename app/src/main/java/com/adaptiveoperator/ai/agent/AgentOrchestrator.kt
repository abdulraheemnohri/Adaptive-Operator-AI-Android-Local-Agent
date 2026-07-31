package com.adaptiveoperator.ai.agent

import com.adaptiveoperator.ai.ai.context.ContextBuilder
import com.adaptiveoperator.ai.ai.runtime.GemmaEngineWrapper
import com.adaptiveoperator.ai.ai.tools.DispatchOutcome
import com.adaptiveoperator.ai.ai.tools.RiskLevel
import com.adaptiveoperator.ai.ai.tools.ToolCall
import com.adaptiveoperator.ai.ai.tools.ToolRegistry
import com.adaptiveoperator.ai.ai.tools.ToolResult
import com.adaptiveoperator.ai.memory.repository.MemoryRepository
import com.adaptiveoperator.ai.security.EmergencyStop
import com.adaptiveoperator.ai.skills.SkillEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val MAX_STEPS_PER_TASK = 25

@Singleton
class AgentOrchestrator @Inject constructor(
    private val engine: GemmaEngineWrapper,
    private val contextBuilder: ContextBuilder,
    private val planner: Planner,
    private val toolRegistry: ToolRegistry,
    private val retryEngine: RetryEngine,
    private val memoryRepository: MemoryRepository,
    private val skillEngine: SkillEngine,
    private val emergencyStop: EmergencyStop
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var activeJob: Job? = null

    private val _state = MutableStateFlow(AgentTaskState())
    val state: StateFlow<AgentTaskState> = _state.asStateFlow()

    private var pendingConfirmation: Pair<Long?, ToolCall>? = null

    fun start(userRequest: String, sourceApp: String? = null) {
        stop() // a fresh request supersedes anything mid-flight
        activeJob = scope.launch { runLoop(userRequest, sourceApp) }
    }

    fun stop() {
        activeJob?.cancel()
        activeJob = null
        pendingConfirmation = null
        _state.value = AgentTaskState(status = AgentStatus.IDLE)
    }

    /** Called from the Operator UI when the user taps "Approve" on a confirmation card. */
    fun confirmPendingAction() {
        val (taskId, call) = pendingConfirmation ?: return
        pendingConfirmation = null
        activeJob = scope.launch { executeToolAndContinue(taskId, call, RiskLevel.CONFIRM_REQUIRED, userRequest = _state.value.taskDescription.orEmpty()) }
    }

    fun rejectPendingAction() {
        pendingConfirmation = null
        _state.value = _state.value.copy(status = AgentStatus.IDLE, awaitingConfirmation = null, lastMessage = "Action cancelled")
    }

    private suspend fun runLoop(userRequest: String, sourceApp: String?) {
        if (!engine.isLoaded) {
            _state.value = AgentTaskState(status = AgentStatus.ERROR, lastMessage = "Model is not loaded -- open Model Manager first")
            return
        }

        val taskId = memoryRepository.startTask(userRequest, sourceApp)
        _state.value = AgentTaskState(status = AgentStatus.THINKING, taskDescription = userRequest)

        val startTime = System.currentTimeMillis()
        val actionsLog = StringBuilder("[")
        var stepCount = 0
        var succeeded = false
        var failureReason: String? = null

        while (stepCount < MAX_STEPS_PER_TASK) {
            if (emergencyStop.triggered.value) {
                failureReason = "Stopped by emergency stop"
                break
            }
            stepCount++

            val context = contextBuilder.build(userRequest, includeScreenshot = false)
            val promptText = contextBuilder.toPromptText(context)

            _state.value = _state.value.copy(status = AgentStatus.THINKING)
            val modelOutput = collectFullResponse(promptText)

            when (val step = planner.parse(modelOutput)) {
                is PlannedStep.TaskComplete -> {
                    succeeded = true
                    _state.value = _state.value.copy(status = AgentStatus.SUCCESS, lastMessage = "Task complete")
                }

                is PlannedStep.ConversationalReply -> {
                    memoryRepository.appendConversationTurn("assistant", step.text)
                    succeeded = true
                    _state.value = _state.value.copy(status = AgentStatus.SUCCESS, lastMessage = step.text)
                }

                is PlannedStep.ToolInvocation -> {
                    _state.value = _state.value.copy(status = AgentStatus.ACTING)
                    if (actionsLog.length > 1) actionsLog.append(",")
                    actionsLog.append(step.call.rawJson) // full {"tool":...,"arguments":{...}} for skill replay

                    when (val outcome = toolRegistry.dispatch(step.call, taskId)) {
                        is DispatchOutcome.NeedsConfirmation -> {
                            pendingConfirmation = taskId to outcome.call
                            _state.value = _state.value.copy(
                                status = AgentStatus.OBSERVING,
                                awaitingConfirmation = outcome.call
                            )
                            return // pause here; confirmPendingAction()/rejectPendingAction() resume or end it
                        }
                        is DispatchOutcome.Rejected -> {
                            if (retryEngine.shouldRetry(stepCount)) {
                                retryEngine.recordAttempt(stepCount)
                                continue
                            }
                            failureReason = outcome.reason
                        }
                        is DispatchOutcome.Executed -> {
                            _state.value = _state.value.copy(status = AgentStatus.OBSERVING)
                            when (val result = outcome.result) {
                                is ToolResult.Success -> {
                                    // Feed the observation back so the next planning step sees it.
                                    collectFullResponse("Tool result: ${result.summary}")
                                }
                                is ToolResult.Failure -> {
                                    if (result.retryable && retryEngine.shouldRetry(stepCount)) {
                                        retryEngine.recordAttempt(stepCount)
                                        collectFullResponse("Tool failed: ${result.reason}. Try a different approach.")
                                        continue
                                    }
                                    failureReason = result.reason
                                }
                                is ToolResult.Denied -> failureReason = result.reason
                            }
                        }
                    }
                }
            }

            if (succeeded || failureReason != null) break
        }

        if (!succeeded && failureReason == null && stepCount >= MAX_STEPS_PER_TASK) {
            failureReason = "Exceeded maximum steps for a single task ($MAX_STEPS_PER_TASK)"
        }

        finishTask(taskId, userRequest, actionsLog.append("]").toString(), succeeded, failureReason, startTime)
    }

    private suspend fun executeToolAndContinue(taskId: Long?, call: ToolCall, riskLevel: RiskLevel, userRequest: String) {
        _state.value = _state.value.copy(status = AgentStatus.ACTING, awaitingConfirmation = null)
        val result = toolRegistry.executeConfirmed(call, riskLevel, taskId)
        _state.value = _state.value.copy(status = AgentStatus.OBSERVING)
        val summary = when (result) {
            is ToolResult.Success -> result.summary
            is ToolResult.Failure -> result.reason
            is ToolResult.Denied -> result.reason
        }
        collectFullResponse("Tool result: $summary")
        // Re-enter the main loop for the next planning step rather than duplicating it here.
        runLoop(userRequest, sourceApp = null)
    }

    private suspend fun collectFullResponse(prompt: String): String {
        val builder = StringBuilder()
        engine.sendMessage(prompt).fold(builder) { acc, token -> acc.append(token) }
        return builder.toString()
    }

    private suspend fun finishTask(
        taskId: Long,
        goal: String,
        actionsJson: String,
        succeeded: Boolean,
        failureReason: String?,
        startTime: Long
    ) {
        val duration = System.currentTimeMillis() - startTime
        memoryRepository.finishTaskById(taskId, if (succeeded) "SUCCESS" else "FAILED")
        memoryRepository.recordExperience(taskId, goal, actionsJson, succeeded, failureReason, duration)
        retryEngine.resetAll()

        _state.value = _state.value.copy(
            status = if (succeeded) AgentStatus.SUCCESS else AgentStatus.ERROR,
            lastMessage = failureReason ?: _state.value.lastMessage
        )

        // Section 26/28: only after the experience is written do we check whether this
        // goal has now been repeated enough times to promote into a Skill.
        if (succeeded) skillEngine.evaluateForSkillPromotion(goal)
    }
}
