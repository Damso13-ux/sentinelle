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

### Installer la clé de release sur une nouvelle machine

Deux fichiers, aucun des deux dans le dépôt (`.gitignore` bloque
`keystore.properties`, `*.jks` et `*.keystore`) :

1. **le keystore** `sentinelle-release.jks` ;
2. **`keystore.properties`**, à la racine du dépôt, qui pointe dessus.

Le keystore et son mot de passe sont des secrets. Les transférer par un
moyen que l'on maîtrise — clé USB, gestionnaire de mots de passe,
dossier chiffré. **Jamais par e-mail, messagerie, ticket, ni dans une
conversation avec un assistant** : ce qui y passe y reste écrit.

Sur la nouvelle machine :

```bash
# 1. déposer le .jks hors du dépôt, par exemple :
#    ~/.android-signing/sentinelle-release.jks
# 2. créer keystore.properties à la racine du dépôt
```

```properties
storeFile=/chemin/absolu/vers/sentinelle-release.jks
storePassword=…
keyAlias=…
keyPassword=…
```

Sous Windows, échapper les antislashs (`C:\\Users\\…`) ou utiliser des
slashs — c'est un fichier `.properties`, pas un chemin shell.

**Vérifier que c'est la bonne clé** avant de perdre du temps sur un envoi
refusé. Construire un APK release et lire son certificat :

```bash
./gradlew assembleRelease
apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk
```

Le SHA-256 doit être `2324b0aa…d451` — l'empreinte de la clé
d'importation, visible dans Play Console → Signature d'application. Toute
autre valeur signifie que le keystore ou l'alias n'est pas le bon, et Play
rejettera l'envoi.

`apksigner` a besoin de `JAVA_HOME`. Si le shell ne l'a pas :

```bash
export JAVA_HOME="/chemin/vers/Android Studio/jbr"
```

**Perdre la clé d'importation n'est pas fatal.** Play App Signing est
activé : la clé qui signe ce que reçoivent les utilisateurs appartient à
Google et n'est pas sur nos machines. En cas de perte, on demande une
réinitialisation de la clé d'importation à Google. À ne pas provoquer pour
autant — la procédure prend plusieurs jours.

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
