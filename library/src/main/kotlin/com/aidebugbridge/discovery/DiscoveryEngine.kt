package com.aidebugbridge.discovery

import android.app.Activity
import android.app.Application
import android.view.View
import com.aidebugbridge.events.EventBus

/**
 * Main orchestrator for app discovery. Coordinates all trackers
 * to build a complete picture of the running application.
 */
class DiscoveryEngine(
    private val application: Application,
    private val eventBus: EventBus,
) {
    val activityTracker = ActivityTracker(eventBus)
    val fragmentTracker = FragmentTracker(eventBus)
    val navGraphMapper = NavGraphMapper()
    val viewTreeMapper = ViewTreeMapper()
    val composeSemantics = ComposeSemantics()

    fun start() {
        application.registerActivityLifecycleCallbacks(activityTracker)
    }

    fun stop() {
        application.unregisterActivityLifecycleCallbacks(activityTracker)
    }

    /**
     * Get the currently resumed (foreground) Activity, if any.
     */
    fun getCurrentActivity(): Activity? = activityTracker.currentActivity

    /**
     * Build a complete app map: activities, fragments, nav graphs, view trees.
     */
    fun buildAppMap(): Map<String, Any> {
        val activity = getCurrentActivity()
        val map = mutableMapOf<String, Any>(
            "activities" to activityTracker.getActivityStack(),
            "currentActivity" to (activity?.javaClass?.name ?: "none"),
        )

        if (activity != null) {
            map["fragments"] = fragmentTracker.getFragments(activity)
            map["navGraph"] = navGraphMapper.mapNavGraph(activity)
            map["viewTree"] = viewTreeMapper.mapViewTree(activity.window.decorView)
            map["composeTree"] = composeSemantics.mapSemanticsTree(activity.window.decorView)
        }

        return map
    }

    /**
     * Get the current screen info: activity, visible fragments, view tree snapshot.
     */
    fun getCurrentScreen(): Map<String, Any> {
        val activity = getCurrentActivity() ?: return mapOf("error" to "No active activity")

        return mapOf(
            "activity" to activity.javaClass.name,
            "title" to (activity.title?.toString() ?: ""),
            "fragments" to fragmentTracker.getFragments(activity),
            "viewTree" to viewTreeMapper.mapViewTree(activity.window.decorView),
            "composeTree" to composeSemantics.mapSemanticsTree(activity.window.decorView),
        )
    }

    /**
     * Get the root view of the current activity.
     */
    fun getRootView(): View? = getCurrentActivity()?.window?.decorView
}
