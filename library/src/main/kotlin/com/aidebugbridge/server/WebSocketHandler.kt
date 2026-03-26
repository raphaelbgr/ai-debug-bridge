package com.aidebugbridge.server

import com.aidebugbridge.events.EventBus
import com.aidebugbridge.protocol.BridgeResponse
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * WebSocket handler that streams real-time events to connected AI agents.
 *
 * Clients connect to /ws and receive a continuous stream of:
 * - Activity lifecycle events
 * - Fragment transitions
 * - View hierarchy changes
 * - User interactions
 * - Navigation events
 * - State changes
 *
 * Supports client-side filtering by event type via subscribe messages.
 */
class WebSocketHandler(
    private val eventBus: EventBus,
) {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    /**
     * Handle a WebSocket session for a connected AI agent.
     * Sends a welcome message, then continuously streams events.
     */
    suspend fun handle(session: DefaultWebSocketServerSession) {
        // Track which event types this client wants (empty = all)
        val subscribedTypes = mutableSetOf<String>()

        // Send welcome message
        val welcome = BridgeResponse.EventMessage(
            type = "connected",
            timestamp = System.currentTimeMillis(),
            data = mapOf("message" to "AI Debug Bridge WebSocket connected")
        )
        session.send(Frame.Text(json.encodeToString(welcome)))

        // Forward filtered events from the bus to this WebSocket client
        val job = session.launch {
            eventBus.events.collectLatest { event ->
                try {
                    // Apply type filter if client has subscribed to specific types
                    if (subscribedTypes.isNotEmpty() && event.type !in subscribedTypes) {
                        return@collectLatest
                    }

                    val message = BridgeResponse.EventMessage(
                        type = event.type,
                        timestamp = event.timestamp,
                        data = event.data
                    )
                    session.send(Frame.Text(json.encodeToString(message)))
                } catch (_: Exception) {
                    // Client disconnected or send failed
                }
            }
        }

        try {
            // Keep session alive, listen for incoming commands
            session.incoming.consumeEach { frame ->
                if (frame is Frame.Text) {
                    val text = frame.readText()
                    handleClientMessage(text, subscribedTypes, session)
                }
            }
        } finally {
            job.cancel()
        }
    }

    /**
     * Parse incoming commands from AI agents.
     * Supported messages:
     * - `{"subscribe": ["activity.*", "fragment.*"]}` — filter event types
     * - `{"subscribe": []}` — receive all events
     * - `{"ping": true}` — respond with pong
     */
    private suspend fun handleClientMessage(
        text: String,
        subscribedTypes: MutableSet<String>,
        session: DefaultWebSocketServerSession
    ) {
        try {
            val parsed = json.parseToJsonElement(text)
            val obj = parsed as? kotlinx.serialization.json.JsonObject ?: return

            // Handle subscribe command
            obj["subscribe"]?.let { element ->
                val array = element as? kotlinx.serialization.json.JsonArray ?: return@let
                subscribedTypes.clear()
                array.forEach { item ->
                    val typeStr = item.toString().removeSurrounding("\"")
                    if (typeStr.isNotBlank()) subscribedTypes.add(typeStr)
                }
                val ack = BridgeResponse.EventMessage(
                    type = "subscribed",
                    timestamp = System.currentTimeMillis(),
                    data = mapOf("types" to subscribedTypes.joinToString(","))
                )
                session.send(Frame.Text(json.encodeToString(ack)))
            }

            // Handle ping
            if (obj.containsKey("ping")) {
                val pong = BridgeResponse.EventMessage(
                    type = "pong",
                    timestamp = System.currentTimeMillis(),
                )
                session.send(Frame.Text(json.encodeToString(pong)))
            }
        } catch (_: Exception) {
            // Ignore malformed messages
        }
    }
}
