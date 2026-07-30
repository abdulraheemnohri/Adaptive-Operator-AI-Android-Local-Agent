Android AI Operator — V1

Design Specification

Version: 1.0
Platform: Android
AI Model: Google Gemma 4 E2B-it
Runtime: LiteRT-LM
Architecture: On-device / Local-first
UI: Jetpack Compose + Material 3
Memory: Room / SQLite
Android Integration: Accessibility Service + MediaProjection + Android TTS
Agent System: Tool Calling + Skill Engine
Network: Optional, only for model download/update and explicitly enabled features

---

1. Product Vision

Android AI Operator is a local-first, on-device Android AI agent that can understand the user's request, observe the Android screen, reason about the current state, select tools/skills, perform permitted Android actions, and continuously learn user preferences through local memory.

Core principle

«Gemma thinks. Android provides capabilities. The Skill Engine decides which capability to use.»

The Gemma model itself does not receive unrestricted Android control.

Instead:

User
 │
 ▼
AI Operator UI
 │
 ▼
Gemma 4 E2B-it
 │
 │ reasoning + tool selection
 ▼
Agent / Tool Controller
 │
 ├── Screen Reader
 ├── Accessibility Actions
 ├── Screenshot
 ├── OCR
 ├── App Launcher
 ├── Text Input
 ├── Tap
 ├── Swipe
 ├── Back
 ├── Home
 ├── Notifications
 ├── Clipboard
 ├── TTS
 └── Custom Skills
 │
 ▼
Android OS

---

2. V1 Goals

V1 focuses on:

- Fully local AI inference
- Gemma 4 E2B-it
- Android-optimized AI runtime
- Screen understanding
- Accessibility-based interaction
- Screenshot analysis
- OCR
- Tool calling
- Agentic workflows
- Floating AI assistant
- Voice input
- Android TTS
- Local memory
- Skill system
- Model download manager
- Model verification
- Model lifecycle management
- Permission management
- Safety confirmation system
- Offline operation after model installation

---

3. Technology Stack

Android

Kotlin
Jetpack Compose
Material 3
Android SDK
Coroutines
Flow
WorkManager
Room
DataStore
Android Keystore
AccessibilityService
MediaProjection
SpeechRecognizer
TextToSpeech
NotificationManager
PackageManager
ClipboardManager

AI

Gemma 4 E2B-it
LiteRT-LM
On-device inference
CPU / GPU / NPU where supported

Architecture

Clean Architecture
        +
MVVM
        +
Repository Pattern
        +
Dependency Injection
        +
Event-driven Agent Runtime

---

4. High-Level Architecture

┌──────────────────────────────────────────┐
│                Android UI                │
│                                          │
│ Compose Dashboard                        │
│ Chat                                     │
│ Floating Assistant                       │
│ Settings                                 │
│ Model Manager                            │
│ Skills                                   │
└────────────────┬─────────────────────────┘
                 │
                 ▼
┌──────────────────────────────────────────┐
│             AI Operator Core             │
│                                          │
│ Intent Analyzer                          │
│ Context Builder                          │
│ Agent Planner                            │
│ Tool Selector                             │
│ Safety Controller                        │
│ Execution Manager                        │
└────────────────┬─────────────────────────┘
                 │
       ┌─────────┴─────────┐
       ▼                   ▼
┌──────────────┐    ┌────────────────────┐
│ Gemma Engine │    │ Skill / Tool Engine│
│              │    │                    │
│ Gemma 4 E2B  │    │ Android Tools      │
│ LiteRT-LM    │    │ Screen Tools       │
│ Tokenizer    │    │ Voice Tools        │
│ Context      │    │ App Tools          │
└──────────────┘    └────────────────────┘
       │                   │
       └─────────┬─────────┘
                 ▼
┌──────────────────────────────────────────┐
│             Android Services             │
│                                          │
│ AccessibilityService                     │
│ MediaProjection                          │
│ SpeechRecognizer                         │
│ TextToSpeech                             │
│ Notification APIs                        │
│ PackageManager                           │
│ Clipboard                                │
└────────────────┬─────────────────────────┘
                 │
                 ▼
              Android OS

---

5. AI Runtime

5.1 Gemma 4 E2B-it

The primary AI model is:

google/gemma-4-E2B-it

The model provides:

- Text understanding
- Reasoning
- Image understanding
- Screen understanding
- OCR
- Document understanding
- Handwriting recognition
- Multilingual understanding
- Coding
- Audio input
- Tool/function calling
- Agent planning
- Multimodal reasoning

The model does not directly control Android.

Android capabilities are exposed as controlled tools.

---

6. LiteRT-LM Runtime

LiteRT-LM is the model execution layer.

Gemma Model
     │
     ▼
Model Adapter
     │
     ▼
LiteRT-LM
     │
 ┌───┼─────────────┐
 ▼   ▼             ▼
CPU GPU            NPU

The runtime abstraction must prevent the rest of the application from depending directly on model implementation details.

Required interface

interface AiRuntime {

    suspend fun loadModel(
        modelPath: String
    )

    suspend fun unloadModel()

    suspend fun generate(
        request: GenerationRequest
    ): GenerationResult

    suspend fun generateWithTools(
        request: AgentRequest
    ): AgentResult

    fun isLoaded(): Boolean

    fun getRuntimeInfo(): RuntimeInfo
}

---

7. Runtime Lifecycle

Application Start
      │
      ▼
Check Model
      │
 ┌────┴─────┐
 │          │
Missing    Available
 │          │
 ▼          ▼
Download   Verify
 │          │
 ▼          ▼
Verify     Load
 │          │
 └────┬─────┘
      ▼
Initialize Runtime
      │
      ▼
Warm-up
      │
      ▼
AI Ready

---

8. Model Download System

The app must include a complete Model Manager.

Model Manager features

- Model discovery
- Download
- Pause
- Resume
- Cancel
- Retry
- Progress
- Speed
- ETA
- Storage check
- Integrity verification
- Version detection
- Delete model
- Reinstall
- Model activation
- Runtime compatibility check

---

9. Model Download Flow

Model Manager
      │
      ▼
Select Gemma 4 E2B-it
      │
      ▼
Check Storage
      │
      ▼
Check Compatibility
      │
      ▼
Download
      │
      ▼
Checksum Verification
      │
      ▼
Model Installation
      │
      ▼
Runtime Preparation
      │
      ▼
Warm-up
      │
      ▼
READY

The download system should support resumable downloads where the hosting source permits it.

---

10. Model Storage

Recommended structure:

Android/data/<package>/
    models/
        gemma4-e2b/
            manifest.json
            model/
            tokenizer/
            config/
            metadata.json
            checksum

Never load an unverified model.

---

11. Model Manifest

Example:

{
  "id": "gemma-4-e2b-it",
  "name": "Gemma 4 E2B-it",
  "version": "1.0",
  "runtime": "litert-lm",
  "modalities": [
    "text",
    "image",
    "audio"
  ],
  "contextLength": 128000,
  "toolCalling": true,
  "thinking": true
}

---

12. Model Status

The UI must display:

● Ready
● Loading
● Downloading
● Verifying
● Preparing
● Running
● Paused
● Error
● Incompatible
● Storage Required

---

13. AI Context Pipeline

Every agent request passes through:

User Request
     │
     ▼
Intent Detection
     │
     ▼
Context Collection
     │
 ┌───┼─────────────┐
 ▼   ▼             ▼
Text Screen      Memory
     │
     ▼
Context Builder
     │
     ▼
Gemma
     │
     ▼
Reasoning
     │
     ▼
Tool Selection

---

14. Android Screen Understanding

Screen understanding combines:

Accessibility Tree
+
Screenshot
+
OCR
+
Current App Metadata
+
Recent Interaction History

The agent should not depend only on screenshots.

Preferred hierarchy:

Accessibility Tree
       ↓
Semantic UI information
       ↓
Screenshot
       ↓
Vision reasoning
       ↓
OCR fallback

---

15. Accessibility Service

Accessibility Service provides controlled Android interaction.

Capabilities:

- Read visible accessibility nodes
- Identify buttons
- Identify text fields
- Identify lists
- Identify checkboxes
- Identify UI descriptions
- Click supported elements
- Focus elements
- Scroll
- Set text where permitted
- Navigate backward
- Perform supported global actions

The service must expose only approved actions to the AI agent.

---

16. Accessibility Architecture

Android App
     │
     ▼
AccessibilityService
     │
     ▼
Accessibility Tree
     │
     ▼
ScreenState
     │
     ▼
Context Builder
     │
     ▼
Gemma

For actions:

Gemma
  │
  ▼
Tool Call
  │
  ▼
Safety Validator
  │
  ▼
Accessibility Controller
  │
  ▼
Android UI

---

17. Screen Capture

MediaProjection is used for visual screen context.

Flow:

User grants screen capture permission
          │
          ▼
MediaProjection
          │
          ▼
Frame capture
          │
          ▼
Resize / optimize
          │
          ▼
Gemma Vision
          │
          ▼
Screen understanding

The system should avoid continuous full-resolution capture.

Use:

- On-demand screenshots
- Low-frequency observation
- Region capture where possible
- Frame throttling
- Automatic release of temporary frames

---

18. OCR

Gemma vision capabilities can be used for visual text extraction.

OCR pipeline:

Screen
  │
  ▼
Screenshot
  │
  ▼
Vision Tokenization
  │
  ▼
Gemma
  │
  ▼
Detected Text
  │
  ▼
Structured Screen Context

Example output:

{
  "text": "Settings",
  "location": {
    "x": 120,
    "y": 240
  },
  "confidence": 0.92
}

The agent should prefer accessibility-provided text when available.

---

19. Tool Calling

Gemma should communicate with Android through structured tools.

Example:

{
  "tool": "tap",
  "arguments": {
    "target": "Settings"
  }
}

The application validates the call before execution.

---

20. Tool Execution Pipeline

Gemma
  │
  ▼
Tool Call
  │
  ▼
Schema Validation
  │
  ▼
Permission Check
  │
  ▼
Safety Check
  │
  ▼
User Confirmation?
  │
 ┌┴─────────┐
No          Yes
│            │
▼            ▼
Execute     Ask User
             │
             ▼
          Approved?
             │
       ┌─────┴─────┐
       ▼           ▼
      Yes          No
       │            │
       ▼            ▼
    Execute       Cancel

---

21. Core Android Tools

Navigation

go_home()
go_back()
open_recent_apps()
open_notifications()

Application

open_app(package)
close_app()
get_installed_apps()

Screen

get_screen_tree()
capture_screen()
find_text()
find_element()

Interaction

tap(target)
long_press(target)
swipe(start, end)
scroll(direction)
focus(target)

Text

type_text(text)
clear_text()
copy_text()
paste_text()

Voice

start_voice_input()
stop_voice_input()
speak(text)

System

Only expose permitted operations.

---

22. Tool Permission Levels

Every tool has a risk level.

LEVEL 0 — Read Only
LEVEL 1 — Low Risk
LEVEL 2 — User Confirmation
LEVEL 3 — Restricted

Example:

Level 0

read_screen
read_accessibility_tree
get_current_app

Level 1

open_app
scroll
tap
go_back

Level 2

send_message
delete_content
submit_form
purchase
change_account_settings

Level 3

Potentially dangerous/system-restricted actions should not be exposed automatically.

---

23. Agent Loop

The main AI Operator loop:

OBSERVE
   ↓
UNDERSTAND
   ↓
PLAN
   ↓
SELECT TOOL
   ↓
VALIDATE
   ↓
EXECUTE
   ↓
OBSERVE AGAIN
   ↓
VERIFY
   ↓
CONTINUE / FINISH

Example:

User:
"Open YouTube and search for Linux tutorials"

       ↓

Gemma:
Intent = navigation + search

       ↓

Tool:
open_app("YouTube")

       ↓

Observe screen

       ↓

Find search field

       ↓

Tool:
tap(search_field)

       ↓

Tool:
type_text("Linux tutorials")

       ↓

Tool:
tap(search_button)

       ↓

Verify result

       ↓

Finish

---

24. Self-Learning System

V1 should implement local adaptive memory, not unrestricted automatic weight training.

This is important.

The agent can learn:

- User preferences
- Frequently used apps
- Preferred actions
- Common commands
- Workflow patterns
- Preferred language
- Preferred response style
- Skill usage history
- Successful workflows

Memory is stored locally.

---

25. Memory Architecture

User Interaction
       │
       ▼
Event
       │
       ▼
Memory Extractor
       │
 ┌─────┴────────┐
 ▼              ▼
Preference     Workflow
Memory         Memory
       │
       ▼
SQLite / Room
       │
       ▼
Future Context
       │
       ▼
Gemma

---

26. Memory Types

Short-term memory

Current task.

Current app
Current screen
Current objective
Recent actions

Session memory

Current conversation/session.

Long-term memory

User-approved persistent information.

Example:

Preferred language: Urdu
Preferred TTS speed: 1.0
Frequently used app: WhatsApp

---

27. Memory Safety

The app must provide:

Memory enabled
Memory disabled
Ask before saving
Automatic memory
Clear session
Clear all memory
Export memory
Delete individual memory

Sensitive information should not automatically become long-term memory.

---

28. Skill Engine

Skills are reusable agent workflows.

Example:

Skill:
Send WhatsApp Message

Input:
recipient
message

Steps:
1. Open WhatsApp
2. Search recipient
3. Open conversation
4. Enter message
5. Ask confirmation
6. Send
7. Verify

---

29. Skill Structure

skills/
    whatsapp_message/
        skill.json
        instructions.md
        tools.json

    browser_search/
        skill.json

    screenshot_analysis/
        skill.json

    app_launcher/
        skill.json

---

30. Skill Definition

Example:

{
  "id": "app_launcher",
  "name": "Application Launcher",
  "version": "1.0",
  "permissions": [
    "open_app"
  ],
  "tools": [
    "get_installed_apps",
    "open_app"
  ]
}

---

31. Skill Marketplace Architecture

V1 should prepare the architecture for future local skill packages.

Skill Manager
     │
     ├── Installed
     ├── Enabled
     ├── Disabled
     ├── Updates
     └── Import

Imported skills must be validated before activation.

---

32. Floating AI System

The app includes a floating AI assistant.

          ┌─────────────────┐
          │   AI Assistant  │
          │       ●         │
          └─────────────────┘

The floating bubble can appear above other apps after the required Android permission is granted.

---

33. Floating Bubble

Features:

- Drag
- Snap to edge
- Expand
- Collapse
- Hide
- Voice activation
- Screenshot analysis
- Quick command
- Current-screen analysis
- Start/stop agent
- Open full assistant

---

34. Floating Panel

Expanded:

┌──────────────────────────────────┐
│ AI Operator                 ×    │
├──────────────────────────────────┤
│                                  │
│ What should I do?                │
│                                  │
│ [ Speak ] [ Screenshot ]         │
│                                  │
│ ───────────────────────────────  │
│                                  │
│ Quick Actions                    │
│                                  │
│ Analyze Screen                   │
│ Read Screen                      │
│ Explain This                     │
│ Summarize                        │
│                                  │
└──────────────────────────────────┘

---

35. Floating Screen Commands

Examples:

"What is this screen?"

"Explain this page."

"Find the login button."

"Read this screen."

"Scroll down."

"Open settings."

"Summarize this."

"Translate this screen."

---

36. Voice System

Architecture:

Microphone
    │
    ▼
Android SpeechRecognizer
    │
    ▼
Text
    │
    ▼
Gemma
    │
    ▼
Response
    │
    ▼
Android TTS
    │
    ▼
Speaker

Gemma E2B provides audio understanding capabilities, but native TTS is not supplied by Gemma.

Therefore V1 uses Android's built-in TTS.

---

37. Wake Word

Wake-word support should be treated as an optional future module unless a suitable local wake-word engine is bundled.

Example:

"Hey Operator"

Pipeline:

Microphone
   ↓
Wake Word Detector
   ↓
Gemma / Speech Recognition
   ↓
Agent

The wake-word detector should operate independently of Gemma to avoid keeping the large model continuously active.

---

38. Main Dashboard

┌────────────────────────────────────┐
│ AI Operator                        │
│ ● Gemma 4 E2B — Ready              │
├────────────────────────────────────┤
│                                    │
│        Good evening, Abdul         │
│                                    │
│ What can I do for you?             │
│                                    │
│ [ 🎙 Voice ] [ ⌨ Chat ]            │
│                                    │
├────────────────────────────────────┤
│ Quick Actions                      │
│                                    │
│ Analyze Screen                     │
│ Open App                           │
│ Read Screen                        │
│ Start Workflow                     │
│                                    │
├────────────────────────────────────┤
│ Runtime                            │
│ CPU  ███████░░                     │
│ RAM  ██████░░░                     │
│ AI   Ready                         │
└────────────────────────────────────┘

---

39. Navigation

Bottom navigation:

Home
Chat
Operator
Skills
Settings

Secondary pages:

Models
Memory
History
Permissions
Runtime
Logs
Privacy
About

---

40. Chat Screen

Features:

- Text input
- Voice input
- Image attachment
- Screen capture
- Tool-call visualization
- Thinking indicator
- Agent status
- Stop generation
- Retry
- Copy
- Share
- Save
- Clear conversation

---

41. Agent Visualization

The user should see what the agent is doing without exposing private chain-of-thought.

Example:

● Understanding request

✓ Screen inspected

● Finding search field

✓ Search field found

● Entering text

✓ Completed

● Verifying result

✓ Done

Do not display hidden internal reasoning.

---

42. Operator Mode

Operator Mode is the main autonomous execution interface.

┌───────────────────────────────┐
│ Operator Mode                 │
├───────────────────────────────┤
│                               │
│ Goal                          │
│ "Open Chrome and search..."   │
│                               │
│ ───────────────────────────── │
│                               │
│ ● Planning                    │
│ ✓ Open Chrome                 │
│ ✓ Find search                 │
│ ● Enter query                 │
│ ○ Verify                      │
│                               │
├───────────────────────────────┤
│ [ Pause ] [ Stop ]            │
└───────────────────────────────┘

---

43. Settings

AI Settings

Model
Thinking Mode
Maximum Output Tokens
Context Limit
Temperature
Top-P
Tool Calling
Agent Mode

Runtime Settings

Runtime
CPU/GPU/NPU preference
Threads
Memory limit
KV cache
Context size
Power saving
Thermal protection
Model warm-up
Unload when idle

---

44. Performance Modes

Battery Saver

Low resolution
Low context
Short responses
Aggressive model unloading
Reduced screen capture

Balanced

Default configuration.

Performance

Higher context
Faster generation
Higher visual quality
Less aggressive unloading

Maximum

Uses the maximum safe resources available on the device.

---

45. Thermal Protection

The application must monitor Android thermal status.

Normal
    ↓
Warm
    ↓
Hot
    ↓
Critical

At high temperatures:

Reduce inference
Reduce capture rate
Reduce context
Unload model if necessary
Notify user

---

46. Memory Management

Gemma inference can consume significant RAM.

The runtime manager should support:

Model loaded
Model idle
Model warm
Model unload

When Android memory pressure increases:

Stop generation
Release temporary images
Release KV cache
Unload model

---

47. Battery Optimization

Avoid:

Continuous screenshot capture
Continuous vision inference
Permanent model generation
Aggressive background processing

Use event-driven processing.

---

48. Permission Center

Dedicated screen:

Accessibility
Screen Capture
Microphone
Notifications
Overlay
Speech Recognition

Each permission shows:

Status
Why required
Enable button
Test button

Example:

Accessibility Service

Status: OFF

Required for:
• Reading supported UI elements
• Clicking supported elements
• Scrolling
• Text input

[ Enable ]

---

49. Security Center

Features:

Tool permissions
Confirmation policy
Memory controls
App allowlist
Sensitive app protection
Activity log
Clear temporary data
Delete all local data

---

50. App Allowlist

The user can choose which applications the operator can interact with.

Allowed Apps

Chrome       ✓
YouTube      ✓
WhatsApp     ✓
Settings     ✓
Banking App  ✕
Password App ✕

---

51. Sensitive App Protection

The user can mark applications as:

Blocked
Read-only
Confirmation required
Full access

Recommended default:

Financial apps → blocked
Password managers → blocked
Authentication apps → blocked
Messaging → confirmation required
Browser → normal

---

52. Confirmation System

Before sensitive actions:

┌─────────────────────────────┐
│ Confirmation Required       │
├─────────────────────────────┤
│ AI wants to:                │
│ Send this message           │
│                             │
│ "Hello, I'll call later."   │
│                             │
│ [ Cancel ] [ Allow ]        │
└─────────────────────────────┘

---

53. Action History

Every tool action can be logged locally.

10:21 Open Chrome
10:22 Read screen
10:22 Tap search field
10:22 Type query
10:23 Search

The user can disable logging.

---

54. AI Runtime Monitor

Display:

Model:
Gemma 4 E2B-it

Runtime:
LiteRT-LM

Status:
Running

Device:
CPU/GPU/NPU

Tokens/sec:
XX

Context:
XXXX / XXXXX

RAM:
XXX MB

Temperature:
XX°C

Power:
XX%

---

55. Model Manager UI

┌────────────────────────────────┐
│ Models                         │
├────────────────────────────────┤
│                                │
│ Gemma 4 E2B-it                 │
│                                │
│ ● Installed                    │
│ ● Compatible                   │
│                                │
│ Runtime: LiteRT-LM             │
│ Modalities: Text Image Audio   │
│                                │
│ [ Launch ] [ Manage ]          │
│                                │
├────────────────────────────────┤
│ Storage                        │
│ Models: 5.2 GB                 │
│ Free: 18.4 GB                  │
└────────────────────────────────┘

---

56. Model Download UI

Gemma 4 E2B-it

Downloading...

████████████████░░░ 82%

4.1 GB / 5.0 GB

Speed: 18 MB/s
ETA: 52 sec

[ Pause ] [ Cancel ]

Actual model size depends on the exact runtime/quantization package selected; the UI must read size from the model manifest rather than hard-code it.

---

57. Model Compatibility Check

Before installation:

Device Check

RAM             ✓
Storage         ✓
Android Version ✓
CPU             ✓
GPU             ✓
NPU             ?
Runtime         ✓

Result:
Compatible

If not:

Not Recommended

Reason:
Insufficient available memory

Suggested:
Close background applications
or use a smaller/quantized package.

---

58. Data Layer

Room database entities:

Conversation
Message
Memory
Preference
Skill
SkillExecution
AgentTask
ToolExecution
Model
ModelDownload
AppPermission
UserSetting
ActionLog

---

59. Repository Layer

AiRepository
ModelRepository
MemoryRepository
SkillRepository
ConversationRepository
ToolRepository
SettingsRepository
PermissionRepository

---

60. Agent Engine

Core modules:

AgentEngine
IntentAnalyzer
ContextBuilder
Planner
ToolSelector
ToolValidator
ToolExecutor
ObservationManager
VerificationManager
MemoryManager

---

61. Agent Engine Interface

interface AgentEngine {

    suspend fun execute(
        request: AgentRequest
    ): AgentResult

    suspend fun pause()

    suspend fun resume()

    suspend fun stop()

    fun observeState(): Flow<AgentState>
}

---

62. Agent State

IDLE
THINKING
OBSERVING
PLANNING
WAITING_CONFIRMATION
EXECUTING
VERIFYING
COMPLETED
FAILED
STOPPED

---

63. Error Recovery

If an action fails:

Action Failed
     │
     ▼
Observe Again
     │
     ▼
Re-plan
     │
     ▼
Try Alternative
     │
     ▼
Success / Ask User

Maximum retry count must be configurable.

Default:

3 attempts

---

64. Infinite Loop Protection

The agent must stop when:

same action repeated
same screen repeated
no progress detected
tool repeatedly fails
maximum steps reached
timeout reached
user presses stop

Example:

Maximum Agent Steps: 30
Maximum Runtime: 5 minutes

---

65. Task Manager

Users can create tasks:

One-time task
Scheduled task
Recurring task
Manual task

Important: Android background restrictions mean not every operator workflow can run indefinitely in the background. The app must respect Android's service and background-execution policies.

---

66. Workflow Builder

Future-ready visual workflow:

START
  ↓
OPEN APP
  ↓
WAIT
  ↓
READ SCREEN
  ↓
IF
 ├── Found → TAP
 └── Not Found → SEARCH
  ↓
VERIFY
  ↓
END

---

67. Settings — Agent

Agent Mode
Maximum Steps
Maximum Runtime
Auto Retry
Verification
Confirmation Policy
Allow Background Operation
Auto Stop
Loop Detection

---

68. Settings — Vision

Enable Vision
Image Resolution
Visual Token Budget
Screenshot Frequency
OCR
Screen Region
Auto Capture

---

69. Settings — Voice

Voice Input
Speech Recognition
TTS
TTS Voice
Speech Rate
Pitch
Auto Speak
Voice Language

---

70. Settings — Memory

Enable Memory
Automatic Memory
Ask Before Saving
Session Memory
Long-Term Memory
Memory Retention
Clear Memory
Export Memory

---

71. Settings — Floating Assistant

Enable Floating Bubble
Bubble Position
Bubble Size
Opacity
Auto Hide
Show on App Launch
Voice Button
Screen Analysis Button
Quick Actions

---

72. Settings — Privacy

Local-only mode
Cloud features
Screen capture retention
Audio retention
Conversation retention
Action logs
Crash logs
Automatic deletion

Default:

No screen/audio retention
No cloud AI
No automatic upload

---

73. Offline Architecture

After model installation:

Internet
   X
   │
   ▼
Android AI Operator
   │
   ├── Gemma
   ├── Memory
   ├── Skills
   ├── Screen
   ├── Accessibility
   ├── TTS
   └── Agent

The core AI experience should remain functional without Internet access.

Internet may be required for:

Model download
Model update
Skill package download
Optional external features

---

74. Model Adapter

Do not couple the entire application to Gemma-specific code.

Use:

AiModelAdapter
      │
      ▼
Gemma4E2BAdapter
      │
      ▼
LiteRT-LM

This keeps the architecture future-ready.

V1 nevertheless ships with only Gemma 4 E2B-it.

---

75. Project Structure

android-ai-operator/
│
├── app/
│
├── core/
│   ├── ai/
│   ├── runtime/
│   ├── agent/
│   ├── tools/
│   ├── skills/
│   ├── memory/
│   ├── security/
│   └── common/
│
├── data/
│   ├── database/
│   ├── repositories/
│   ├── datastore/
│   └── models/
│
├── domain/
│   ├── agent/
│   ├── ai/
│   ├── memory/
│   ├── skills/
│   └── tools/
│
├── feature/
│   ├── home/
│   ├── chat/
│   ├── operator/
│   ├── models/
│   ├── skills/
│   ├── memory/
│   ├── history/
│   ├── settings/
│   └── permissions/
│
├── service/
│   ├── accessibility/
│   ├── floating/
│   ├── projection/
│   ├── voice/
│   └── tts/
│
├── runtime/
│   └── gemma/
│
└── docs/
    ├── DESIGN.md
    ├── ARCHITECTURE.md
    ├── SECURITY.md
    └── MODEL.md

---

76. UI Design Language

Use:

Material 3
Dynamic color
Dark mode
Light mode
Large rounded cards
Minimal visual noise
Clear AI status
Motion feedback
Accessible typography

Visual personality:

Modern
Premium
Technical
Minimal
AI-native
Android-native

---

77. Color Semantics

Do not use colors merely for decoration.

Use semantic states:

Primary → AI actions
Success → completed
Warning → confirmation
Error → failure
Neutral → information

Support dynamic Android colors.

---

78. AI Status Indicator

Global status:

● AI Ready
● AI Thinking
● AI Observing
● AI Executing
● AI Waiting
● AI Error

The floating bubble changes its state visually.

---

79. Accessibility UX

The application itself must support:

- Screen readers
- Large fonts
- TalkBack
- High contrast
- Touch targets
- Reduced motion
- Keyboard navigation where applicable

---

80. Privacy Architecture

Default data flow:

Microphone
    ↓
Local Speech Recognition
    ↓
Gemma
    ↓
Local Response
    ↓
Android TTS

No unnecessary external transmission.

Screenshots are temporary unless explicitly saved.

---

81. Security Boundaries

Critical rule:

Gemma ≠ Android permissions

Gemma proposes actions.

Android application decides whether actions are allowed.

MODEL
 ↓
PROPOSE
 ↓
VALIDATE
 ↓
AUTHORIZE
 ↓
EXECUTE

This separation is mandatory.

---

82. Prompt Architecture

System prompt should define:

Role
Capabilities
Available tools
Tool schemas
Safety policy
Current screen context
User preferences
Current task
Response format

The model should never be told that it has unrestricted Android access.

Correct:

"You can request Android tools.
The application will validate every tool request."

---

83. Tool Schema

Example:

{
  "name": "tap",
  "description": "Tap an available UI element",
  "parameters": {
    "type": "object",
    "properties": {
      "target": {
        "type": "string"
      }
    },
    "required": [
      "target"
    ]
  }
}

---

84. Screen Context Format

The context builder should provide structured information.

Example:

{
  "package": "com.android.settings",
  "activity": "Settings",
  "screen": {
    "width": 1080,
    "height": 2400
  },
  "elements": [
    {
      "text": "Network & Internet",
      "role": "button",
      "clickable": true
    }
  ]
}

Screenshot can be attached when visual understanding is required.

---

85. Model Context Optimization

Do not send everything to Gemma every time.

Use:

Relevant UI nodes
+
Current screen
+
Relevant memory
+
Current task
+
Recent actions

This reduces:

- Latency
- RAM usage
- Token consumption
- Battery usage

---

86. Screen Observation Strategy

Use adaptive observation.

Stable screen
   ↓
No screenshot needed

Action performed
   ↓
Capture new state

Screen changed
   ↓
Analyze

No change
   ↓
Re-plan / stop

---

87. Example Full Workflow

User:

«"Open Chrome and search for Android AI tools."»

System:

1. Parse request
2. Identify Chrome
3. Open Chrome
4. Observe
5. Find search field
6. Tap search field
7. Type query
8. Submit
9. Observe results
10. Verify search completed
11. Report completion

---

88. Example Screen Question

User:

«"What is on my screen?"»

Pipeline:

Floating Assistant
      ↓
Capture Screen
      ↓
Accessibility Tree
      ↓
Gemma Vision
      ↓
Screen Understanding
      ↓
Answer
      ↓
Android TTS

---

89. Example Voice Workflow

User speaks
      ↓
SpeechRecognizer
      ↓
Text
      ↓
AgentEngine
      ↓
Gemma
      ↓
Tool call
      ↓
Android
      ↓
Result
      ↓
Gemma
      ↓
Response
      ↓
TTS

---

90. Startup Sequence

Application Start
       ↓
Load settings
       ↓
Check permissions
       ↓
Check model
       ↓
Initialize Room
       ↓
Initialize Skill Engine
       ↓
Initialize Tool Registry
       ↓
Initialize Gemma runtime
       ↓
Model warm-up
       ↓
AI READY

---

91. Shutdown Sequence

Stop generation
      ↓
Cancel agent
      ↓
Release screenshot
      ↓
Release audio
      ↓
Save required state
      ↓
Flush logs
      ↓
Unload model
      ↓
Release runtime

---

92. V1 Feature Matrix

Feature| V1
Gemma 4 E2B-it| ✅
LiteRT-LM| ✅
Local inference| ✅
Text chat| ✅
Reasoning| ✅
Vision| ✅
OCR| ✅
Screen understanding| ✅
Accessibility| ✅
Screen capture| ✅
Tool calling| ✅
Agent loop| ✅
Skills| ✅
Local memory| ✅
Room| ✅
Floating assistant| ✅
Voice input| ✅
Android TTS| ✅
Model downloader| ✅
Model manager| ✅
Runtime monitor| ✅
Thermal protection| ✅
Permission center| ✅
Security center| ✅
Action history| ✅
Multi-model| ✕
Cloud AI| ✕
Provider APIs| ✕
External AI APIs| ✕

---

93. V1 Non-Goals

Do not add:

Multiple AI providers
Cloud LLM routing
OpenAI API
Gemini API
Claude API
Remote AI server
Cloud memory
Automatic unrestricted Android control
Password collection
Cookie/session extraction
CAPTCHA bypass

V1 is intentionally:

«One model. One local runtime. One Android agent.»

---

94. Development Phases

Phase 1 — Foundation

Android project
Compose
Navigation
Room
DataStore
Dependency injection
Settings

Phase 2 — Model

Model downloader
Model verification
Gemma package
LiteRT-LM integration
Runtime abstraction
Model manager

Phase 3 — AI

Chat
Context
Generation
Thinking mode
Streaming
Stop generation

Phase 4 — Vision

Screen capture
Image preprocessing
Screen context
OCR
Vision prompting

Phase 5 — Operator

Accessibility
Tool registry
Tool calling
Agent loop
Execution
Verification

Phase 6 — Skills

Skill engine
Skill manager
Skill execution
Skill permissions

Phase 7 — Floating System

Overlay
Bubble
Panel
Quick actions
Screen analysis
Voice

Phase 8 — Memory

Short-term
Session
Long-term
Preferences
Memory controls

Phase 9 — Hardening

Security
Permissions
Thermal handling
Memory pressure
Battery optimization
Crash recovery
Loop protection

---

95. Final Architecture

The final V1 system should look like:

                 USER
                   │
          ┌────────┴────────┐
          │                 │
       Chat UI        Floating AI
          │                 │
          └────────┬────────┘
                   ▼
             AGENT ENGINE
                   │
       ┌───────────┼───────────┐
       │           │           │
   Context      Planner      Memory
   Builder                    │
       │           │          │
       └───────────┼──────────┘
                   ▼
              GEMMA 4 E2B
                   │
             LiteRT-LM
                   │
       ┌───────────┼──────────────┐
       ▼           ▼              ▼
     CPU          GPU            NPU
                   │
                   ▼
              TOOL ENGINE
                   │
       ┌───────────┼───────────────┐
       ▼           ▼               ▼
 Accessibility  Screen Capture   Voice/TTS
       │           │               │
       └───────────┼───────────────┘
                   ▼
              ANDROID OS

---

96. Core Design Principle

The application should not be designed as:

Chatbot + some buttons

It should be designed as:

On-device AI Runtime
        +
Agent Engine
        +
Android Operating Interface
        +
Memory
        +
Skills
        +
Safety
        +
Floating Assistant

The resulting product is essentially a local Android AI operating layer.

Gemma 4 E2B-it provides the intelligence, LiteRT-LM provides local inference, Accessibility/MediaProjection provide controlled observation and interaction, Tool Calling connects the model to Android capabilities, Room provides persistent local memory, Skills provide reusable workflows, and Android TTS provides voice output.

---

97. V1 Success Criteria

V1 is considered successful when a supported Android device can:

1. Download and install the Gemma 4 E2B-it runtime package.
2. Verify the model.
3. Load the model through LiteRT-LM.
4. Run inference locally.
5. Accept text and voice requests.
6. Analyze screenshots.
7. Understand supported Android UI through Accessibility.
8. Generate structured tool calls.
9. Validate tool calls.
10. Execute permitted Android actions.
11. Observe the resulting screen.
12. Verify whether the action succeeded.
13. Recover from simple failures.
14. Store user-approved preferences locally.
15. Execute reusable Skills.
16. Operate through the floating assistant.
17. Speak responses using Android TTS.
18. Continue core AI experience without Internet after model installation.
19. Respect Android permissions and security boundaries.
20. Stop safely when the user requests it.

---

98. Product Identity

Name: Android AI Operator

Short name: AIOperator

Tagline:

«Your Android. Your AI. Running locally.»

Alternative:

«One Device. One Local Model. One AI Operator.»

---

99. Design Philosophy

LOCAL FIRST
PRIVACY FIRST
USER CONTROLLED
MODEL CONTROLLED
TOOL VALIDATED
ANDROID NATIVE
RESOURCE AWARE
OFFLINE CAPABLE
EXTENSIBLE
SAFE BY DEFAULT
