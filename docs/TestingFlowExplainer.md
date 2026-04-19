# How Our Testing Actually Works

A layer-by-layer breakdown of the test pipeline — from CI trigger down to why `@Test` actually does anything.

## 1. CI trigger — `.github/workflows/test.yml`

GitHub Actions just: checks out repo, installs JDK 17, installs Gradle, runs `./gradlew testDebugUnitTest`. That's it. GitHub knows nothing about tests.

## 2. `testDebugUnitTest` — who creates it?

We didn't. The **Android Gradle Plugin** generates it the moment a module applies `com.android.library`. AGP scans `src/test/` per build variant and creates `testDebugUnitTest`, `testReleaseUnitTest`, etc.

Under the hood it extends Gradle's built-in `Test` task (from the core `java` plugin). That task forks a JVM, builds the classpath, and runs whatever test framework it detects.

## 3. How Gradle finds JUnit

Gradle's `Test` task scans the test classpath. It sees `junit-4.13.2.jar` (added via `testImplementation(libs.junit)`) and switches into JUnit 4 mode. No config — the JAR being there is the signal.

## 4. How `@Test` works

JUnit is just a library. `@Test` / `@Before` / `@Rule` are plain annotations. At runtime JUnit reflects over your class, finds `@Test` methods, instantiates, runs `@Before` → `@Test` → `@After`. Exception = fail, no exception = pass. That's the whole contract.

## 5. What we lifted from Now in Android

JUnit alone can't handle coroutines or ViewModels. The scaffolding in `core:testing` is cloned/adapted from NIA:

- **MainDispatcherRule** — swaps `Dispatchers.Main` for a `TestDispatcher` so `viewModelScope.launch` doesn't crash in JVM tests (no Android main looper exists)
- **Test fake repositories** — in-memory impls of our repository interfaces so tests don't touch Firebase/Room
- **TestData** — shared fixtures
- Plus third-party: `kotlinx-coroutines-test` (`runTest`), Turbine (Flow assertions), `kotlin-test` (`assertEquals`)

### Why fake repositories matter

Our ViewModels depend on repository *interfaces* (`CalendarRepository`, `AuthRepository`, etc.), not concrete impls. In production those are backed by Firestore, Room, and network calls. In a unit test we can't — and don't want to — actually talk to Firestore:

- **Speed** — a real Firestore round-trip is hundreds of ms; a unit test suite would crawl
- **Determinism** — network flakes would turn CI red for no code reason
- **Control** — we need to put *specific* data in the repo to assert the ViewModel reacts correctly (e.g. "when this event has no location, hide the map button"). With a real repo you can't easily force a given state
- **Isolation** — a ViewModel test should fail when the ViewModel breaks, not when a Firestore query rule changes

The fakes (`TestCalendarRepository`, etc.) implement the same interface with a `MutableStateFlow` backing an in-memory list. Tests push data in (`fake.sendEvents(listOf(...))`), construct the ViewModel with the fake, and assert on the ViewModel's emitted state. Swapping the real impl for the fake is the *whole reason* we program against interfaces + inject dependencies in the first place.

## Who owns what

| Layer | Source |
|---|---|
| CI runner, JDK, Gradle | GitHub Actions |
| Generic `Test` task | Gradle core |
| `testDebugUnitTest` wiring | Android Gradle Plugin |
| `@Test` annotations + runner | JUnit 4 JAR |
| Framework auto-detection | Gradle's `Test` task |
| Coroutine/Flow helpers | kotlinx-coroutines-test, Turbine |
| `MainDispatcherRule`, fake repos | Now in Android (cloned into `core:testing`) |
| Per-module `testImplementation` lines | Us, in each `build.gradle.kts` |

Small but load-bearing — without the NIA rule + fakes, none of the ViewModel tests would even start.
