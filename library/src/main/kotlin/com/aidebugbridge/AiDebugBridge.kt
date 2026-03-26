package com.aidebugbridge

import android.app.Application
import android.util.Log
import com.aidebugbridge.auth.HmacAuth
import com.aidebugbridge.discovery.DiscoveryEngine
import com.aidebugbridge.events.EventBus
import com.aidebugbridge.server.BridgeServer

/**
 * AI Debug Bridge — main entry point.
 *
 * Embeds an HTTP/WebSocket server inside Android apps for AI agent debugging.
 * One-line initialization in your Application class:
 *
 * ```kotlin
 * AiDebugBridge.init(this)
 * ```
 */
object AiDebugBridge {

    private const val TAG = "AiDebugBridge"

    @Volatile
    private var initialized = false

    private lateinit var application: Application
    private lateinit var server: BridgeServer
    private lateinit var discoveryEngine: DiscoveryEngine
    private lateinit var eventBus: EventBus

    /**
     * Initialize the debug bridge. Call once from Application.onCreate().
     *
     * @param application The Application instance
     * @param port HTTP/WS server port (default 8735)
     * @param hmacSecret Optional HMAC secret for authentication. If null, auth is disabled.
     */
    @JvmStatic
    @JvmOverloads
    fun init(
        application: Application,
        port: Int = 8735,
        hmacSecret: String? = null,
    ) {
        if (initialized) {
            Log.w(TAG, "AiDebugBridge already initialized — ignoring duplicate init()")
            return
        }

        this.application = application

        // Initialize subsystems
        eventBus = EventBus()
        discoveryEngine = DiscoveryEngine(application, eventBus)
        val auth = hmacSecret?.let { HmacAuth(it) }

        server = BridgeServer(
            port = port,
            discoveryEngine = discoveryEngine,
            eventBus = eventBus,
            auth = auth,
        )

        // Start lifecycle tracking and server
        discoveryEngine.start()
        server.start()

        initialized = true
        Log.i(TAG, "AI Debug Bridge started on port $port")
    }

    /**
     * Shut down the bridge server and stop all tracking.
     */
    @JvmStatic
    fun shutdown() {
        if (!initialized) return

        server.stop()
        discoveryEngine.stop()
        initialized = false
        Log.i(TAG, "AI Debug Bridge shut down")
    }

    /**
     * Check if the bridge is currently running.
     */
    @JvmStatic
    fun isRunning(): Boolean = initialized && server.isRunning()

    internal fun getApplication(): Application = application
    internal fun getDiscoveryEngine(): DiscoveryEngine = discoveryEngine
    internal fun getEventBus(): EventBus = eventBus
}
