# AI Debug Bridge — Comprehensive Research (2026-03-25)

## Vision
A universal library that when added to ANY project, auto-generates an internal "map" of the app (screens, elements, nav graph, state) and opens a debug server the AI connects to. The AI knows exactly where everything is — from INSIDE, not outside. And the AI can ADAPT the library deeper into the project over time.

## Why Embedded > External (ADB/Appium/Screenshots)

| Dimension | ADB/Appium (External) | Our Library (Embedded) |
|-----------|----------------------|------------------------|
| Latency | 50-200ms per command | **<1ms** — same process |
| View hierarchy | `uiautomator dump` → stale XML | **Live object references** — never stale |
| Navigation | `input keyevent` × N, hope focus lands right | `navController.navigate(dest)` — **instant** |
| Element interaction | Find bounds → `input tap X Y` — fragile | `view.performClick()` — **direct ref** |
| State access | **ZERO** — cannot read ViewModel/LiveData | **Full access** to any variable |
| Off-screen elements | Invisible — not in hierarchy | Access adapter data directly |
| Compose/custom views | Flat featureless node | Traverse slot table from inside |
| AI can extend it | No — ADB is frozen | **YES — AI writes new endpoints per project** |
| Security | Full device access (dangerous) | Only THIS app's process (sandboxed) |

## The $61B Problem We Solve

| Stat | Value | Source |
|------|-------|-------|
| Dev time on debugging | **35-50%** of all work | ACM Queue |
| Time finding vs fixing | **90% finding / 10% fixing** | Industry consensus |
| Global debugging cost | **$61 billion/year** | Cambridge/Undo |
| #1 barrier | **"Cannot reproduce" (41%)** | Cambridge survey |
| Unresolved bugs from irreproducibility | **91% of backlogs** | Shake |
| Average time per bug fix | **13 hours** | Undo |
| ROI per dev per year | **$33,600 saved** (7 hrs/week) | Calculated from Stripe data |

## 15 Pain Points Solved

| # | Pain Point | Current Failure | Our Solution |
|---|-----------|----------------|--------------|
| 1 | Stale element refs | `StaleElementReferenceException` on re-render | Semantic IDs bound to logic, survive re-renders |
| 2 | Missing accessibility IDs | Dev forgot `contentDescription` → invisible | Auto-discover ALL views from inside — no annotations |
| 3 | Appium lookup latency | 2-10s per `findElement()` (serialize+HTTP+XPath) | In-process memory access → microseconds |
| 4 | Espresso async flakiness | Manual `IdlingResource` registration, incomplete | Auto-hook coroutines, OkHttp, animation controllers |
| 5 | AI screenshot misclicks | Pixel coordinates, DPI errors, 2px miss = wrong element | `POST /action/element_id` → `performClick()`, zero coordinates |
| 6 | Fire TV DPAD focus loss | Blind keycode sequences, no focus tracking | `navigateToElement(id)` → optimal DPAD path + confirm focus |
| 7 | Cannot reproduce bugs | Stack trace only, no state context, 41% give up | Always-on state snapshots: screen + data + player + user actions |
| 8 | ExoPlayer/Media3 debugging | `ERROR_CODE_IO_UNSPECIFIED`, no context | Real-time buffer %, bandwidth, decoder, DRM status via API |
| 9 | Self-healing masks real bugs | Fixes 28% (selectors only), 72% unfixed | Stable semantic IDs make self-healing unnecessary |
| 10 | Compose/custom views opaque | UIAutomator sees flat featureless node | Traverse Compose slot table directly from inside runtime |
| 11 | Deep link verification | Can verify screen visually, not internal nav state | Expose `currentDestination`, back-stack, fragment args directly |
| 12 | Device fragmentation | 1000s of configs, tests × devices = slow | Library inside APK, reports device context + unified API |
| 13 | Test maintenance burden | Every UI tweak breaks locators | Stable semantic contract layer survives redesigns |
| 14 | Off-screen elements | RecyclerView items invisible until scrolled | Access adapter data directly, `scrollToPosition()` + confirm |
| 15 | Multi-step workflow branching | AI stuck on unexpected dialog | Expose current state enum + `getAvailableActions()` |

## Research: 10 Parallel Agent Swarm (70+ search queries, EN/CN/RU)

---

## 1. CORE ARCHITECTURE

```
npm install @raphael/ai-debug-bridge
```

```
Your App
  │
  └── initAIDebug({ auth: 'ssh-key', port: 0 })
      │
      ├── Layer 1: Module Interception
      │   ├── require-in-the-middle (CJS)
      │   ├── import-in-the-middle (ESM)
      │   └── module.registerHooks() (Node 25+ future)
      │
      ├── Layer 2: Framework Detection + Auto-Patching
      │   ├── Scan package.json deps
      │   ├── Load matching instrumentors (Express, React, Next, etc.)
      │   └── shimmer.wrap() on key methods
      │
      ├── Layer 3: State Bridge
      │   ├── Redux DevTools protocol (SocketCluster WebSocket)
      │   ├── vue-mcp-next pattern (MCP tools)
      │   ├── MobX spy() + intercept()
      │   ├── CopilotKit AG-UI protocol (SSE events)
      │   └── Custom state snapshots
      │
      ├── Layer 4: UI Tree Access
      │   ├── Accessibility APIs (platform-native, auto)
      │   ├── React Fiber tree traversal
      │   ├── Playwright MCP (web)
      │   └── ADB accessibility service (Android)
      │
      ├── Layer 5: Visual Capture
      │   ├── Playwright locator.screenshot() (web, headless)
      │   ├── snapDOM @zumer/snapdom (client-side, 148x fast)
      │   ├── Android View.draw(Canvas) via ADB
      │   └── Applitools Eyes (AI-powered visual diff)
      │
      ├── Layer 6: Hot Reload / Live Patching
      │   ├── V8 Inspector Runtime.evaluate / setScriptSource
      │   ├── Vite/Webpack HMR (write file → auto-reload)
      │   ├── Flutter VM Service reloadSources
      │   ├── Python Jurigged (live __code__ replacement)
      │   └── Erlang code:load_file/1
      │
      ├── Layer 7: Protocol / Communication
      │   ├── MCP (primary — AI agent tool calls)
      │   ├── DAP (Debug Adapter Protocol — cross-language debug)
      │   ├── AG-UI (agent-to-app SSE streaming)
      │   ├── CDP (Chrome DevTools Protocol)
      │   └── REST + WebSocket (universal fallback)
      │
      └── Layer 8: Security
          ├── SSH key HMAC auth (default ~/.ssh/id_rsa.pub)
          ├── Debug mode only (DEBUG_AI=true)
          ├── Localhost/LAN binding only
          ├── Audit log of all AI actions
          └── Production builds strip everything
```

---

## 2. KEY EXISTING TOOLS TO INTEGRATE

### Must-Have (build on top of these)

| Tool | Role | Install |
|------|------|---------|
| `require-in-the-middle` | CJS module interception | `npm i require-in-the-middle` |
| `import-in-the-middle` | ESM module interception | `npm i import-in-the-middle` |
| `shimmer` | Safe function wrapping | `npm i shimmer` |
| CopilotKit / AG-UI | Agent-to-app state bridge (React) | `npm i @copilotkit/react-core` |
| vue-mcp-next | MCP tools for Vue apps | `npm i vue-mcp-next` |
| MCP-FE | Cross-framework MCP bridge | `mcp-fe.ai` |
| Playwright MCP | Browser accessibility tree for AI | `npm i @playwright/mcp` |
| NexusCore MCP | Frida + MCP bridge | github.com/sjkim1127/Nexuscore_MCP |
| mcp-debugger | DAP over MCP (6+ languages) | github.com/debugmcp/mcp-debugger |
| debug-that | Universal AI debugger CLI | github.com/theodo-group/debug-that |

### Strong Candidates

| Tool | Role | Why |
|------|------|-----|
| Reactotron | React/RN debug server (WebSocket) | Mature, state + network + async |
| Python Manhole | REPL injection into running Python | 2 lines, Unix socket |
| debugpy | Python DAP server | Full debug protocol |
| Arthas | JVM runtime diagnostics (HTTP API) | Most AI-ready Java tool |
| Frida | Universal process injection | Any OS, any language |
| njsTrace | Node.js auto-trace via Module._compile() | Zero code changes |
| Jurigged | Python hot reload (live __code__ swap) | Best Python live patching |
| AccessKit | Cross-platform accessibility tree (Rust) | Universal UI query |
| Stagehand v3 | AI-native web control (act/extract/observe) | Built for AI agents |
| Midscene.js | Vision-based UI + MCP service | Android via ADB too |

### State Management Integration

| Library | Protocol | AI Access |
|---------|----------|-----------|
| Redux | SocketCluster WebSocket | Full read/write, time-travel |
| Zustand | Redux DevTools (via middleware) | Same as Redux |
| MobX | spy() + intercept() | Pre-mutation gating |
| Vue/Pinia | vue-mcp-next MCP tools | Read/write via MCP |
| XState | WebSocket inspection protocol | Read-only actors/events |
| Jotai | Redux DevTools bridge | Snapshot read/write |
| TanStack Query | window global + queryClient API | Cache read/write |

---

## 3. PLATFORM DEBUG PROTOCOLS

| Platform | Protocol | AI Readiness |
|----------|----------|:---:|
| Electron/Chrome | CDP (WebSocket) | Best |
| Node.js | V8 Inspector (CDP-compatible) | Best |
| Android | ADB + AccessibilityService | Great |
| Flutter | Dart VM Service (WebSocket) | Great |
| Python | debugpy (DAP) + PEP 768 | Great |
| iOS | XCUITest + WebDriverAgent | Good (gated) |
| Tauri | WebView CDP + CrabNebula plugin | Good |
| Web | Playwright MCP / WebDriver BiDi | Best |
| Cross-platform | AccessKit (Rust) | Emerging |
| **Android TV** | ADB + Leanback accessibility + DPAD nav | Great |
| **Fire TV / Fire OS** | ADB (WiFi default) + Fire TV accessibility | Great |
| **tvOS** | XCUITest + Focus Engine + Instruments | Good (gated) |
| **Wear OS** | ADB + Compose accessibility | Good |

### TV / Embedded Platform Deep Dive

| Platform | Debug Access | Input Model | Unique Challenges | AI Bridge Strategy |
|----------|-------------|-------------|-------------------|-------------------|
| **Android TV** | Same ADB as mobile. WiFi ADB common. `uiautomator dump` works. AccessibilityService works. | DPAD (5-way nav) — no touch. Focus-based UI. | Focus state is critical — AI must track which element has focus. Leanback library has custom focus logic. Long-press = context menu. | ADB `input keyevent` for DPAD. AccessibilityService for focus tracking. `View.draw()` for element screenshots. Custom instrumentor for Leanback `BrowseSupportFragment`, `DetailsSupportFragment`. |
| **Fire TV / Fire OS** | ADB over WiFi (enabled by default in dev mode). Same Android debug stack. Fire TV launcher has custom UI. | DPAD + Alexa Voice. Same as Android TV but with Amazon extensions. | Fire OS is forked Android — some APIs differ. Amazon Appstore review restrictions. FireTV-specific `MediaSession` handling. | Same as Android TV. Additionally hook `MediaBrowserService` and `ExoPlayer`/`Media3` for streaming state. Hook `IMA` (Interactive Media Ads) for ad state. |
| **tvOS** | Xcode + Instruments. XCUITest via USB only (no WiFi debug). Limited to Apple ecosystem. | Siri Remote (trackpad + buttons). Focus Engine replaces touch. | No `UIWebView`/`WKWebView` (no web content debugging via CDP). Focus Engine is proprietary — no public API for focus prediction. TestFlight-only side-loading. | XCUITest for element tree. Instruments for performance/network. Custom debug server in app (localhost HTTP, same SSH key auth). Focus Engine state via private `UIFocusSystem` API (debug builds only). |
| **Wear OS** | ADB over Bluetooth or WiFi. Same Android debug tools. | Touch + crown/bezel rotation. Tiny screen (1.2-1.9"). | Screen too small for vision-based AI. Tile-based UI (not Activities for many interactions). Ambient mode. Battery constraints. | ADB + accessibility. Vision approach useless — must use structured tree. Wear Compose accessibility works well. Crown events via `RotaryScrollEvent`. |

### Streaming/Media-Specific Hooks (Raphael's expertise)

| Component | What to hook | AI gets access to |
|-----------|-------------|-------------------|
| **ExoPlayer / Media3** | `Player.Listener` events, `MediaItem`, `TrackSelection`, `LoadEventInfo` | Playback state, current position, buffer %, track info, DRM license status, load errors |
| **IMA SDK** (ads) | `AdsLoader.AdsLoadedListener`, `AdEvent.AdEventListener` | Ad pod state, current ad index, skip countdown, VAST errors, companion ads |
| **Widevine/PlayReady DRM** | `MediaDrm.OnEventListener`, key request/response | DRM session state, license expiry, key errors, security level |
| **HLS/DASH manifest** | `MediaSource.Factory`, `DashMediaSource`, `HlsMediaSource` | Manifest URL, variant streams, selected quality, ABR state, segment download timing |
| **Cast/Chromecast** | `CastContext`, `SessionManager`, `RemoteMediaClient` | Cast session state, connected device, remote playback position |
| **HDMI-CEC** | `HdmiControlManager` (Android TV) | Connected HDMI devices, CEC commands, power state |

---

## 4. HOT RELOAD MECHANISMS

| Runtime | Best Mechanism | AI Triggers It? | State Preserved? |
|---------|---------------|:---:|:---:|
| Node.js | V8 Inspector `Runtime.evaluate` | Yes (WebSocket) | Yes |
| Web (Vite) | Write file → HMR auto-reload | Yes (save file) | Yes |
| Flutter | VM Service `reloadSources` | Yes (WebSocket) | Yes |
| Python | Jurigged (live __code__ swap) | Yes (save file) | Yes |
| Erlang | `code:load_file/1` | Yes (RPC) | Yes |
| Java | JRebel / ByteBuddy | Yes (compile + save) | Yes |
| Android | ART JVMTI / Apply Changes | Partial | Yes |

Universal pattern: **write file → let watcher reload**

---

## 5. VISUAL CAPTURE STACK

| Use Case | Best Tool | Element-Level? |
|----------|-----------|:-:|
| Web (server-side) | Playwright `locator.screenshot()` | Yes |
| Web (client-side) | snapDOM `@zumer/snapdom` | Yes |
| Android | `View.draw(Canvas)` via ADB | Yes |
| Visual diff (free) | Playwright `toHaveScreenshot()` | Yes |
| Visual diff (AI) | Applitools Eyes | Yes |

---

## 6. SECURITY MODEL

| Layer | Mechanism |
|-------|-----------|
| Auth | HMAC token from SSH public key (configurable, default `~/.ssh/id_rsa.pub`) |
| Activation | `DEBUG_AI=true` env var or `--ai-debug` flag |
| Binding | Localhost/LAN only, never public |
| Audit | All AI actions logged with timestamp + caller |
| Production | All debug endpoints stripped at build time |
| Process | Read-only by default, write requires explicit opt-in |

---

## 7. RESEARCH SOURCES (by agent)

### Agent 1: Runtime Instrumentation
- CopilotKit, AG-UI Protocol, AgentBridge, Reactotron, Flipper, debugpy, Manhole, Stagehand, Midscene.js, Lightrun, QBDI, Python 3.14 remote debug

### Agent 2: Chinese/Russian Forums
- Frida, Arthas (Alibaba), Xposed, ByteBuddy, SkyWalking, GammaRay, Stetho, Flipper, DynamoRIO, Intel PIN, Valgrind, DynInst, Pyrasite

### Agent 3: Reddit/HN/Forums
- mcp-debugger, debug-that, Augur, Zentara Code, NexusCore MCP, njsTrace, Langfuse, AgentPrism, Lucidic

### Agent 4: Per-Platform Protocols
- ADB, XCUITest, CDP, React DevTools Hook, Dart VM Service, V8 Inspector, debugpy, PEP 768, CrabNebula, AccessKit, Appium

### Agent 5: Auto-Instrumentation
- require-in-the-middle, import-in-the-middle, shimmer, module.registerHooks(), OpenTelemetry, dd-trace, elastic-apm-node, newrelic, wrapt, ByteBuddy, Orchestrion

### Agent 6: UI Accessibility
- Windows UIA, macOS AXUIElement, Linux AT-SPI2, Android AccessibilityService, Playwright MCP, WebDriver BiDi, MutationObserver, React Fiber, Appium

### Agent 7: State Management
- Redux DevTools (SocketCluster), Zustand, MobX spy/intercept, vue-mcp-next, Svelte, Angular, XState, Recoil, Jotai, Pinia, TanStack Query, tRPC Panel, MCP-FE

### Agent 8: Screenshots
- html2canvas, html-to-image, snapDOM, Playwright, Puppeteer, Electron capturePage, Android View.draw, react-native-view-shot, Percy, Applitools, Chromatic, BackstopJS

### Agent 9: Hot Reload
- V8 Inspector setScriptSource, Vite HMR, Webpack HMR, React Fast Refresh, importlib.reload, Jurigged, Erlang hot swap, JRebel, Android DEX patching, Flutter VM Service, Clojure nREPL, InjectionIII

### Agent 10: AI Agent Control (State of the Art 2026)
- Claude Computer Use (screenshot + pixel coords, macOS, 14.9% OSWorld)
- OpenAI CUA/ChatGPT Agent (screenshot + RL vision, 38.1% OSWorld)
- Agent-S3 (72.6% OSWorld — first to beat humans, screenshot + grounding model)
- Browser Use (89.1% WebVoyager, DOM + Playwright, 70-100x faster than screenshots)
- AgentQL (semantic NL selectors for web elements)
- OmniParser V2 (Microsoft, screenshot → structured format)
- UI-TARS (ByteDance, pure vision 2B/7B/72B, Apache 2.0)
- NeuralBridge (in-app MCP server on Android, 2ms tap latency, 100x faster)
- AppAgent (Tencent, vision-based, 180s/task)
- Agent Device (accessibility-tree-first, iOS + Android, Claude Code skills)
- OpenClaw (247K GitHub stars, OS-level skills architecture)
- Devin v3.0 (sandboxed terminal + editor + browser, dynamic re-planning)
- Google Gemini Screen Auto (Android AppFunctions API — closest to our concept but opt-in)

**Critical finding:** ALL tools operate on Layer 4 (pixels), Layer 3 (DOM/a11y tree), or Layer 2 (OS shell). NONE access Layer 1 (application internal state). This is our gap.

---

## 8. COMPLEXITY ANALYSIS

### Effort by Platform

| Platform | Complexity | Effort | Existing primitives | Priority |
|----------|:---:|--------|--------------------|----|
| **Node.js / Web** | Low | 1-2 weeks | require-in-the-middle, shimmer, V8 Inspector, CDP, OTel, AG-UI | P0 — start here |
| **React** | Low | 3-5 days | React DevTools Hook, Fiber tree, CopilotKit, Fast Refresh | P0 — most common frontend |
| **Express/Fastify** | Low | 2-3 days | OTel instrumentors exist, shimmer wrapping proven | P0 — most common backend |
| **Next.js** | Medium | 1 week | import-in-the-middle, Vite/Webpack HMR, React instrumentor | P1 |
| **Vue** | Low | 3-5 days | vue-mcp-next already exists, Pinia devtools auto-register | P1 |
| **Android (mobile)** | Medium | 2-3 weeks | ADB, AccessibilityService, View.draw(), Frida, Flipper | P1 |
| **Android TV** | Medium | 1-2 weeks | Same as Android + Leanback hooks + DPAD focus tracking | P1 — Raphael's expertise |
| **Fire TV / Fire OS** | Medium | 1 week | Same as Android TV + ExoPlayer/IMA/DRM hooks | P1 — Raphael's expertise |
| **Python** | Medium | 1-2 weeks | debugpy, Manhole, Jurigged, PEP 768, OTel | P2 |
| **Flutter** | Medium | 1-2 weeks | Dart VM Service, mcp_toolkit, hot reload protocol | P2 |
| **Electron** | Low | 3-5 days | CDP (full access), Electron MCP Server (34 tools) | P2 |
| **iOS** | High | 3-4 weeks | XCUITest, WebDriverAgent, Instruments — Apple gated | P3 |
| **tvOS** | High | 3-4 weeks | Same as iOS + Focus Engine (proprietary) | P3 |
| **Java (backend)** | Medium | 2 weeks | Arthas, ByteBuddy, JVMTI, JRebel | P3 |
| **React Native** | Medium | 1-2 weeks | Flipper + Reactotron + Hermes Inspector | P2 |

### Estimated Total Scope

| Phase | Scope | Timeline | Deliverable |
|-------|-------|----------|-------------|
| **Phase 1: Core + Web** | Node.js module interception + Express/React instrumentors + MCP server + auth | 2-3 weeks | `npm install` → auto-instruments Express + React apps. `/ai-debug` skill for Claude Code |
| **Phase 2: Android/TV** | ADB bridge + AccessibilityService + ExoPlayer/Media3/IMA hooks + Leanback focus | 3-4 weeks | Android/Fire TV/Android TV support. Streaming-specific state exposure |
| **Phase 3: Expand** | Vue, Flutter, Python, Electron, React Native | 4-6 weeks | Multi-platform coverage |
| **Phase 4: iOS/tvOS** | XCUITest integration, Focus Engine, Instruments bridge | 3-4 weeks | Apple platform support |
| **Phase 5: Polish** | Visual diff, hot reload triggers, state time-travel, action recording | 2-3 weeks | Advanced features |

### Risk Assessment

| Risk | Impact | Mitigation |
|------|--------|------------|
| Framework version fragmentation | High — React 17 vs 18 vs 19 internal APIs differ | Version-detect + adapter per version (OTel pattern) |
| Performance overhead of instrumentation | Medium — shimmer wrapping adds ~1μs per call | Benchmark gating, disable individual hooks |
| Security — debug endpoints in production | Critical — accidental leak = full app state exposed | Triple-gate: env var + auth + localhost-only. Build-time strip |
| Fire OS forked Android differences | Low-Medium — some APIs missing/changed | Test on Fire TV stick specifically |
| Apple rejecting apps with debug server | High for App Store — internal HTTP server triggers review | Strip entirely from release builds, no conditional inclusion |
| ESM vs CJS fragmentation in Node.js | Medium — different interception mechanisms | Dual-loader via import-in-the-middle + require-in-the-middle |

---

## 9. NEXT STEPS

1. Create `@raphael/ai-debug-bridge` npm package (or `ai-debug-bridge` for public)
2. Phase 1: Core + Web (2-3 weeks)
   - Module interception (require-in-the-middle + import-in-the-middle)
   - Express/React auto-instrumentors
   - MCP server exposing debug tools
   - SSH key HMAC auth
   - Claude Code skill (`/ai-debug`)
3. Phase 2: Android/TV (3-4 weeks) — Raphael's expertise
   - ADB bridge + AccessibilityService instrumentor
   - ExoPlayer/Media3/IMA/DRM state hooks
   - Leanback focus tracking (Android TV)
   - Fire OS compatibility layer
   - DPAD navigation state
4. Phase 3: Expand (4-6 weeks)
   - Vue (via vue-mcp-next patterns), Flutter, Python, Electron, React Native
5. Phase 4: iOS/tvOS (3-4 weeks)
   - XCUITest, Focus Engine, Instruments bridge
6. Open source on GitHub — potential to become a foundational tool in the AI agent ecosystem

### LinkedIn Post #3: This Project
This research itself is a LinkedIn post: "Every AI agent in 2026 treats your app as a black box. I'm building the missing layer."
