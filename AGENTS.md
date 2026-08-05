# AGENTS.md

Sentinelle est une application Android de blocage d'appels/SMS utilisant l'API `CallScreeningService` d'Android. C'est un fork de Saracroche (GPLv3) — voir NOTICE.

## Stack technique

- Kotlin 2
- Compose
- MVVM
- Room 2 (KSP 2)
- WorkManager 2
- DataStore 1
- Gson 2
- minSdk 29, targetSdk/compileSdk 37, Java 11
- Gradle 9, AGP 9

## Consignes

- `Icons.Rounded` pour les icônes Material par défaut
- Les motifs utilisent `#` comme joker en fin de chaîne uniquement (ex. `33162######`)
- Lancer `make lint` après chaque modification de code
- Ne pas commit

## Commandes

```bash
gradle build                # Compile le projet
gradle test                 # Lance les tests unitaires
gradle assembleDebug        # Compile l'APK de debug
gradle app:lint             # Android Lint
make lint                   # Lint Kotlin (formatage + style)
```
