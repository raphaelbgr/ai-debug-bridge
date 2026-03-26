package com.aidebugbridge

import com.aidebugbridge.events.EventBus
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for EventBus.
 */
class EventBusTest {

    private lateinit var eventBus: EventBus

    @Before
    fun setup() {
        eventBus = EventBus()
    }

    @Test
    fun `emit adds event to buffer`() {
        eventBus.emit("test.event", mapOf("key" to "value"))

        val events = eventBus.getRecentEvents()
        assertEquals(1, events.size)
        assertEquals("test.event", events[0].type)
        assertEquals("value", events[0].data["key"])
    }

    @Test
    fun `buffer respects max size`() {
        repeat(1500) { i ->
            eventBus.emit("test.event.$i", mapOf("index" to i.toString()))
        }

        val events = eventBus.getRecentEvents()
        assertTrue(events.size <= 1000)
    }

    @Test
    fun `clear removes all events`() {
        eventBus.emit("test.event", mapOf("key" to "value"))
        eventBus.clear()

        assertTrue(eventBus.getRecentEvents().isEmpty())
    }

    @Test
    fun `causal chain tracking`() {
        eventBus.emit("click", mapOf("view" to "button"), causedBy = null)
        eventBus.emit("navigate", mapOf("to" to "SecondActivity"), causedBy = "click")

        val events = eventBus.getRecentEvents()
        assertEquals(2, events.size)
        assertNull(events[0].causedBy)
        assertEquals("click", events[1].causedBy)
    }
}
