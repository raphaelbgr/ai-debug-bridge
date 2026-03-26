# AI Debug Bridge

A Kotlin Android library (AAR) that embeds an HTTP/WebSocket server inside Android apps, enabling AI agents to inspect, navigate, and interact with running applications in real-time.

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
