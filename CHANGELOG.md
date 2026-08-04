# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed

- Forked from Saracroche v5.1.1 and rebranded as Sentinelle (new package `com.sentinelle.app`); see [NOTICE](NOTICE) for attribution and a summary of changes.

## [5.1.1] - 2026-07-22

### Changed

- Increase list update interval from 12 to 24 hours

## [5.1.0] - 2026-07-16

### Added

- Add list update interval configuration (12 hours) in Config
- Add force update capability to ListUpdateWorker with KEY_FORCE_UPDATE input data
- Trigger force update from AppUpdateReceiver after app updates

### Changed

- Update Compose BOM to 2026.06.01 and Kotlin to 2.4.10
- Refactor phone number reporting to use String type instead of Long throughout the pipeline
- Remove battery not low constraint from list update work requests
- Simplify health check request to send empty JSON body
- Use first country code only for X-Country-Code header
- Relax phone number validation in ReportViewModel (2-15 digits, accepts + prefix, cleans input)

### Fixed

- Fix phone number type mismatch between client and API

### Removed

- Remove HealthCheckRequest data class
- Remove input instruction text from ReportScreen
- Remove stringToLong helper function from ReportViewModel

## [5.0.0] - 2026-06-30

### Added

- Add hierarchical list system with `PatternListEntity` and `PatternListItemEntity` database entities for nested pattern organization
- Add API v2 support with new endpoints: lists, list patterns download, and health check
- Add `ListService` for updating and managing pattern lists from API
- Add `ListSyncService` for bidirectional synchronization between local and remote lists
- Add `ListPriorityService` for sorting lists by priority, type, and name
- Add `HealthCheckWorker` for periodic device health checks in organization mode
- Add MDM restrictions support with `restrictions.xml` configuration for enterprise deployments
- Add new API models: `ListSummary`, `ListPatternInfo`, `HealthCheckRequest`, `HealthCheckResponse`
- Add `NetworkError` sealed class for structured error handling across network operations
- Add comprehensive list management UI with new `ListsScreen` and `PatternListSheet` with pagination support
- Add unit tests for `ListPriorityService`, `ListSyncService`, `NetworkService`, and `ApiModels`
- Add `applyMdmRestrictions` function to `PreferencesManager` for API key injection in enterprise mode

### Changed

- **BREAKING:** Migrate database schema from version 1 to 2 with new hierarchical list structure, replacing flat pattern storage
- Update `AppDatabase` to use new list entities and add seed data for user allow/block lists
- Update `Config` to use API v2 base URL `app.saracroche.org` and add health check interval constant
- Update `NetworkService` with new API v2 endpoints and improved error handling with `NetworkError` type
- Update `CallScreeningService` with better error handling and preference validation checks
- Update `SaracrocheApplication` to schedule periodic health checks and read MDM configuration on startup
- Update `MainActivity` navigation to use new unified list screen
- Update `PreferencesManager` with additional methods for country codes, device management, and MDM restriction application
- Update `PatternManager` to use new list-based pattern loading with caching and proper cache invalidation
- Update `PhoneNumberMatcher` with empty input validation to prevent crashes
- Update build configuration with new test dependencies (mockwebserver, coroutines-test)
- Reorganize screen files: move from subdirectories (`ui/screen/home/`, `ui/screen/report/`, `ui/screen/settings/`) to root `ui/screen/` directory
- Update `AGENTS.md` to reflect current tech stack: Kotlin 2, Compose, MVVM, Room 2 (KSP 2), WorkManager 2, DataStore 1, Gson 2, minSdk 29, targetSdk/compileSdk 37, Java 11, Gradle 9, AGP 9
- Update Gradle wrapper from 9.5.0 to 9.6.0 and dependency versions
- Remove redundant count method from `BlockedCallDao`

### Removed

- Remove legacy `PatternEntity` and `PatternDao` (replaced by hierarchical `PatternListEntity` and `PatternListItemEntity` system)
- Remove old list screens: `APIPatternListScreen`, `MyListScreen`, and legacy `ListsScreen`

### Fixed

- Add empty input check in `PhoneNumberMatcher` to prevent crashes on null/empty phone numbers
- Improve error handling in call screening logic with proper preference checks
- Clear pattern cache after list updates to ensure consistency between local storage and API
- Add call filtering preference check before processing calls to respect user settings

## [4.2.0] - 2026-06-22

### Changed

- Update API base URL from `saracroche.org` to `app.saracroche.org`
- Remove `API_LISTS_URL` constant from Config
- Update dependencies: Compose BOM 2026.05.00 → 2026.06.00, core-ktx 1.18.0 → 1.19.0, Kotlin 2.3.21 → 2.4.0, Lifecycle ViewModel Compose 2.10.0 → 2.11.0, Material 1.13.0 → 1.14.0

## [4.1.2] - 2026-06-18

### Changed

- Consolidate list update logic into a single `ListService.updateList` method with a `force` parameter, unifying the schedule, worker, app-update, reinstall, and reset entry points
- Skip list download and database rewrite when the remote list version is unchanged
- Simplify `updateDatabase` to clear all API patterns and bulk-insert the new set
- Remove duplicate "Download list"/"Update list" buttons and the inline HTTP helper from the debug sheet

### Fixed

- Skip non-incoming calls in `CallScreeningService` by responding with an empty `CallResponse` and returning early

## [4.1.1] - 2026-05-30

### Fixed

- Fix list update to only delete removed API patterns instead of all

## [4.1.0] - 2026-05-13

### Changed

- Update pattern counting to include both block and identify patterns via new `getTotalPatternCount()` function
- Rename `calculateTotalBlockedNumbers()` to `calculateTotalCoveredNumbers()` for improved clarity
- Add DataStore corruption handler for protobuf format changes in PreferencesManager
- Improve pattern validation error messages in PatternService
- Refine text and punctuation throughout UI for better clarity
- Remove redundant echo from Makefile lint command

### Fixed

- Correct variable naming from `totalPatterns` to `totalPatternCount` for consistency

## [4.0.0] - 2026-05-08

### Added

- Add SMS blocking feature via NotificationListenerService to hide SMS notifications from blocked numbers
- Add SmsNotificationListener service to monitor and filter SMS notifications
- Add SmsNumberExtractor utility for parsing sender phone numbers from SMS notification extras
- Add SmsSettingsSheet for configuring SMS blocking toggle and notification preferences
- Add call identification notifications showing pattern name for matched identify patterns
- Add CallScreeningFailedDialog guiding users through manual call screening activation
- Add new notification channels for blocked SMS and identified calls
- Add blocked SMS notification toggle in preferences
- Add PhoneNumberMatcher.findMatchingIdentifyPattern for identify pattern matching
- Add Atkinson Hyperlegible font
- Add Makefile for running ktlint
- Add SmsNumberExtractorTest unit tests

### Changed

- **BREAKING:** Rename filtering preference from `filtering_enabled` to `call_filtering_enabled` to separate call and SMS filtering
- Upgrade Kotlin from 2.2.20 to 2.3.21
- Upgrade Gradle from 9.3.1 to 9.5.0 and AGP from 9.1.0 to 9.2.1
- Upgrade compileSdk and targetSdk from 36 to 37
- Upgrade dependencies: Gson 2.13.2 → 2.14.0, Compose BOM 2026.03.01 → 2026.05.00, Navigation Compose 2.9.7 → 2.9.8
- Separate call filtering and SMS blocking as independent toggles in preferences
- Move list download on launch from Application to MainActivity
- Add charging requirement to background WorkManager update constraints
- Reorganize UI package structure: move screens to `ui/screen/` and viewmodels to `ui/viewmodel/`
- Rename BusinessCodeSheet to BusinessSheet and AdvancedSettingsSheet to CallSettingsSheet
- Update AGENTS.md with full tech stack documentation
- Update README.md with SMS blocking feature and expanded technology stack
- Move launcher icon files from mipmap-anydpi-v26 to mipmap-anydpi
- Remove font_certs.xml (no longer required)
- Update .editorconfig with ktlint function naming rule

## [3.1.0] - 2026-04-29

### Added

- Add click action to notifications to open MainActivity

### Changed

- Hide blocked calls from call log and notifications
- Update notification icon vector drawable
- Move notification permission check to remember state

### Fixed

- Fix typo in report screen

## [3.0.2] - 2026-04-26

### Added

- Refresh block lists automatically after app updates via `MY_PACKAGE_REPLACED` broadcast receiver

### Changed

- Move list update scheduling from MainActivity to Application class for more reliable startup execution
- Simplify empty blocked calls text and clarify hidden number label to include private numbers

## [3.0.1] - 2026-04-19

### Fixed

- Use `WindowInsets.systemBars` instead of `WindowInsets.statusBars` in all bottom sheets to properly account for navigation bar insets

## [3.0.0] - 2026-04-17

### Added

- Add Room database for local pattern and blocked call storage (`AppDatabase`, `PatternEntity`, `PatternDao`, `BlockedCallEntity`, `BlockedCallDao`)
- Add Lists tab in bottom navigation to browse API patterns and manage custom patterns
- Add user pattern management with add/validate/delete flows, including overlap and duplicate detection
- Add blocked call notifications with dedicated notification channels for known and unknown blocked calls
- Add blocked call history in info sheet with per-call phone number, timestamp, and clear-all action
- Add automatic list updates via WorkManager periodic background download
- Add advanced settings sheet accessible from home screen for filtering toggle and contacts-only mode
- Add debug sheet with force update, download list, clear database, and reset preferences actions
- Add notification permission handling for blocked call notifications (Android 13+ rationale dialog)
- Add call screening failure dialog guiding users through manual activation steps
- Add donation dismissal cooldown that hides the donation card for a configurable period after dismissal

### Changed

- **BREAKING:** Migrate pattern storage from bundled JSON to Room database (patterns now downloaded from API and persisted locally)
- **BREAKING:** Change phone number handling from `String` to `Long` throughout the call screening pipeline for improved performance
- Redesign home screen with inline protection controls and info sheet showing blocked call history and background update status
- Simplify settings screen by moving filtering toggle, block anonymous, and contacts-only switches to home screen/advanced sheet
- Migrate list download to API endpoint instead of bundled asset file
- Replace `BlockedPatternManager` with `PatternManager` backed by Room
- Expand `Config` with API base URL, list endpoint, background/list update intervals, and donation dismiss interval
- Schedule WorkManager periodic list updates in `MainActivity` and trigger initial download on first launch
- Upgrade targetSdk/compileSdk from 36 to 37
- Upgrade dependencies: AGP 9.1.1, Compose BOM 2026.03.01, Activity 1.13.0, Lifecycle 2.10.0, Navigation 2.9.7, DataStore 1.2.1, Room 2.8.4, WorkManager 2.11.2
- Replace `kotlin-android` plugin with KSP for Room annotation processing
- Convert UI composable naming from PascalCase to camelCase (e.g. `HomeScreen()` → `homeScreen()`)
- Update `PhoneNumberMatcher` to use `Long` internally and add `generateVariants()` for multi-prefix matching

### Removed

- Remove `BlockedPatternManager.kt` (replaced by `PatternManager` with Room backend)
- Remove `french-list-arcep-operators.json` bundled asset (patterns now downloaded from API)

## [2.8.0] - 2026-03-25

### Added

- Add multi-country prefix support for phone number normalization on multi-SIM devices
- Add `PhoneNumberMatcher` utility class for centralized phone number processing with improved normalization and pattern matching
- Refactor device ID retrieval to use app-generated identifiers instead of device-specific ones for enhanced privacy

### Changed

- Update `CallScreeningService` to use new `PhoneNumberMatcher` API with centralized processing
- Improve wildcard pattern matching logic with better number cleaning

### Fixed

- Fix pattern matching divergence between production and test implementations by unifying logic in `PhoneNumberMatcher`

## [2.7.0] - 2026-03-20

### Added

- Add dismiss button to donation sheet with "Plus tard, non merci" option

### Changed

- Update blocked call description in info sheet to clarify that blocked calls appear in phone app call log with 🚫 symbol
- Update project overview and structure in AGENTS.md

## [2.6.0] - 2026-02-27

### Added

- Add global filtering toggle to enable/disable call filtering in settings
- Add contacts-only filtering option to block all calls from non-contacts
- Add enterprise code input sheet for business fleet protection (centralized management, custom allow lists, reporting, MDM deployment, automatic updates)
- Add call blocking statistics display with total blocked numbers, blocked patterns, and current filtering settings
- Add info sheet displaying call blocking statistics and settings
- Add business code sheet for enterprise code activation

### Changed

- Replace `blocked-patterns.json` with `french-list-arcep-operators.json` using a structured JSON format with metadata and `action` field
- Reorganize settings screen with global filtering toggle, contacts-only switch, and enterprise code action item
- Improve home screen with integrated blocked patterns stats, info button, and updated call screening card title
- Change report icon from `Icons.Rounded.Report` to `Icons.Rounded.Campaign`
- Enhance call screening permission card with blocked numbers count
- Remove GitHub Sponsors button from donation sheet and promote Liberapay button to full-width with Euro icon
- Move donation sheet to `com.cbouvat.android.saracroche.ui.sheet` package and remove unused imports
- Update documentation: remove outdated JAVA_HOME reference, update iOS repository link and PayPal donation link
- Update repository links to Codeberg in CONTRIBUTING.md and SettingsScreen

### Removed

- Remove contact section from settings screen
- Remove email bug reporting functionality
- Remove GitHub Sponsors donation button and `.github/FUNDING.yml`
- Remove separate `BlockedPatternsStatsCard` (integrated into call screening card)
- Remove `app/src/main/assets/blocked-patterns.json` (replaced by `french-list-arcep-operators.json`)

## [2.5.0] - 2025-12-25

### Changed

- Remove 23 BICS operator entries from blocked-patterns.json
- Update README to announce F-Droid availability with direct download link
- Replace GitHub Sponsors link with saracroche.org support link in README
- Remove ARCEP data sources documentation section from README, replace with reference to local JSON file
- Update versionCode to 21 and versionName to 2.5.0

## [2.4.0] - 2025-11-11

### Changed

- Restructure blocked-patterns.json with `start`/`end` numeric range fields, add 26 new operators, remove 3 operators (net decrease from 859 to 827 entries)
- Update phone screenshots
- Update versionCode to 20 and versionName to 2.4.0

## [2.3.0] - 2025-11-11

### Changed

- Migrate report API to `saracroche.org/api` with new contract (`phone: Long`, `device_id`, `Accept` header)
- Update website URLs from `cbouvat.com/saracroche` to `saracroche.org`
- Change Stripe donation link from `buy.stripe.com` to `donate.stripe.com`
- Add Stripe custom donation link to `.github/FUNDING.yml`
- Add France-only usage note in app metadata
- Update LICENSE file to correct GNU General Public License v3.0 text
- Update versionCode to 19 and versionName to 2.3.0

### Removed

- Remove French phone number validation for 12 or 16 digits in `ReportViewModel`

## [2.2.0] - 2025-10-22

### Added

- Add support for French phone numbers with both 12 and 16 characters (including `+33` prefix)

### Changed

- Rename `.github/copilot-instructions.md` to `AGENTS.md` with updated title
- Change terminology in SettingsScreen from "anonymes" to "masques" to "prives"
- Remove unused `AddComment` and `Help` icon imports and reformat privacy policy intent
- Remove TODO comment from data extraction rules XML
- Update versionCode to 18 and versionName to 2.2.0

## [2.1.0] - 2025-10-22

### Added

- Add external web links for help and privacy policy in SettingsScreen (replacing in-app HelpScreen)
- Add "Contact" section in SettingsScreen with email and Mastodon links
- Add `dependenciesInfo` configuration to exclude dependencies from APK and bundle

### Changed

- Update icons in SettingsScreen: anonymous call blocking to `Icons.Rounded.PhoneDisabled`, contact developer to `Icons.Rounded.Mail`, new help and privacy icons
- Move "Bisou" footer text and `openPlayStore()` function from HelpScreen to SettingsScreen
- Refactor imports, whitespace, and formatting across multiple files
- Update `distributionSha256Sum` in gradle-wrapper.properties
- Update versionCode to 17 and versionName to 2.1.0

### Removed

- Remove HelpScreen file and "Aide" bottom navigation tab (reduced from 4 to 3 tabs)
- Remove unused coroutine imports from CallScreeningService

## [2.0.0] - 2025-10-22

### Added

- Add anonymous/private call blocking feature with DataStore-based preferences and toggle switch
- Add `datastore-preferences` 1.1.7 to version catalog

### Changed

- Revert AGP version from 8.13.0 to 8.11.1 in libs.versions.toml
- Update versionCode to 16 and versionName to 2.0.0

### Fixed

- Add missing `platform()` wrapper for Compose BOM test dependency

### Removed

- Remove `SettingsSection` composable function from SettingsScreen

## [1.9.0] - 2025-10-22

### Changed

- Migrate Kotlin compiler options from `kotlinOptions` block to `kotlin { compilerOptions {} }` DSL syntax
- Update Gradle wrapper to version 9.0.0 with new security properties (`networkTimeout`, `validateDistributionUrl`, `distributionSha256Sum`)
- Update `gradlew` and `gradlew.bat` for POSIX compliance and improved error handling (part of Gradle 9.0.0 upgrade)
- Update versionCode to 15 and versionName to 1.9.0

### Removed

- Remove `androidx-ui-test-junit4` from build configuration

## [1.8.0] - 2025-09-18

### Added

- Add app metadata files in English and French (descriptions, titles, screenshots)
- Add F-Droid availability information to README
- Add `blocked-patterns.json` with pattern-based format (`operator_name`, `pattern` fields with `#` wildcards)
- Add `BlockedPatternManager` utility class for loading and matching blocked patterns

### Changed

- Rewrite call screening logic from prefix matching to wildcard pattern matching with `normalizePhoneNumber()` and `matchesPattern()` methods
- Change `setSkipNotification` from `true` to `false` so blocked calls now generate notifications
- Replace Send icon with AddAlert icon in ReportScreen
- Simplify building instructions and remove configuration section in README
- Update library versions in libs.versions.toml
- Remove `appVersion` from `ReportRequest` and related logic in `NetworkService`
- Replace example configuration file (`Config.kt.example`) with actual `Config.kt` and update `.gitignore`
- Update email contact address in Code of Conduct, Security Policy, HelpScreen, and SettingsScreen
- Update versionCode to 14 and versionName to 1.8.0

### Removed

- Remove `blocked-prefixes.json` and `BlockedPrefixManager` (replaced by pattern-based system)

## [1.7.0] - 2025-09-18

### Added

- Add "Noter l'application" rating button in DonationSheet

### Changed

- Replace reusable `DonationButton` composable with inline buttons using `CreditCard` and `Wallet` icons, change "Carte bancaire" text to "Carte bancaire & Google Pay"
- Translate all inline comments in NetworkService from French to English
- Update blocked calls description in HomeScreen to present tense
- Update versionCode to 13 and versionName to 1.7.0

## [1.6.0] - 2025-09-18

### Added

- Add credit card/Stripe payment option in DonationSheet
- Add rate app button in DonationSheet
- Add `SupportSection` composable with email and GitHub issue buttons in HelpScreen
- Add FAQ entries for 33700 service, ARCEP operator lookup, and troubleshooting in HelpScreen

### Changed

- Rewrite color system in `Color.kt` with new base color variables
- Disable Material You dynamic theming (`dynamicColor` set to `false`)
- Restructure HomeScreen layout and update content
- Simplify ReportScreen layout, remove Service 33700 and ARCEP operator cards, change icon to `Send`
- Reorganize SettingsScreen sections and rename labels
- Redesign DonationSheet header, enlarge heart icon, and reorder buttons
- Rework HelpScreen FAQ items and replace bug report section with SupportSection
- Inline `AppNavigation` into `SaracrocheApp` and remove unused `WindowInsets`
- Update versionCode to 12 and versionName to 1.6.0

### Removed

- Remove Service 33700 card and ARCEP operator lookup card from ReportScreen
- Remove `AppNavigation` composable (inlined into `SaracrocheApp`)

## [1.5.0] - 2025-09-18

### Added

- Enable code minification (`isMinifyEnabled`) and resource shrinking (`isShrinkResources`) for release builds
- Add ProGuard rule `-keep class com.cbouvat.android.** { *; }` to prevent class removal
- Update versionCode to 11 and versionName to 1.5.0

### Removed

- Remove `applicationIdSuffix = ".debug"` from debug build type

## [1.4.0] - 2025-09-18

### Added

- Add COCR operator blocked prefixes to blocked-prefixes.json

### Changed

- Migrate inline dependency declarations in `build.gradle.kts` to version catalog references in `gradle/libs.versions.toml`
- Update Android Gradle Plugin from 8.11.1 to 8.12.0
- Disable minification (`isMinifyEnabled`, `isShrinkResources`) and add debug build type configuration
- Remove commented line for sensitive configurations in Config.kt.example
- Update versionCode to 8 and versionName to 1.4.0

## [1.3.3] - 2025-09-18

### Changed

- Refactor SupportProjectCard placement in HomeScreen for improved readability
- Remove unused `IconButton` import from HomeScreen
- Update versionCode to 7 and versionName to 1.3.3

## [1.2.0] - 2025-09-18

### Added

- Add `SupportProjectCard` composable in HomeScreen with donation option
- Add help item about Sarah ("Pourquoi une patte d'ours ?") in HelpScreen
- Add question mark to privacy title in HelpScreen

### Changed

- Reduce bottom padding from 64dp to 32dp and add 64dp spacer in SettingsScreen and HelpScreen to prevent content cutoff
- Update README.md with correct iOS link and remove French sections for clarity
- Update versionCode to 3 and versionName to 1.2.0

### Removed

- Remove heart icon button from HomeScreen top app bar (replaced by SupportProjectCard)

## [1.1.0] - 2025-09-18

### Added

- Add iOS availability notice to README.md

### Changed

- Update README.md to reflect current Google Play Store availability status with download link
- Update versionName to 1.1.0

## [1.0.0] - 2025-09-18

### Added

- Release first stable version of Saracroche Android call blocking app
- Add `CallScreeningService` with `BlockedPrefixManager` for JSON-based prefix loading and pattern matching
- Add HomeScreen, SettingsScreen, ReportScreen, HelpScreen, and DonationSheet UI components
- Add `NetworkService`, `ApiModels`, and `ReportViewModel` for phone number reporting
- Add custom Material 3 theme with Google Fonts integration
- Add `PermissionUtils` for managing call screening permissions
- Add initial set of French telemarketing number prefixes in `blocked-prefixes.json`
- Add project infrastructure: LICENSE, README, CODE_OF_CONDUCT, CONTRIBUTING, SECURITY, FUNDING.yml
- Add `CallScreeningLogicTest` for call blocking logic validation
- Add `Config.kt.example` for sensitive configuration management
