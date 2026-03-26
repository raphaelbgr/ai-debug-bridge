# AI Debug Bridge -- Implementation Plan

## Detailed 4-Phase Build Plan

### Overview

Four-phase build plan taking AI Debug Bridge from core Android introspection to cross-platform support. Each phase builds on the previous, with clear acceptance criteria, dependency tracking, and risk assessment.

---

## Architecture Overview

### System Architecture (ASCII)

```
+-----------------------------------------------------------------------+
|  AI Agent (Claude Code, custom agent, CI/CD pipeline)                  |
|                                                                        |
|  Connects via HTTP/WebSocket/MCP to localhost:8690                     |
|  (or via adb forward tcp:8690 tcp:8690 for remote access)             |
+-----------------------------------+-----------------------------------+
                                    |
                            HTTP / WebSocket / MCP
                            (localhost:8690)
                                    |
+-----------------------------------v-----------------------------------+
|                        AI DEBUG BRIDGE LIBRARY                         |
|                    (debugImplementation, in-process)                    |
|                                                                        |
|  +------------------+  +------------------+  +-------------------+     |
|  |   BridgeServer   |  |  CausalEvent     |  |   MCP Protocol    |     |
|  |   (Ktor/Netty)   |  |  Stream          |  |   Handler         |     |
|  |                   |  |  (Ring Buffer)   |  |   (JSON-RPC 2.0)  |     |
|  |  - REST routes    |  |                  |  |                   |     |
|  |  - WebSocket      |  |  - Event emit    |  |  - tools/list     |     |
|  |  - HMAC auth      |  |  - Causal chain  |  |  - tools/call     |     |
|  |  - CORS           |  |  - History query |  |  - Error mapping  |     |
|  +--------+---------+  +--------+---------+  +---------+---------+     |
|           |                      |                      |              |
|  +--------v----------------------v----------------------v---------+    |
|  |                    DISCOVERY ENGINE                             |    |
|  |                                                                 |    |
|  |  +----------------+  +-----------------+  +----------------+    |    |
|  |  | ActivityTracker|  | ViewTreeScanner |  | FocusGraph     |    |    |
|  |  |                |  |                 |  | Builder        |    |    |
|  |  | - Lifecycle    |  | - Recursive     |  |                |    |    |
|  |  |   callbacks    |  |   traversal     |  | - Directed     |    |    |
|  |  | - Fragment     |  | - Property      |  |   graph        |    |    |
|  |  |   tracking     |  |   extraction    |  | - BFS path     |    |    |
|  |  | - NavController|  | - Compose       |  |   finding      |    |    |
|  |  |   discovery    |  |   semantics     |  | - Cycle detect |    |    |
|  |  +----------------+  +-----------------+  +----------------+    |    |
|  |                                                                 |    |
|  |  +----------------+  +-----------------+  +----------------+    |    |
|  |  | OverlayDetector|  | StateInspector  |  | InputSimulator |    |    |
|  |  |                |  |                 |  |                |    |    |
|  |  | - Dialog       |  | - SharedPrefs   |  | - Touch inject |    |    |
|  |  | - Popup        |  | - ViewModel     |  | - Key inject   |    |    |
|  |  | - Toast        |  | - Room/SQLite   |  | - Multi-touch  |    |    |
|  |  | - BottomSheet  |  | - DataStore     |  | - Gesture path |    |    |
|  |  | - System       |  | - Custom        |  | - Secure text  |    |    |
|  |  +----------------+  +-----------------+  +----------------+    |    |
|  +-----------------------------------------------------------------+    |
|                                                                        |
|  +------------------------------------------------------------------+  |
|  |                   EXTENSION LAYER                                 |  |
|  |                                                                   |  |
|  |  +-------------------+  +--------------------+  +--------------+  |  |
|  |  | Custom Endpoints  |  | Plugin System      |  | Project-     |  |  |
|  |  | (registerEndpoint)|  | (ServiceLoader)    |  | Specific     |  |  |
|  |  |                   |  |                    |  | Hooks        |  |  |
|  |  | - Auto MCP export |  | - Network plugin   |  |              |  |  |
|  |  | - Type-safe I/O   |  | - Database plugin  |  | - Player     |  |  |
|  |  | - Hot registration|  | - Perf plugin      |  | - Auth       |  |  |
|  |  +-------------------+  +--------------------+  | - Analytics  |  |  |
|  |                                                  +--------------+  |  |
|  +------------------------------------------------------------------+  |
+------------------------------------------------------------------------+
                                    |
                    Runs inside the Android app process
                    (debug builds only, via debugImplementation)
                                    |
+-----------------------------------v-----------------------------------+
|                         ANDROID APPLICATION                            |
|                                                                        |
|  Application.onCreate() {                                              |
|      // One-line init via reflection (safe no-op in release):          |
|      try { Class.forName("com.aidebugbridge.AiDebugBridge")           |
|              .getMethod("init", Context::class.java)                   |
|              .invoke(null, this) } catch (_: Exception) {}             |
|  }                                                                     |
|                                                                        |
|  Activities, Fragments, Compose screens, Services, ViewModels...       |
+------------------------------------------------------------------------+
```

### Data Flow Architecture

```
AI Agent Request Flow:
======================

Agent                Bridge Server         Discovery Engine        App Objects
  |                       |                       |                      |
  |-- GET /current ------>|                       |                      |
  |                       |-- scanCurrentScreen ->|                      |
  |                       |                       |-- Activity.window -->|
  |                       |                       |<-- rootView --------|
  |                       |                       |-- recursive scan -->|
  |                       |                       |<-- ViewNode tree ---|
  |                       |                       |-- Fragment list --->|
  |                       |                       |<-- FragmentInfo ----|
  |                       |                       |-- NavController --->|
  |                       |                       |<-- NavGraph --------|
  |                       |<-- ScreenInfo --------|                      |
  |<-- JSON response -----|                       |                      |
  |                       |                       |                      |
  Total latency: <1ms (all in-process, no IPC)


Event Streaming Flow:
=====================

App Event                CausalEventStream       WebSocket Clients
  |                           |                        |
  |-- onClick emitted ------->|                        |
  |                           |-- assign ID, parentId  |
  |                           |-- store in ring buffer  |
  |                           |-- index by ID           |
  |                           |-- SharedFlow.emit() --->|
  |                           |                        |<-- JSON event
  |                           |                        |
  Total latency: <100 microseconds from event to client
```

### Dependency Graph

```
Phase 1 Dependencies:
=====================

BridgeConfig ─────────────────────────────────┐
                                               │
Data Models ──────────────────────────────────┐│
  (AppMap, ScreenInfo, ViewNode, FocusInfo,   ││
   EventInfo, OverlayInfo, ActionResult,      ││
   McpTypes, NavigationGraph)                 ││
                                               ││
ActivityTracker ──────────────┐               ││
  depends on: Data Models     │               ││
                              v               vv
ViewTreeScanner ──────> BridgeServer <── CausalEventStream
  depends on:             depends on:      depends on:
  - Data Models           - BridgeConfig   - Data Models
  - ActivityTracker       - All scanners
                          - Event stream
         │                     │
         v                     v
    FocusGraphBuilder    REST Endpoints ──────> MCP Handler
    depends on:          depends on:           depends on:
    - ViewTreeScanner    - BridgeServer        - REST Endpoints
    - Data Models        - All scanners        - Data Models
                         - Event stream
                              │
                              v
                       WebSocket Events
                       depends on:
                       - BridgeServer
                       - CausalEventStream


Phase 2 Dependencies (extends Phase 1):
========================================

Phase 1 Core ──> ComposeIntrospector ──> Compose State Observer
                   depends on:             depends on:
                   - ViewTreeScanner       - ComposeIntrospector
                   - Compose UI (compileOnly)
                        │
                        v
                 Recomposition Tracker ──> Modifier Inspector
                   depends on:
                   - ComposeIntrospector
                   - CausalEventStream


Phase 3 Dependencies (extends Phase 1):
========================================

Phase 1 Core ──> Custom Endpoint API ──> Plugin System
                   depends on:            depends on:
                   - BridgeServer         - Custom Endpoint API
                   - MCP Handler          - ClassLoader
                        │
                        v
                 ML Model Hooks
                   depends on:
                   - Plugin System
                   - CausalEventStream


Phase 4 Dependencies (extends Phase 1):
========================================

Phase 1 Core ──> KMP Shared Module ──> iOS Bridge
                   depends on:          depends on:
                   - Data Models        - KMP Shared
                   - MCP Handler        - UIKit introspection
                        │
                        v
                 Flutter Bridge ──────> React Native Bridge
                   depends on:          depends on:
                   - KMP Shared          - KMP Shared
                   - Flutter engine      - React Native bridge
```

---

## Phase 1: Core Android (Current Phase)

**Goal:** A functional embedded debug server that can introspect any Android app and serve AI agents over HTTP/WebSocket/MCP. This is the minimum viable product that proves the architecture works and provides immediate value.

**Duration estimate:** 4-6 weeks

### 1.1 Project Scaffold and Build System

| Task | Complexity | Status | Notes |
|------|-----------|--------|-------|
| Multi-module Gradle setup (library + sample) | Low | Done | KGP + AGP coordinated versions |
| Kotlin Serialization plugin integration | Low | Done | kotlinx.serialization-json |
| Ktor dependency configuration | Low | Done | Ktor 2.x with Netty engine |
| `debugImplementation` consumer setup in sample | Low | Done | Zero release footprint verified |
| Maven publishing configuration | Low | Done | Maven Central via Sonatype |
| ProGuard/R8 consumer rules | Low | Pending | Keep all @Serializable classes |
| Version catalog (`libs.versions.toml`) | Low | Done | Centralized dependency management |
| CI/CD pipeline (GitHub Actions) | Low | Pending | Build, test, lint, publish |

**Acceptance criteria:** `./gradlew :ai-debug-bridge:assembleDebug` succeeds. Library AAR is publishable. Sample app uses `debugImplementation`. Release build contains zero bridge code (verified by APK analysis).

### 1.2 Data Models

| Task | Complexity | Status | Notes |
|------|-----------|--------|-------|
| `AppMap` -- complete app structure | Low | Done | Activities, fragments, nav graph |
| `ScreenInfo` -- current screen state | Low | Done | Activity + fragments + views |
| `ViewNode` -- view tree node with bounds | Low | Done | Recursive, typed properties |
| `FocusInfo` -- DPAD focus state and graph | Medium | Done | Graph + BFS path |
| `EventInfo` -- causal event with parent/child | Low | Done | ID, parentId, childIds, data |
| `OverlayInfo` -- dialog/popup/toast detection | Low | Done | Type enum, bounds, blocking |
| `ActionResult` -- action outcome with timing | Low | Done | Success/failure + nanoTime |
| `McpTypes` -- MCP request/response/tool types | Low | Done | JSON-RPC 2.0 compliant |
| `NavigationGraph` -- nav destinations and edges | Low | Done | From NavController |
| `BridgeConfig` -- configuration options | Low | Pending | Port, auth, buffer size |
| `HealthInfo` -- server health and capabilities | Low | Pending | Uptime, version, endpoints |
| `MemoryInfo` -- memory and object counts | Low | Pending | Runtime.getRuntime() |
| `StateInfo` -- application state snapshot | Low | Pending | SharedPrefs, extras, custom |
| `SimulateRequest` -- input simulation params | Low | Pending | Touch, key, gesture definitions |
| `ScreenshotResult` -- base64 encoded capture | Low | Pending | PNG with metadata |

**Acceptance criteria:** All models are `@Serializable`, have complete KDoc, and round-trip through JSON without data loss. Serialization adds <0.1ms overhead for typical payloads.

### 1.3 Activity Lifecycle Tracking

| Task | Complexity | Status | Notes |
|------|-----------|--------|-------|
| `ActivityTracker` -- lifecycle callbacks | Medium | Done | Application.registerActivityLifecycleCallbacks |
| Activity stack management (WeakReference) | Low | Done | Prevents memory leaks |
| Resumed activity detection | Low | Done | Always know the foreground activity |
| Fragment lifecycle tracking (recursive) | Medium | Done | FragmentManager.registerFragmentLifecycleCallbacks |
| NavController discovery from NavHostFragment | Medium | Done | Automatic, no configuration |
| Fragment collection (depth-first) | Low | Done | Including nested fragments |
| Lifecycle event emission to CausalEventStream | Low | Done | onCreate, onResume, etc. as events |

**Acceptance criteria:** All activity and fragment lifecycle events are tracked. Activity stack is always accurate. NavControllers are discovered automatically. Events are emitted to the event stream with correct causal linking (e.g., Activity.onCreate -> Fragment.onAttach -> Fragment.onCreate chain).

### 1.4 View Tree Scanning

| Task | Complexity | Dependencies | Notes |
|------|-----------|-------------|-------|
| `ViewTreeScanner` -- recursive view hierarchy traversal | Medium | ViewNode model | Depth-first, all ViewGroups |
| Resource ID resolution (integer to string) | Low | None | Resources.getResourceEntryName() |
| Bounds calculation (local to global coordinates) | Low | None | View.getLocationOnScreen() |
| Property extraction (visibility, enabled, checked, etc.) | Low | None | Standard View properties |
| Text extraction (TextView, EditText, Button) | Low | None | getText(), getHint() |
| Content description extraction | Low | None | getContentDescription() |
| Custom view property extraction via reflection | Medium | None | Public getters, annotated fields |
| View identity hash for stable references | Low | None | System.identityHashCode() |
| RecyclerView adapter data access | Medium | None | Adapter.getItemCount(), item types |
| Performance: tree scan under 10ms for 500 views | Medium | None | Benchmarked on Pixel 4a |

**Acceptance criteria:** Full view tree is captured with all properties. Custom views show their class hierarchy and public properties. Scan completes in under 10ms for typical app screens (200-500 views). RecyclerView adapter data count is included.

### 1.5 Causal Event Stream

| Task | Complexity | Status | Notes |
|------|-----------|--------|-------|
| Ring buffer with configurable capacity (default 1000) | Low | Done | Array + atomic index |
| Thread-safe writes with Mutex | Low | Done | Kotlin coroutine Mutex |
| Event indexing by ID (ConcurrentHashMap) | Low | Done | O(1) lookup |
| Parent-child relationship tracking | Low | Done | parentId + childIds |
| SharedFlow broadcast to subscribers | Low | Done | Buffered SharedFlow |
| History query with timestamp/type filter | Low | Done | Linear scan of ring buffer |
| Single event lookup with populated childIds | Low | Done | Index lookup + child collection |
| Causal chain traversal (up to root, BFS down) | Medium | Done | Walk up via parentId, BFS down via childIds |
| State snapshot provider hook | Low | Done | Lambda registered per event type |
| Pre-built event emission (for replay/forward) | Low | Done | Emit pre-constructed EventInfo |
| Eviction cleanup on ring buffer wrap | Low | Done | Remove from index on overwrite |

**Acceptance criteria:** 1000-event buffer with O(1) insert, O(1) lookup, O(n) history query. Causal chains are traversable in both directions. Thread-safe under concurrent emission from multiple coroutines. Memory bounded regardless of event rate.

### 1.6 Ktor Embedded Server with Authentication

| Task | Complexity | Dependencies | Notes |
|------|-----------|-------------|-------|
| `BridgeServer` -- Ktor Netty embedded server | Medium | BridgeConfig | Coroutine-native |
| Localhost-only binding (127.0.0.1) | Low | None | Security: no LAN exposure |
| HMAC-SHA256 authentication plugin | Medium | Debug keystore | Token in header |
| Content negotiation (JSON) | Low | Kotlin Serialization | ContentNegotiation plugin |
| WebSocket configuration | Low | None | Ktor WebSockets plugin |
| Graceful startup/shutdown on app lifecycle | Medium | ActivityTracker | Start on first activity, stop on last |
| Error handling and structured error responses | Low | None | Consistent error JSON |
| Request logging (debug only) | Low | None | CallLogging plugin |
| CORS configuration (for browser-based agents) | Low | None | CORS plugin |
| Port conflict detection and retry | Low | None | Try 8690, then 8691, 8692... |

**Acceptance criteria:** Server starts on app launch, stops on app termination. All requests require valid HMAC token. Responses are structured JSON with consistent error format. Server handles 100+ concurrent requests without OOM. Startup time <100ms.

### 1.7 REST Endpoints (All 12)

| Endpoint | Method | Complexity | Dependencies | Description |
|----------|--------|-----------|-------------|-------------|
| `/health` | GET | Low | BridgeServer | Server status, version, uptime, available endpoints |
| `/map` | GET | Medium | ActivityTracker, ViewTreeScanner | Full app map: all activities, fragments, nav graph |
| `/current` | GET | Medium | ActivityTracker, ViewTreeScanner | Current screen: activity, fragments, view tree, focus |
| `/navigate` | POST | Medium | ActivityTracker (NavController) | Navigate to destination by ID with type-safe args |
| `/action` | POST | High | ViewTreeScanner, main thread | Click, long-click, setText, scroll by view ID |
| `/input` | POST | Medium | ViewTreeScanner, IME | Secure text input bypassing input method |
| `/state` | GET | Medium | SharedPreferences, reflection | Read app state: prefs, ViewModel, extras |
| `/state` | POST | Medium | SharedPreferences, reflection | Write app state: prefs, flags, custom |
| `/focus` | GET | High | FocusGraphBuilder | DPAD focus graph + BFS shortest path |
| `/overlays` | GET | Medium | WindowManager | All overlays with type, bounds, blocking status |
| `/memory` | GET | Low | Runtime.getRuntime() | Memory usage: heap, native, object counts |
| `/simulate` | POST | Medium | InputManager, KeyEvent | Input simulation: touch, key, gesture, multi-touch |
| `/screenshot` | GET | Medium | PixelCopy / Canvas | Screen capture as base64 PNG from within process |

**Acceptance criteria:** All endpoints return correct data for the sample app. Actions execute on the main thread via `withContext(Dispatchers.Main)` and return results within 10ms. Error responses include HTTP status codes, error messages, and machine-readable error codes.

### 1.8 WebSocket Event Streaming

| Task | Complexity | Dependencies | Notes |
|------|-----------|-------------|-------|
| `WS /events` -- live event stream | Medium | CausalEventStream, BridgeServer | SharedFlow -> WebSocket |
| `GET /events/history` -- buffered event history | Low | CausalEventStream | Ring buffer query with filters |
| `GET /events/chain/{id}` -- causal chain query | Low | CausalEventStream | Walk up + BFS down |
| Client connection tracking | Low | None | Count active subscribers |
| Event filtering via query params | Medium | None | ?type=LIFECYCLE&source=MainActivity |
| Backpressure handling for slow clients | Medium | SharedFlow config | Buffer + DROP_OLDEST |

**Acceptance criteria:** WebSocket clients receive all events in real time (<1ms from emission to delivery). History endpoint returns events from ring buffer with optional type/source/timestamp filters. Chain endpoint traverses full causal tree. Slow clients are dropped gracefully without blocking the event stream.

### 1.9 MCP Protocol Support

| Task | Complexity | Dependencies | Notes |
|------|-----------|-------------|-------|
| `POST /mcp` -- JSON-RPC 2.0 endpoint | Medium | All REST endpoints | Single entry point |
| `tools/list` method | Medium | McpToolDefinition | Return all tool definitions |
| `tools/call` method | Medium | All endpoint handlers | Dispatch to handler by name |
| Tool definition auto-generation | Medium | Endpoint metadata | Reflect endpoint signatures |
| Input schema generation (JSON Schema) | Medium | McpInputSchema | Type-safe tool parameters |
| Error mapping (endpoint errors to MCP errors) | Low | McpError | -32600 to -32603 codes |
| MCP protocol version negotiation | Low | None | Version in initialize response |

**Acceptance criteria:** Claude Code (or any MCP client) can connect to `/mcp`, discover tools via `tools/list`, and invoke any tool via `tools/call` with correct structured results. Tool definitions include complete JSON Schema for all parameters.

### 1.10 Focus Tracking (Basic)

| Task | Complexity | Dependencies | Notes |
|------|-----------|-------------|-------|
| Current focus detection | Low | View.findFocus() | Main thread query |
| Focusable element enumeration | Medium | ViewTreeScanner | Filter by isFocusable() |
| Focus change event emission | Low | OnGlobalFocusChangeListener | Real-time focus events |
| Focus history in ring buffer | Low | CausalEventStream | Causally linked |

**Acceptance criteria:** `/focus` returns the currently focused view with its properties. Focus changes emit events to the event stream. All focusable elements are enumerated with their bounds and IDs.

### Phase 1 Testing Strategy

```
Unit Tests (JVM):
  - Data model serialization round-trips
  - Ring buffer capacity and eviction
  - Causal chain traversal correctness
  - MCP protocol message parsing
  - HMAC authentication validation
  - Event filtering logic

Instrumented Tests (Android device/emulator):
  - BridgeServer startup and shutdown
  - ActivityTracker lifecycle callback correctness
  - ViewTreeScanner accuracy (sample app screens)
  - REST endpoint response correctness
  - WebSocket event delivery
  - MCP tool discovery and invocation
  - Focus detection and enumeration
  - Overlay detection
  - Input simulation

Integration Tests (AI agent simulation):
  - Full workflow: connect, discover, query, act, verify
  - Multi-step navigation scenario
  - Concurrent request handling
  - Reconnection after app restart
  - Event stream subscription and filtering

Performance Tests:
  - View tree scan latency for 100, 500, 1000 views
  - Event emission throughput (events per second)
  - Memory footprint of bridge library
  - Server startup time
  - Concurrent request response time (10, 50, 100 clients)
```

### Phase 1 Security Model

```
Threat Model:
=============

1. Localhost-only binding
   - Server binds to 127.0.0.1, not 0.0.0.0
   - No network exposure, no LAN access
   - Only processes on the same device can connect

2. HMAC-SHA256 authentication
   - Token derived from the app's debug signing key
   - Sent in X-Bridge-Token header
   - Prevents unauthorized local processes from connecting
   - Token is deterministic (same key = same token) for ease of use

3. Debug-only deployment
   - Library is debugImplementation only
   - Zero code in release builds (verified by R8 tree shaking)
   - init() via Class.forName() is a safe no-op in release

4. No sensitive data in responses by default
   - Password field text is redacted unless explicitly requested
   - Authentication tokens are not included in state dumps
   - Screenshot endpoint can be disabled via BridgeConfig

5. ADB forwarding security
   - adb forward creates a local-to-device tunnel
   - ADB requires USB debugging enabled + device authorization
   - The tunnel is destroyed when ADB is disconnected

Attack Vectors Considered:
==========================

A1. Malicious app on same device connects to bridge
    Mitigation: HMAC auth requires knowledge of debug signing key
    Residual risk: Low (debug builds are not distributed to users)

A2. Network-based attack via open port
    Mitigation: Localhost binding, no network port
    Residual risk: None (physically impossible)

A3. Bridge accidentally included in release build
    Mitigation: debugImplementation + R8 + CI/CD APK analysis
    Residual risk: Very low (multiple safeguards)

A4. Information leak via logcat
    Mitigation: No sensitive data logged. Request/response logging
    is configurable and off by default.
    Residual risk: Low
```

---

## Phase 2: Compose Deep Integration

**Goal:** Full Jetpack Compose introspection including semantics tree traversal, state observation, recomposition tracking, and modifier chain inspection. This makes AI Debug Bridge the most complete Compose inspection tool available.

**Dependencies:** Phase 1 complete.

**Duration estimate:** 3-4 weeks.

### 2.1 Full Semantics Tree Traversal

| Task | Complexity | Dependencies | Notes |
|------|-----------|-------------|-------|
| `ComposeIntrospector` -- traverse SemanticsOwner tree | High | Compose UI (compileOnly) | Access via AndroidComposeView |
| Semantics node to ViewNode mapping | Medium | ViewNode model | Unified tree |
| Role, testTag, contentDescription extraction | Low | SemanticsProperties | Standard properties |
| Action detection (click, scroll, setText, etc.) | Medium | SemanticsActions | Available actions per node |
| State detection (toggled, selected, disabled, etc.) | Medium | SemanticsProperties | Boolean/enum states |
| Merged vs unmerged semantics handling | Medium | None | Both trees available |
| Performance: semantics scan under 5ms | Medium | None | Benchmarked |

**Technical approach:** Access the `SemanticsOwner` from `AndroidComposeView` (which is a regular `View` in the hierarchy). The `SemanticsOwner.rootSemanticsNode` provides the tree root. Traverse using `SemanticsNode.children`. Extract properties from `SemanticsConfiguration`.

**Acceptance criteria:** All Compose semantics nodes appear in the view tree. Test tags, roles, actions, and states are correctly extracted. Both merged and unmerged trees are available via query parameter.

### 2.2 Compose State Observation

| Task | Complexity | Dependencies | Notes |
|------|-----------|-------------|-------|
| `remember` state value reading via reflection | High | Compose runtime internals | SlotTable access |
| MutableState change detection | High | Snapshot system | Snapshot.registerApplyObserver |
| StateFlow/SharedFlow collection monitoring | Medium | Coroutine introspection | Active collectors |
| ViewModel state exposure (Compose) | Medium | Reflection | Compose ViewModel integration |
| State change event emission | Medium | CausalEventStream | Causally linked to recomposition |

**Technical approach:** Compose stores state in `SlotTable` within the `Composition`. Access state holders via `remember` slot inspection. For change detection, register a `Snapshot.registerApplyObserver` callback that fires when any snapshot state is modified. Map state changes to the composables that read them.

**Risk:** Compose runtime internals are not stable API. Version-specific adapters may be needed. Graceful degradation when internals change.

**Acceptance criteria:** AI agents can read current values of `remember { mutableStateOf() }` holders. State changes emit events. ViewModel state accessible through Compose integration points.

### 2.3 Recomposition Tracking

| Task | Complexity | Dependencies | Notes |
|------|-----------|-------------|-------|
| Recomposition count per composable | High | Compose compiler metadata | CompositionData |
| Recomposition trigger identification | High | Snapshot system | Which state caused it |
| Skip count tracking | Medium | Compose compiler metadata | Skipped vs executed |
| Recomposition scope mapping | High | None | State -> composable mapping |
| Performance impact measurement | Medium | System.nanoTime | Nanos per recomposition |

**Technical approach:** Use `CompositionData` (available from `Composition.compositionData`) to inspect recomposition statistics. The Compose compiler generates recomposition tracking metadata that includes group keys, skip counts, and slot changes.

**Acceptance criteria:** `/compose/recompositions` returns per-composable recomposition counts, triggers, skip counts, and cumulative time. Useful for AI-assisted performance debugging.

### 2.4 Modifier Chain Inspection

| Task | Complexity | Dependencies | Notes |
|------|-----------|-------------|-------|
| Modifier chain extraction from LayoutNode | High | Compose UI internals | LayoutNode.modifier |
| Padding, size, offset modifier reading | Medium | Reflection | Concrete modifier types |
| Click/gesture modifier detection | Medium | Reflection | PointerInputModifier |
| Background, border, clip modifier reading | Medium | Reflection | DrawModifier subtypes |
| Custom modifier identification | Medium | Class inspection | toString + properties |

**Technical approach:** Access `LayoutNode.modifier` which is a `Modifier` chain. Traverse the chain using `foldIn`/`foldOut`. For each `Modifier.Element`, inspect its concrete type to extract properties (padding values, size constraints, colors, etc.).

**Acceptance criteria:** `/compose/modifiers/{nodeId}` returns the full modifier chain with extracted values for standard modifiers and class names for custom modifiers.

---

## Phase 3: AI Extensions

**Goal:** Make AI Debug Bridge extensible per-project with custom endpoints, a plugin system, and ML model integration hooks. This transforms the bridge from a fixed tool into a platform.

**Dependencies:** Phase 1 complete.

**Duration estimate:** 4-6 weeks.

### 3.1 Custom Endpoint Registration API

| Task | Complexity | Dependencies | Notes |
|------|-----------|-------------|-------|
| `AiDebugBridge.registerEndpoint()` API | Medium | BridgeServer | Public API |
| Auto-generated MCP tool definition from endpoint | Medium | McpToolDefinition | Reflect handler signature |
| Type-safe request/response handling | Medium | Kotlin Serialization | Reified type parameters |
| Endpoint documentation generation | Low | None | KDoc + OpenAPI |
| Endpoint versioning support | Low | None | /v1/custom/... |
| Hot registration (add without restart) | Medium | BridgeServer route rebuild | Dynamic routing |

**API design:**

```kotlin
// Simple: no input, return data
AiDebugBridge.registerEndpoint("player-state") {
    mapOf("state" to player.playbackState.name,
          "bitrate" to player.videoFormat?.bitrate)
}

// With typed input and output
AiDebugBridge.registerEndpoint<PlayerCommand, PlayerState>(
    name = "player-control",
    description = "Control video player",
    handler = { command ->
        when (command.action) {
            "play" -> player.play()
            "pause" -> player.pause()
            "seek" -> player.seekTo(command.positionMs)
        }
        PlayerState(player.playbackState, player.currentPosition)
    }
)
```

**Acceptance criteria:** Custom endpoints are accessible via REST (`/custom/{name}`) and MCP (`tools/call`). Auto-generated MCP tool definitions include parameter schemas. Hot registration works without server restart.

### 3.2 Plugin System

| Task | Complexity | Dependencies | Notes |
|------|-----------|-------------|-------|
| Plugin interface definition | Medium | None | BridgePlugin interface |
| Plugin discovery (ServiceLoader) | Medium | None | META-INF/services |
| Plugin lifecycle management | Medium | None | init, start, stop, destroy |
| Plugin isolation (separate classloader) | High | None | Security boundary |
| Built-in: Network Inspector plugin | High | OkHttp (compileOnly) | Interceptor-based |
| Built-in: Database Inspector plugin | High | Room/SQLite | Query + schema |
| Built-in: Performance Monitor plugin | Medium | Choreographer, FrameMetrics | FPS, jank, memory |
| Plugin SDK with documentation | Medium | None | Sample plugin project |

**Plugin interface:**

```kotlin
interface BridgePlugin {
    val id: String
    val version: String
    val description: String

    fun onInit(bridge: BridgePluginContext)
    fun onStart()
    fun onStop()
    fun onDestroy()
}

interface BridgePluginContext {
    fun registerEndpoint(name: String, handler: EndpointHandler)
    fun registerEventEmitter(emitter: EventEmitter)
    fun registerStateProvider(provider: StateProvider)
    val eventStream: CausalEventStream
    val config: BridgeConfig
}
```

**Acceptance criteria:** Third-party plugins can be created as separate AARs, discovered via ServiceLoader, and loaded by the bridge. Plugins register endpoints, event emitters, and state providers through the plugin context. Plugin failures do not crash the bridge.

### 3.3 ML Model Integration Hooks

| Task | Complexity | Dependencies | Notes |
|------|-----------|-------------|-------|
| TensorFlow Lite model state inspection | Medium | TFLite (compileOnly) | Interpreter state |
| ONNX Runtime model state inspection | Medium | ONNX (compileOnly) | Session state |
| ML Kit integration | Medium | ML Kit (compileOnly) | Detector state |
| Model input/output capture | Medium | None | Log inference I/O |
| Model performance metrics | Low | System.nanoTime | Inference latency |
| Model event emission | Low | CausalEventStream | Inference as events |

**Acceptance criteria:** When an app uses TFLite, ONNX, or ML Kit, the bridge can report model names, input/output shapes, inference latency, and last input/output values. Model inferences are emitted as causal events linked to the UI events that triggered them.

### 3.4 Project-Specific Plugin Examples

**Streaming app plugin:**
```kotlin
class StreamingPlugin : BridgePlugin {
    override fun onInit(ctx: BridgePluginContext) {
        ctx.registerEndpoint("player") { getPlayerState() }
        ctx.registerEndpoint("drm") { getDrmState() }
        ctx.registerEndpoint("buffer") { getBufferHealth() }
        ctx.registerEndpoint("tracks") { getAvailableTracks() }
        ctx.registerEndpoint("ads") { getAdState() }
    }
}
```

**E-commerce app plugin:**
```kotlin
class EcommercePlugin : BridgePlugin {
    override fun onInit(ctx: BridgePluginContext) {
        ctx.registerEndpoint("cart") { getCartContents() }
        ctx.registerEndpoint("user") { getUserProfile() }
        ctx.registerEndpoint("pricing") { getPricingTier() }
        ctx.registerEndpoint("inventory") { getInventoryState() }
    }
}
```

---

## Phase 4: Cross-Platform

**Goal:** Extend AI Debug Bridge beyond Android to iOS, Flutter, and React Native using Kotlin Multiplatform for shared protocol and data model code.

**Dependencies:** Phase 1 complete. Phase 3 (plugin system) recommended.

**Duration estimate:** 6-8 weeks.

### 4.1 Kotlin Multiplatform Shared Module

| Task | Complexity | Dependencies | Notes |
|------|-----------|-------------|-------|
| Extract data models to KMP common module | Medium | None | @Serializable already works |
| Extract MCP protocol handler to common | Medium | None | Pure Kotlin logic |
| Extract causal event stream to common | Medium | None | Coroutine-based, platform-agnostic |
| Ktor client/server in common (expect/actual) | Medium | Ktor KMP | Platform-specific engines |
| Common plugin interface | Low | None | Shared between platforms |
| Common test suite | Medium | None | kotlin.test |

**Architecture:**

```
ai-debug-bridge/
  shared/                    <-- KMP module
    commonMain/
      models/               <-- @Serializable data classes
      mcp/                  <-- MCP protocol handler
      events/               <-- CausalEventStream
      plugin/               <-- BridgePlugin interface
    androidMain/
      server/               <-- Ktor Netty server
      discovery/            <-- Android-specific scanners
    iosMain/
      server/               <-- Ktor Darwin server
      discovery/            <-- UIKit/SwiftUI scanners
  android/                   <-- Android-specific library (current)
  ios/                       <-- iOS framework (CocoaPods/SPM)
  flutter/                   <-- Flutter plugin
  react-native/              <-- React Native module
```

**Acceptance criteria:** Data models, MCP handler, and event stream compile for all targets (Android, iOS, JVM). Tests pass on all platforms.

### 4.2 iOS Bridge (via KMP)

| Task | Complexity | Dependencies | Notes |
|------|-----------|-------------|-------|
| Ktor server with Darwin engine | Medium | KMP shared | CIO or Darwin engine |
| UIKit view hierarchy traversal | High | None | UIView recursive scan |
| SwiftUI inspection (limited) | High | None | Mirror API, _ViewDebug |
| UIAccessibility tree access | Medium | None | UIAccessibilityElement |
| Navigation stack inspection | Medium | None | UINavigationController |
| Gesture recognizer detection | Medium | None | UIGestureRecognizer |
| Core Data / UserDefaults inspection | Medium | None | State access |
| App lifecycle tracking | Low | None | UIApplicationDelegate |

**Technical challenges:**
- SwiftUI view hierarchy is not directly inspectable (unlike Android's View system)
- iOS uses the `Mirror` reflection API which is less powerful than Java reflection
- Ktor on iOS uses the Darwin engine (URLSession-based) which has different performance characteristics
- iOS apps cannot bind to network ports in the same way (App Transport Security restrictions apply to localhost too in some configurations)

**Acceptance criteria:** An AI agent connecting to the iOS bridge gets comparable data to the Android bridge: view tree, text content, navigation state, accessibility tree, and state inspection. SwiftUI support is best-effort with graceful degradation.

### 4.3 Flutter Bridge

| Task | Complexity | Dependencies | Notes |
|------|-----------|-------------|-------|
| Flutter platform channel setup | Medium | None | MethodChannel/EventChannel |
| Widget tree traversal via debug mode | High | Flutter framework | debugDumpRenderTree |
| Semantics tree access | Medium | None | SemanticsBinding |
| State inspection (StatefulWidget) | High | None | Element tree traversal |
| Navigator stack inspection | Medium | None | NavigatorState |
| Flutter DevTools protocol integration | Medium | None | VM Service protocol |
| Platform view bridge (Android/iOS host) | Medium | None | PlatformView detection |

**Technical approach:** Flutter provides rich inspection APIs in debug mode through the `dart:developer` ServiceExtension and the Widget Inspector. The bridge registers a Dart service extension that the embedded server calls through a platform channel. The widget tree, render tree, and semantics tree are all accessible through debug APIs.

**Acceptance criteria:** Widget tree with all properties, navigation state, and semantics tree are accessible via the same REST/MCP API. Flutter and native views are unified in the response when platform views are used.

### 4.4 React Native Bridge

| Task | Complexity | Dependencies | Notes |
|------|-----------|-------------|-------|
| React Native bridge module (native side) | Medium | None | TurboModule or NativeModule |
| Component tree traversal via JS bridge | High | None | React DevTools protocol |
| State/props inspection | High | None | Fiber tree access |
| Navigation state (React Navigation) | Medium | None | NavigationContainer state |
| Redux/MobX store inspection | Medium | None | Store.getState() |
| JS-to-native event correlation | High | None | Bridge event tracking |
| Hermes engine integration | Medium | None | Hermes debug API |

**Technical approach:** The bridge has two sides: a native module (Java/Kotlin on Android, ObjC/Swift on iOS) that runs the Ktor server, and a JS module that provides React component tree access. The native module communicates with the JS module through the React Native bridge (or JSI for synchronous access). Component trees are serialized from the JS side and merged with native view trees.

**Acceptance criteria:** React component tree with props and state, navigation state, and Redux store contents are accessible. Native views and React components are unified in the response.

---

## Timeline Estimate

| Phase | Duration | Depends On | Parallelizable |
|-------|----------|------------|---------------|
| Phase 1: Core Android | 4-6 weeks | -- | -- |
| Phase 2: Compose Deep | 3-4 weeks | Phase 1 | Yes (with Phase 3) |
| Phase 3: AI Extensions | 4-6 weeks | Phase 1 | Yes (with Phase 2) |
| Phase 4: Cross-Platform | 6-8 weeks | Phase 1 + Phase 3 (recommended) | Partially |

```
Week:  1  2  3  4  5  6  7  8  9 10 11 12 13 14 15 16 17 18 19 20
       |------ Phase 1 ------|
                              |--- Phase 2 ---|
                              |------ Phase 3 ------|
                                                     |-------- Phase 4 --------|
```

Phases 2 and 3 can proceed in parallel after Phase 1 is complete. Phase 4 benefits from Phase 3's plugin system for cross-platform plugin development, but the core KMP extraction (4.1) can start after Phase 1.

---

## Risk Register

| Risk | Impact | Likelihood | Mitigation |
|------|--------|-----------|------------|
| Ktor server OOM on low-memory devices | High | Medium | Configurable buffer sizes, lazy init, memory monitoring |
| Compose runtime internals change between versions | Medium | High | Version-specific adapters, compileOnly dependency, graceful degradation |
| HMAC auth bypass via localhost ADB forwarding | Low | Low | Document security model, HMAC still required for all requests |
| Focus graph incorrect for custom focus handlers | Medium | Medium | Fallback to reported focus, flag unreliable paths |
| Large view trees cause scan timeout | Medium | Low | Depth limits, lazy child loading, scan timeout config |
| Android process kill during bridge operation | Low | High | Stateless server, automatic restart on next activity |
| iOS SwiftUI introspection breaks on new iOS versions | Medium | High | Best-effort SwiftUI, full UIKit support as fallback |
| KMP compilation issues across targets | Medium | Medium | Strict common module, platform-specific via expect/actual |
| React Native JSI bridge version incompatibility | Medium | Medium | Fallback to NativeModule (slower but stable) |
| Plugin classloader leaks memory | Medium | Low | WeakReference plugin instances, lifecycle enforcement |
| Ktor Netty conflicts with app's Netty (rare) | Low | Low | Shade/relocate Netty classes in bridge AAR |
| MCP specification evolves incompatibly | Low | Medium | Version negotiation, backwards-compatible additions |

---

## Success Metrics

### Phase 1 Success Criteria

| Metric | Target | Measurement |
|--------|--------|-------------|
| `/current` response time | <5ms | Benchmarked on Pixel 4a |
| `/focus` path computation | <10ms for 100 focusable elements | Benchmarked |
| Event delivery latency | <1ms emit to WebSocket client | Measured |
| Memory overhead | <10MB heap | Profiled |
| APK size increase | <2MB (debug only) | APK Analyzer |
| Server startup time | <200ms | Measured from init to first request |
| Release build size impact | 0 bytes | Verified by APK diff |
| MCP tool discovery | All 12 endpoints as tools | Integration test |
| Unit test coverage | >80% line coverage | JaCoCo |
| Instrumented test pass rate | 100% | CI/CD |

### Long-Term Success Criteria

| Metric | Target | Timeline |
|--------|--------|----------|
| GitHub stars | 1000+ | 6 months |
| Maven Central downloads | 500+/month | 3 months |
| Community plugins | 5+ third-party | 6 months |
| AI agent integrations | Claude Code, GPT, Gemini | 3 months |
| Cross-platform coverage | Android + iOS | 6 months |
| Documentation completeness | All endpoints with examples | Phase 1 |
