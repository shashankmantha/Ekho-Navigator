package com.ekhonavigator.core.testing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * JUnit 4 rule that swaps [Dispatchers.Main] for a [TestDispatcher].
 *
 * ## Why this exists
 *
 * ViewModels use `viewModelScope`, which dispatches on [Dispatchers.Main].
 * On a real device, Main = the Android UI thread. In a JVM unit test there
 * IS no Android UI thread, so `viewModelScope.launch { … }` would crash
 * with "Module with the Main dispatcher had failed to initialize".
 *
 * This rule replaces Main with an [UnconfinedTestDispatcher] before each
 * test and restores it after. "Unconfined" means coroutines run eagerly
 * (no waiting for dispatch), which makes tests simpler and deterministic.
 *
 * ## Usage
 *
 * ```kotlin
 * class MyViewModelTest {
 *     @get:Rule
 *     val mainDispatcherRule = MainDispatcherRule()
 *
 *     @Test
 *     fun myTest() = runTest {
 *         // viewModelScope.launch { } works here now
 *     }
 * }
 * ```
 *
 * ## How TestWatcher works
 *
 * [TestWatcher] is a JUnit 4 class with `starting()` / `finished()` hooks.
 * JUnit calls `starting()` before each `@Test` method and `finished()` after,
 * even if the test fails — so Main always gets cleaned up properly.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {

    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
