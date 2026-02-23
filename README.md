# Ekho Navigator

A lightweight, mobile-first Android app that brings campus life together in one place. Built for students who are tired of juggling multiple campus websites and apps just to find a study room or check when their next class starts.

## Team

- **Christopher Vlavianos** - Engineer
- **Cristian Meza** - Engineer
- **Marcos Ramirez** - Engineer
- **Shashank Mantha** - Enginner
- **Robert Silva** - Engineer


## Overview

Ekho Navigator consolidates the essential parts of campus life (events, navigation, scheduling, and coordination) into a single, intuitive mobile experience. Instead of replacing official university systems, we complement them by making campus resources more discoverable and easier to use.

**Target Users:** College students looking for a simpler way to navigate campus life
**Platform:** Android mobile app
**Team Size:** 5 developers
**Development Timeline:** Academic semester project

## The Problem

Students currently face:
- **Fragmented tools** - Class schedules, event calendars, maps, and messaging are spread across different platforms
- **Clunky web portals** - Campus systems are often desktop-focused and not optimized for mobile
- **Poor discoverability** - Finding study spaces or campus events requires visiting multiple sites
- **Coordination friction** - Planning study sessions with classmates involves juggling multiple apps

## Our Solution

A mobile-first app that provides:
- **Unified dashboard** - See your day at a glance: next class, upcoming events
- **Interactive campus map** - Find buildings, study spaces, and points of interest
- **Event discovery** - Browse campus events with calendar integration
- **Study room finder** - Check availability and get directions
- **Social features** - Connect with classmates and coordinate study sessions
- **Calendar** - Manage classes, meetings, and events in one place

## Core Features

### Authentication
- User accounts and profiles
- Secure sign-in

### Dashboard
- At-a-glance view of your day
- Next scheduled class/meeting
- Upcoming events preview

### Campus Map
- Interactive campus navigation
- Searchable points of interest
- Category filtering (libraries, labs, dining, etc.)
- Integration with events and study rooms

### Study Rooms
- Browse available study spaces
- View capacity and amenities
- Deep links to official reservation system
- Map integration for directions

### Events
- Discover campus events from official feeds
- Filter by category, date, and location
- View event locations on map
- Add events to personal calendar

### Social
- Connect with other students
- Coordinate study sessions with classmates

### Calendar
- Manage classes, meetings, and events in one place
- Calendar integration

### Weather
- Campus weather information

## Tech Stack

| Layer | Technology |
|---|---|
| **Language** | Kotlin |
| **UI** | Jetpack Compose, Material 3 |
| **Navigation** | Navigation3 |
| **Architecture** | MVVM, unidirectional data flow, offline-first |
| **Networking** | OkHttp, iCal4j |
| **Local storage** | Room (SQLite) |
| **Background sync** | WorkManager |
| **DI** | Hilt |
| **Build** | Gradle (AGP 9), KSP |
| **Testing** | JUnit, Turbine, kotlin.test |
| **CI** | GitHub Actions |

Architecture is modeled after Google's [Now in Android](https://github.com/android/nowinandroid) reference app — see [Architecture Guide](docs/ArchitectureGuide.md) for details.

## Getting Started

```bash
git clone https://github.com/your-org/Ekho-Navigator.git
cd Ekho-Navigator
./gradlew assembleDebug
```

Open in Android Studio and run the `app` configuration on an emulator or device (API 29+).

To run tests:
```bash
./gradlew testDebugUnitTest
```

## Scope & Limitations

**What we are building:**
- Event discovery and browsing
- Study room finder with links to booking systems
- Social coordination features for students
- Unified calendar for campus activities

**What we are NOT building:**
- Grade tracking
- Course registration systems
- Cross-platform support (iOS, web)

This focused scope ensures we deliver a polished, functional app within our timeline.

---
