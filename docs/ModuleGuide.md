# Module Guide

For module descriptions, dependency graph, and architecture overview, see the [Architecture Guide](ArchitectureGuide.md).

This document covers practical expansion patterns — how to add new features and data sources.


## Adding a New Feature Module

```
feature/newfeature/
    src/main/kotlin/.../
        NewFeatureScreen.kt          ← Composable UI
        NewFeatureViewModel.kt       ← Transforms data into UI state
    src/test/kotlin/.../
        NewFeatureViewModelTest.kt   ← Uses core:testing fakes
    build.gradle.kts                 ← Depends on core:data, core:model, core:designsystem
```

Wire it in `app`:
1. Add `implementation(projects.feature.newfeature)` to `app/build.gradle.kts`
2. Add a `NavEntry` in `EkhoNavigatorApp.kt`
3. Add a navigation key in `feature/newfeature/navigation/`


## Adding a New Data Source

When a feature needs data from a new source (API, feed, Firestore, etc.), files go flat within existing modules — no subfolders per feature:

```
core/model/      + NewModel.kt                    domain data class
core/network/    + NewDataSource.kt                fetch/parse logic
core/network/    + model/NetworkNewModel.kt        network DTO
core/database/   + dao/NewDao.kt                   Room queries (if needed)
core/database/   + model/NewEntity.kt              Room entity (if needed)
core/data/       + repository/NewRepository.kt     interface
core/data/       + repository/DefaultNewRepo.kt    implementation
core/testing/    + TestNewRepository.kt            fake for tests
```

Not every data source needs a Room DAO. For example, Firestore provides its own offline persistence and reactive listeners, so a Firestore-backed source would have a network data source and repository but no separate DAO.

Update `DataModule.kt` to bind the new repository interface → implementation.


## NIA Side-by-Side

| NIA Module | Ekho Module | Notes |
|---|---|---|
| `app` | `app` | Direct match |
| `core:data` | `core:data` | Direct match |
| `core:database` | `core:database` | Direct match |
| `core:network` | `core:network` | Direct match — different tech (iCal vs Retrofit) |
| `core:model` | `core:model` | Direct match |
| `core:designsystem` | `core:designsystem` | Direct match |
| `core:navigation` | `core:navigation` | Direct match |
| `core:testing` | `core:testing` | Direct match |
| `core:domain` | *not yet* | Needed when combining multiple repositories |
| `core:datastore` | *not yet* | Needed for user preferences |
| `core:common` | *not yet* | Needed for shared dispatchers/utilities |
| `core:ui` | *not yet* | Needed for model-aware shared composables |
