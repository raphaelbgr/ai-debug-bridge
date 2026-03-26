package com.aidebugbridge.endpoints

import android.os.Debug
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*

/**
 * GET /memory — Returns memory usage information for the app process.
 * Includes Java heap, native heap, and overall memory stats.
 */
class MemoryEndpoint {

    suspend fun handle(call: ApplicationCall) {
        try {
            val runtime = Runtime.getRuntime()
            val memoryInfo = Debug.MemoryInfo()
            Debug.getMemoryInfo(memoryInfo)

            val info = mapOf(
                "java" to mapOf(
                    "heapUsed" to (runtime.totalMemory() - runtime.freeMemory()),
                    "heapMax" to runtime.maxMemory(),
                    "heapFree" to runtime.freeMemory(),
                    "heapTotal" to runtime.totalMemory(),
                ),
                "native" to mapOf(
                    "heapSize" to Debug.getNativeHeapSize(),
                    "heapAllocated" to Debug.getNativeHeapAllocatedSize(),
                    "heapFree" to Debug.getNativeHeapFreeSize(),
                ),
                "pss" to mapOf(
                    "total" to memoryInfo.totalPss,
                    "java" to memoryInfo.dalvikPss,
                    "native" to memoryInfo.nativePss,
                    "other" to memoryInfo.otherPss,
                ),
                "summary" to mapOf(
                    "javaHeapMB" to ((runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)),
                    "nativeHeapMB" to (Debug.getNativeHeapAllocatedSize() / (1024 * 1024)),
                    "totalPssMB" to (memoryInfo.totalPss / 1024),
                ),
            )

            call.respond(HttpStatusCode.OK, info)
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("error" to (e.message ?: "Memory info failed"))
            )
        }
    }
}
