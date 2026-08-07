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

## In-app purchase ("Sentinelle Pro") — Play Console setup

Client-side plumbing is done (`BillingManager`, gated features, price display,
restore-purchases button). Everything below only happens inside Play Console
itself, most of it requiring the account owner's own login (and, for the
merchant profile, banking/identity details Claude must never touch):

- [ ] Merchant/payments profile verified (Play Console → Monetization setup) —
      was still "pending verification" as of the Billing plumbing commit
- [ ] App uploaded to at least the Internal Testing track — real product
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
- [ ] New developer accounts must run a closed test (Play's current policy: ~12 testers for ~14 days) before Production release is unlocked
- [ ] Content rating questionnaire (Play Console, straightforward — no mature content)
- [ ] Target audience / age declaration (Play Console)

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

## Status: ready to submit

All repo-side prep is done — AAB, store assets, screenshots, privacy policy,
permission justifications, and the release build is device-verified. What's
left only happens inside Play Console itself (upload, forms, closed test).

The in-app purchase is not a submission blocker: every Pro-gated feature
degrades gracefully to the free default when the product doesn't resolve
(no crash, just a bare "Débloquer" label with no price and a purchase
attempt that fails softly with a toast). Submitting before finishing the
Play Console IAP setup above is fine — Pro can go live in a later update.
