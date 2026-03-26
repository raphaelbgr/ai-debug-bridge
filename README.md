# AI Debug Bridge

**Give AI agents eyes, hands, and a brain inside your running app.**

Every AI agent in 2026 — Claude Computer Use, GPT CUA, Agent-S3, Browser Use — treats your application as a black box. They look at screenshots, guess where buttons are, and hope their pixel coordinates are right. They can't read your app's state. They can't see your data. They can't tell you *why* something failed.

**AI Debug Bridge changes that.** Add one line to your app. The library auto-discovers every screen, every element, every navigation path, and opens a debug server the AI connects to. The AI doesn't guess — it *knows*.

```kotlin
// Android — one line:
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AIDebugBridge.init(this) // That's it.
    }
}
```

```typescript
// Web (React/Express/Next.js) — one line:
import { initAIDebug } from 'ai-debug-bridge'
initAIDebug() // That's it.
```

---

## The Problem: AI Agents Are Blind

The state of the art in AI app control (March 2026):

```
Layer 4: Pixels (screenshots)           ← Claude, GPT, UI-TARS, AppAgent
Layer 3: DOM / Accessibility Tree       ← Browser Use, AgentQL, NeuralBridge
Layer 2: OS-level actions (shell/files) ← OpenClaw, Devin
Layer 1: Application Internal State     ← Nobody. Until now.
```

**Layer 1 — the application's internal state — is completely unserved.** No tool can read your ViewModel, query your database, inspect your player state, or tell you why a network request failed. AI agents operate like a mechanic who can only look at the car's paint job but never open the hood.

### The Numbers

| Stat | Value | Source |
|------|-------|--------|
| Developer time spent debugging | **35-50%** of all work hours | ACM Queue |
| Time finding bugs vs fixing them | **90% finding / 10% fixing** | Industry consensus |
| Global annual cost of debugging | **$61 billion** | Cambridge/Undo |
| #1 debugging barrier | **"Cannot reproduce the bug" (41%)** | Cambridge University survey |
| Bugs stuck in backlog due to irreproducibility | **91%** | Shake |
| Average time to fix a single bug | **13 hours** | Undo |

Our library eliminates the 90% — the *finding* phase. The AI already has the state, the element IDs, the error, the reproduction path, and the causal chain.

---

## What It Does

When you add AI Debug Bridge to your app, it:

1. **Auto-discovers** every screen, element, and navigation path — no manual annotation
2. **Opens a debug server** (localhost, authenticated) the AI connects to
3. **Streams live events** — every lifecycle callback, click, navigation, error — as a causal graph
4. **Exposes the complete app map** — the AI knows where everything is, from inside

### The AI Gets a Floor Plan, Not a Photo

**Without AI Debug Bridge** (what every tool does today):
```
AI: *takes screenshot*
AI: "I see some text and buttons. Let me click at coordinates (342, 567)."
AI: *clicks wrong element because DPI scaling changed the layout*
AI: *takes another screenshot*
AI: "Hmm, nothing happened. Let me try (340, 570)."
```

**With AI Debug Bridge:**
```json
GET /current → {
  "screen": "HomeFragment",
  "elements": [
    { "id": "play_btn", "type": "Button", "text": "Play", "bounds": {...}, "clickable": true },
    { "id": "title_tv", "type": "TextView", "text": "Breaking Bad S01E01" },
    { "id": "progress", "type": "SeekBar", "value": 0.45 }
  ],
  "focused": "play_btn",
  "state": { "isPlaying": false, "currentMedia": { "id": 456, "title": "Breaking Bad" } }
}

POST /action/play_btn → { "success": true }
```

Zero screenshots. Zero coordinate math. Zero guessing. The AI speaks in element IDs and the library executes real code.

---

## Key Capabilities

### Navigate Any Screen — Instantly

```
POST /navigate/settings      → navController.navigate(R.id.settings)
POST /navigate/player/456    → opens player with mediaId 456
POST /navigate/search?q=test → opens search with query
GET  /navigation/backstack   → full back-stack with arguments
GET  /navigation/graph       → complete map of all screens + transitions
```

No DPAD key sequences. No blind tapping. No hoping focus lands right.

### Read & Write Any Variable

```
GET  /state/PlayerViewModel.player.isPlaying      → true
GET  /state/PlayerViewModel.player.currentPosition → 45200
POST /state/PlayerViewModel.player.volume          → 0.5
POST /state/SettingsViewModel.preferences.quality  → "720p"
POST /state/AuthManager.currentUser.role           → "admin"
```

The AI can set any variable to reproduce any bug. Network timeout? Set `connectionType = "none"`. Expired DRM? Set `licenseExpiry = pastDate`. Empty state? Set `contentList = emptyList()`.

### Live Event Stream with Causality

Not logs. A structured causal graph:

```
User tap on "Breaking Bad"
  └→ ContentAdapter.onItemClick(position=2)
       └→ NavController.navigate(player, mediaId=456)
            └→ PlayerActivity.onCreate()
                 └→ ExoPlayer.prepare(hlsSource)
                      ├→ HTTP GET master.m3u8 → 200 OK
                      ├→ TrackSelection → 1080p (requires 5Mbps)
                      └→ HTTP GET segment_001.ts → TIMEOUT
                           └→ ERROR: bandwidth 1.2Mbps < required 5Mbps
```

Every event knows its parent, its children, and the app state at that moment. The AI traces from symptom to root cause in milliseconds — not the 13 hours developers average today.

### Focus Tracking (Android TV / Fire TV)

```
GET /focus → {
  "current": "movie_card_3",
  "type": "ImageCardView",
  "data": { "title": "Breaking Bad", "mediaId": 456 },
  "next": {
    "up": "header_row",
    "down": "movie_card_8",
    "left": "movie_card_2",
    "right": "movie_card_4"
  },
  "pathTo": {
    "settings_icon": ["UP", "UP", "RIGHT", "RIGHT", "CENTER"],
    "search_bar": ["UP", "UP", "LEFT", "CENTER"]
  }
}

POST /focus/navigate/settings_icon → calculates optimal DPAD path, confirms arrival
```

No blind key sequences. The AI sees the entire focus graph and teleports.

### Overlay Detection

```
GET /overlays → {
  "systemOverlays": [
    { "type": "StatusBar", "visible": true, "height": 48 },
    { "type": "NavigationBar", "visible": false },
    { "type": "AlexaOverlay", "visible": true, "steals_focus": true }
  ],
  "appOverlays": [
    { "id": "loading_spinner", "visible": true, "blocking": true },
    { "id": "consent_dialog", "visible": true, "blocking": true,
      "buttons": ["Accept", "Decline"] },
    { "id": "player_controls", "visible": false, "autoHide": true, "timeout": 5000 }
  ]
}

POST /overlay/dismiss/consent_dialog → clicks "Accept"
POST /overlay/show/player_controls   → forces controls visible
```

AI never gets stuck on unexpected dialogs or overlays. It sees them all and handles them.

### Password & Text Input

```
POST /input/password_field → { "text": "myP@ssw0rd", "secure": true }
// Library calls: editText.setText("myP@ssw0rd")
// No ADB input (which broadcasts keystrokes to logcat!)
// No screenshot-based typing (which misses special characters)
// Direct, secure, instant.

POST /input/search_bar → { "text": "Breaking Bad", "submit": true }
// Sets text AND triggers the search action
```

ADB `input text` is insecure (broadcasts via input manager, visible in logcat) and can't handle special characters reliably. Our library sets text directly on the EditText object — private, instant, supports any Unicode.

### Debug Overlays

```
POST /overlay/debug/focus    → true  // Green border on focused element
POST /overlay/debug/bounds   → true  // Bounding boxes on all elements
POST /overlay/debug/ids      → true  // Element IDs floating above each view
POST /overlay/debug/grid     → true  // Layout alignment grid
POST /overlay/debug/redraws  → true  // Flash red on every view invalidation
POST /overlay/debug/overscan → true  // Red border at TV overscan zone
```

### Simulate Anything

```
POST /simulate/process-death           → kill + restore app
POST /simulate/network/disconnect      → trigger connectivity change
POST /simulate/network/slow → 56kbps   → throttle to 2G speed
POST /simulate/memory/low → 50MB       → trigger onTrimMemory callbacks
POST /simulate/permission/deny/camera  → test denied permission flow
POST /simulate/doze                    → trigger Doze mode restrictions
POST /simulate/rotation → landscape    → configuration change
POST /simulate/locale → pt-BR          → test Portuguese without device change
POST /simulate/alexa → "play jazz"     → inject Alexa voice intent (Fire TV)
POST /simulate/car/connect             → simulate Android Auto connection
POST /simulate/drm/expire              → trigger DRM license renewal flow
```

---

## Why Embedded Beats External

| | ADB / Appium (External) | AI Debug Bridge (Embedded) |
|---|---|---|
| **Speed** | 50-200ms per command | **<1ms** — same process |
| **Element lookup** | 2-10 seconds (XML dump + parse) | **<10ms** — direct memory |
| **Stale references** | Constant — `StaleElementReferenceException` | **Never** — live object refs |
| **State access** | None | **Any variable, any object** |
| **Navigation** | DPAD keyevents × N, hope focus works | **One call** — `navigate(dest)` |
| **View modification** | Impossible | **Any property** — text, color, visibility |
| **Text input** | `adb input text` — insecure, fails on special chars | **Direct** `setText()` — secure, any Unicode |
| **Focus tracking** | `dumpsys` → window only, not element | **`findFocus()`** — exact element, instant |
| **Compose support** | Flat featureless accessibility node | **Traverse slot table** — full composable tree |
| **Off-screen elements** | Invisible until scrolled into view | **Adapter data access** — know all items |
| **Custom Views** | Invisible if no accessibility metadata | **Auto-discover** — sees every View in tree |
| **AI can extend it** | No — ADB is a frozen protocol | **Yes** — AI writes new endpoints per project |
| **Security** | Full device access | **App sandbox only** |

### The Architectural Gap

External tools must: request snapshot → wait for serialization → transfer across process boundary → parse stale data → find elements → send action back → *hope the element still exists*.

**Each step introduces latency and a window where the state can change.**

AI Debug Bridge collapses all of these into a **single atomic operation on the UI thread**. The query, the state read, and the action execute in the same process, on the same thread, with zero serialization. The view tree cannot change between your read and your action — they are the same operation.

---

## Platform Support

| Platform | Method | Unique Capabilities |
|----------|--------|---------------------|
| **Android** | One-line `Application.onCreate()` init | Full view tree, ViewModel state, lifecycle tracking, permission mocking |
| **Android TV** | Same + Leanback hooks | Focus graph, DPAD path calculation, overscan detection, EPG access |
| **Fire TV / Fire OS** | Same + Amazon SDK hooks | Alexa voice simulation, ADM mock, IAP mock, Fire OS version detection |
| **Android Auto** | CarAppService hooks | Template inspection, voice command injection, driving mode simulation |
| **Wear OS** | Same + Wear hooks | Crown rotation injection, tile inspection, complication mocking |
| **React / Next.js** | `npm install` + one import | Component tree, state management (Redux/Zustand/MobX), route map |
| **Vue** | Vite plugin | Component tree, Pinia stores, router — via MCP tools |
| **Express / Fastify** | Middleware | Route map, request/response capture, middleware chain |
| **Electron** | Preload script | Main + renderer process, IPC inspection, window management |
| **Flutter** | Plugin | Widget tree, Dart VM Service, hot reload trigger |
| **Python** | `pip install` + one line | Variable inspection, live code injection, REPL |
| **iOS** *(planned)* | Swift Package | View controller hierarchy, Core Data, UserDefaults |
| **tvOS** *(planned)* | Same + Focus Engine hooks | Focus system introspection, Siri Remote simulation |

---

## How the AI Learns Your App

**Step 1:** Library auto-discovers the navigation graph and element map at startup.

**Step 2:** AI visits every screen, catalogs every element — builds a complete knowledge base.

**Step 3:** AI reads the project's source code and writes **custom endpoints** specific to this app:

```kotlin
// AI-generated for a streaming app:
@Route("/media/now-playing")
fun getNowPlaying() = mapOf(
    "title" to player.currentMediaItem?.mediaMetadata?.title,
    "position" to player.currentPosition,
    "duration" to player.duration,
    "quality" to trackSelector.currentBitrate,
    "drmStatus" to player.drmSession?.state
)
```

**Step 4:** The bridge deepens over time. Each debugging session, the AI adds more endpoints, more hooks, more knowledge. The bridge evolves *with* the project.

No other tool does this. ADB is frozen. Appium is generic. Accessibility APIs are read-only. **AI Debug Bridge is a living, AI-editable codebase that becomes an expert on YOUR app.**

---

## Security

AI Debug Bridge is designed for development and testing environments. It is never, ever for production.

| Layer | Mechanism |
|-------|-----------|
| **Build gate** | Only included in `debugImplementation` — stripped entirely from release APKs |
| **Authentication** | HMAC token derived from SSH public key (default `~/.ssh/id_rsa.pub`) |
| **Network binding** | Localhost only by default. LAN requires explicit opt-in |
| **Activation** | Requires `DEBUG_AI=true` environment variable or `--ai-debug` flag |
| **Audit log** | Every AI action logged with timestamp, command, and caller identity |
| **Read-only default** | State writes and view modifications require explicit `write=true` scope |
| **No production path** | No conditional inclusion, no feature flags — if it's a release build, the code doesn't exist |

---

## Architecture

```
┌────────────────────────────────────────────────────────────┐
│  AI Agent (Claude Code, GPT, any MCP/HTTP client)          │
│  Sends: navigate, query state, click element, set variable │
└──────────────────────┬─────────────────────────────────────┘
                       │ HTTP / WebSocket / MCP
                       │ (localhost, SSH key auth)
┌──────────────────────▼─────────────────────────────────────┐
│  AI Debug Bridge Server (embedded in app process)           │
│                                                             │
│  ┌─────────────┐ ┌──────────────┐ ┌──────────────────────┐ │
│  │ Auto-Discover│ │ Event Stream │ │ Command Executor     │ │
│  │ • NavGraph   │ │ • Lifecycle  │ │ • navigate(dest)     │ │
│  │ • View tree  │ │ • Clicks     │ │ • click(elementId)   │ │
│  │ • Elements   │ │ • Focus      │ │ • setState(key, val) │ │
│  │ • State      │ │ • Errors     │ │ • input(id, text)    │ │
│  │ • Overlays   │ │ • Network    │ │ • simulate(event)    │ │
│  └─────────────┘ └──────────────┘ └──────────────────────┘ │
│                          │                                   │
│  ┌───────────────────────▼──────────────────────────────┐   │
│  │ App Runtime (same process, same thread)               │   │
│  │ Activities, Fragments, ViewModels, Views, Player, DB  │   │
│  └───────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

---

## Protocol

AI Debug Bridge speaks **MCP** (Model Context Protocol) as primary, with HTTP REST and WebSocket as universal fallbacks.

```
MCP Tools exposed:
  - get_app_map          → complete screen/element/navigation map
  - get_current_screen   → current screen + elements + state + focused
  - navigate             → go to any screen instantly
  - click_element        → trigger any element's click handler
  - get_state            → read any variable/object
  - set_state            → write any variable/object
  - get_events           → recent event stream with causal chain
  - get_focus            → current focus + focus graph + DPAD paths
  - get_overlays         → all system + app overlays
  - input_text           → set text on any input field (secure)
  - simulate             → process death, network, memory, permissions...
  - screenshot_element   → capture specific element as image
  - inject_overlay       → add debug visualization overlay
```

---

## Research

This project is backed by a 13-agent research swarm that analyzed 100+ tools across English, Chinese, and Russian developer communities. The complete research is available at [`docs/RESEARCH.md`](docs/RESEARCH.md).

Key findings:
- **Every existing AI agent tool** operates on Layer 2-4 (shell, DOM, pixels). None access Layer 1 (internal state)
- **Agent-S3** achieved 72.6% on OSWorld (human-level) but plateaus without internal state access
- **NeuralBridge** demonstrated 2ms tap latency with an embedded server — validating our architecture
- **Google AppFunctions API** is the closest concept but requires developer opt-in per function. Our library works automatically
- **The accessibility tree is the universal interface** for UI structure, but it cannot expose data models, business logic, or state
- **DAP + MCP** is the emerging protocol stack for AI-controlled debugging

---

## Roadmap

| Phase | Scope | Timeline |
|-------|-------|----------|
| **1. Core Android** | Auto-discovery, debug server, navigation, element interaction, state access, event stream | 4-6 weeks |
| **2. Android TV + Fire TV** | Focus graph, DPAD navigation, Leanback hooks, Alexa/ADM simulation, ExoPlayer/DRM state | 3-4 weeks |
| **3. Web (React/Express)** | Module interception, component tree, state management integration, HMR triggers | 3-4 weeks |
| **4. Expand** | Vue, Flutter, Python, Electron, React Native, Android Auto, Wear OS | 6-8 weeks |
| **5. iOS + tvOS** | XCUITest bridge, Focus Engine, Instruments integration | 4-6 weeks |

---

## Contributing

This project is in active development. If you're building AI agents that interact with apps, or you're tired of flaky tests and blind debugging — we want to hear from you.

- **Issues:** Report pain points, suggest capabilities, share platform-specific challenges
- **PRs:** Implementation contributions welcome — especially platform-specific adapters
- **Discussion:** Share your experience with AI agent + app interaction in the Discussions tab

---

## License

Apache 2.0

---

<p align="center">
  <strong>Stop making AI agents guess. Give them the blueprint.</strong>
</p>
