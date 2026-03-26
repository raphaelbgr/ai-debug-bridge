package com.aidebugbridge.focus

import android.view.View
import android.view.ViewGroup

/**
 * Builds a graph of focusable views and their DPAD navigation relationships.
 * Critical for Android TV / Fire TV debugging where DPAD is the primary input.
 */
class FocusGraph {

    data class FocusNode(
        val id: String,
        val className: String,
        val bounds: Map<String, Int>,
        val isFocused: Boolean,
        val nextFocusUp: String?,
        val nextFocusDown: String?,
        val nextFocusLeft: String?,
        val nextFocusRight: String?,
        val contentDescription: String?,
    )

    /**
     * Build a complete focus graph from the view hierarchy.
     */
    fun buildFocusGraph(rootView: View): Map<String, Any> {
        val focusableViews = mutableListOf<FocusNode>()
        collectFocusableViews(rootView, focusableViews)

        val currentFocused = rootView.findFocus()

        return mapOf(
            "currentFocused" to (currentFocused?.let { getViewId(it) } ?: "none"),
            "focusableCount" to focusableViews.size,
            "views" to focusableViews.map { node ->
                mapOf(
                    "id" to node.id,
                    "class" to node.className,
                    "bounds" to node.bounds,
                    "focused" to node.isFocused,
                    "nextUp" to (node.nextFocusUp ?: "auto"),
                    "nextDown" to (node.nextFocusDown ?: "auto"),
                    "nextLeft" to (node.nextFocusLeft ?: "auto"),
                    "nextRight" to (node.nextFocusRight ?: "auto"),
                    "contentDescription" to (node.contentDescription ?: ""),
                )
            }
        )
    }

    private fun collectFocusableViews(view: View, list: MutableList<FocusNode>) {
        if (view.isFocusable && view.visibility == View.VISIBLE) {
            val location = IntArray(2)
            view.getLocationOnScreen(location)

            list.add(FocusNode(
                id = getViewId(view),
                className = view.javaClass.simpleName,
                bounds = mapOf(
                    "x" to location[0],
                    "y" to location[1],
                    "width" to view.width,
                    "height" to view.height,
                ),
                isFocused = view.isFocused,
                nextFocusUp = view.nextFocusUpId.takeIf { it != View.NO_ID }?.let { resolveId(view, it) },
                nextFocusDown = view.nextFocusDownId.takeIf { it != View.NO_ID }?.let { resolveId(view, it) },
                nextFocusLeft = view.nextFocusLeftId.takeIf { it != View.NO_ID }?.let { resolveId(view, it) },
                nextFocusRight = view.nextFocusRightId.takeIf { it != View.NO_ID }?.let { resolveId(view, it) },
                contentDescription = view.contentDescription?.toString(),
            ))
        }

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                collectFocusableViews(view.getChildAt(i), list)
            }
        }
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

    private fun resolveId(view: View, resId: Int): String {
        return try {
            view.resources.getResourceEntryName(resId)
        } catch (e: Exception) {
            "id/$resId"
        }
    }
}
