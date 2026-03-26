package com.aidebugbridge.mcp

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * MCP transport layer supporting stdio and HTTP modes.
 *
 * - stdio: Reads JSON-RPC from stdin, writes to stdout (for local dev)
 * - HTTP: Served via the bridge's Ktor server on /mcp endpoint
 *
 * The HTTP transport is integrated into BridgeServer's routing.
 * The stdio transport is useful for direct MCP client connections.
 */
class McpTransport(
    private val protocol: McpProtocol,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Handle a raw JSON-RPC string and return the response string.
     * Used by both stdio and HTTP transports.
     */
    fun handleMessage(rawMessage: String): String {
        return try {
            val request = json.decodeFromString<McpProtocol.JsonRpcRequest>(rawMessage)
            val response = protocol.handleRequest(request)
            json.encodeToString(response)
        } catch (e: Exception) {
            val errorResponse = McpProtocol.JsonRpcResponse(
                error = McpProtocol.JsonRpcError(
                    code = -32700,
                    message = "Parse error: ${e.message}"
                )
            )
            json.encodeToString(errorResponse)
        }
    }

    /**
     * Start stdio transport — reads from System.in, writes to System.out.
     * Blocks the calling coroutine. Useful for local development.
     */
    fun startStdio() {
        scope.launch {
            val reader = System.`in`.bufferedReader()
            var line = reader.readLine()

            while (line != null) {
                if (line.isNotBlank()) {
                    val response = handleMessage(line)
                    println(response)
                    System.out.flush()
                }
                line = reader.readLine()
            }
        }
    }
}
