package com.aidebugbridge.discovery

import android.util.Log
import android.view.View

/**
 * Maps the Jetpack Compose semantics tree for AI agent inspection.
 *
 * Uses reflection to access Compose internals since the library
 * declares compose-ui as compileOnly (optional dependency).
 * If Compose is not present in the host app, this gracefully returns empty data.
 */
class ComposeSemantics {

    companion object {
        private const val TAG = "ComposeSemantics"
        private var composeAvailable: Boolean? = null
    }

    /**
     * Attempt to extract the Compose semantics tree from the view hierarchy.
     * Returns an empty map if Compose is not available or not in use.
     */
    fun mapSemanticsTree(rootView: View): List<Map<String, Any>> {
        if (composeAvailable == false) return emptyList()

        return try {
            // Check if Compose is available via reflection
            val composeViewClass = Class.forName("androidx.compose.ui.platform.AbstractComposeView")
            composeAvailable = true

            val composeViews = findComposeViews(rootView, composeViewClass)
            composeViews.flatMap { composeView ->
                extractSemanticsFromComposeView(composeView)
            }
        } catch (e: ClassNotFoundException) {
            composeAvailable = false
            emptyList()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to map Compose semantics", e)
            emptyList()
        }
    }

    private fun findComposeViews(view: View, composeViewClass: Class<*>): List<View> {
        val results = mutableListOf<View>()

        if (composeViewClass.isInstance(view)) {
            results.add(view)
        }

        if (view is android.view.ViewGroup) {
            for (i in 0 until view.childCount) {
                results.addAll(findComposeViews(view.getChildAt(i), composeViewClass))
            }
        }

        return results
    }

    private fun extractSemanticsFromComposeView(composeView: View): List<Map<String, Any>> {
        // TODO: Use SemanticsOwner to traverse the semantics tree.
        // This requires reflection into:
        //   - AndroidComposeView.semanticsOwner
        //   - SemanticsOwner.rootSemanticsNode
        //   - SemanticsNode.config (SemanticsConfiguration)
        //
        // Each SemanticsNode exposes:
        //   - testTag, contentDescription, text, role
        //   - enabled, focused, selected states
        //   - onClick, onLongClick actions
        //   - bounds (position and size)
        //
        // For now, return basic info about the Compose view container.
        return listOf(
            mapOf(
                "type" to "ComposeView",
                "class" to composeView.javaClass.name,
                "bounds" to mapOf(
                    "width" to composeView.width,
                    "height" to composeView.height,
                ),
                "note" to "Full semantics tree extraction requires runtime reflection"
            )
        )
    }
}
