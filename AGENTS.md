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
- Play Billing 9 (achat unique « Sentinelle Pro »)
- minSdk 29, targetSdk/compileSdk 37, Java 11
- Gradle 9, AGP 9

## Consignes

- `Icons.Rounded` pour les icônes Material par défaut
- Les motifs utilisent `#` comme joker en fin de chaîne uniquement (ex. `33162######`)
- Lancer `make lint` après chaque modification de code
- Ne pas commit

## Tests

La plupart des tests sont du JVM pur : la logique testable est isolée dans
des fonctions sans dépendance Android (ex. `HeuristicSpamDetector.scoreFromHistory`,
`PhoneNumberMatcher`). Privilégier cette approche.

Pour ce qui a besoin d'un `Context` (typiquement `PreferencesManager`, qui
s'appuie sur DataStore), Robolectric est disponible dans le source set
`test` — voir `ProEntitlementTest`. Annoter avec
`@RunWith(RobolectricTestRunner::class)` et `@Config(sdk = [34])` ; le SDK
est épinglé car Robolectric ne fournit pas d'environnement simulé pour
chaque niveau d'API récent.

Les tests unitaires compilent contre la variante debug, donc
`BuildConfig.DEBUG` y vaut `true` — le comportement release des chemins
gardés par ce drapeau n'est pas couvrable depuis ce source set.

## Commandes

```bash
gradle build                # Compile le projet
gradle test                 # Lance les tests unitaires
gradle assembleDebug        # Compile l'APK de debug
gradle app:lint             # Android Lint
make lint                   # Lint Kotlin (formatage + style)
```
