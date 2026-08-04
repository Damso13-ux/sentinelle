# AGENTS.md

Sentinelle is an Android call/SMS blocking app using Android's CallScreeningService API. It is a fork of Saracroche (GPLv3) — see NOTICE.

## Tech Stack

- Kotlin 2
- Compose
- MVVM
- Room 2 (KSP 2)
- WorkManager 2
- DataStore 1
- Gson 2
- minSdk 29, targetSdk/compileSdk 37, Java 11
- Gradle 9, AGP 9

## Guidelines

- `Icons.Rounded` for Material icons by default
- Patterns use `#` as trailing-only wildcard (e.g. `33162######`)
- Run `make lint` after each code modification
- Don't commit

## Commands

```bash
gradle build                # Build project
gradle test                 # Run unit tests
gradle assembleDebug        # Build debug APK
gradle app:lint             # Android Lint
make lint                   # Kotlin lint (format + style)
```
