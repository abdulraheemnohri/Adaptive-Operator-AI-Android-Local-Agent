# Android AI Operator — V1 Architecture Specification

Version: 1.0
Platform: Android
AI Model: Google Gemma 4 E2B-it
Runtime: LiteRT-LM
Architecture: On-device / Local-first / Clean Architecture

---

## 1. Core Architectural Principles

The Android AI Operator (AIOperator) is built on a strictly local-only, offline-first design. It utilizes a single foundational AI model—Google Gemma 4 E2B-it—running locally on the device using the LiteRT-LM runtime. There is no cloud AI inference, no remote LLM router, no external API keys, and no backend dependencies for core operations.

### Key Separation of Concerns
*   **Brain (Intelligence & Reasoning):** Google Gemma 4 E2B-it (Local via LiteRT-LM).
*   **Eyes (Observation):** Android MediaProjection Screen Capture + Accessibility Tree.
*   **Hands (Interaction):** Android Accessibility Service (clicks, swipes, text entry).
*   **Voice (Output):** Android TextToSpeech (TTS) Engine.
*   **Hearing (Input):** Android SpeechRecognizer.
*   **Memory (Persistence):** Room/SQLite local database.
*   **Skill Engine (Procedural Knowledge):** Structured local workflows representing learned or predefined procedures.

### Security Boundary Principle
```
┌─────────────┐     Propose Action     ┌──────────────┐     Validate Action     ┌───────────────┐
│ Gemma Model │ ─────────────────────> │ Agent Engine │ ──────────────────────> │ Android OS    │
└─────────────┘                        └──────────────┘ (Policy/Confirmation)   └───────────────┘
```
The AI model has no direct execution capabilities on Android. All model suggestions are structured as tool-call requests that are validated, checked against privacy/security policies, and executed strictly through native Android platform APIs by the application wrapper.

---

## 2. Module & Package Structure

The project is structured following **Clean Architecture** and **MVVM (Model-View-ViewModel)** guidelines. This divides the codebase into distinct layers (Presentation, Domain, Data) and functional modules, ensuring that the business logic and AI orchestration remain decoupled from the platform implementation.

### High-Level Folder Layout
```
android-ai-operator/
├── app/                      # Application entry point, Hilt DI modules, Application class
├── core/                     # Shared abstractions and core engines
│   ├── ai/                   # LiteRT-LM abstractions, context building, model management
│   ├── agent/                # Orchestrator, planning, loop execution, retry, verification
│   ├── tools/                # Tool Registry and Android OS execution bridges
│   ├── skills/               # Skill Engine, discovery, and execution management
│   ├── memory/               # Short-term and episodic memory handlers
│   ├── security/             # Blocklist, safe-mode, confirmation policy enforcement
│   └── common/               # Shared utilities, extensions, and dispatchers
├── data/                     # Data layer implementation
│   ├── database/             # Room Database, DAOs, and Entities
│   ├── repositories/         # Implementation of Repository interfaces
│   ├── datastore/            # User settings and flags via Jetpack DataStore
│   └── models/               # Model definitions, manifest parsing, and storage paths
├── domain/                   # Business logic and abstraction layer
│   ├── agent/                # Use cases for Agent loop control
│   ├── ai/                   # Use cases for Model management and text generation
│   ├── memory/               # Use cases for short/long-term episodic memory
│   ├── skills/               # Use cases for Skill management and discovery
│   └── tools/                # Declarations of available tools and schemas
├── feature/                  # Jetpack Compose UI Features
│   ├── home/                 # Main Dashboard, resource monitor cards
│   ├── chat/                 # Conversational UI and thinking indicators
│   ├── operator/             # Fullscreen Agent control, plan visualizer, live status
│   ├── models/               # Model Downloader, installer, benchmark, and status
│   ├── skills/               # Skill Library UI, enable/disable toggle, edit workflow
│   ├── memory/               # Episodic history explorer, memory management, export/clear
│   ├── history/              # Task logs, execution statistics
│   ├── settings/             # Settings hub (AI runtime, Voice, Thermal, Floating UI)
│   └── permissions/          # Permission Center (Accessibility, Overlay, MediaProjection)
├── service/                  # Android Background and System Services
│   ├── accessibility/        # AccessibilityService execution and Tree parser
│   ├── floating/             # Floating Bubble overlay and quick actions panel
│   ├── projection/           # MediaProjection Screen capture background service
│   ├── voice/                # Android SpeechRecognizer speech-to-text service
│   └── tts/                  # Android TTS manager
└── runtime/                  # Specific local inference bindings
    └── gemma/                # LiteRT-LM Android SDK wrapper and Gemma 4 adapter
```

---

## 3. Database Schema & Room Entities

Persistent storage is managed locally using **Room/SQLite**. The database tracks conversation history, episodic task executions, learned skills, user preferences, tool histories, and model metadata.

### Entity Relationships
```
  ┌─────────────────┐             ┌─────────────────┐
  │   AgentTask     │1           *│  ToolExecution   │
  │ (Overall Goal)  │────────────>│  (Action Log)   │
  └─────────────────┘             └─────────────────┘
           │1
           │
           │*
  ┌─────────────────┐             ┌─────────────────┐
  │   Experience    │             │   Conversation  │
  │ (Episodic Mem)  │             │   (User Chat)   │
  └─────────────────┘             └─────────────────┘
                                           │1
                                           │
                                           │*
  ┌─────────────────┐             ┌─────────────────┐
  │      Skill      │1           *│     Message     │
  │ (Learned Proc)  │────────────>│  (Chat History) │
  └─────────────────┘             └─────────────────┘
           │1
           │
           │*
  ┌─────────────────┐
  │    SkillStep    │
  │ (Ordered Steps) │
  └─────────────────┘
```

### Detailed Room Entities

#### 1. `AgentTask`
Represents an overall goal requested by the user.
```kotlin
@Entity(tableName = "agent_tasks")
data class AgentTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val description: String,               // e.g. "Open YouTube and search for AI videos"
    val timestamp: Long,                   // System time milliseconds
    val status: String,                    // PENDING, RUNNING, COMPLETED, FAILED, STOPPED
    val totalSteps: Int,
    val durationMs: Long,
    val usedSkillId: String?               // ID of the skill used, if any
)
```

#### 2. `ToolExecution`
Tracks individual tool execution logs under a specific `AgentTask`.
```kotlin
@Entity(
    tableName = "tool_executions",
    foreignKeys = [
        ForeignKey(
            entity = AgentTaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ToolExecutionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val toolName: String,                  // e.g., "tap", "type_text", "swipe"
    val argumentsJson: String,             // JSON string of input parameters
    val timestamp: Long,
    val resultJson: String?,               // Return value or error details
    val isSuccess: Boolean,
    val executionDurationMs: Long
)
```

#### 3. `ExperienceEntity` (Episodic Memory)
Used by the Memory System to capture the success/failure context of past task executions.
```kotlin
@Entity(tableName = "experiences")
data class ExperienceEntity(
    @PrimaryKey val id: String,            // UUID
    val taskDescription: String,
    val stepsSequenceJson: String,         // Ordered list of actions executed
    val result: String,                    // SUCCESS, FAILURE
    val durationMs: Long,
    val timestamp: Long,
    val applicationPackage: String         // Target app package where this occurred
)
```

#### 4. `SkillEntity`
Defines a reusable agent workflow that has been programmed or discovered.
```kotlin
@Entity(tableName = "skills")
data class SkillEntity(
    @PrimaryKey val id: String,            // unique identifier
    val name: String,                      // e.g., "Search YouTube"
    val triggerPhrasesJson: String,        // List of trigger phrases
    val description: String,
    val successCount: Int = 0,
    val failureCount: Int = 0,
    val lastUsedTimestamp: Long = 0,
    val averageDurationMs: Long = 0,
    val confidence: Double = 0.0,          // Derived score based on success rate
    val isEnabled: Boolean = true
)
```

#### 5. `SkillStepEntity`
Defines the individual steps that make up a `Skill`.
```kotlin
@Entity(
    tableName = "skill_steps",
    foreignKeys = [
        ForeignKey(
            entity = SkillEntity::class,
            parentColumns = ["id"],
            childColumns = ["skillId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SkillStepEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val skillId: String,
    val stepOrder: Int,                    // 0, 1, 2...
    val toolName: String,                  // e.g., "open_app"
    val argumentsTemplateJson: String,     // Arguments, supporting placeholders like {query}
    val expectedTargetScreen: String?      // Verification criteria
)
```

#### 6. `PreferenceEntity`
Stores key-value configurations and learned user settings.
```kotlin
@Entity(tableName = "user_preferences")
data class PreferenceEntity(
    @PrimaryKey val key: String,           // e.g. "voice_language", "tts_pitch"
    val value: String,
    val type: String                       // STRING, INT, FLOAT, BOOLEAN
)
```

#### 7. `ConversationEntity` & `MessageEntity`
Maintains conversational chat screens.
```kotlin
@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,            // Session UUID
    val startTime: Long,
    val title: String                      // Generated based on initial message
)

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversationId: String,
    val role: String,                      // USER, ASSISTANT, SYSTEM
    val text: String,
    val timestamp: Long,
    val imagePath: String?,                // Local URI of associated screenshot, if any
    val structuredToolCallsJson: String?   // Optional associated tool interactions
)
```

---

## 4. AI Runtime Subsystem

The AI Runtime isolates the application logic from the underlying model loading mechanics. It encapsulates LiteRT-LM (the TensorFlow Lite runtime optimized for Large Language Models on edge devices) and exposes a hardware-agnostic API.

```
┌───────────────────────────────────────────────────────────────┐
│                          Core Agent                           │
└───────────────────────────────┬───────────────────────────────┘
                                │
                                ▼
┌───────────────────────────────────────────────────────────────┐
│                       AiModelAdapter                          │
└───────────────────────────────┬───────────────────────────────┘
                                │
                                ▼
┌───────────────────────────────────────────────────────────────┐
│                      Gemma4E2BAdapter                         │
└───────────────────────────────┬───────────────────────────────┘
                                │
                                ▼
┌───────────────────────────────────────────────────────────────┐
│                         LiteRT-LM                             │
└───────────────┬───────────────┬───────────────┬───────────────┘
                ▼               ▼               ▼
           NPU Backend     GPU Backend     CPU Backend
```

### Key Interfaces

#### `AiRuntime`
```kotlin
interface AiRuntime {
    suspend fun loadModel(modelPath: String, config: RuntimeConfig): Result<Unit>
    suspend fun unloadModel(): Result<Unit>
    suspend fun generate(request: GenerationRequest): Flow<GenerationResult>
    suspend fun generateWithTools(request: AgentRequest): Flow<AgentResult>
    fun isLoaded(): Boolean
    fun getRuntimeInfo(): RuntimeInfo
    fun monitorResources(): Flow<ResourceStatus>
}
```

#### `AiModelAdapter`
```kotlin
interface AiModelAdapter {
    fun formatPrompt(context: StructuredContext): String
    fun parseResponse(rawOutput: String): ResponseType
}

sealed interface ResponseType {
    data class TextResponse(val text: String) : ResponseType
    data class ToolCallResponse(val toolCalls: List<ToolCall>) : ResponseType
    data class ThoughtAndAction(val thoughts: String, val toolCalls: List<ToolCall>) : ResponseType
}
```

### Models & Configurations
```kotlin
data class RuntimeConfig(
    val backend: BackendType = BackendType.AUTOMATIC,
    val threads: Int = 4,
    val contextLength: Int = 128000,
    val maxOutputTokens: Int = 2048,
    val temperature: Float = 1.0f,
    val topP: Float = 0.95f,
    val topK: Int = 64,
    val isThinkingModeEnabled: Boolean = true,
    val visualTokenBudget: Int = 512,
    val modelLifecycle: ModelLifecyclePolicy = ModelLifecyclePolicy.AUTOMATIC
)

enum class BackendType { AUTOMATIC, NPU, GPU, CPU }

enum class ModelLifecyclePolicy { AUTOMATIC, KEEP_WARM, TIMEOUT_30S, TIMEOUT_5M }

data class RuntimeInfo(
    val modelId: String,
    val status: ModelStatus,
    val activeBackend: BackendType,
    val averageLatencyMs: Double,
    val tokensPerSecond: Float,
    val allocatedMemoryMb: Long
)

enum class ModelStatus {
    UNINSTALLED, DOWNLOADING, VERIFYING, COMPATIBILITY_FAILED, INSTALLED, READY, LOADING, OPERATIONAL, ERROR
}
```

### Model Lifecycle Flow
1.  **Check:** Check local disk storage `/data/models/gemma4-e2b/` for presence of `metadata.json` and weight chunks.
2.  **Verify:** Validate actual files against SHA-256 checksums stored in `checksum` manifest.
3.  **Allocate:** Device Analyzer examines thermal states, CPU cores, active memory availability, and NPU/GPU drivers.
4.  **Load:** Load Gemma 4 into LiteRT-LM with the chosen hardware backend.
5.  **Warm-up:** Run single prompt test sequence ("Hello") to initialize KV caches and measure latency.
6.  **Idle/Unload:** Monitor activity timer. If inactive and `ModelLifecyclePolicy` is set to automatic/timeout, release the model from memory to preserve battery.

---

## 5. Agent Orchestrator & Execution Loop

The `AgentEngine` is the heartbeat of AIOperator. It drives the core goal-seeking behavior loop, ensuring that the model observes, reasons, acts, and verifies continuously until completion.

```kotlin
interface AgentEngine {
    suspend fun execute(request: AgentRequest): Flow<AgentState>
    suspend fun pause()
    suspend fun resume()
    suspend fun stop()
    fun getCurrentState(): AgentState
}
```

### Agent State Transition State Machine
```
   ┌──────────────────────────────────────────────┐
   │                     IDLE                     │
   └──────────────────────┬───────────────────────┘
                          │
                          │ execute()
                          ▼
   ┌──────────────────────────────────────────────┐
   │                   THINKING                   │ <─────────────────┐
   └──────────────────────┬───────────────────────┘                   │
                          │                                           │
                          ▼                                           │
   ┌──────────────────────────────────────────────┐                   │
   │                   PLANNING                   │                   │
   └──────────────────────┬───────────────────────┘                   │
                          │                                           │
                          ▼                                           │
   ┌──────────────────────────────────────────────┐                   │
   │             WAITING_CONFIRMATION             │                   │
   └──────────────────────┬───────────────────────┘                   │
                          │ Approved                                  │
                          ▼                                           │
   ┌──────────────────────────────────────────────┐                   │
   │                  EXECUTING                   │                   │
   └──────────────────────┬───────────────────────┘                   │
                          │                                           │
                          ▼                                           │
   ┌──────────────────────────────────────────────┐                   │
   │                  OBSERVING                   │                   │
   └──────────────────────┬───────────────────────┘                   │
                          │                                           │
                          ▼                                           │
   ┌──────────────────────────────────────────────┐                   │
   │                  VERIFYING                   │ ──────────────────┘
   └──────────────────────┬───────────────────────┘
                          │
                  ┌───────┴───────┐
                  ▼               ▼
              COMPLETED         FAILED
```

### Complete Core Execution Loop Pipeline
1.  **Context Construction:** The `ContextBuilder` aggregates:
    *   The overall task goal (User Request).
    *   The currently active package name and screen metadata.
    *   The raw accessibility tree serialized to a compact JSON format.
    *   Recent interaction logs (last 5 steps).
    *   Ephemeral screenshot visual tokens (if relevant).
    *   Retrieved long-term preferences and matching procedural Skills.
2.  **Generation & Inference:** Gemma processes the prompt payload and generates output structured with thinking blocks and tool call blocks.
3.  **Tool Selection:** The execution controller extracts the tool calls.
4.  **Security & Policy Verification:**
    *   Is the target app currently blocklisted or highly sensitive (e.g. banking)? If yes, abort/fail.
    *   Does the requested tool require explicit authorization (e.g. Level 2 - Send WhatsApp)?
    *   If confirmation is required, change state to `WAITING_CONFIRMATION` and trigger user visual approval.
5.  **Execution Phase:** Dispatch action to corresponding low-level Android driver (Accessibility or Screen/OS tools).
6.  **Observation Phase:** Pause briefly (500ms - 1500ms depending on action) to let the Android UI render. Obtain new layout status from AccessibilityService and capture visual frames from MediaProjection on-demand.
7.  **Verification Phase:** Evaluate whether the visual/tree state matches the expected step outcome.
    *   *Match:* Step success. Log action. Transition back to Thinking/Planning for next step.
    *   *Mismatch / Failure:* Increment retry count. If count exceeds threshold (configurable, default 3), trigger the **Retry Engine** or request manual intervention.
8.  **Completion:** If the goal is met or unrecoverable error occurs, trigger standard teardown, flush Room logs, unload temporary media resources, and transition to `COMPLETED` or `FAILED`.

---

## 6. Tool Calling & Validation Framework

Gemma interacts with Android via structured tool calling. Every available tool schema must be registered centrally in the `ToolRegistry` and strictly validated.

### Core Tool schemas in ToolRegistry
```kotlin
object ToolSchemas {
    val TAP = ToolSchema(
        name = "tap",
        description = "Taps a visible UI element by its text identifier, resource ID, or content description.",
        parameters = mapOf(
            "target" to Parameter(type = "string", description = "Label or visual text of the element to tap", required = true)
        )
    )

    val TYPE_TEXT = ToolSchema(
        name = "type_text",
        description = "Enters specified text into the currently focused input field.",
        parameters = mapOf(
            "text" to Parameter(type = "string", description = "The text to input", required = true)
        )
    )

    val SWIPE = ToolSchema(
        name = "swipe",
        description = "Performs a swipe gesture on the screen.",
        parameters = mapOf(
            "startX" to Parameter(type = "integer", description = "Starting X coordinate", required = true),
            "startY" to Parameter(type = "integer", description = "Starting Y coordinate", required = true),
            "endX" to Parameter(type = "integer", description = "Ending X coordinate", required = true),
            "endY" to Parameter(type = "integer", description = "Ending Y coordinate", required = true)
        )
    )
}
```

### Risk Classifications
Tools are divided into distinct safety categories:
*   **LEVEL 0 (Read-Only):** `get_screen_tree`, `capture_screen`, `find_text`, `get_current_app`. (Allowed autonomously).
*   **LEVEL 1 (Low-Risk Interactions):** `open_app`, `scroll`, `tap`, `go_back`, `go_home`. (Allowed autonomously unless overall confirmation policy is strict).
*   **LEVEL 2 (High-Risk Interactions / State Changes):** `type_text`, `send_message`, `delete_content`, `confirm_transaction`. (Requires explicit user confirmation).
*   **LEVEL 3 (System Restricted):** Modifying deep operating settings, installing other packages, system-level adjustments. (Excluded from model schemas to guarantee runtime integrity).

### Security Filters & Blocklist Controls
Before any tool execution occurs, the system passes the proposed interaction through the `PolicyController`:
1.  **Blocklist Scan:** Reads the currently active package from Android `AccessibilityService`. If the package corresponds to any user-designated blocked app (e.g. Password Manager, Authentication, Banking application), tool execution is immediate terminated with a `SecurityException`.
2.  **Confirmation Mode Enforcement:**
    *   `Ask for risky actions` (Default): Checks if tool is LEVEL 2. Shows dialog box before running.
    *   `Ask for every action`: Pauses and prompts the user before *every* click, scroll, or entry.
    *   `Autonomous low-risk actions`: Executes LEVEL 0 & LEVEL 1 instantly.

---

## 7. Skill Engine & Self-Learning System

The Skill Engine turns successful task trajectories into reusable local procedural skills. This allows the system to operate more efficiently next time without requiring Gemma to plan from raw screen layouts for routine tasks.

```
                  ┌──────────────────────┐
                  │ Successful Trajectory│
                  └──────────┬───────────┘
                             │
                             ▼
                  ┌──────────────────────┐
                  │   Workflow Analyzer  │
                  └──────────┬───────────┘
                             │
                             ▼
                  ┌──────────────────────┐
                  │   Skill Discovery    │
                  │  (Pattern Matching)  │
                  └──────────┬───────────┘
                             │
                             ▼
                  ┌──────────────────────┐
                  │    Skill Creation    │
                  │   (Parametrization)  │
                  └──────────┬───────────┘
                             │
                             ▼
                  ┌──────────────────────┐
                  │   Safety Validation  │
                  └──────────┬───────────┘
                             │
                             ▼
                  ┌──────────────────────┐
                  │     Room Storage     │
                  └──────────────────────┘
```

### Learning Mechanics
*   **Discovery Phase:** When an `AgentTask` finishes with `status = COMPLETED`, the system extracts the sequence of `ToolExecution` states from the SQLite database.
*   **Pattern Matching:** If a sequence occurs repeatedly (e.g. "Launch YouTube -> Click search -> Type text"), the `WorkflowAnalyzer` categorizes it as a candidate for a reusable skill.
*   **Skill Template Creation:** The sequence is converted into an abstract template, replacing explicit inputs with parameters (e.g., specific search query string gets parameterized to `{query}`).
*   **Validation:** Before the discovered skill is activated, it undergoes validation:
    *   No execution of LEVEL 2 tools without confirmation can be hardcoded inside a skill.
    *   Skills can be manually tested, edited, disabled, or exported from the Skill Library UI.
*   **Confidence Scoring:** Each skill maintains real-time confidence parameters:
    $$\text{Confidence Score} = \frac{\text{Success Count}}{\text{Success Count} + \text{Failure Count}} \times \left(1 - e^{-n/10}\right)$$
    Where $n$ is total usage count. If confidence drops below a configured threshold (e.g. 50%), the skill is deactivated or fallback is triggered back to raw Gemma planning.

---

## 8. Android Platform Services Integration

```
                 ┌────────────────────────────────────────────────────────┐
                 │                   Android Services                     │
                 └──────────┬───────────────────┬───────────────────┬─────┘
                            │                   │                   │
                            ▼                   ▼                   ▼
                 ┌──────────────────┐┌──────────────────┐┌──────────────────┐
                 │  Accessibility   ││ MediaProjection  ││ Voice Input/TTS  │
                 │     Service      ││   (Screenshot)   ││     (Native)     │
                 └──────────────────┘└──────────────────┘└──────────────────┘
```

### 1. Accessibility Service Integration
*   Extends standard Android `AccessibilityService`.
*   Acts as both observer (by compiling the UI layout into a virtual accessibility tree) and executioner (by targeting specific node resource IDs or bound coordinates to trigger system actions).
*   Maintains a `Safe Mode` (read-only state analyzer) and an `Operator Mode` (full read + write layout controller).

### 2. Screen Capture Preprocessing (MediaProjection)
*   Integrates with Android's secure `MediaProjection` API.
*   To prevent excessive thermal usage and power drain, **continuous high-frequency screenshot capture is strictly forbidden**.
*   Instead, captures are triggered purely on-demand, immediately following a tool action or when the `AccessibilityService` signals a stable window change event.
*   Captured frames are processed inside an isolated pipeline:
    *   **Crop:** Crop irrelevant system bars (status bar/navigation bars) to conserve pixels.
    *   **Resize:** Downsample the image to match the spatial token resolution budget of the local Gemma vision adapter.
    *   **Recycle:** Ensure rapid JVM memory release by calling `.recycle()` on bitmaps instantly post-tokenization to prevent Out-Of-Memory (OOM) errors.

### 3. Native Voice Input & TTS
*   **Audio Input:** Standard Android system `SpeechRecognizer` is invoked on-demand to process verbal inputs, converting them to structured text before loading them into the prompt workspace.
*   **Audio Output:** Native Android `TextToSpeech` (TTS) system handles output. Features speech rate controls (0.8x to 1.5x) and customizable auto-speak settings. This design avoids loading heavy on-device neural voice generators, maintaining clean local model efficiency.

---

## 9. Floating UI & Control Center

The `FloatingOperatorService` provides a continuous floating overlay that overlays other applications. It acts as the direct, quick-access portal for initiating workflows, analyzing screens, and inspecting progress in real-time.

```
┌──────────────────────────────────────┐
│  Floating Bubble  (Ready / Thinking) │
└──────────────────┬───────────────────┘
                   │ Tap
                   ▼
┌──────────────────────────────────────┐
│  Quick Action / Panel Overlay        │
│  - Start Operator Task               │
│  - Screen Analyzer (Visual Highlight)│
│  - Quick Command Input (Voice/Chat)  │
└──────────────────────────────────────┘
```

### Components
*   **Floating Bubble Component:** A lightweight Draggabe Jetpack Compose view bound directly to the Android `WindowManager`. Uses basic snap-to-edge physics and dynamic color rings to reflect global AI runtime states (Green: Ready, Blue: Thinking, Purple: Acting, Yellow: Warning, Red: Error).
*   **Quick Action Panel:** Slides open horizontally or vertically on tap, exposing actions such as "Analyze Screen", "Read Screen", "Translate Screen", or "Stop All".
*   **Privacy Overlay System:** Integrates directly with screen focus monitoring. If a user transitions to a screen containing a password field or sensitive app, the floating bubble instantly fades to semi-transparency and disables screen capture functionality.

---

## 10. Performance, Resource & Thermal Protection

Running advanced model networks like Google Gemma on edge consumer chipsets requires protective resource-awareness built into the architecture.

```
                               ┌─────────────────┐
                               │ Device Analyzer │
                               └────────┬────────┘
                                        │
                                        ▼
                               ┌─────────────────┐
                               │  Thermal State  │
                               └────────┬────────┘
                                        │
                 ┌──────────────────────┼──────────────────────┐
                 ▼                      ▼                      ▼
         ┌───────────────┐      ┌───────────────┐      ┌───────────────┐
         │    Normal     │      │     Warm      │      │     Critical  │
         └───────┬───────┘      └───────┬───────┘      └───────┬───────┘
                 │                      │                      │
                 ▼                      ▼                      ▼
         Full Performance        Balanced Mode         Thermal-Safe Mode
         Aggressive Warmup       Shorter Context       Unload Model
         Max Visual Tokens       Throttle Screen       Notify User
```

### Thermal State Machine & Adaptive Profiles
*   **Normal Profile:** Maximum available acceleration (GPU/NPU). Full visual token budget. Continuous warm status for instant responsiveness.
*   **Balanced Profile:** Reduced context length. On-demand screen capture only. Slower generation cadence.
*   **Thermal-Safe Profile:** Switched entirely to low-power CPU threads. Drops visual inputs entirely, operating purely on compact Accessibility trees. If temperature hits Critical threshold, the AI model is safely unloaded from RAM, current operations are halted, and the user is notified.

### RAM & Lifecycle Management
*   **Active Tracking:** Monitors system memory allocation flags.
*   **Memory Pressure Unloader:** If Android triggers `onTrimMemory()` with level `TRIM_MEMORY_RUNNING_CRITICAL` or higher, the runtime system terminates active processing pipelines, flushes cached key-value structures, and immediately executes `unloadModel()` to prevent the OS from force-killing the background Accessibility or Overlay services.
