package com.aidebugbridge.discovery

import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView

/**
 * Maps the Android View hierarchy into a serializable tree structure.
 * Captures view types, IDs, bounds, visibility, text content, and
 * accessibility info for AI agent inspection.
 */
class ViewTreeMapper {

    /**
     * Recursively map the entire view tree starting from the given root.
     *
     * @param root The root view to start mapping from
     * @param maxDepth Maximum depth to traverse (prevents stack overflow on deep trees)
     * @return A nested map representing the view hierarchy
     */
    fun mapViewTree(root: View, maxDepth: Int = 30): Map<String, Any> {
        return mapView(root, depth = 0, maxDepth = maxDepth)
    }

    private fun mapView(view: View, depth: Int, maxDepth: Int): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "class" to view.javaClass.simpleName,
            "fullClass" to view.javaClass.name,
            "id" to getViewIdName(view),
            "visibility" to when (view.visibility) {
                View.VISIBLE -> "visible"
                View.INVISIBLE -> "invisible"
                View.GONE -> "gone"
                else -> "unknown"
            },
            "bounds" to mapOf(
                "left" to view.left,
                "top" to view.top,
                "right" to view.right,
                "bottom" to view.bottom,
                "width" to view.width,
                "height" to view.height,
            ),
            "enabled" to view.isEnabled,
            "focusable" to view.isFocusable,
            "focused" to view.isFocused,
            "clickable" to view.isClickable,
            "contentDescription" to (view.contentDescription?.toString() ?: ""),
        )

        // Extract text content from common view types
        when (view) {
            is TextView -> {
                map["text"] = view.text?.toString() ?: ""
                map["hint"] = view.hint?.toString() ?: ""
            }
            is EditText -> {
                map["text"] = view.text?.toString() ?: ""
                map["hint"] = view.hint?.toString() ?: ""
                map["inputType"] = view.inputType
            }
            is ImageView -> {
                map["imageDescription"] = view.contentDescription?.toString() ?: ""
            }
        }

        // Recursively map children
        if (view is ViewGroup && depth < maxDepth) {
            val children = mutableListOf<Map<String, Any>>()
            for (i in 0 until view.childCount) {
                children.add(mapView(view.getChildAt(i), depth + 1, maxDepth))
            }
            map["children"] = children
            map["childCount"] = view.childCount
        }

        return map
    }

    private fun getViewIdName(view: View): String {
        return if (view.id != View.NO_ID) {
            try {
                view.resources.getResourceEntryName(view.id)
            } catch (e: Exception) {
                "id/${view.id}"
            }
        } else {
            "no-id"
        }
    }
}
