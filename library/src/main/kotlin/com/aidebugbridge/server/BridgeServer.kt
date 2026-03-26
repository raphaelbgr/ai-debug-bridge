package com.aidebugbridge.server

import android.util.Log
import com.aidebugbridge.auth.HmacAuth
import com.aidebugbridge.discovery.DiscoveryEngine
import com.aidebugbridge.endpoints.*
import com.aidebugbridge.events.EventBus
import com.aidebugbridge.mcp.McpProtocol
import com.aidebugbridge.mcp.McpToolRegistry
import com.aidebugbridge.mcp.McpTransport
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/**
 * Ktor embedded HTTP/WebSocket server that exposes debugging endpoints
 * for AI agents to inspect and interact with the running Android app.
 */
class BridgeServer(
    private val port: Int,
    private val discoveryEngine: DiscoveryEngine,
    private val eventBus: EventBus,
    private val auth: HmacAuth?,
) {
    companion object {
        private const val TAG = "BridgeServer"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var engine: NettyApplicationEngine? = null

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun start() {
        scope.launch {
            try {
                engine = embeddedServer(Netty, port = port) {
                    install(ContentNegotiation) {
                        json(this@BridgeServer.json)
                    }
                    install(WebSockets)

                    configureAuth()
                    configureRouting()
                }.start(wait = false)

                Log.i(TAG, "Bridge server listening on port $port")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start bridge server", e)
            }
        }
    }

    fun stop() {
        engine?.stop(gracePeriodMillis = 1000, timeoutMillis = 3000)
        engine = null
        Log.i(TAG, "Bridge server stopped")
    }

    fun isRunning(): Boolean = engine != null

    private fun Application.configureAuth() {
        if (auth == null) return

        intercept(ApplicationCallPipeline.Plugins) {
            val token = call.request.header("Authorization")?.removePrefix("Bearer ")
            val timestamp = call.request.header("X-Timestamp")
            val signature = call.request.header("X-Signature")

            if (signature != null && timestamp != null) {
                // HMAC signature auth
                val body = call.request.header("X-Body-Hash") ?: ""
                if (!auth.verify(timestamp, body, signature)) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid HMAC signature"))
                    finish()
                    return@intercept
                }
            } else if (token != null) {
                // Bearer token auth
                if (!auth.verifyToken(token)) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid token"))
                    finish()
                    return@intercept
                }
            } else {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Authentication required"))
                finish()
                return@intercept
            }
        }
    }

    private fun Application.configureRouting() {
        val mapEndpoint = MapEndpoint(discoveryEngine)
        val currentEndpoint = CurrentEndpoint(discoveryEngine)
        val navigateEndpoint = NavigateEndpoint(discoveryEngine)
        val actionEndpoint = ActionEndpoint(discoveryEngine)
        val inputEndpoint = InputEndpoint(discoveryEngine)
        val stateEndpoint = StateEndpoint(discoveryEngine)
        val eventsEndpoint = EventsEndpoint(eventBus)
        val focusEndpoint = FocusEndpoint(discoveryEngine)
        val overlaysEndpoint = OverlaysEndpoint(discoveryEngine)
        val memoryEndpoint = MemoryEndpoint()
        val simulateEndpoint = SimulateEndpoint(discoveryEngine)
        val screenshotEndpoint = ScreenshotEndpoint(discoveryEngine)
        val webSocketHandler = WebSocketHandler(eventBus)
        val mcpToolRegistry = McpToolRegistry()
        val mcpProtocol = McpProtocol(mcpToolRegistry)
        val mcpTransport = McpTransport(mcpProtocol)

        routing {
            get("/") {
                call.respond(
                    mapOf(
                        "name" to "AI Debug Bridge",
                        "version" to "0.1.0",
                        "endpoints" to listOf(
                            "/map", "/current", "/navigate", "/action",
                            "/input", "/state", "/events", "/focus",
                            "/overlays", "/memory", "/simulate", "/screenshot",
                            "/mcp"
                        )
                    )
                )
            }

            get("/map") { mapEndpoint.handle(call) }
            get("/current") { currentEndpoint.handle(call) }
            post("/navigate") { navigateEndpoint.handle(call) }
            post("/action") { actionEndpoint.handle(call) }
            post("/input") { inputEndpoint.handle(call) }
            get("/state") { stateEndpoint.handleGet(call) }
            post("/state") { stateEndpoint.handlePost(call) }
            get("/events") { eventsEndpoint.handle(call) }
            get("/focus") { focusEndpoint.handle(call) }
            get("/overlays") { overlaysEndpoint.handle(call) }
            get("/memory") { memoryEndpoint.handle(call) }
            post("/simulate") { simulateEndpoint.handle(call) }
            get("/screenshot") { screenshotEndpoint.handle(call) }

            // MCP JSON-RPC 2.0 endpoint
            post("/mcp") {
                val body = call.receiveText()
                val response = mcpTransport.handleMessage(body)
                call.respondText(response, io.ktor.http.ContentType.Application.Json)
            }

            webSocket("/ws") {
                webSocketHandler.handle(this)
            }
        }
    }
}
