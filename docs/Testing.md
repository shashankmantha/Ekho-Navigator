# Testing Guide

For context on where test modules fit in the architecture, see the [Module Guide](ModuleGuide.md).


## Running Tests

**All unit tests:**
```bash
./gradlew testDebugUnitTest
```

**A single module:**
```bash
./gradlew :feature:events:testDebugUnitTest
```

**Android Studio:** Open any test file and click the green play button next to a test function or class name.

Tests also run automatically on every push and pull request via GitHub Actions.


## Where Tests Live

| Module | Type | Location | What's tested |
|---|---|---|---|
| `core/model/` | Unit | `src/test/` | Enum mapping, computed properties |
| `core/database/` | Instrumented | `src/androidTest/` | DAO queries, type converters |
| `core/network/` | Unit | `src/test/` | iCal response parsing |
| `core/data/` | Unit | `src/test/` | Repository sync logic |
| `feature/events/` | Unit | `src/test/` | List filtering, search, categories |
| `feature/event/` | Unit | `src/test/` | Detail loading, bookmark toggling |
| `feature/calendar/` | Unit | `src/test/` | Date range queries, month filtering |

**Unit tests** (`src/test/`) run on JVM — fast, no device needed.
**Instrumented tests** (`src/androidTest/`) run on a device/emulator — needed when the code depends on the Android runtime (e.g. Room's SQLite).


## The `core/testing` Module

A shared module that holds fakes, rules, and factory functions so feature tests don't reinvent them:

- **`MainDispatcherRule`** — swaps `Dispatchers.Main` with a test dispatcher so `viewModelScope` works on JVM
- **`TestCalendarRepository`** — fake backed by a `MutableSharedFlow` you can push data into
- **`testCalendarEvent()`** — factory function with defaults so tests only specify fields they care about


## Writing a New Test

### 1. Add dependencies

In the module's `build.gradle.kts`:

```kotlin
testImplementation(projects.core.testing)
testImplementation(libs.junit)
testImplementation(libs.kotlinx.coroutines.test)
testImplementation(libs.turbine)
```

### 2. Create the test file

```
feature/<name>/src/test/kotlin/com/ekhonavigator/feature/<name>/YourViewModelTest.kt
```

### 3. Use this skeleton

```kotlin
class YourViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: TestCalendarRepository
    private lateinit var viewModel: YourViewModel

    @Before
    fun setup() {
        repository = TestCalendarRepository()
        viewModel = YourViewModel(repository)
    }

    @Test
    fun `describe what you expect to happen`() = runTest {
        // Arrange
        val event = testCalendarEvent(id = "1", title = "Test")
        repository.emit(listOf(event))

        // Act
        viewModel.doSomething()

        // Assert
        val result = viewModel.state.first { it.isNotEmpty() }
        assertEquals("Test", result[0].title)
    }
}
```


## Creating Test Data

`testCalendarEvent()` builds a fake event — only specify what the test cares about:

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

We use `kotlin.test` assertions (note: message is the **last** parameter, unlike JUnit):

```kotlin
assertEquals("expected", actual)
assertTrue(list.isEmpty())
assertNull(result)
assertNotNull(result)
```


## Existing Tests (by complexity)

1. `core/model/.../EventCategoryTest.kt` — no coroutines, just input/output
2. `feature/event/.../EventDetailViewModelTest.kt` — ViewModel with one data stream
3. `feature/events/.../EventsViewModelTest.kt` — ViewModel with search + category filters
4. `core/database/.../CalendarEventDaoTest.kt` — instrumented DAO tests
