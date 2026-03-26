package com.aidebugbridge.events

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Tracks parent-child event relationships to help AI agents understand
 * what caused what. For example: click event -> navigation event -> fragment created.
 *
 * Each event chain is assigned a unique trace ID, and child events
 * reference their parent event's ID.
 */
class CausalChain {

    data class ChainNode(
        val eventId: String,
        val eventType: String,
        val parentId: String?,
        val timestamp: Long,
        val data: Map<String, String>,
    )

    private val chains = ConcurrentHashMap<String, MutableList<ChainNode>>()
    private val activeTraceId = ThreadLocal<String?>()

    /**
     * Start a new causal chain. Returns the trace ID.
     */
    fun startChain(eventType: String, data: Map<String, String>): String {
        val traceId = UUID.randomUUID().toString()
        val eventId = UUID.randomUUID().toString()

        val node = ChainNode(
            eventId = eventId,
            eventType = eventType,
            parentId = null,
            timestamp = System.currentTimeMillis(),
            data = data,
        )

        chains[traceId] = mutableListOf(node)
        activeTraceId.set(traceId)
        return traceId
    }

    /**
     * Add a child event to the current active chain.
     */
    fun addToChain(eventType: String, data: Map<String, String>, parentEventId: String? = null): String? {
        val traceId = activeTraceId.get() ?: return null
        val chain = chains[traceId] ?: return null

        val eventId = UUID.randomUUID().toString()
        val parent = parentEventId ?: chain.lastOrNull()?.eventId

        chain.add(ChainNode(
            eventId = eventId,
            eventType = eventType,
            parentId = parent,
            timestamp = System.currentTimeMillis(),
            data = data,
        ))

        return eventId
    }

    /**
     * End the current active chain.
     */
    fun endChain() {
        activeTraceId.remove()
    }

    /**
     * Get a complete chain by trace ID.
     */
    fun getChain(traceId: String): List<ChainNode>? = chains[traceId]?.toList()

    /**
     * Get all recent chains.
     */
    fun getRecentChains(limit: Int = 20): Map<String, List<ChainNode>> {
        return chains.entries
            .sortedByDescending { it.value.firstOrNull()?.timestamp ?: 0 }
            .take(limit)
            .associate { it.key to it.value.toList() }
    }
}
