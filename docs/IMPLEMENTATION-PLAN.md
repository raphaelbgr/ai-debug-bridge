# Implementation Plan

## Phase 1: Core Android (4-6 weeks)

### 1.1 Auto-Discovery Engine
- Hook `ActivityLifecycleCallbacks` to track all Activities
- Hook `FragmentManager.FragmentLifecycleCallbacks` for all Fragments
- Parse `NavController.graph` for navigation destinations and actions
- Traverse `View.getRootView()` recursively for element map
- Detect framework: Compose vs View system vs hybrid
- Compose: traverse semantics tree via `SemanticsNode`

### 1.2 Embedded Debug Server
- Lightweight HTTP server (Ktor or NanoHTTPD) on localhost
- WebSocket for live event streaming
- MCP protocol support for Claude Code / AI agent tool calls
- Auto-port selection (avoid conflicts)
- SSH key HMAC authentication

### 1.3 Core Endpoints
```
GET  /map              → full app map (screens, elements, nav graph)
GET  /current          → current screen + elements + state + focus
POST /navigate/:dest   → navigate to any screen
POST /action/:id       → click/tap any element
POST /input/:id        → set text on input fields
GET  /state/:path      → read any variable
POST /state/:path      → write any variable
GET  /events           → event stream (SSE)
GET  /focus            → current focus + graph
GET  /overlays         → all overlays
GET  /memory           → heap, native, bitmap usage
POST /simulate/:event  → process death, network, permissions, etc.
GET  /screenshot       → current screen
GET  /screenshot/:id   → specific element
```

### 1.4 Event System
- Intercept lifecycle events (Activity, Fragment, View)
- Intercept click events via `View.AccessibilityDelegate` or `ViewTreeObserver`
- Intercept navigation events via `NavController.OnDestinationChangedListener`
- Build causal graph: each event knows parent + children
- Nanosecond timestamps
- State snapshot attached to each event

### 1.5 Security
- `debugImplementation` only — stripped from release builds
- HMAC auth from SSH public key
- Localhost binding by default
- Audit log

## Phase 2: Android TV + Fire TV (3-4 weeks)

### 2.1 Focus System
- `ViewTreeObserver.OnGlobalFocusChangeListener` for real-time focus tracking
- Focus graph builder: for each focusable, compute `nextFocusUp/Down/Left/Right`
- `focusSearch()` prediction: what WILL receive focus in each direction
- Focus history log
- Focus trap detection (elements that can't be escaped via DPAD)
- `POST /focus/navigate/:elementId` → compute shortest DPAD path + execute

### 2.2 Leanback Library Hooks
- `BrowseSupportFragment.getSelectedPosition()` → current row
- `RowsSupportFragment` → item selection within rows
- `HeadersSupportFragment` → header focus zone detection
- `DetailsSupportFragment` → action buttons + related content
- `SearchSupportFragment` → voice/text search state
- `PlaybackSupportFragment` → transport controls state

### 2.3 Streaming/Media Hooks
- ExoPlayer/Media3 `Player.Listener` → all playback events
- Track selection state (video quality, audio language, subtitles)
- Buffer health (buffered percentage, bandwidth estimate)
- DRM session state (license status, expiry, security level)
- IMA SDK ad state (ad pod, current ad, skip countdown, VAST errors)
- HLS/DASH manifest info (variants, selected quality, ABR state)

### 2.4 Fire OS Specifics
- Platform detection: `Build.MANUFACTURER == "Amazon"`
- ADM mock: inject push notifications without Amazon infrastructure
- IAP mock: simulate purchase responses (success, fail, cancel)
- Alexa voice intent injection
- Fire OS version + base Android version reporting
- Memory monitoring with Fire TV-specific thresholds

### 2.5 TV-Specific Features
- Overscan detection and visualization overlay
- HDMI-CEC command sending/receiving
- Remote control button mapping + injection
- Audio output info (HDMI, optical, Bluetooth, passthrough)
- Voice search simulation
- Recommendations notification management

## Phase 3: Web — React / Express / Next.js (3-4 weeks)

### 3.1 Module Interception
- `require-in-the-middle` for CJS modules
- `import-in-the-middle` for ESM modules
- `shimmer` for safe function wrapping
- Auto-detect framework from `package.json`

### 3.2 React Instrumentor
- Traverse React Fiber tree for component map
- Hook into React DevTools `__REACT_DEVTOOLS_GLOBAL_HOOK__`
- State access via Redux DevTools protocol (SocketCluster)
- Zustand/Jotai/Recoil via their debug middleware
- Component re-render tracking

### 3.3 Express/Fastify Instrumentor
- Route map (all registered routes with handlers)
- Request/response capture (headers, body, timing)
- Middleware chain visualization
- Error handler inspection

### 3.4 MCP + HTTP Server
- Express middleware mounting debug routes
- WebSocket for live event stream
- MCP tool registration for Claude Code

## Phase 4: Expand (6-8 weeks)

### 4.1 Vue
- Leverage `vue-mcp-next` patterns
- Component tree via `@vue/devtools-api`
- Pinia store inspection (auto-registered)
- Router state + navigation guards

### 4.2 Flutter
- Dart VM Service extension registration
- Widget tree via `debugDumpApp()`
- `reloadSources` for AI-triggered hot reload
- `mcp_toolkit` integration

### 4.3 Python
- `debugpy` DAP server embedding
- `manhole` REPL injection (2 lines)
- Jurigged hot reload integration
- OpenTelemetry auto-instrumentation patterns

### 4.4 Electron
- CDP bridge (Chrome DevTools Protocol)
- Main + renderer process inspection
- IPC message interception
- Window management

### 4.5 React Native
- Flipper plugin integration
- Reactotron WebSocket bridge
- Hermes Inspector Protocol
- Native module bridge inspection

### 4.6 Android Auto
- CarAppService lifecycle hooks
- Template inspection (ListTemplate, GridTemplate, etc.)
- MediaBrowserService tree access
- Voice command injection
- Audio focus simulation
- Driving/parking mode simulation

### 4.7 Wear OS
- Crown rotation event injection
- Tile rendering inspection
- Complication data mocking
- Ambient mode simulation
- Health sensor data injection

## Phase 5: iOS + tvOS (4-6 weeks)

### 5.1 iOS
- Swift Package Manager distribution
- UIViewController hierarchy traversal
- Core Data / UserDefaults inspection
- URLSession network interception
- Embedded HTTP server (GCDWebServer)

### 5.2 tvOS
- Focus Engine introspection (`UIFocusSystem`)
- Focus guide and environment inspection
- Siri Remote event injection
- Menu button handling
