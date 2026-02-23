# Testing Guide

## How to Run Tests

**Terminal:**
```bash
./gradlew testDebugUnitTest
```

**Android Studio:** Open any test file and click the green play button next to a test function or class name.

## Where Tests Live

Tests are split into two sides matching our architecture: **core** (data layer) and **feature** (UI layer). Each side has different test types and patterns.

### Core Tests

Core modules contain the data pipeline — models, database, network, and repositories. Tests here verify that data is shaped, stored, and fetched correctly.

| Module | Test type | Location | What's tested |
|---|---|---|---|
| `core/model/` | Unit tests | `src/test/` | Data class logic, enum mapping, computed properties |
| `core/database/` | Instrumented tests | `src/androidTest/` | DAO queries, type converters (needs Android's SQLite) |
| `core/network/` | Unit tests | `src/test/` | Response parsing, error handling |
| `core/data/` | Unit tests | `src/test/` | Repository logic (sync orchestration, caching) |

**Unit tests** (`src/test/`) run on your machine — fast, no device needed.
**Instrumented tests** (`src/androidTest/`) run on a device/emulator — needed when the code depends on the Android runtime (e.g. Room's SQLite engine).

### Feature Tests

Feature modules contain ViewModels and UI logic. Tests here verify that the ViewModel transforms repository data into the correct UI state.

| Module | Test type | Location | What's tested |
|---|---|---|---|
| `feature/events/` | Unit tests | `src/test/` | List filtering, search, category filters |
| `feature/event/` | Unit tests | `src/test/` | Detail loading, bookmark toggling |
| `feature/calendar/` | Unit tests | `src/test/` | Date range queries, month filtering |

Feature tests use fakes from `core/testing/` instead of real repositories, which keeps them fast and deterministic.

## The `core/testing` Module

`core/testing/` is a **convenience module** — it holds shared fakes, rules, and factory functions so every feature test doesn't have to reinvent them. It's a dependency of feature test suites, not something that gets tested itself.

**What's in it now:**
- **`MainDispatcherRule`** — swaps `Dispatchers.Main` with a test dispatcher so `viewModelScope` works in JVM tests
- **`TestCalendarRepository`** — fake `CalendarRepository` backed by a `MutableSharedFlow` you can push data into
- **`testCalendarEvent()`** — factory function with sensible defaults so tests only specify the fields they care about

### When to Expand It

Every time a new data domain is added, `core/testing/` grows to match. For example, if we build the **social tab**, the new classes across the project might look like:

```
core/model/          + SocialProfile, FriendRequest         (data classes)
core/database/       + SocialDao, social_profile table       (Room queries)
core/network/        + SocialApiService                      (auth + API calls)
core/data/           + SocialRepository                      (ties it together)

core/testing/        + TestSocialRepository                  (fake for VM tests)
                     + testSocialProfile()                   (factory function)

feature/social/      + SocialViewModel                       (UI logic)
feature/social/test/ + SocialViewModelTest                   (uses the above fakes)
```

The pattern: each new repository interface in `core/data/` gets a corresponding fake in `core/testing/`, and each new data class that shows up in tests gets a factory function. Feature tests then depend on `core/testing` and get everything they need.

## Writing a New Test

### 1. Add dependencies

If the module doesn't have test dependencies yet, add these to its `build.gradle.kts`:

```kotlin
testImplementation(projects.core.testing)
testImplementation(libs.junit)
testImplementation(libs.kotlinx.coroutines.test)
testImplementation(libs.turbine)
```

### 2. Create the test file

Put it at: `feature/<name>/src/test/kotlin/com/ekhonavigator/feature/<name>/YourViewModelTest.kt`

### 3. Use this skeleton

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

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: TestCalendarRepository
    private lateinit var viewModel: YourViewModel  // <-- change this

    @Before
    fun setup() {
        repository = TestCalendarRepository()
        viewModel = YourViewModel(repository)      // <-- change this
    }

    @Test
    fun `describe what you expect to happen`() = runTest {
        // 1. ARRANGE - create fake data and push it to the repository
        val event = testCalendarEvent(id = "1", title = "Test")
        repository.emit(listOf(event))

        // 2. ACT - call the ViewModel method being tested
        viewModel.doSomething()

        // 3. ASSERT - check the result
        val result = viewModel.state.first { it.isNotEmpty() }
        assertEquals("Test", result[0].title)
    }
}
```

## Creating Test Data

`testCalendarEvent()` builds a fake event with defaults — only specify what the test cares about:

```kotlin
val event = testCalendarEvent(id = "1")

val alumniEvent = testCalendarEvent(
    id = "2",
    title = "Alumni Mixer",
    categories = listOf(EventCategory.ALUMNI),
)

val oldEvent = testCalendarEvent(
    id = "3",
    startTime = Instant.parse("2020-01-01T00:00:00Z"),
)
```

## Assertions

```kotlin
assertEquals("expected", actual)    // are they equal?
assertTrue(list.isEmpty())          // is this true?
assertNull(result)                  // is this null?
assertNotNull(result)               // is this NOT null?
```

## Existing Tests

Ordered by complexity:

1. `core/model/src/test/.../EventCategoryTest.kt` — no coroutines, just input/output
2. `feature/event/src/test/.../EventDetailViewModelTest.kt` — ViewModel with one data stream
3. `feature/events/src/test/.../EventsViewModelTest.kt` — ViewModel with search + category filters
4. `core/database/src/androidTest/.../CalendarEventDaoTest.kt` — instrumented DAO tests

## CI (Continuous Integration)

Tests run automatically on every push and pull request via GitHub Actions. Check the status icon on your commit to see results.
