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

## Signature et clés

Deux clés distinctes, à ne pas confondre.

**Clé de release.** Renseignée via `keystore.properties` à la racine du
dépôt (gitignoré), qui pointe vers un fichier `.jks` conservé en dehors du
dépôt :

```properties
storeFile=/chemin/absolu/vers/sentinelle-release.jks
storePassword=…
keyAlias=…
keyPassword=…
```

Sans ce fichier, `assembleRelease` produit un APK **non signé**, donc
non installable. Pour tester un build release minifié sur une machine qui
n'a pas la clé, utiliser `-PuseDebugSigningForRelease` (voir
`app/build.gradle.kts`) : l'APK est alors signé avec la clé de debug,
installable, mais rejeté par Play.

**Clé de debug.** Générée automatiquement dans `~/.android/debug.keystore`,
**propre à chaque machine**. Conséquence concrète en travaillant sur
plusieurs postes : un APK construit sur la machine A ne peut pas se
mettre à jour par-dessus une installation venant de la machine B — Android
voit deux certificats différents pour le même `applicationId` et refuse.
Il faut désinstaller, ce qui **efface toutes les données locales** (base
Room, DataStore : historique des blocages, labels, réglages, historique
qui alimente l'heuristique).

Pour éviter ça, copier `~/.android/debug.keystore` d'une machine vers
l'autre une bonne fois — les deux postes signent alors à l'identique et
les mises à jour passent sans désinstallation.

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
