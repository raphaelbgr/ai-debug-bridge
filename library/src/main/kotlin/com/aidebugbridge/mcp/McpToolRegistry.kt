package com.aidebugbridge.mcp

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Registry of MCP tools that map to the bridge's HTTP endpoints.
 * Each tool has a name, description, input schema, and handler function.
 */
class McpToolRegistry {

    data class McpTool(
        val name: String,
        val description: String,
        val inputSchema: JsonObject,
        val handler: (JsonObject) -> JsonElement,
    )

    private val tools = mutableMapOf<String, McpTool>()

    init {
        registerDefaultTools()
    }

    fun registerTool(tool: McpTool) {
        tools[tool.name] = tool
    }

    fun listTools(): List<McpTool> = tools.values.toList()

    fun callTool(name: String, arguments: JsonObject): JsonElement {
        val tool = tools[name]
            ?: throw IllegalArgumentException("Unknown tool: $name")
        return tool.handler(arguments)
    }

    private fun registerDefaultTools() {
        registerTool(McpTool(
            name = "app_map",
            description = "Get a complete map of the app: activities, fragments, navigation graph, view trees",
            inputSchema = buildJsonObject {
                put("type", "object")
                put("properties", buildJsonObject {})
            },
            handler = { _ ->
                // TODO: Delegate to MapEndpoint logic
                buildJsonObject { put("status", "ok") }
            }
        ))

        registerTool(McpTool(
            name = "current_screen",
            description = "Get the currently visible screen: activity, fragments, view tree",
            inputSchema = buildJsonObject {
                put("type", "object")
                put("properties", buildJsonObject {})
            },
            handler = { _ ->
                // TODO: Delegate to CurrentEndpoint logic
                buildJsonObject { put("status", "ok") }
            }
        ))

        registerTool(McpTool(
            name = "navigate",
            description = "Navigate to a specific screen via deep link, intent, or nav component",
            inputSchema = buildJsonObject {
                put("type", "object")
                put("properties", buildJsonObject {
                    put("destination", buildJsonObject {
                        put("type", "string")
                        put("description", "Target destination (URL, class name, or nav route)")
                    })
                    put("method", buildJsonObject {
                        put("type", "string")
                        put("enum", kotlinx.serialization.json.buildJsonArray {
                            add(kotlinx.serialization.json.JsonPrimitive("DEEP_LINK"))
                            add(kotlinx.serialization.json.JsonPrimitive("INTENT"))
                            add(kotlinx.serialization.json.JsonPrimitive("NAV_COMPONENT"))
                            add(kotlinx.serialization.json.JsonPrimitive("BACK"))
                        })
                    })
                })
                put("required", kotlinx.serialization.json.buildJsonArray {
                    add(kotlinx.serialization.json.JsonPrimitive("destination"))
                })
            },
            handler = { _ ->
                // TODO: Delegate to NavigateEndpoint logic
                buildJsonObject { put("status", "ok") }
            }
        ))

        registerTool(McpTool(
            name = "simulate_input",
            description = "Simulate DPAD navigation, taps, swipes, and key events",
            inputSchema = buildJsonObject {
                put("type", "object")
                put("properties", buildJsonObject {
                    put("gesture", buildJsonObject {
                        put("type", "string")
                        put("description", "Gesture type: TAP, LONG_PRESS, SWIPE, DPAD_UP/DOWN/LEFT/RIGHT/CENTER, KEY_EVENT")
                    })
                })
                put("required", kotlinx.serialization.json.buildJsonArray {
                    add(kotlinx.serialization.json.JsonPrimitive("gesture"))
                })
            },
            handler = { _ ->
                // TODO: Delegate to SimulateEndpoint logic
                buildJsonObject { put("status", "ok") }
            }
        ))

        registerTool(McpTool(
            name = "screenshot",
            description = "Capture a screenshot of the current screen as base64 PNG",
            inputSchema = buildJsonObject {
                put("type", "object")
                put("properties", buildJsonObject {
                    put("format", buildJsonObject {
                        put("type", "string")
                        put("default", "png")
                    })
                    put("scale", buildJsonObject {
                        put("type", "number")
                        put("default", 1.0)
                    })
                })
            },
            handler = { _ ->
                // TODO: Delegate to ScreenshotEndpoint logic
                buildJsonObject { put("status", "ok") }
            }
        ))
    }
}
