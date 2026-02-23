# Architecture Guide

Ekho Navigator follows the [official Android architecture guidance](https://developer.android.com/jetpack/guide) and draws direct inspiration from Google's [Now in Android](https://github.com/android/nowinandroid) (NIA) reference app. This document explains the layers, data flow, and key decisions behind our architecture.

> **Why NIA?** Now in Android is published by Google's Android team under the official [`android/`](https://github.com/android) GitHub organization. From their README:
>
> *"[Now in Android] follows Android design and development best practices and is intended to be a useful reference for developers."*
>
> It demonstrates the same patterns recommended in their official docs — unidirectional data flow, offline-first repositories, reactive streams, and multi-module structure. Its docs are written as "learning journeys" specifically for other developers to study and apply. We did exactly that: studied NIA's [Architecture Learning Journey](https://github.com/android/nowinandroid/blob/main/docs/ArchitectureLearningJourney.md) and [Modularization Learning Journey](https://github.com/android/nowinandroid/blob/main/docs/ModularizationLearningJourney.md), then adopted the patterns that fit our project's scope.


## Architecture Overview

The app has three layers, matching the [official guidance](https://developer.android.com/jetpack/guide):

```
┌─────────────────────────────────────┐
│            UI Layer                 │  Compose screens + ViewModels
├─────────────────────────────────────┤
│           Data Layer                │  Repositories, DAOs, data sources
├─────────────────────────────────────┤
│         Network / Storage           │  OkHttp, Room, WorkManager
└─────────────────────────────────────┘
```

**Key principles:**
- Higher layers react to changes in lower layers
- Events flow down (user taps → ViewModel → repository)
- Data flows up (database → repository → ViewModel → UI)
- All data is exposed as [Kotlin Flows](https://developer.android.com/kotlin/flow), never snapshots


## Data Flow: Campus Events

This is the complete pipeline from an iCal feed URL to pixels on screen.

```
EkhoNavigatorApplication.onCreate()        ← App starts
        │
        ▼
SyncInitializer                            ← Enqueues WorkManager jobs
        │
        ▼
CalendarSyncWorker                         ← Runs on background thread
        │
        ▼
DefaultCalendarRepository.sync()           ← Orchestrates the sync
        │
        ▼
ICalFeedDataSource.fetchEvents()           ← OkHttp fetch + iCal4j parse
        │
        ▼
NetworkCalendarEvent                       ← Network model (raw data)
        │
        ▼
Mappers.toEntity()                         ← Converts to DB model, preserves bookmarks
        │
        ▼
CalendarEventDao.upsertEvents()            ← Room writes to SQLite
        │
        ▼
Room Flow emissions                        ← Automatic — any active query re-emits
        │
        ▼
CalendarRepository.observe*()              ← Maps entities to domain models
        │
        ▼
ViewModel (combine / flatMapLatest)        ← Transforms to UI state
        │
        ▼
Composable (collectAsStateWithLifecycle)   ← Renders on screen
```

### How this compares to NIA

NIA follows the same pipeline for news resources:

| Step | NIA | Ekho |
|---|---|---|
| Trigger | `Sync.initialize` on app start | `SyncInitializer` on app start |
| Background work | `SyncWorker` via WorkManager | `CalendarSyncWorker` via WorkManager |
| Network fetch | `RetrofitNiaNetwork` (Retrofit + JSON) | `ICalFeedDataSource` (OkHttp + iCal4j) |
| Mapping | `NetworkNewsResource` → entity | `NetworkCalendarEvent` → entity |
| Local storage | Room (source of truth) | Room (source of truth) |
| Reactive reads | DAO returns `Flow<List<Entity>>` | DAO returns `Flow<List<Entity>>` |
| UI consumption | `stateIn` + `collectAsStateWithLifecycle` | `stateIn` + `collectAsStateWithLifecycle` |

The only real difference is the network layer: NIA talks to a REST API via Retrofit, while we parse an iCal (.ics) feed with iCal4j. Everything downstream is structurally identical.


## Data Layer

The data layer is offline-first: once data is fetched from the network, it's written to Room immediately. The UI never reads from the network directly — Room is the single source of truth.

### Repositories

Repositories are the **only public API** for accessing app data. They:
- Expose `Flow`-based read methods (reactive, not snapshot-based)
- Provide `suspend` functions for writes
- Orchestrate sync between network and local storage

```
CalendarRepository (interface)
    │
    └── DefaultCalendarRepository (implementation)
            │
            ├── ICalFeedDataSource    ← network reads
            └── CalendarEventDao      ← local reads/writes
```

### Model Separation

Data passes through three distinct model types as it moves through layers. This isolates each layer from changes in the others.

| Model | Lives in | Purpose |
|---|---|---|
| `NetworkCalendarEvent` | `core:network` | Raw shape of network response |
| `CalendarEventEntity` | `core:database` | Room table schema |
| `CalendarEvent` | `core:model` | Clean domain model consumed by UI |

Mappers sit in `core:data` and handle conversion: `NetworkCalendarEvent` → `CalendarEventEntity` → `CalendarEvent`.

### Data Sync

Sync is handled by WorkManager with two schedules:
- **Periodic:** Every 2 hours (keeps data fresh in background)
- **Immediate:** On app launch and manual pull-to-refresh

The sync process:
1. Fetch events from the iCal feed
2. Read existing bookmarks from the DB (so they survive the upsert)
3. Map network models to entities
4. Upsert all events
5. Delete stale events no longer in the feed (but preserve bookmarked ones)

This matches NIA's `OfflineFirstNewsRepository.syncWith` pattern — network data is reconciled with local storage, and the UI reacts automatically through Room's Flow emissions.


## UI Layer

The UI layer has two components:
- **ViewModels** transform repository Flows into UI state
- **Composables** render that state and send user actions back to the ViewModel

### Reactive State

ViewModels use Flow operators to build reactive pipelines:

```kotlin
// CalendarViewModel — re-queries events whenever the selected date changes
val eventsForSelectedDate = selectedDate.flatMapLatest { date ->
    repository.observeEventsByDateRange(date.startOfDay, date.endOfDay)
}

// EventsViewModel — combines three streams for filtering
val events = combine(allEvents, searchQuery, selectedCategory) { events, query, category ->
    events.filter { matchesQuery(it, query) && matchesCategory(it, category) }
}
```

This is the same pattern NIA uses: cold Flows from repositories, transformed with `combine`/`flatMapLatest`/`map`, converted to hot `StateFlow`s via `stateIn`.

### State Collection

Composables collect state with lifecycle awareness:

```kotlin
val events by viewModel.events.collectAsStateWithLifecycle()
```

This ensures the Flow is only active when the UI is visible — no wasted work when the app is in the background.


## Where We Diverge from NIA (and Why)

| NIA has... | We don't (yet) | Reasoning |
|---|---|---|
| `core:common` | — | Contains injectable dispatchers and a `Result` wrapper. We don't have enough cross-module utilities to justify a dedicated module yet. |
| `core:datastore` | — | NIA stores user prefs (theme, bookmarks as ID sets) in Proto DataStore. Our bookmarks live directly on the event entity in Room, which is simpler for our current scope. DataStore becomes valuable when we add app-wide preferences (theme, notification settings, onboarding state). |
| `core:domain` (use cases) | — | NIA uses these to combine data from multiple repositories (e.g., merging news with user data). We have one repository. When custom events or social features add more repositories, use cases will be the right abstraction. |
| `core:ui` | — | Shared composables that depend on domain models (e.g., `NewsResourceCard`). Our shared UI lives in `core:designsystem` for now. As features grow, splitting model-aware composables into `core:ui` would make sense. |

These aren't gaps — they're intentional scope decisions. NIA's own [Modularization Learning Journey](https://github.com/android/nowinandroid/blob/main/docs/ModularizationLearningJourney.md) recommends adding granularity as the codebase grows, not upfront:

> *"If your data layer is small, it's fine to keep it in a single module. But once the number of repositories and data sources starts to grow, it might be worth considering splitting them into separate modules."*


## Further Reading

- [Module Guide](ModuleGuide.md) — detailed breakdown of every module and its responsibilities
- [Testing Guide](Testing.md) — how to run tests, where they live, how to write new ones
- [Official Android Architecture Guide](https://developer.android.com/jetpack/guide)
- [NIA Architecture Learning Journey](https://github.com/android/nowinandroid/blob/main/docs/ArchitectureLearningJourney.md)
- [NIA Modularization Learning Journey](https://github.com/android/nowinandroid/blob/main/docs/ModularizationLearningJourney.md)
