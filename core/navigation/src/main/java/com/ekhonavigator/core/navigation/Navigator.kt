/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ekhonavigator.core.navigation

import androidx.navigation3.runtime.NavKey

/**
 * Handles navigation events (forward and back) by updating the navigation state.
 *
 * @param state - The navigation state that will be updated in response to navigation events.
 */
class Navigator(val state: NavigationState) {

    /**
     * Navigate to a navigation key
     *
     * @param key - the navigation key to navigate to.
     */
    fun navigate(key: NavKey) {
        when (key) {
            in state.topLevelKeys -> {
                goToTopLevel(key)
                // Reset the root — a prior navigateAsTabSwitch may have parameterized it.
                resetSubStack(key)
            }
            else -> goToKey(key)
        }
    }

    fun navigateAsDetour(key: NavKey) = goToKey(key)

    fun navigateAsTabSwitch(key: NavKey) {
        val topLevelClass = state.topLevelKeys.firstOrNull { it::class == key::class }
            ?: error("navigateAsTabSwitch: ${key::class.simpleName} is not a top-level destination")
        state.subStacks.getValue(topLevelClass).apply {
            clear()
            add(key)
        }
        goToTopLevel(topLevelClass)
    }

    /**
     * Go back to the previous navigation key.
     */
    fun goBack() {
        when {
            state.currentKey == state.startKey -> error("You cannot go back from the start route")
            state.currentSubStack.size > 1 -> state.currentSubStack.removeLastOrNull()
            else -> state.topLevelStack.removeLastOrNull()
        }
    }

    /**
     * Go to a non-top level key.
     */
    private fun goToKey(key: NavKey) {
        state.currentSubStack.apply {
            // Remove it if it's already in the stack so it's added at the end.
            remove(key)
            add(key)
        }
    }

    /**
     * Go to a top level stack.
     */
    private fun goToTopLevel(key: NavKey) {
        state.topLevelStack.apply {
            if (key == state.startKey) {
                // This is the start key. Clear the stack so it's added as the only key.
                clear()
            } else {
                // Remove it if it's already in the stack so it's added at the end.
                remove(key)
            }
            add(key)
        }
    }

    private fun resetSubStack(rootKey: NavKey) {
        state.subStacks.getValue(rootKey).apply {
            clear()
            add(rootKey)
        }
    }
}
