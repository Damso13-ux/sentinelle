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

## Other blockers before submission

- [x] Enable GitHub Pages for the privacy policy — live at https://damso13-ux.github.io/sentinelle/
- [ ] New developer accounts must run a closed test (Play's current policy: ~12 testers for ~14 days) before Production release is unlocked
- [ ] Content rating questionnaire (Play Console, straightforward — no mature content)
- [ ] Target audience / age declaration (Play Console)

## Status: ready to submit

All repo-side prep is done — AAB, store assets, screenshots, privacy policy,
permission justifications. What's left only happens inside Play Console
itself (upload, forms, closed test).
