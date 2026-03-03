# Architecture Guide

Ekho Navigator follows the [official Android architecture guidance](https://developer.android.com/jetpack/guide) and draws direct inspiration from Google's [Now in Android](https://github.com/android/nowinandroid) (NIA) reference app.

> **Why NIA?** It's published by Google's Android team as a reference for "Android design and development best practices." We studied NIA's [Architecture Learning Journey](https://github.com/android/nowinandroid/blob/main/docs/ArchitectureLearningJourney.md) and [Modularization Learning Journey](https://github.com/android/nowinandroid/blob/main/docs/ModularizationLearningJourney.md), then adopted the patterns that fit our scope.


## Dependency Graph

```
core:model          ← foundation, depends on NOTHING
  ↑
  ├── core:database ← depends on core:model
  ├── core:network  ← depends on core:model
  │
  └── core:data     ← depends on core:model, core:database, core:network
        ↑
        └── feature:calendar, feature:event, etc.

core:navigation     ← island, depends on Compose only
core:designsystem   ← island, depends on Compose only
core:testing        ← depends on core:model, core:data (test-only)
```

**Dependency rules** (same as NIA):
- `app` depends on all `feature` modules + required `core` modules
- `feature` modules depend on `core` modules, never on each other
- `core` modules can depend on other `core` modules, never on `feature` or `app`
- `core:model` depends on nothing — pure Kotlin


## Core Modules

### `core:model` — The Vocabulary
**Depends on:** nothing · **Depended on by:** everything

Pure Kotlin data classes and enums. No Android imports, no frameworks. Defines what a "calendar event" *is* across the whole app. `CalendarEvent`, `EventCategory` — these are the domain language every other module speaks. If you ever see `import android.*` in here, something's wrong.

**Main file:** `CalendarEvent.kt` — the domain data class every layer consumes.

### `core:database` — The Local Store
**Depends on:** `core:model` · **Depended on by:** `core:data` only

Room (SQLite ORM) layer. Owns the database, DAO, entity class, and type converters. Its job: persist `CalendarEventEntity` rows and emit reactive `Flow`s when they change. It has no idea where the data came from — network, user input, doesn't matter.

**Main file:** `CalendarEventDao.kt` — declares reactive SQL queries. Room generates the implementation at compile time.

### `core:network` — The Remote Parser
**Depends on:** `core:model` · **Depended on by:** `core:data` only

OkHttp + iCal4j layer. Its job: given a URL, fetch raw bytes and return typed Kotlin objects (`NetworkCalendarEvent`). It has no idea where the data goes — database, UI, doesn't matter. All parsing complexity (VTIMEZONE workarounds, HTML entity decoding, Temporal type conversion) is contained here.

**Main file:** `ICalFeedDataSource.kt` — fetches + parses the ICS feed into network DTOs.

### `core:data` — The Orchestrator
**Depends on:** `core:model`, `core:database`, `core:network` · **Depended on by:** features, `core:testing`

The **only** module that sees both `core:database` and `core:network`. Owns the repository interface (the contract upstream code depends on), the sync logic, the mappers (network→entity translation), and WorkManager scheduling. This is where **policy** lives: sync every 2 hours, preserve bookmarks during sync, delete stale events.

**Main file:** `DefaultCalendarRepository.kt` — orchestrates network→database sync and exposes domain Flows.

```
          ┌──────────────┐
          │  core:data   │  ← the only module that sees both
          │  (repository)│
          └──┬───────┬───┘
             │       │
     uses    │       │   uses
             ▼       ▼
   ┌──────────┐   ┌──────────┐
   │core:     │   │core:     │
   │database  │   │network   │
   │(Room/SQL)│   │(HTTP/ICS)│
   └──────────┘   └──────────┘
         │              │
         │  both translate to/from
         ▼              ▼
      ┌────────────────────┐
      │    core:model      │
      └────────────────────┘
```

`core:database` and `core:network` are **peers** — same level, both depend on `core:model`, neither knows about the other.

### `core:navigation` — The Router
**Depends on:** Compose, Navigation3 · **Depended on by:** app, features

Manages tab-based back stacks. Adapted from AOSP's Navigation3 sample. No business logic — you could swap the calendar for a weather app and this module wouldn't change.

### `core:designsystem` — The Look
**Depends on:** Compose Material3 · **Depended on by:** features

Theme (colors, typography), reusable composables (top bar, navigation bar, async images), and icon definitions. Uses `api()` dependencies so feature modules get Compose/Material3 transitively.

### `core:testing` — The Toolbox
**Depends on:** `core:model`, `core:data` · **Depended on by:** feature test source sets

Not shipped in the APK. Provides `TestCalendarRepository` (fake with `MutableSharedFlow`), `testCalendarEvent()` (factory with defaults), and `MainDispatcherRule` (swaps `Dispatchers.Main` for JVM tests). Uses `api()` so test modules inherit JUnit, Turbine, and coroutines-test.

See the [Testing Guide](Testing.md) for how tests use this module.


## Data Flow: Campus Events

The complete pipeline from an iCal feed URL to pixels on screen:

```
App.onCreate()
  → SyncInitializer enqueues WorkManager jobs
    → CalendarSyncWorker runs on background thread
      → DefaultCalendarRepository.sync()
        → ICalFeedDataSource.fetchEvents()     ← OkHttp + iCal4j
        → NetworkCalendarEvent                 ← network DTO
        → Mappers.toEntity()                   ← preserves bookmarks
        → CalendarEventDao.upsertEvents()      ← Room writes to SQLite
  → Room Flow re-emits automatically
    → CalendarRepository.observe*()            ← maps entities → domain models
      → ViewModel (flatMapLatest / combine)    ← transforms to UI state
        → Composable (collectAsStateWithLifecycle) ← renders
```

### Model Separation

Data passes through three distinct types as it moves through layers:

| Model | Module | Purpose |
|---|---|---|
| `NetworkCalendarEvent` | `core:network` | Raw iCal shape — uses iCal vocabulary (`summary`, `dtStart`) |
| `CalendarEventEntity` | `core:database` | Room table schema — has DB concerns (`@PrimaryKey`, `isBookmarked`) |
| `CalendarEvent` | `core:model` | Clean domain model — uses app vocabulary (`title`, `startTime`) |

Mappers in `core:data` handle conversion between them.

### Sync Strategy

WorkManager runs on two schedules:
- **Periodic:** Every 2 hours (background freshness)
- **Immediate:** On app launch + pull-to-refresh

The sync process:
1. Fetch events from the iCal feed
2. Look up existing bookmarks (so they survive the upsert)
3. Map network → entity
4. Upsert all events
5. Delete stale events no longer in feed (preserve bookmarked ones)


## Key Patterns

**Offline-first.** Room is the single source of truth. The UI never reads from the network directly.

**`Flow` for reads, `suspend` for writes.** Every read operation returns a reactive `Flow`. Every write operation is a `suspend` function. This split is consistent from DAO to ViewModel.

**Interface-driven boundaries.** ViewModels depend on `CalendarRepository` (interface), not `DefaultCalendarRepository`. Tests swap in `TestCalendarRepository`. Same interface, different wiring.

**Module boundaries enforce access.** `internal` classes in database/network can't leak. Feature modules can't import Room or OkHttp directly — there's no Gradle dependency path.


## Where We Diverge from NIA

| NIA has... | We don't (yet) | Trigger to add |
|---|---|---|
| `core:domain` (use cases) | — | When a second data source (e.g. Firestore user events) needs combining with campus events |
| `core:datastore` | — | When we add app-wide preferences (theme, notifications, onboarding) |
| `core:common` | — | When injectable dispatchers or cross-module utilities are needed in 3+ modules |
| `core:ui` | — | When model-aware shared composables outgrow `core:designsystem` |
| `feature:*/api` + `impl` split | — | When feature-to-feature navigation creates unwanted coupling at scale |

These are intentional scope decisions, not gaps. NIA's own modularization docs recommend adding granularity as the codebase grows, not upfront.


## Further Reading

- [Module Guide](ModuleGuide.md) — expansion patterns for adding new features and data sources
- [Testing Guide](Testing.md) — how to run tests, where they live, how to write new ones
- [Official Android Architecture Guide](https://developer.android.com/jetpack/guide)
- [NIA Architecture Learning Journey](https://github.com/android/nowinandroid/blob/main/docs/ArchitectureLearningJourney.md)
