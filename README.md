# Sentinelle

## Description

Sentinelle is an Android app that protects you from unwanted calls and SMS. It started as a fork of [Saracroche](https://codeberg.org/cbouvat/saracroche-android) and is evolving into a broader on-device protection app: spam call/SMS blocking, a local statistics & history dashboard, and an on-device heuristic (rule-based) spam-scoring engine designed to later support a trained on-device machine-learning model.

## Origin & Acknowledgements

Sentinelle is a derivative work of **Saracroche**, created by [cbouvat](https://codeberg.org/cbouvat) and licensed under the GNU General Public License v3.0. The original project can be found at [codeberg.org/cbouvat/saracroche-android](https://codeberg.org/cbouvat/saracroche-android) (also available on [Google Play](https://play.google.com/store/apps/details?id=com.cbouvat.android.saracroche) and [F-Droid](https://f-droid.org/en/packages/com.cbouvat.android.saracroche/)).

Sentinelle forked the Saracroche codebase at version 5.1.1. See the [NOTICE](NOTICE) file for a summary of changes made since the fork. Huge thanks to cbouvat and the Saracroche contributors for the original work this project builds on.

## Features

- 🛡️ Automatically blocks unwanted numbers
- 💬 Blocks unwanted SMS messages
- 📊 Local statistics & history dashboard for blocked calls/SMS
- 🧠 On-device heuristic spam scoring (no data leaves the device)
- 📱 Native Android application
- 🔒 Privacy-respecting: nothing is uploaded off-device; any local history used for scoring is opt-in and stays on-device
- 🔄 Regular updates of the number database

## Installation

Sentinelle is not yet published on any app store. For now, build it from source:

### Building from Source

1. Clone the repository
2. Open the project in Android Studio
3. Sync the project with Gradle files
4. Build and run the project on your device or emulator

**Requirements:**

- Android Studio
- Android SDK API level 29 or higher
- Gradle

## Technology Stack

- **Kotlin** - Primary programming language
- **Jetpack Compose** - Modern UI toolkit
- **MVVM Architecture** - Clean architecture pattern
- **Android Call Screening API** - For call blocking functionality
- **Room** - Persistence library
- **WorkManager** - Background task scheduling
- **DataStore** - Data storage
- **Gson** - JSON parsing

## Contributing

Contributions are welcome! Here's how you can help:

1. Fork the repository
2. Create a new branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the GNU General Public License v3.0 - see the [LICENSE](LICENSE) file for details. As a derivative of Saracroche, it remains GPLv3-licensed as a whole; see [NOTICE](NOTICE) for attribution details.
