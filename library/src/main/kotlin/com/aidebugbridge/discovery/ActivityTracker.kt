package com.aidebugbridge.discovery

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import com.aidebugbridge.events.EventBus
import java.lang.ref.WeakReference

/**
 * Tracks Activity lifecycle events across the entire application.
 * Maintains a stack of active activities and emits lifecycle events
 * to the EventBus for real-time streaming to AI agents.
 */
class ActivityTracker(
    private val eventBus: EventBus,
) : ActivityLifecycleCallbacks {

    private val activityStack = mutableListOf<WeakReference<Activity>>()

    @Volatile
    var currentActivity: Activity? = null
        private set

    fun getActivityStack(): List<Map<String, Any>> {
        return activityStack.mapNotNull { ref ->
            ref.get()?.let { activity ->
                mapOf(
                    "name" to activity.javaClass.name,
                    "simpleName" to activity.javaClass.simpleName,
                    "hashCode" to activity.hashCode().toString(),
                    "isFinishing" to activity.isFinishing,
                    "title" to (activity.title?.toString() ?: ""),
                )
            }
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        activityStack.add(WeakReference(activity))
        eventBus.emit("activity.created", mapOf(
            "activity" to activity.javaClass.name,
            "hasState" to (savedInstanceState != null).toString(),
        ))
    }

    override fun onActivityStarted(activity: Activity) {
        eventBus.emit("activity.started", mapOf("activity" to activity.javaClass.name))
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
        eventBus.emit("activity.resumed", mapOf("activity" to activity.javaClass.name))
    }

    override fun onActivityPaused(activity: Activity) {
        if (currentActivity === activity) {
            currentActivity = null
        }
        eventBus.emit("activity.paused", mapOf("activity" to activity.javaClass.name))
    }

    override fun onActivityStopped(activity: Activity) {
        eventBus.emit("activity.stopped", mapOf("activity" to activity.javaClass.name))
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        eventBus.emit("activity.saveState", mapOf("activity" to activity.javaClass.name))
    }

    override fun onActivityDestroyed(activity: Activity) {
        activityStack.removeAll { it.get() === activity || it.get() == null }
        eventBus.emit("activity.destroyed", mapOf("activity" to activity.javaClass.name))
    }
}
