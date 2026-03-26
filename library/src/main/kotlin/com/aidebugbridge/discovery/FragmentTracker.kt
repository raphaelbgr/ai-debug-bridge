package com.aidebugbridge.discovery

import android.app.Activity
import com.aidebugbridge.events.EventBus

/**
 * Tracks Fragment lifecycle across all activities.
 * Uses FragmentManager callbacks to detect fragment additions,
 * removals, and visibility changes.
 */
class FragmentTracker(
    private val eventBus: EventBus,
) {

    /**
     * Get all fragments currently attached to the given activity.
     * Works with both AndroidX FragmentActivity and legacy fragments.
     */
    fun getFragments(activity: Activity): List<Map<String, Any>> {
        val fragments = mutableListOf<Map<String, Any>>()

        // Try AndroidX FragmentActivity
        try {
            val fragmentActivity = activity as? androidx.fragment.app.FragmentActivity
            fragmentActivity?.supportFragmentManager?.fragments?.forEach { fragment ->
                fragments.add(mapFragment(fragment))
                // Recursively map child fragments
                fragment.childFragmentManager.fragments.forEach { child ->
                    fragments.add(mapFragment(child, parentName = fragment.javaClass.simpleName))
                }
            }
        } catch (e: NoClassDefFoundError) {
            // AndroidX fragments not available
        }

        return fragments
    }

    private fun mapFragment(
        fragment: androidx.fragment.app.Fragment,
        parentName: String? = null,
    ): Map<String, Any> {
        return buildMap {
            put("name", fragment.javaClass.name)
            put("simpleName", fragment.javaClass.simpleName)
            put("tag", fragment.tag ?: "")
            put("isVisible", fragment.isVisible)
            put("isAdded", fragment.isAdded)
            put("isDetached", fragment.isDetached)
            put("isHidden", fragment.isHidden)
            put("id", fragment.id)
            parentName?.let { put("parent", it) }
            fragment.arguments?.let { args ->
                put("arguments", args.keySet().associateWith { key ->
                    args.get(key)?.toString() ?: "null"
                })
            }
        }
    }
}
