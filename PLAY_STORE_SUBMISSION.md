# Play Store submission notes

Working notes for publishing Sentinelle on Google Play. Not published anywhere,
just a reference for filling out Play Console. **Not legal advice** — Google's
exact Data Safety category definitions change over time; verify against the
current Play Console help docs before submitting, especially the phone-number
and device-identifier items below.

## Sensitive permissions declarations

Play Console requires a written justification for each "restricted permission"
group used. Draft text below (English — Google's review team is global and
review tends to be faster/more reliable in English even for a French app).

### Call-related permission (CallScreeningService / ROLE_CALL_SCREENING)

> Sentinelle is a call-blocking and spam-protection app. Its core function is
> to automatically screen incoming calls and block those matching known
> spam/scam number patterns (crowd-sourced blocklists) or exhibiting
> suspicious calling behavior (on-device heuristic detection, opt-in and off
> by default). The app requests Android's dedicated ROLE_CALL_SCREENING role
> via RoleManager specifically for this purpose — it is not a full dialer
> replacement, does not access the call log, contacts, or call audio, and
> does not require being set as the default Phone app. All screening
> decisions happen entirely on-device.

### Notification access (BIND_NOTIFICATION_LISTENER_SERVICE)

> Sentinelle uses notification listener access exclusively to detect incoming
> SMS notifications from the user's default messaging app, in order to
> identify and hide notifications from known spam senders or messages
> matching user-defined keyword filters. This is necessary because Android
> does not expose a public SMS-screening API equivalent to
> CallScreeningService for third-party apps — notification listener access is
> the only available mechanism to detect and suppress unwanted SMS
> notifications without becoming the device's default SMS app (which would
> require reimplementing full SMS/MMS/RCS messaging, out of scope for this
> app). Sentinelle only inspects notifications originating from the user's
> designated default SMS app, only to decide locally whether to hide the
> notification, and never reads, stores, or transmits notification content
> from any other app.

### Display over other apps (SYSTEM_ALERT_WINDOW)

> Used exclusively for an optional, off-by-default feature ("caller ID
> bubble") that shows a small floating overlay with identification info
> (local label, block-list match, or official spam-registry match) while an
> unblocked incoming call is ringing. Requires explicit user opt-in via a
> dedicated settings toggle before the permission is even requested.

## Data Safety form — draft answers

**Does the app collect or share any user data?** Yes, two narrow cases:

| Data type | Collected? | Shared? | Purpose | Notes |
|---|---|---|---|---|
| Phone numbers | Yes, only if the user manually uses the "Signaler" (report) screen | Yes, sent to `app.saracroche.org` (third-party infrastructure of the upstream Saracroche project) | App functionality (crowd-sourced spam list) | Optional, explicit user action each time — never automatic |
| Device or other IDs | Yes — an app-generated random UUID (not IMEI/hardware-derived) | Yes, sent to `app.saracroche.org` with every list-sync and report request | App functionality | Not linked to any user identity; no account exists |
| SMS/message content | Processed on-device only (read via NotificationListenerService to decide whether to hide a notification) | **Not** transmitted off-device | App functionality | Confirm with Play's current definition of "collected" — on-device-only processing that never leaves the device typically doesn't count as "collected," but must still be disclosed as data *accessed* |
| Purchase history | Yes — the one-time "Sentinelle Pro" purchase, handled entirely by Google Play Billing | Shared with Google Play (required for the purchase itself to work) — never with Sentinelle's own infrastructure, there is none for this | App functionality (unlocking Pro features) | Sentinelle never sees payment details; `BillingManager` only reads back purchase state (bought/not bought) from Play, cached locally as a boolean |
| Everything else (location, contacts, financial info, health, photos, analytics/app activity, etc.) | No | No | — | No accounts, no ads SDK, no analytics/telemetry of any kind |

**Is data encrypted in transit?** Yes (HTTPS to `app.saracroche.org`).

**Can users request data deletion?** There is no user account or server-side
storage of personal data to delete from. Locally, Settings → "Réinitialiser
l'application" wipes everything on-device immediately.

## Store listing text status

- Short descriptions (`metadata/{en-US,fr-FR}/short_description.txt`) — updated for Sentinelle ✅
- Full descriptions (`metadata/{en-US,fr-FR}/full_description.txt`) — updated for Sentinelle ✅
- Store icon `metadata/{en-US,fr-FR}/images/icon.png` (512×512) — generated from the "Garde" shield, Play-ready ✅
- Feature graphic `metadata/{en-US,fr-FR}/images/featureGraphic.png` (1024×500) — generated, Play-ready ✅
- Screenshots (`metadata/*/images/phoneScreenshots/1-5.jpg`) — real captures from the current build: Accueil, Statistiques, Signaler, Listes, Réglages ✅

## Identifiant de l'application

`io.github.damso13ux.sentinelle`, et non `com.sentinelle.app` : ce dernier
était déjà pris sur Play au moment de créer la fiche.

À ne pas confondre avec le `namespace` de `app/build.gradle.kts`, resté à
`com.sentinelle.app`. Le namespace est le package des sources — classes,
`R`, `BuildConfig` — et n'a aucune existence côté Play. C'est
l'`applicationId` qui identifie l'app, et c'est lui qui devait changer.
D'où un correctif tenant en deux fichiers, sans déplacer une seule classe.

Deux conséquences à garder en tête :

- **L'applicationId est définitif** une fois la fiche créée. Il ne peut
  plus être modifié, jamais ; en changer imposerait une nouvelle fiche,
  sans les avis ni les installations de l'ancienne.
- **Un applicationId différent est une application différente** pour
  Android. Un appareil qui a l'ancienne version installée verra la
  nouvelle comme une app distincte et les installera côte à côte, chacune
  avec ses propres données.

## Versioning

Both numbers live at the top of `app/build.gradle.kts` (`appVersionCode` /
`appVersionName`).

**Before every upload**, bump `appVersionCode` by 1. No exceptions — Play
rejects a reused value, and it can never be lowered afterwards. It's
invisible to users; it exists purely so Play knows which build is newer.

`appVersionName` is the string users see. Move it when the *content*
changes meaningfully, not on every upload:

| Phase | versionName | Play track |
|---|---|---|
| First internal builds | `1.0.0-alpha01`, `-alpha02`, … | Internal testing |
| Mandatory closed test (~12 testers / ~14 days) | `1.0.0-beta01`, `-beta02`, … | Closed testing |
| Public launch | `1.0.0` | Production |
| Bug fixes after launch | `1.0.1`, `1.0.2` | Production |
| New features | `1.1.0` | Production |
| Breaking change / redesign | `2.0.0` | Production |

The suffix is documentation, not a mechanism: who actually receives a
build is decided by the Play Console track it's uploaded to. Keeping the
two aligned just avoids confusion about which build a tester is running —
worth it, since testers report bugs by version name.

### Un versionCode est brûlé dès l'upload, pas à la publication

Vécu le 8 août 2026. Un AAB en versionCode 3 est importé, la release
n'est jamais envoyée pour examen. À la tentative suivante, Play refuse :

> Le code de version 3 a déjà été utilisé. Choisissez-en un autre.

**Le compteur est consommé au moment où le bundle atteint Play**, pas
quand la release est soumise ni publiée. Abandonner la release, supprimer
le brouillon, ne rien envoyer : rien ne le libère, et il n'existe aucun
moyen de le récupérer. Il a fallu reconstruire en versionCode 4 —
`appVersionName` restant à `1.0.0-alpha03` puisque le contenu était
identique, ce que le bloc de versioning de `app/build.gradle.kts` prévoit
explicitement.

Conséquence pratique : **ne jamais compter les versionCodes comme des
publications**. Après deux envois réels, on en était déjà au 4. En cas de
doute sur ce qui est parti, lire l'activité d'envoi, jamais le numéro.

### Reconnaître une release qui n'a pas de bundle

Trois signaux, tous vus le même soir :

- la piste affiche **« Release sans nom »** en brouillon — Play nomme une
  release d'après son bundle, donc l'absence de nom signale l'absence de
  bundle ;
- la page de préparation dit *« Aucun app bundle de votre précédente
  release ne sera inclus dans celle-ci »* ;
- le bouton **Suivant** est grisé, et le champ « Nom de la version » est
  vide alors qu'il est normalement pré-rempli.

Dans cet état, Play réclame le nom de la release. C'est un symptôme, pas
la cause : remplir le nom ne débloque rien tant que le bundle manque.

**Piège associé.** La section « Non inclus » propose un lien *Inclure* à
côté du bundle de la release précédente. Cliquer dessus produit une
release valide — mais qui redéploie l'ancienne version sous un nouveau
numéro. Les testeurs ne reçoivent rien de neuf, et rien ne le signale.

### Notes de version : 500 caractères par langue

Dépassement silencieux jusqu'à la validation, qui répond alors
« La note de version pour fr-FR est trop longue ». Compter avant de
coller, ou raccourcir dès le premier refus.

## In-app purchase ("Sentinelle Pro") — Play Console setup

Client-side plumbing is done (`BillingManager`, gated features, price display,
restore-purchases button). Everything below only happens inside Play Console
itself, most of it requiring the account owner's own login (and, for the
merchant profile, banking/identity details Claude must never touch):

- [ ] Merchant/payments profile — **not created at all** as of 7 August 2026.
      "Produits ponctuels" refuses access outright: *« Vous devez configurer
      un compte marchand Google Payments pour pouvoir accéder à cette
      page »*. Everything else in this section is blocked behind it. It asks
      for identity and banking details, so it can only be done by the
      account owner in person.
- [x] App uploaded to at least the Internal Testing track — real product
      lookups generally don't resolve on a sideloaded debug APK, the app
      needs a Play Console listing to test against
- [ ] Create the one-time product: Monetize → Products → Product ID
      **exactly** `sentinelle_pro` (must match `BillingManager.PRO_PRODUCT_ID`,
      cannot be changed after creation), name "Sentinelle Pro", a short
      description of what it unlocks, and a price
- [ ] **Activate** the product after creating it — a draft/inactive product
      resolves to nothing, even for license testers
- [ ] Add your own Gmail (and any other testers) under License Testing so
      test purchases don't charge real money
- [ ] End-to-end test: install from the Internal Testing track (not
      sideloaded), open Réglages, confirm the real price now shows on the
      purchase button, complete a test purchase, confirm Pro features unlock
      and "Restaurer mes achats" reports success on a second device/reinstall

## Other blockers before submission

- [x] Enable GitHub Pages for the privacy policy — live at https://damso13-ux.github.io/sentinelle/
- [x] Content rating questionnaire (Play Console, straightforward — no mature content)
- [x] Target audience / age declaration (Play Console)
- [ ] New developer accounts must run a closed test (Play's current policy: ~12 testers for ~14 days) before Production release is unlocked

## Catégorie, tags et coordonnées

Saisis le 7 août 2026.

**Catégorie : Communication**, et non Outils. C'est là que se trouvent
Truecaller, Hiya et les autres filtres d'appels — donc là que les
utilisateurs cherchent. « Outils » est un fourre-tout nettement plus
concurrentiel.

**Tags retenus : « Identification de l'appelant » (Communication) et « Vie
privée et sécurité » (Outils).** Deux seulement, sur cinq possibles.

Les tags décident du groupe d'applications auquel Play compare la fiche,
d'où deux exclusions volontaires malgré leur nom trompeur :

- *SMS* — sa définition Play vise les applis qui **envoient** des
  messages (« permettent d'envoyer des messages à un ou plusieurs autres
  utilisateurs »). Sentinelle n'en envoie aucun.
- *Communication* (le tag générique, distinct de la catégorie) — même
  problème : « échanger des fichiers et des SMS […] messagerie, SMS/MMS
  et chat ».

Le cocher aurait fait comparer Sentinelle à WhatsApp et Messages. Rien
d'autre dans le catalogue Play ne correspond : pas de tag « spam », «
blocage » ni « téléphonie ».

**Coordonnées publiques** : `damdam13122@gmail.com` (déjà publique sur la
politique de confidentialité, donc aucune exposition nouvelle) et
`https://github.com/Damso13-ux/sentinelle` comme site Web — le dépôt est
la vraie page projet, et la politique de confidentialité a déjà son
propre champ. Numéro de téléphone laissé vide : facultatif, et il serait
affiché publiquement.

## Test interne — en place depuis le 7 août 2026

Release `2 (1.0.0-alpha02)` publiée sur le canal Tests internes, liste de
diffusion « Testeurs internes » (1 adresse). Lien d'inscription :
`https://play.google.com/apps/internaltest/4701642787629830767`

Deux avertissements à l'aperçu, tous deux inoffensifs :

- *Aucun testeur désigné* — normal, la liste a été créée après coup.
- *Code natif sans symboles de débogage* — les seuls `.so` du bundle sont
  `libandroidx.graphics.path.so` (tiré par Compose) et
  `libdatastore_shared_counter.so`. Aucun code à nous : importer des
  symboles n'apporterait rien, Google possède déjà ceux d'AndroidX.

## Release build verified on device

The ProGuard rules were narrowed during the audit (the blanket
`-keep class com.sentinelle.app.**` was replaced by targeted rules), so R8
now actually shrinks app code — around 278 KB less. Anything that had only
survived because of the old catch-all rule would have broken at runtime,
in release builds only, and no debug build or unit test would have caught
it.

A signed release APK of 1.0.0-alpha02 (versionCode 2) was installed and
exercised on a real device: every screen opened, list update triggered,
call blocking and the identified-call notification confirmed working. That
closes the one unknown that couldn't be checked from the build alone.

Worth repeating for any future change to `proguard-rules.pro`: test a real
release build on a device before uploading. `-PuseDebugSigningForRelease`
makes that possible on a machine without the signing key.

## Sécurité des données — réponses effectivement saisies

Saisi dans Play Console le 7 août 2026. Conservé ici parce que c'est une
attestation : si Google la conteste plus tard, ou pour la prochaine
version, il faut pouvoir retrouver le raisonnement.

**Étape 2 — collecte et sécurité**
- Collecte ou partage de données : **Oui** (UUID aléatoire à chaque
  synchro de listes, numéro signalé via « Signaler »)
- Chiffrement en transit : **Oui** (NetworkService n'utilise que HTTPS)
- Création de compte : **aucune**
- Connexion via comptes externes : **Non**
- Moyen de demander la suppression : **Non** — les données partent vers
  une infra tierce non contrôlée, promettre une suppression serait
  intenable

**Étape 3 — types de données**
- Cochés : **Numéro de téléphone**, **ID de l'appareil**
- Volontairement non cochés :
  - *Messages (SMS)* — lus via NotificationListenerService mais traités
    uniquement sur l'appareil. Play définit « collecter » comme
    transmettre hors de l'appareil.
  - *Infos financières / historique des achats* — corrige un brouillon
    antérieur de ce fichier qui disait l'inverse. Sentinelle ne transmet
    aucune donnée d'achat : Play traite la transaction, l'app lit
    seulement l'état « acheté ou non ».

**Étape 4 — numéro de téléphone**
- Collectées **et** partagées. Le partage est la déclaration la plus
  lourde : la fiche affichera « partage des données avec des tiers ».
  C'est exact, `app.saracroche.org` appartient au projet Saracroche et
  non au développeur ; l'exception « sous-traitant agissant pour votre
  compte » ne s'applique pas, faute de contrat de traitement.
- Traitement éphémère : Non (conservé dans la liste partagée)
- Facultative : l'utilisateur choisit (le signalement est volontaire)
- Objectifs, collecte et partage : fonctionnement de l'appli +
  prévention des fraudes et sécurité

**Étape 4 — ID de l'appareil** : mêmes réponses, sauf que la collecte est
**requise** — elle a lieu à chaque synchronisation, sans action de
l'utilisateur.

## Test fermé — approuvé et en ligne depuis le 7 août 2026

Envoi n° 1 à 21:44, **approuvé et publié à 21:53** (neuf minutes). C'était
le premier examen humain de Sentinelle : Google y lit les justifications
de permissions sensibles (filtrage d'appels, accès aux notifications,
overlay). Elles sont donc passées telles qu'écrites plus haut.

État constaté dans la console : canal « Tests fermés - Alpha » **Actif**,
release `2 (1.0.0-alpha02)` « Disponible pour certains testeurs ».

Canal « Tests fermés - Alpha », release `2 (1.0.0-alpha02)` reprise depuis
la bibliothèque — aucun nouveau build, donc le binaire examiné est
exactement celui déjà validé sur appareil.

- **Pays** : France (+ 11 territoires), Belgique, Suisse, Luxembourg,
  Monaco. Les listes sont des numéros français, mais ouvrir aux pays
  francophones voisins évite qu'un testeur soit bloqué à l'installation —
  et réunir les 12 est le vrai chemin critique.
- **Testeurs** : liste « Testeurs test fermé », 3 adresses sur les 12
  requises. Liste éditable à tout moment sans nouvel examen. **Attention :
  une adresse sur la liste n'est pas un testeur inscrit** — voir la section
  suivante.
- **Retours** : `damdam13122@gmail.com`, annoncé dans les notes de version.
- **Publication gérée : désactivée** — dès approbation, ça part sur le
  canal fermé sans revalidation. Voulu : les 14 jours démarrent au plus tôt.

### Deux pièges rencontrés à l'envoi

**Déclaration « identifiant publicitaire » manquante.** Bloquant réel et
non évident : elle ne figure ni dans les tâches du tableau de bord ni dans
le canal de test, mais dans Contenu de l'application → Identifiant
publicitaire (`/app-content/ad-id-declaration`). Obligatoire dès qu'on
cible Android 13+. Réponse : **Non**, vérifiée deux fois — aucun SDK
publicitaire dans les sources, et surtout aucune permission
`com.google.android.gms.permission.AD_ID` dans le manifeste **fusionné**
du bundle. C'est ce dernier contrôle qui compte : Google avertit qu'un SDK
tiers peut injecter la permission sans déclaration explicite, et le
manifeste fusionné est justement l'endroit où cela se verrait.

**Enregistrer ≠ envoyer.** Deux actions distinctes, et rien dans l'écran
ne le souligne. Le seul endroit qui fait foi est
`/publishing/submission-activity` : tant qu'il affiche « Vous n'avez aucun
envoi récent », rien n'est parti, quoi qu'affiche le reste de la console.
À revérifier là à chaque envoi.

## Accès en production : « inscrit » ≠ « invité »

Relevé dans la console (Tableau de bord → Production → Demander un accès en
production), formulation exacte de Google et avancement réel :

| | Condition | État |
|---|---|---|
| ✅ | Publier une version de test fermé | Fait |
| ⭕ | Avoir au moins 12 testeurs **inscrits** à votre test fermé | *0 testeur actuellement inscrit* |
| ⭕ | Exécuter votre test fermé avec au moins 12 testeurs **pour au moins 14 jours** | Pas commencé |

Deux pièges de comptage, tous deux coûteux en temps :

**Une adresse ajoutée à la liste de diffusion ne compte pas.** La liste
« Testeurs test fermé » contient 3 adresses, et la console affiche pourtant
*0 testeur actuellement inscrit*. Google ne compte que les personnes qui
ont réellement suivi le lien d'inscription et rejoint le programme depuis
leur compte Google. Inviter, c'est le début du travail, pas la fin — il
faut que chacun clique et accepte.

**Les 14 jours ne courent pas tant que les 12 ne sont pas réunis.** La
condition dit « avec au moins 12 testeurs pour au moins 14 jours » : c'est
une durée *pendant laquelle* le seuil est tenu, pas un délai qui s'écoule
depuis l'ouverture du test. Le compteur démarre au douzième inscrit.
Chaque jour de recrutement décale donc la production d'autant.

**Aucun e-mail n'est envoyé automatiquement.** Ajouter une adresse à la
liste de diffusion ne fait qu'autoriser la personne à rejoindre le test :
c'est au développeur de lui transmettre le lien, et à elle de l'ouvrir et
d'accepter depuis son compte Google. Rien ne part de chez Google.

Lien d'inscription (vérifié le 8 août 2026 — il affiche bien la page
« Become a tester » pour Sentinelle) :

```
https://play.google.com/apps/testing/io.github.damso13ux.sentinelle
```

Il se retrouve aussi dans le canal fermé → onglet Testeurs → « Participer
sur Android » / « Participer sur le Web » → *Copier le lien*.

Le compte utilisé pour ouvrir le lien doit être **exactement** celui inscrit
sur la liste de diffusion. Sur un téléphone avec plusieurs comptes Google,
c'est la cause d'échec la plus courante.

## Signature Play ≠ signature locale : désinstaller avant d'installer

Piège rencontré à la première installation depuis Play, le 7 août 2026.
L'installation échouait sur un « Une erreur s'est produite de notre
côté » peu bavard.

Cause réelle, mesurée :

| Binaire | SHA-256 du certificat |
|---|---|
| APK construit ici (`app-release.apk`) | `2324b0aa…d451` |
| Ce que Play distribue | `9a7a8813…99ab` |

Depuis l'activation de Play App Signing, le keystore local n'est plus
qu'une **clé d'importation** : elle prouve à Google que l'envoi vient
bien de nous, mais ce n'est plus elle qui signe ce que les utilisateurs
reçoivent. Google a généré sa propre clé de distribution.

Même `applicationId`, signatures différentes : Android refuse d'installer
par-dessus, et c'est voulu — c'est ce qui empêche de remplacer une appli
par une autre.

Conséquences pratiques :

- Un appareil portant un build sideloadé doit **désinstaller** avant
  d'installer depuis Play. La désinstallation efface toutes les données
  locales (listes personnelles, historique, réglages) : tout est local
  par conception, il n'existe aucune sauvegarde serveur.
- Cela vaut pour tout testeur ayant reçu un APK à la main.
- Un APK local ne s'installera plus par-dessus la version Play. Pour
  tester une modification, soit désinstaller à nouveau, soit passer par
  le canal de test interne — plus lent, mais c'est ce qui teste
  réellement le binaire distribué.

Les empreintes se retrouvent dans Play Console → Signature d'application,
et côté APK via
`apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk`
(nécessite `JAVA_HOME`, par exemple le JBR d'Android Studio).

## Status: live on the Internal Testing track

All repo-side prep is done — AAB, store assets, screenshots, privacy policy,
permission justifications, and the release build is device-verified. Every
"Terminer la configuration de votre appli" task is now closed (11/11) and
the section has disappeared from the dashboard.

What's left: the mandatory closed test (~12 testers / ~14 days) before
Production unlocks, and the merchant profile that gates the in-app product.

The in-app purchase is not a submission blocker: every Pro-gated feature
degrades gracefully to the free default when the product doesn't resolve
(no crash, just a bare "Débloquer" label with no price and a purchase
attempt that fails softly with a toast). Submitting before finishing the
Play Console IAP setup above is fine — Pro can go live in a later update.
