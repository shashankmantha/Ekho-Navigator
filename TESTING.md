# Testing Guide

## How to Run Tests

**Terminal:**
```bash
./gradlew testDebugUnitTest
```

**Android Studio:** Open any test file and click the green play button in the left margin next to a test function or class name. Results appear in the Run panel at the bottom.

## How Our Code is Organized

Our app is split into layers. Each layer has a job, and tests go next to the code they test:

```
core/model/          Data classes (CalendarEvent, EventCategory, etc.)
core/database/       Database tables and queries (stores events locally)
core/data/           Repositories (connect database + network, provide data to UI)
core/network/        Network calls (fetches the .ics feed)
core/testing/        Shared test utilities (fakes, helpers - you don't test this, you use it)

feature/events/      Events list screen + ViewModel
feature/event/       Event detail screen + ViewModel
feature/calendar/    Calendar screen + ViewModel
```

**The rule: test files mirror source files.** If your code is in `feature/events/src/main/`, your tests go in `feature/events/src/test/`.

## Two Kinds of Tests

**Unit tests** (`src/test/`) run on your computer. No phone or emulator needed. Fast (seconds). Use these for ViewModels, data classes, and business logic.

**Instrumented tests** (`src/androidTest/`) run on a device or emulator. Slower. Use these only when the code needs Android to work, like database queries (Room needs Android's SQLite engine to run).

## Writing a New Test

### Step 1: Add dependencies

If the module doesn't have test dependencies yet, add these to its `build.gradle.kts`:

```kotlin
testImplementation(projects.core.testing)
testImplementation(libs.junit)
testImplementation(libs.kotlinx.coroutines.test)
testImplementation(libs.turbine)
```

### Step 2: Create the test file

Put it at: `feature/<name>/src/test/kotlin/com/ekhonavigator/feature/<name>/YourViewModelTest.kt`

### Step 3: Copy this skeleton and change the ViewModel class

```kotlin
package com.ekhonavigator.feature.yourfeature

import com.ekhonavigator.core.testing.MainDispatcherRule
import com.ekhonavigator.core.testing.TestCalendarRepository
import com.ekhonavigator.core.testing.testCalendarEvent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class YourViewModelTest {

    // Boilerplate - copy these 3 blocks into every ViewModel test as-is.
    // MainDispatcherRule makes coroutines work in tests.
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: TestCalendarRepository
    private lateinit var viewModel: YourViewModel  // <-- change this

    @Before
    fun setup() {
        repository = TestCalendarRepository()
        viewModel = YourViewModel(repository)      // <-- change this
    }

    // Write one @Test function per behavior you want to verify.
    // Name it as a sentence describing what should happen.
    @Test
    fun `describe what you expect to happen`() = runTest {
        // 1. ARRANGE - create fake data and feed it to the repository
        val event = testCalendarEvent(id = "1", title = "Test")
        repository.emit(listOf(event))

        // 2. ACT - call the ViewModel method you're testing
        viewModel.doSomething()

        // 3. ASSERT - check that the result is what you expected
        val result = viewModel.state.first { it.isNotEmpty() }
        assertEquals("Test", result[0].title)
    }
}
```

### Step 4: Run it

Click the green play button next to your test function, or run `./gradlew testDebugUnitTest` from the terminal.

## How to Create Test Data

`testCalendarEvent()` builds a fake event. Every field has a default value, so you only need to specify the fields your test cares about:

```kotlin
// Just need an event with an ID
val event = testCalendarEvent(id = "1")

// Need specific fields for this test
val alumniEvent = testCalendarEvent(
    id = "2",
    title = "Alumni Mixer",
    categories = listOf(EventCategory.ALUMNI),
)

// Need an event in the past to test filtering
val oldEvent = testCalendarEvent(
    id = "3",
    startTime = Instant.parse("2020-01-01T00:00:00Z"),
)
```

## How to Check Results

These are the assertion functions you'll use. If the check fails, the test fails and tells you what went wrong:

```kotlin
assertEquals("expected value", actualValue)     // are they equal?
assertTrue(list.isEmpty())                       // is this true?
assertNull(viewModel.event.value)                // is this null?
assertNotNull(result)                            // is this NOT null?
```

## Existing Tests to Learn From

Read these in order of complexity:

1. `core/model/src/test/kotlin/.../EventCategoryTest.kt` - simplest, no coroutines, just input/output checks
2. `feature/event/src/test/kotlin/.../EventDetailViewModelTest.kt` - ViewModel with one data stream
3. `feature/events/src/test/kotlin/.../EventsViewModelTest.kt` - ViewModel with search + category filters
4. `core/database/src/androidTest/kotlin/.../CalendarEventDaoTest.kt` - database query tests (instrumented)

## Adding Tests for a New Feature

If you're building a new feature (e.g. friends, maps), follow this checklist:

1. Add data classes to `core/model/` - write simple assertion tests in `core/model/src/test/`
2. Add database tables and queries to `core/database/` - write instrumented tests in `core/database/src/androidTest/`
3. Add a repository interface to `core/data/` - add a fake (`TestFriendsRepository`) to `core/testing/` by copying `TestCalendarRepository` and changing the interface
4. Add a ViewModel to `feature/<name>/` - write unit tests in `feature/<name>/src/test/` using the skeleton above

## CI (Continuous Integration)

Tests run automatically on every push and pull request via GitHub Actions. When you push code, GitHub spins up a server, runs `./gradlew testDebugUnitTest`, and shows a green checkmark or red X on your commit. Click the status icon on GitHub to see which tests passed or failed.
