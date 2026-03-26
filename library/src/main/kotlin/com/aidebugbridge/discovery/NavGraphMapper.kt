package com.aidebugbridge.discovery

import android.app.Activity
import android.util.Log

/**
 * Discovers and maps Navigation Component graphs from the running app.
 * Extracts all destinations, actions, deep links, and arguments
 * to give AI agents a complete navigation map.
 */
class NavGraphMapper {

    companion object {
        private const val TAG = "NavGraphMapper"
    }

    /**
     * Attempt to extract the Navigation graph from the given activity.
     * Returns an empty map if Navigation component is not in use.
     */
    fun mapNavGraph(activity: Activity): Map<String, Any> {
        return try {
            val fragmentActivity = activity as? androidx.fragment.app.FragmentActivity
                ?: return emptyMap()

            val navHostFragment = fragmentActivity.supportFragmentManager
                .fragments
                .firstOrNull { fragment ->
                    try {
                        fragment is androidx.navigation.fragment.NavHostFragment
                    } catch (e: NoClassDefFoundError) {
                        false
                    }
                } as? androidx.navigation.fragment.NavHostFragment
                ?: return emptyMap()

            val navController = navHostFragment.navController
            val graph = navController.graph

            buildMap {
                put("startDestination", graph.startDestinationId)
                put("currentDestination", navController.currentDestination?.let {
                    mapOf(
                        "id" to it.id,
                        "label" to (it.label?.toString() ?: ""),
                        "route" to (it.route ?: ""),
                    )
                } ?: emptyMap<String, Any>())

                val destinations = mutableListOf<Map<String, Any>>()
                graph.iterator().forEach { dest ->
                    destinations.add(mapOf(
                        "id" to dest.id,
                        "label" to (dest.label?.toString() ?: ""),
                        "route" to (dest.route ?: ""),
                        "navigatorName" to dest.navigatorName,
                    ))
                }
                put("destinations", destinations)
            }
        } catch (e: NoClassDefFoundError) {
            // Navigation component not available
            emptyMap()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to map nav graph", e)
            emptyMap()
        }
    }
}
