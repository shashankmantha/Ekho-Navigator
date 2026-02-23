# Module Guide

This document explains how Ekho Navigator is modularized, what each module does, and how our structure maps to Google's [Now in Android](https://github.com/android/nowinandroid) (NIA) reference architecture.

For the theory behind Android modularization, see the [official guidance](https://developer.android.com/topic/modularization) and NIA's [Modularization Learning Journey](https://github.com/android/nowinandroid/blob/main/docs/ModularizationLearningJourney.md).


## Module Map

```
┌──────────────────────────────────────────────────────────────┐
│  app                                                         │
│  Scaffolding, navigation, Application class                  │
│  Depends on all feature modules + required core modules      │
└────────┬────────────────────────────────┬────────────────────┘
         │                                │
    ┌────▼──────────┐            ┌────────▼────────┐
    │  feature:*    │            │  feature:*       │
    │  calendar     │            │  events, event   │
    │  home, map    │            │  social          │
    │  Screen + VM  │            │  Screen + VM     │
    └────┬──────────┘            └────────┬─────────┘
         │         depends on             │
    ┌────▼────────────────────────────────▼─────────┐
    │  core:data                                     │
    │  Repositories, mappers, sync                   │
    ├────────────┬─────────────────┬────────────────┤
    │ core:network│  core:database  │  core:model    │
    │ OkHttp,     │  Room DB, DAOs, │  Domain data   │
    │ iCal4j,     │  entities,      │  classes       │
    │ net models  │  converters     │  (pure Kotlin) │
    └────────────┴─────────────────┴────────────────┘

    core:designsystem    Theme, icons, shared UI components
    core:navigation      Navigation keys and routes
    core:testing         Test fakes, rules, factories
```

### Structural Simplifications from NIA

NIA splits each feature into `api` and `impl` submodules (e.g. `feature:topic:api`, `feature:topic:impl`) so that features can reference each other's navigation keys without depending on the full implementation. It also includes several auxiliary modules (`app-nia-catalog`, `benchmarks`, `ui-test-hilt-manifest`) that support its role as a showcase project.

We intentionally flattened this. Each of our features is a single module (e.g. `feature:calendar`, not `feature:calendar:api` + `feature:calendar:impl`). The `api`/`impl` split solves a build-time isolation problem that matters at scale — with 5 developers and 6 features, the added Gradle complexity isn't worth the marginal build speed gain. If the project grows to the point where feature-to-feature navigation creates unwanted coupling, the split is straightforward to introduce later.

### Dependency Rules

These match NIA's rules exactly:
- **`app`** depends on all `feature` modules and required `core` modules
- **`feature`** modules depend on `core` modules, never on each other
- **`core`** modules can depend on other `core` modules, never on `feature` or `app`
- **`core:model`** has no dependencies — it's pure Kotlin data classes


## Module Breakdown

### `app`

| | |
|---|---|
| **Role** | Entry point. Wires everything together. |
| **Contains** | `EkhoNavigatorApplication`, `MainActivity`, top-level navigation (`NavHost`, bottom bar) |
| **NIA equivalent** | `app` — same role: `NiaApp`, `MainActivity`, `NiaNavHost` |

### `feature:calendar`

| | |
|---|---|
| **Role** | Month-view calendar with event indicators and day-detail list |
| **Contains** | `CalendarScreen`, `CalendarViewModel` |
| **Depends on** | `core:data`, `core:model`, `core:designsystem` |

### `feature:events`

| | |
|---|---|
| **Role** | Searchable, filterable list of upcoming campus events |
| **Contains** | `EventsScreen`, `EventsViewModel` |
| **Depends on** | `core:data`, `core:model`, `core:designsystem` |

### `feature:event`

| | |
|---|---|
| **Role** | Single event detail view (description, location, bookmark, share) |
| **Contains** | `EventScreen`, `EventDetailViewModel` |
| **Depends on** | `core:data`, `core:model`, `core:designsystem` |

### `feature:home`

| | |
|---|---|
| **Role** | Dashboard — at-a-glance view of the student's day |
| **Depends on** | `core:designsystem` |

### `feature:map`

| | |
|---|---|
| **Role** | Interactive campus map with searchable points of interest |
| **Depends on** | `core:designsystem` |

### `feature:social`

| | |
|---|---|
| **Role** | Student connections and study session coordination |
| **Depends on** | `core:designsystem` |

---

### `core:data`

| | |
|---|---|
| **Role** | Single source of truth for all app data. Contains repository interfaces, implementations, mappers, and sync logic. |
| **Contains** | `CalendarRepository`, `DefaultCalendarRepository`, `Mappers`, `CalendarSyncWorker`, `SyncInitializer` |
| **Depends on** | `core:network`, `core:database`, `core:model` |
| **NIA equivalent** | `core:data` — same role: `TopicsRepository`, `OfflineFirstNewsRepository`, `SyncUtilities` |

### `core:database`

| | |
|---|---|
| **Role** | Local persistence via Room |
| **Contains** | `EkhoDatabase`, `CalendarEventDao`, `CalendarEventEntity`, type converters |
| **Depends on** | `core:model` |
| **NIA equivalent** | `core:database` — same role: `NiaDatabase`, DAO classes, entity classes |

### `core:network`

| | |
|---|---|
| **Role** | Network data fetching and parsing |
| **Contains** | `ICalFeedDataSource`, `NetworkCalendarEvent`, OkHttp + iCal4j integration |
| **Depends on** | `core:model` |
| **NIA equivalent** | `core:network` — same role, different tech: `RetrofitNiaNetworkApi` with Retrofit + JSON |

### `core:model`

| | |
|---|---|
| **Role** | Domain models shared across all layers. Pure Kotlin — no Android dependencies. |
| **Contains** | `CalendarEvent`, `EventCategory` |
| **Depends on** | Nothing |
| **NIA equivalent** | `core:model` — same role: `Topic`, `NewsResource`, `UserData`. NIA also makes this a pure JVM library. |

### `core:designsystem`

| | |
|---|---|
| **Role** | Theme, colors, typography, icons, and reusable UI components |
| **Contains** | `EkhoTheme`, `EkhoIcons`, shared Compose components |
| **NIA equivalent** | `core:designsystem` — same role: `NiaTheme`, `NiaIcons`, `NiaButton` |

### `core:navigation`

| | |
|---|---|
| **Role** | Navigation route definitions and keys |
| **NIA equivalent** | `core:navigation` |

### `core:testing`

| | |
|---|---|
| **Role** | Shared test infrastructure — fakes, rules, factory functions |
| **Contains** | `MainDispatcherRule`, `TestCalendarRepository`, `testCalendarEvent()` |
| **NIA equivalent** | `core:testing` — same role: `NiaTestRunner`, `TestDispatcherRule` |

See the [Testing Guide](Testing.md) for details on how tests use this module.


## Side-by-Side: Ekho vs NIA

| NIA Module | Ekho Module | Status |
|---|---|---|
| `app` | `app` | Direct match |
| `core:data` | `core:data` | Direct match |
| `core:database` | `core:database` | Direct match |
| `core:network` | `core:network` | Direct match |
| `core:model` | `core:model` | Direct match |
| `core:designsystem` | `core:designsystem` | Direct match |
| `core:navigation` | `core:navigation` | Direct match |
| `core:testing` | `core:testing` | Direct match |
| `core:common` | *not yet needed* | Would hold injectable dispatchers and a `Result` wrapper |
| `core:datastore` | *not yet needed* | Would hold user preferences (theme, settings) via DataStore |
| `core:domain` | *not yet needed* | Would hold use cases that combine multiple repositories |
| `core:ui` | *not yet needed* | Would hold model-aware shared composables |
| `core:analytics` | *not applicable* | |
| `core:notifications` | *not applicable* | |
| `feature:*` | `feature:*` | Same pattern — screen + ViewModel per feature |

**8 out of 8 applicable core modules are a direct structural match.**

The four modules we haven't added are intentionally deferred. Each has a clear trigger for when it should be introduced — see the [Architecture Guide](ArchitectureGuide.md#where-we-diverge-from-nia-and-why) for the reasoning.


## Adding a New Feature Module

When the app grows, new features follow this pattern:

```
feature/newfeature/
    src/main/kotlin/.../
        NewFeatureScreen.kt      ← Composable UI
        NewFeatureViewModel.kt   ← Transforms data into UI state
    src/test/kotlin/.../
        NewFeatureViewModelTest.kt
    build.gradle.kts             ← Depends on core:data, core:model, core:designsystem
```

If the feature needs new data:

```
core/model/      + NewModel.kt                  (domain data class)
core/database/   + NewDao.kt, NewEntity.kt       (Room persistence)
core/network/    + NewDataSource.kt              (API / feed parsing)
core/data/       + NewRepository.kt              (ties it all together)
core/testing/    + TestNewRepository.kt          (fake for ViewModel tests)
```

This is the same expansion pattern described in NIA's modularization docs.
