# Adaptive Operator AI — Android Local Agent

“Your Android. One Local AI. Learns How You Work.”

An Android app that runs **Gemma 4 E2B-it** entirely on-device via **LiteRT-LM** and turns it
into an agent that can read your screen (Accessibility Service + MediaProjection), plan and
execute actions, remember what worked, and turn repeated workflows into reusable local Skills.
No cloud AI, no API keys, no backend, no second model — see the architecture doc this scaffold
was built from for the full rationale.

## Status

This is a from-scratch scaffold, not a finished app. It has never been opened in Android Studio
or run through a Gradle build (no Android SDK in the environment this was generated in) — treat
everything below as "should compile" rather than "verified compiling," and expect a few real
build errors on first sync, especially around the LiteRT-LM API surface (see **Known risks**).

### Built and wired end-to-end
- **Project foundation** — Gradle (Kotlin DSL), Hilt DI, Compose/Material 3 dark theme, bottom-nav shell, manifest with accessibility/overlay/MediaProjection/voice permissions, adaptive launcher icon
- **Model Manager** (Sections 4–9) — `ModelSpec`, `ModelDownloader` (OkHttp, real HTTP Range pause/resume, retry+backoff), `ModelVerifier` (SHA-256), `ModelManager` state machine (download → verify → install → warm-up → ready), `HardwareProfiler`
- **AI runtime** — `GemmaEngineWrapper` around the real `com.google.ai.edge.litertlm` Kotlin API, with NPU → GPU → CPU fallback
- **Memory** (Section 24) — full Room schema: tasks, experiences, skills, skill_steps, preferences, tool_history, conversation; `MemoryRepository` facade
- **Tools & Security** (Sections 20–21, 43–45) — 14 tools wired to the Accessibility Service, `ToolRegistry` (parse → validate → policy → execute → log), `SecurityPolicy` (risk classification + confirmation modes), `BlocklistManager`, `PermissionManager`, `EmergencyStop`
- **Agent loop** (Section 22) — `AgentOrchestrator`, `Planner` (parses Gemma's JSON tool calls out of raw/prose output), `RetryEngine` (3-attempt ceiling)
- **Skills** (Sections 26–28) — `SkillEngine` promotes a goal repeated ≥3 times successfully into a replayable `Skill` with real tool-call arguments, and reinforces/decays confidence on reuse
- **Android integration** — `OperatorAccessibilityService` (read tree, tap/swipe/scroll/type/back, Safe Mode vs Operator Mode gate), `ScreenCaptureManager`/`ScreenCaptureService` (event-driven MediaProjection, not continuous), `VoiceInputManager` (SpeechRecognizer), `TextToSpeechManager`
- **All 8 screens** — Home, Chat, Operator, Model Manager, Skills, Memory, Security Center, Settings, each with a real Hilt ViewModel
- **Floating Operator** (Sections 32–38) — draggable, edge-snapping overlay bubble with an expandable status panel, built on plain Android views (not Compose-in-overlay, to avoid hand-rolling a Lifecycle/SavedStateRegistry owner for a bubble this simple)

### Deliberately simplified for V1 (see comments at each site)
- Skill trigger matching is substring match on stored phrases, not embeddings (the Hard Architecture Rule rules out a second model)
- The confirmation UI for risky tool calls lives in Chat/Operator screens; there's no separate modal yet
- Settings screen is one consolidated page, not the fully nested tree in the spec's Section 47
- User-correction-driven learning (the fourth signal in Section 28) has no UI affordance yet — only success/failure/repetition feed the skill engine so far

### Not started
- Screenshot → Gemma multimodal wiring (the `screenshot` tool captures a `Bitmap`; nothing sends it into the Conversation as an image input yet)
- Model update/rollback flow (Section 57), resource monitor dashboard (Section 53), thermal-triggered profile switching at runtime (the policy exists in `HardwareProfiler`, nothing calls it on a timer yet)
- Instrumented/unit tests

## Setup

1. **Android Studio**, latest stable (Koala/Ladybug era or newer), JDK 17.
2. Open the project root. Let Android Studio regenerate the Gradle wrapper jar on first sync
   (this repo ships `gradle/wrapper/gradle-wrapper.properties` pinned to Gradle 8.9, but not the
   wrapper jar binary itself — Android Studio does this automatically; if you're on the CLI
   instead, run `gradle wrapper` once with any local Gradle install).
3. **Hugging Face token**: the LiteRT Community build of Gemma 4 E2B-it
   (`litert-community/gemma-4-E2B-it-litert-lm`) is a gated repo. Create a **read-scoped** access
   token at huggingface.co/settings/tokens, accept the model's license on its model page, and
   paste the token into the Model Manager screen's download field. Don't commit it anywhere.
4. First run: Home → Model Manager → paste token → Download AI Model. The app verifies the
   checksum, installs to `filesDir/models/gemma-4-e2b/`, and warms up the runtime.
5. Enable the Accessibility Service and the overlay permission from the Security Center screen
   before trying Operator Mode — both are OS-level prompts the app can deep-link to but not grant
   itself.

## Known risks / things to verify against the SDK version you actually pull in

- `GemmaEngineWrapper` uses `Engine`, `EngineConfig`, `Conversation`, `ConversationConfig`,
  `Backend.NPU/GPU/CPU`, and `Message.of(...)` from `com.google.ai.edge.litertlm`. This matches
  Google's published Kotlin/Android API as of mid-2026, but LiteRT-LM is a young, fast-moving
  library — check the exact class/parameter names against whatever `litertlm-android` version
  Gradle actually resolves (`latest.release` in `app/build.gradle.kts`) before assuming a build
  failure here is your bug and not a version drift from this scaffold.
- `ModelSpec.expectedSizeBytes` and `sha256` are placeholders — real values need to come from the
  Hugging Face repo's actual file listing (`expectedSizeBytes` is currently advisory-only in
  `ModelVerifier`; `sha256` is null-safe and skips the checksum check until you fill it in).
- The Gradle dependency versions (AGP 8.6.1, Kotlin 2.0.21, Hilt 2.52, Compose BOM 2024.12.01,
  Room 2.6.1) were current as of this scaffold's creation — bump them if Android Studio flags
  newer stable releases.

## Architecture

```
Adaptive Operator AI
├── presentation/   Compose screens + ViewModels (Home, Chat, Operator, Model Manager,
│                   Skills, Memory, Security, Settings, Floating Operator)
├── ai/
│   ├── runtime/    ModelManager, ModelDownloader/Verifier, GemmaEngineWrapper, HardwareProfiler
│   ├── context/    ContextBuilder — assembles the one structured prompt payload
│   └── tools/      Tool interface, 14 implementations, ToolRegistry (policy-gated dispatch)
├── agent/          AgentOrchestrator (understand→plan→execute→observe→verify→done), Planner, RetryEngine
├── android/        AccessibilityService (hands), ScreenCapture (eyes), Voice (SpeechRecognizer + TTS)
├── memory/         Room: tasks, experiences, skills, skill_steps, preferences, tool_history, conversation
├── skills/         SkillEngine — promotes repeated successful goals into replayable skills
└── security/       SecurityPolicy, BlocklistManager, PermissionManager, EmergencyStop
```

Gemma = Brain · Screen Capture = Eyes · Accessibility = Hands · Room = Memory ·
Skill Engine = Learned Procedures · LiteRT-LM = Neural Runtime · Android TTS = Voice.
