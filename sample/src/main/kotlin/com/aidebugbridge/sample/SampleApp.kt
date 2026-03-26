package com.aidebugbridge.sample

import android.app.Application

/**
 * Sample Application that initializes AI Debug Bridge.
 *
 * The bridge is only available in debug builds because
 * the :library module is included as debugImplementation.
 */
class SampleApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize AI Debug Bridge — one line, zero config.
        // Only runs in debug builds (library is debugImplementation).
        try {
            val bridgeClass = Class.forName("com.aidebugbridge.AiDebugBridge")
            val initMethod = bridgeClass.getMethod("init", Application::class.java, Int::class.javaPrimitiveType)
            initMethod.invoke(null, this, 8735)
        } catch (e: ClassNotFoundException) {
            // Release build — bridge not available, which is expected
        }
    }
}
