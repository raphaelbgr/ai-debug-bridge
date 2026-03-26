package com.aidebugbridge.mcp

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * Model Context Protocol (MCP) message handling.
 *
 * Implements the MCP JSON-RPC 2.0 protocol so that AI agents using
 * MCP-compatible clients (Claude Code, etc.) can discover and invoke
 * bridge tools natively.
 */
class McpProtocol(private val toolRegistry: McpToolRegistry) {

    @Serializable
    data class JsonRpcRequest(
        val jsonrpc: String = "2.0",
        val id: Long? = null,
        val method: String,
        val params: JsonObject? = null,
    )

    @Serializable
    data class JsonRpcResponse(
        val jsonrpc: String = "2.0",
        val id: Long? = null,
        val result: JsonElement? = null,
        val error: JsonRpcError? = null,
    )

    @Serializable
    data class JsonRpcError(
        val code: Int,
        val message: String,
        val data: JsonElement? = null,
    )

    /**
     * Handle an incoming MCP JSON-RPC request and return the response.
     */
    fun handleRequest(request: JsonRpcRequest): JsonRpcResponse {
        return when (request.method) {
            "initialize" -> handleInitialize(request)
            "tools/list" -> handleToolsList(request)
            "tools/call" -> handleToolsCall(request)
            "ping" -> JsonRpcResponse(id = request.id, result = JsonObject(emptyMap()))
            else -> JsonRpcResponse(
                id = request.id,
                error = JsonRpcError(
                    code = -32601,
                    message = "Method not found: ${request.method}"
                )
            )
        }
    }

    private fun handleInitialize(request: JsonRpcRequest): JsonRpcResponse {
        val result = kotlinx.serialization.json.buildJsonObject {
            put("protocolVersion", kotlinx.serialization.json.JsonPrimitive("2024-11-05"))
            put("capabilities", kotlinx.serialization.json.buildJsonObject {
                put("tools", kotlinx.serialization.json.buildJsonObject {})
            })
            put("serverInfo", kotlinx.serialization.json.buildJsonObject {
                put("name", kotlinx.serialization.json.JsonPrimitive("ai-debug-bridge"))
                put("version", kotlinx.serialization.json.JsonPrimitive("0.1.0"))
            })
        }
        return JsonRpcResponse(id = request.id, result = result)
    }

    private fun handleToolsList(request: JsonRpcRequest): JsonRpcResponse {
        val tools = toolRegistry.listTools()
        val result = kotlinx.serialization.json.buildJsonObject {
            put("tools", kotlinx.serialization.json.buildJsonArray {
                tools.forEach { tool ->
                    add(kotlinx.serialization.json.buildJsonObject {
                        put("name", kotlinx.serialization.json.JsonPrimitive(tool.name))
                        put("description", kotlinx.serialization.json.JsonPrimitive(tool.description))
                        put("inputSchema", tool.inputSchema)
                    })
                }
            })
        }
        return JsonRpcResponse(id = request.id, result = result)
    }

    private fun handleToolsCall(request: JsonRpcRequest): JsonRpcResponse {
        val params = request.params ?: return JsonRpcResponse(
            id = request.id,
            error = JsonRpcError(-32602, "Missing params")
        )

        val toolName = (params["name"] as? kotlinx.serialization.json.JsonPrimitive)?.content
            ?: return JsonRpcResponse(
                id = request.id,
                error = JsonRpcError(-32602, "Missing tool name")
            )

        val arguments = params["arguments"] as? JsonObject ?: JsonObject(emptyMap())

        return try {
            val result = toolRegistry.callTool(toolName, arguments)
            JsonRpcResponse(id = request.id, result = result)
        } catch (e: Exception) {
            JsonRpcResponse(
                id = request.id,
                error = JsonRpcError(-32603, e.message ?: "Tool execution failed")
            )
        }
    }
}
