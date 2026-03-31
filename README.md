# AI Debug Bridge

A Kotlin Android library (AAR) that embeds an HTTP/WebSocket server inside Android apps, enabling AI agents to inspect, navigate, and interact with running applications in real-time.

## Why This Exists

The dominant paradigm for AI agents interacting with Android apps is the screenshot pipeline: capture the screen, send it to a vision model, receive a structured interpretation, decide on an action, execute it via ADB, then capture again to verify. Each cycle takes **1.5–7 seconds**. A task requiring 10 UI interactions takes 15–70 seconds — and that's on a single device.

ADB commands compound the problem. Every `input tap`, `dumpsys activity`, or `uiautomator dump` crosses the USB/TCP boundary, routed through the ADB daemon, executed in a shell process, and returned. That is 50–200ms minimum per command, 1–3 seconds for a UI dump — and the dump is already stale by the time it arrives.

AI Debug Bridge replaces the entire external-tool stack with an **in-process HTTP/WebSocket server**. It runs inside your debug APK, reads live `View` objects directly, and responds in under 1ms. No screenshots. No ADB round-trips. No stale snapshots. No compile-and-redeploy cycles. The AI agent gets typed, structured, real-time data about the exact state the user sees.

## Pain Points Solved

- **Screenshot latency (1.5–7s/cycle → <1ms)** — `/current` returns the full view tree as typed JSON without vision inference or screen capture
- **ADB round-trip overhead (50–200ms/command → <5ms)** — all operations execute in-process via direct `View` method calls
- **Stale accessibility dumps** — data is read from live objects on the main thread at the moment of the request; no Binder IPC serialization lag
- **Ambiguous element resolution** — views are addressed by `System.identityHashCode`, not fragile text/resource-ID patterns that collide in RecyclerViews
- **No runtime navigation introspection** — exposes the Navigation Component back stack, fragment manager state, and deep-link graph over `/map`
- **DPAD/TV focus blindness** — `/focus` returns the complete focus graph with DPAD path calculations, critical for Fire TV and Android TV agents
- **No live event stream** — WebSocket `/ws` pushes lifecycle, interaction, and state-change events the instant they occur; no polling required
- **Compile-time testing lock-in** — no test code needs to be compiled into the app; agents query and interact entirely over HTTP
- **SharedPreferences/ViewModel opacity** — `/state` exposes and mutates app state without ADB shell commands or reflection hacks
- **Memory profiling gaps** — `/memory` returns Java heap, native heap, and PSS breakdown from within the process for accurate accounting

## Features

- **Full App Map** — Activity stack, fragment hierarchy, navigation graphs, view trees, Compose semantics
- **Real-time Events** — WebSocket stream of lifecycle events, user interactions, state changes
- **Remote Navigation** — Deep links, Navigation Component, intents, back navigation
- **UI Interaction** — Click, input text, scroll, swipe, DPAD simulation (Android TV / Fire TV)
- **Focus Graph** — Complete focus relationship mapping with DPAD path calculation
- **State Inspection** — Read/write SharedPreferences, Intent extras, ViewModel state
- **Memory Profiling** — Java heap, native heap, PSS breakdown
- **Screenshots** — Programmatic screen capture as base64 PNG/JPEG
- **MCP Support** — Model Context Protocol for native AI agent tool integration
- **HMAC Auth** — Optional HMAC-SHA256 authentication for secure access

## Quick Start

### 1. Add the dependency

In your app's `build.gradle.kts`:

```kotlin
// Only available in debug builds — ZERO code in release
debugImplementation("com.aidebugbridge:library:0.1.0")
```

### 2. Initialize (one line)

In your `Application.onCreate()`:

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Reflection-based init — safe in release (class won't exist)
        try {
            val bridge = Class.forName("com.aidebugbridge.AiDebugBridge")
            bridge.getMethod("init", Application::class.java, Int::class.javaPrimitiveType)
                .invoke(null, this, 8735)
        } catch (e: ClassNotFoundException) {
            // Release build — no-op
        }
    }
}
```

Or directly (if using buildType checks):

```kotlin
AiDebugBridge.init(this, port = 8735)
```

### 3. Connect your AI agent

```bash
# Get full app map
curl http://device-ip:8735/map

# Get current screen
curl http://device-ip:8735/current

# Navigate
curl -X POST http://device-ip:8735/navigate \
  -H "Content-Type: application/json" \
  -d '{"destination": "com.example.SecondActivity", "method": "INTENT"}'

# Simulate DPAD
curl -X POST http://device-ip:8735/simulate \
  -H "Content-Type: application/json" \
  -d '{"gesture": "DPAD_DOWN"}'

# Screenshot
curl http://device-ip:8735/screenshot

# WebSocket event stream
websocat ws://device-ip:8735/ws
```

## API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/map` | GET | Full app map (activities, fragments, nav graph, views) |
| `/current` | GET | Current screen info |
| `/navigate` | POST | Navigate to a screen |
| `/action` | POST | Perform UI action (click, scroll, focus) |
| `/input` | POST | Enter text into input fields |
| `/state` | GET/POST | Read/write app state |
| `/events` | GET | Recent event history |
| `/focus` | GET | Focus graph (DPAD navigation map) |
| `/overlays` | GET | Detect overlays, dialogs, popups |
| `/memory` | GET | Memory usage stats |
| `/simulate` | POST | Simulate gestures and key events |
| `/screenshot` | GET | Capture screenshot as base64 |
| `/ws` | WS | Real-time event stream |

## Building

```bash
./gradlew :library:assembleDebug
./gradlew :sample:installDebug
```

## Architecture

```
AiDebugBridge.init(app)
    |
    +-- DiscoveryEngine (orchestrates all trackers)
    |     +-- ActivityTracker (lifecycle callbacks)
    |     +-- FragmentTracker (fragment manager)
    |     +-- NavGraphMapper (navigation component)
    |     +-- ViewTreeMapper (view hierarchy)
    |     +-- ComposeSemantics (Compose semantics tree)
    |
    +-- BridgeServer (Ktor embedded HTTP/WS)
    |     +-- REST endpoints (/map, /current, /navigate, ...)
    |     +-- WebSocket handler (/ws)
    |     +-- HmacAuth (optional authentication)
    |
    +-- EventBus (internal event system)
    |     +-- CausalChain (event relationship tracking)
    |     +-- StateSnapshot (state at each event)
    |
    +-- FocusGraph + DpadPathCalculator (TV/Fire TV)
    |
    +-- McpProtocol (MCP JSON-RPC handler)
          +-- McpToolRegistry (tool definitions)
          +-- McpTransport (stdio/HTTP)
```

## License

Apache License 2.0 — See [LICENSE](LICENSE) for details.
