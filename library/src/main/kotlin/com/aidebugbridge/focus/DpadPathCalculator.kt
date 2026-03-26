package com.aidebugbridge.focus

import android.view.View
import android.view.ViewGroup

/**
 * Calculates the shortest DPAD key sequence to navigate from
 * one focusable view to another. Essential for AI agents
 * controlling Android TV / Fire TV apps via remote control.
 */
class DpadPathCalculator {

    enum class DpadKey {
        UP, DOWN, LEFT, RIGHT, CENTER
    }

    data class PathResult(
        val from: String,
        val to: String,
        val path: List<DpadKey>,
        val steps: Int,
        val reachable: Boolean,
    )

    /**
     * Calculate the shortest DPAD path from the currently focused view
     * to the target view.
     *
     * Uses BFS on the focus graph to find the optimal key sequence.
     *
     * @param rootView Root of the view hierarchy
     * @param targetViewId Resource name or description of the target view
     * @return PathResult with the sequence of DPAD keys, or unreachable flag
     */
    fun calculatePath(rootView: View, targetViewId: String): PathResult {
        val currentFocused = rootView.findFocus()
            ?: return PathResult("none", targetViewId, emptyList(), 0, false)

        val fromId = getViewId(currentFocused)

        if (fromId == targetViewId) {
            return PathResult(fromId, targetViewId, emptyList(), 0, true)
        }

        // BFS through focus graph
        data class State(val view: View, val path: List<DpadKey>)

        val visited = mutableSetOf<Int>() // view hashcodes
        val queue = ArrayDeque<State>()
        queue.add(State(currentFocused, emptyList()))
        visited.add(currentFocused.hashCode())

        while (queue.isNotEmpty()) {
            val (view, path) = queue.removeFirst()

            // Try all four directions
            for ((key, nextView) in getNeighbors(view)) {
                if (nextView == null || nextView.hashCode() in visited) continue
                visited.add(nextView.hashCode())

                val newPath = path + key
                val nextId = getViewId(nextView)

                if (nextId == targetViewId) {
                    return PathResult(fromId, targetViewId, newPath, newPath.size, true)
                }

                if (newPath.size < 50) { // Max depth guard
                    queue.add(State(nextView, newPath))
                }
            }
        }

        return PathResult(fromId, targetViewId, emptyList(), 0, false)
    }

    private fun getNeighbors(view: View): List<Pair<DpadKey, View?>> {
        return listOf(
            DpadKey.UP to view.focusSearch(View.FOCUS_UP),
            DpadKey.DOWN to view.focusSearch(View.FOCUS_DOWN),
            DpadKey.LEFT to view.focusSearch(View.FOCUS_LEFT),
            DpadKey.RIGHT to view.focusSearch(View.FOCUS_RIGHT),
        )
    }

    private fun getViewId(view: View): String {
        return if (view.id != View.NO_ID) {
            try {
                view.resources.getResourceEntryName(view.id)
            } catch (e: Exception) {
                "id/${view.id}"
            }
        } else {
            "view@${view.hashCode()}"
        }
    }
}
