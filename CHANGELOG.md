# Journal des modifications

Toutes les modifications notables apportées à ce projet seront documentées dans ce fichier.

Le format est basé sur [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
et ce projet respecte le [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Modifié

- Forké depuis Saracroche v5.1.1 et rebrandé en Sentinelle (nouveau package `com.sentinelle.app`) ; voir [NOTICE](NOTICE) pour l'attribution et un résumé des changements.

## [5.1.1] - 2026-07-22

### Modifié

- Augmentation de l'intervalle de mise à jour des listes de 12 à 24 heures

## [5.1.0] - 2026-07-16

### Ajouté

- Ajout de la configuration de l'intervalle de mise à jour des listes (12 heures) dans Config
- Ajout de la capacité de forcer la mise à jour dans `ListUpdateWorker` via la donnée d'entrée `KEY_FORCE_UPDATE`
- Déclenchement d'une mise à jour forcée depuis `AppUpdateReceiver` après les mises à jour de l'application

### Modifié

- Mise à jour du BOM Compose vers 2026.06.01 et de Kotlin vers 2.4.10
- Refactorisation du signalement de numéros pour utiliser le type `String` au lieu de `Long` sur toute la chaîne
- Suppression de la contrainte « batterie non faible » des requêtes de mise à jour des listes
- Simplification de la requête de health check pour envoyer un corps JSON vide
- Utilisation du premier indicatif pays uniquement pour l'en-tête `X-Country-Code`
- Assouplissement de la validation des numéros dans `ReportViewModel` (2 à 15 chiffres, accepte le préfixe +, nettoie la saisie)

### Corrigé

- Correction d'une incohérence de type de numéro de téléphone entre le client et l'API

### Supprimé

- Suppression de la data class `HealthCheckRequest`
- Suppression du texte d'instruction de saisie dans `ReportScreen`
- Suppression de la fonction utilitaire `stringToLong` de `ReportViewModel`

## [5.0.0] - 2026-06-30

### Ajouté

- Ajout d'un système de listes hiérarchique avec les entités `PatternListEntity` et `PatternListItemEntity` pour organiser les motifs de façon imbriquée
- Ajout du support de l'API v2 avec de nouveaux endpoints : listes, téléchargement des motifs de liste, et health check
- Ajout de `ListService` pour la mise à jour et la gestion des listes de motifs depuis l'API
- Ajout de `ListSyncService` pour la synchronisation bidirectionnelle entre listes locales et distantes
- Ajout de `ListPriorityService` pour trier les listes par priorité, type et nom
- Ajout de `HealthCheckWorker` pour des vérifications périodiques de l'état de l'appareil en mode organisation
- Ajout du support des restrictions MDM via une configuration `restrictions.xml` pour les déploiements en entreprise
- Ajout de nouveaux modèles d'API : `ListSummary`, `ListPatternInfo`, `HealthCheckRequest`, `HealthCheckResponse`
- Ajout de la sealed class `NetworkError` pour une gestion structurée des erreurs réseau
- Ajout d'une interface complète de gestion des listes avec les nouveaux `ListsScreen` et `PatternListSheet`, avec pagination
- Ajout de tests unitaires pour `ListPriorityService`, `ListSyncService`, `NetworkService` et `ApiModels`
- Ajout de la fonction `applyMdmRestrictions` à `PreferencesManager` pour l'injection de clé API en mode entreprise

### Modifié

- **CASSANT :** Migration du schéma de base de données de la version 1 à la 2 avec la nouvelle structure de listes hiérarchique, remplaçant le stockage à plat des motifs
- Mise à jour d'`AppDatabase` pour utiliser les nouvelles entités de liste et ajout de données initiales pour les listes utilisateur d'autorisation/blocage
- Mise à jour de `Config` pour utiliser l'URL de base de l'API v2 `app.saracroche.org` et ajout d'une constante d'intervalle de health check
- Mise à jour de `NetworkService` avec les nouveaux endpoints API v2 et une meilleure gestion des erreurs via le type `NetworkError`
- Mise à jour de `CallScreeningService` avec une meilleure gestion des erreurs et des vérifications de préférences
- Mise à jour de `SaracrocheApplication` pour planifier des health checks périodiques et lire la configuration MDM au démarrage
- Mise à jour de la navigation de `MainActivity` pour utiliser le nouvel écran de listes unifié
- Mise à jour de `PreferencesManager` avec des méthodes supplémentaires pour les indicatifs pays, la gestion de l'appareil et l'application des restrictions MDM
- Mise à jour de `PatternManager` pour utiliser le nouveau chargement de motifs basé sur les listes, avec mise en cache et invalidation correcte du cache
- Mise à jour de `PhoneNumberMatcher` avec validation des entrées vides pour éviter les plantages
- Mise à jour de la configuration de build avec de nouvelles dépendances de test (mockwebserver, coroutines-test)
- Réorganisation des fichiers d'écran : déplacement des sous-répertoires (`ui/screen/home/`, `ui/screen/report/`, `ui/screen/settings/`) vers le répertoire racine `ui/screen/`
- Mise à jour d'`AGENTS.md` pour refléter la stack technique actuelle : Kotlin 2, Compose, MVVM, Room 2 (KSP 2), WorkManager 2, DataStore 1, Gson 2, minSdk 29, targetSdk/compileSdk 37, Java 11, Gradle 9, AGP 9
- Mise à jour du wrapper Gradle de 9.5.0 à 9.6.0 et des versions de dépendances
- Suppression d'une méthode de comptage redondante dans `BlockedCallDao`

### Supprimé

- Suppression des anciennes `PatternEntity` et `PatternDao` (remplacées par le système hiérarchique `PatternListEntity`/`PatternListItemEntity`)
- Suppression des anciens écrans de liste : `APIPatternListScreen`, `MyListScreen`, et l'ancien `ListsScreen`

### Corrigé

- Ajout d'une vérification des entrées vides dans `PhoneNumberMatcher` pour éviter les plantages sur des numéros nuls/vides
- Amélioration de la gestion des erreurs dans la logique de screening d'appel avec des vérifications de préférences appropriées
- Vidage du cache de motifs après les mises à jour de liste pour garantir la cohérence entre le stockage local et l'API
- Ajout d'une vérification de la préférence de filtrage d'appel avant le traitement des appels, pour respecter les réglages utilisateur

## [4.2.0] - 2026-06-22

### Modifié

- Mise à jour de l'URL de base de l'API de `saracroche.org` vers `app.saracroche.org`
- Suppression de la constante `API_LISTS_URL` dans Config
- Mise à jour des dépendances : Compose BOM 2026.05.00 → 2026.06.00, core-ktx 1.18.0 → 1.19.0, Kotlin 2.3.21 → 2.4.0, Lifecycle ViewModel Compose 2.10.0 → 2.11.0, Material 1.13.0 → 1.14.0

## [4.1.2] - 2026-06-18

### Modifié

- Consolidation de la logique de mise à jour des listes dans une méthode unique `ListService.updateList` avec un paramètre `force`, unifiant les points d'entrée planification, worker, mise à jour d'app, réinstallation et réinitialisation
- Passage du téléchargement de liste et de la réécriture de base quand la version de liste distante n'a pas changé
- Simplification d'`updateDatabase` pour vider tous les motifs API et insérer le nouveau jeu en masse
- Suppression des boutons dupliqués « Télécharger la liste »/« Mettre à jour la liste » et de l'assistant HTTP inline de la feuille de débogage

### Corrigé

- Ignorance des appels non entrants dans `CallScreeningService` en répondant avec un `CallResponse` vide et un retour anticipé

## [4.1.1] - 2026-05-30

### Corrigé

- Correction de la mise à jour de liste pour ne supprimer que les motifs API retirés, au lieu de tous les supprimer

## [4.1.0] - 2026-05-13

### Modifié

- Mise à jour du comptage des motifs pour inclure à la fois les motifs de blocage et d'identification via la nouvelle fonction `getTotalPatternCount()`
- Renommage de `calculateTotalBlockedNumbers()` en `calculateTotalCoveredNumbers()` pour plus de clarté
- Ajout d'un gestionnaire de corruption DataStore pour les changements de format protobuf dans `PreferencesManager`
- Amélioration des messages d'erreur de validation des motifs dans `PatternService`
- Affinage du texte et de la ponctuation dans toute l'UI pour plus de clarté
- Suppression d'un echo redondant dans la commande lint du Makefile

### Corrigé

- Correction du nommage de variable de `totalPatterns` à `totalPatternCount` pour la cohérence

## [4.0.0] - 2026-05-08

### Ajouté

- Ajout du blocage de SMS via `NotificationListenerService` pour masquer les notifications de SMS provenant de numéros bloqués
- Ajout du service `SmsNotificationListener` pour surveiller et filtrer les notifications de SMS
- Ajout de l'utilitaire `SmsNumberExtractor` pour extraire le numéro de l'expéditeur depuis les extras de notification SMS
- Ajout de `SmsSettingsSheet` pour configurer le blocage des SMS et les préférences de notification
- Ajout de notifications d'identification d'appel affichant le nom du motif pour les motifs d'identification correspondants
- Ajout de `CallScreeningFailedDialog` guidant l'utilisateur pour l'activation manuelle du screening d'appel
- Ajout de nouveaux canaux de notification pour les SMS bloqués et les appels identifiés
- Ajout d'un interrupteur de notification pour les SMS bloqués dans les préférences
- Ajout de `PhoneNumberMatcher.findMatchingIdentifyPattern` pour la correspondance des motifs d'identification
- Ajout de la police Atkinson Hyperlegible
- Ajout d'un Makefile pour lancer ktlint
- Ajout de tests unitaires `SmsNumberExtractorTest`

### Modifié

- **CASSANT :** Renommage de la préférence de filtrage de `filtering_enabled` à `call_filtering_enabled` pour séparer le filtrage des appels et des SMS
- Montée de version de Kotlin de 2.2.20 à 2.3.21
- Montée de version de Gradle de 9.3.1 à 9.5.0 et d'AGP de 9.1.0 à 9.2.1
- Montée de version de compileSdk et targetSdk de 36 à 37
- Montée de version des dépendances : Gson 2.13.2 → 2.14.0, Compose BOM 2026.03.01 → 2026.05.00, Navigation Compose 2.9.7 → 2.9.8
- Séparation du filtrage d'appel et du blocage de SMS en interrupteurs indépendants dans les préférences
- Déplacement du téléchargement de liste au lancement, d'Application vers MainActivity
- Ajout d'une exigence de charge à la contrainte de mise à jour WorkManager en arrière-plan
- Réorganisation de la structure des packages UI : déplacement des écrans vers `ui/screen/` et des viewmodels vers `ui/viewmodel/`
- Renommage de `BusinessCodeSheet` en `BusinessSheet` et d'`AdvancedSettingsSheet` en `CallSettingsSheet`
- Mise à jour d'AGENTS.md avec la documentation complète de la stack technique
- Mise à jour du README.md avec la fonctionnalité de blocage SMS et la stack technique étendue
- Déplacement des fichiers d'icône de lancement de `mipmap-anydpi-v26` vers `mipmap-anydpi`
- Suppression de `font_certs.xml` (plus nécessaire)
- Mise à jour de `.editorconfig` avec une règle de nommage de fonction ktlint

## [3.1.0] - 2026-04-29

### Ajouté

- Ajout d'une action de clic sur les notifications pour ouvrir MainActivity

### Modifié

- Masquage des appels bloqués du journal d'appels et des notifications
- Mise à jour du drawable vectoriel de l'icône de notification
- Déplacement de la vérification de la permission de notification vers un `remember` state

### Corrigé

- Correction d'une coquille dans l'écran de signalement

## [3.0.2] - 2026-04-26

### Ajouté

- Rafraîchissement automatique des listes de blocage après mise à jour de l'app via le broadcast receiver `MY_PACKAGE_REPLACED`

### Modifié

- Déplacement de la planification de mise à jour des listes de MainActivity vers la classe Application pour une exécution au démarrage plus fiable
- Simplification du texte d'appels bloqués vides et clarification du libellé des numéros masqués pour inclure les numéros privés

## [3.0.1] - 2026-04-19

### Corrigé

- Utilisation de `WindowInsets.systemBars` au lieu de `WindowInsets.statusBars` dans toutes les bottom sheets pour bien prendre en compte les insets de la barre de navigation

## [3.0.0] - 2026-04-17

### Ajouté

- Ajout d'une base de données Room pour le stockage local des motifs et des appels bloqués (`AppDatabase`, `PatternEntity`, `PatternDao`, `BlockedCallEntity`, `BlockedCallDao`)
- Ajout d'un onglet Listes dans la navigation basse pour parcourir les motifs API et gérer des motifs personnalisés
- Ajout de la gestion des motifs utilisateur avec des parcours ajout/validation/suppression, incluant la détection de chevauchement et de doublon
- Ajout de notifications d'appels bloqués avec des canaux dédiés pour les appels bloqués connus et inconnus
- Ajout d'un historique des appels bloqués dans la feuille d'info, avec numéro, horodatage et action « tout effacer » par appel
- Ajout de mises à jour automatiques des listes via un téléchargement périodique en arrière-plan avec WorkManager
- Ajout d'une feuille de réglages avancés accessible depuis l'écran d'accueil pour le filtrage et le mode contacts uniquement
- Ajout d'une feuille de débogage avec les actions forcer la mise à jour, télécharger la liste, vider la base et réinitialiser les préférences
- Ajout de la gestion de la permission de notification pour les appels bloqués (dialogue explicatif Android 13+)
- Ajout d'un dialogue d'échec de screening d'appel guidant l'utilisateur pour l'activation manuelle
- Ajout d'un délai de rejet pour la carte de don, la masquant pendant une période configurable après fermeture

### Modifié

- **CASSANT :** Migration du stockage des motifs d'un JSON embarqué vers une base de données Room (les motifs sont désormais téléchargés depuis l'API et persistés localement)
- **CASSANT :** Changement de la gestion des numéros de `String` à `Long` sur toute la chaîne de screening d'appel, pour de meilleures performances
- Refonte de l'écran d'accueil avec des contrôles de protection inline et une feuille d'info affichant l'historique des appels bloqués et le statut des mises à jour en arrière-plan
- Simplification de l'écran de réglages en déplaçant les interrupteurs de filtrage, blocage anonyme et contacts uniquement vers l'écran d'accueil/la feuille avancée
- Migration du téléchargement de liste vers un endpoint API au lieu d'un fichier asset embarqué
- Remplacement de `BlockedPatternManager` par `PatternManager` reposant sur Room
- Extension de `Config` avec l'URL de base de l'API, l'endpoint de liste, les intervalles de mise à jour arrière-plan/liste, et l'intervalle de rejet du don
- Planification des mises à jour périodiques de liste via WorkManager dans `MainActivity`, avec déclenchement du téléchargement initial au premier lancement
- Montée de version de targetSdk/compileSdk de 36 à 37
- Montée de version des dépendances : AGP 9.1.1, Compose BOM 2026.03.01, Activity 1.13.0, Lifecycle 2.10.0, Navigation 2.9.7, DataStore 1.2.1, Room 2.8.4, WorkManager 2.11.2
- Remplacement du plugin `kotlin-android` par KSP pour le traitement d'annotations Room
- Conversion du nommage des composables UI de PascalCase à camelCase (ex. `HomeScreen()` → `homeScreen()`)
- Mise à jour de `PhoneNumberMatcher` pour utiliser `Long` en interne et ajout de `generateVariants()` pour la correspondance multi-préfixe

### Supprimé

- Suppression de `BlockedPatternManager.kt` (remplacé par `PatternManager` avec Room)
- Suppression de l'asset embarqué `french-list-arcep-operators.json` (les motifs sont désormais téléchargés depuis l'API)

## [2.8.0] - 2026-03-25

### Ajouté

- Ajout du support multi-pays des préfixes pour la normalisation des numéros sur les appareils multi-SIM
- Ajout de la classe utilitaire `PhoneNumberMatcher` pour centraliser le traitement des numéros, avec une normalisation et une correspondance de motifs améliorées
- Refactorisation de la récupération de l'identifiant d'appareil pour utiliser des identifiants générés par l'app plutôt que spécifiques à l'appareil, pour une meilleure confidentialité

### Modifié

- Mise à jour de `CallScreeningService` pour utiliser la nouvelle API `PhoneNumberMatcher` avec traitement centralisé
- Amélioration de la logique de correspondance des motifs à joker avec un meilleur nettoyage des numéros

### Corrigé

- Correction d'une divergence de correspondance de motifs entre les implémentations de production et de test, en unifiant la logique dans `PhoneNumberMatcher`

## [2.7.0] - 2026-03-20

### Ajouté

- Ajout d'un bouton de fermeture sur la feuille de don avec l'option « Plus tard, non merci »

### Modifié

- Mise à jour de la description des appels bloqués dans la feuille d'info pour préciser qu'ils apparaissent dans le journal d'appels du téléphone avec le symbole 🚫
- Mise à jour de la présentation et de la structure du projet dans AGENTS.md

## [2.6.0] - 2026-02-27

### Ajouté

- Ajout d'un interrupteur global pour activer/désactiver le filtrage d'appel dans les réglages
- Ajout d'une option de filtrage « contacts uniquement » pour bloquer tous les appels hors contacts
- Ajout d'une feuille de saisie de code entreprise pour la protection de flotte professionnelle (gestion centralisée, listes d'autorisation personnalisées, reporting, déploiement MDM, mises à jour automatiques)
- Ajout d'un affichage de statistiques de blocage d'appel avec total des numéros bloqués, motifs bloqués et réglages de filtrage actuels
- Ajout d'une feuille d'info affichant les statistiques et réglages de blocage d'appel
- Ajout d'une feuille de code entreprise pour l'activation du code entreprise

### Modifié

- Remplacement de `blocked-patterns.json` par `french-list-arcep-operators.json`, avec un format JSON structuré incluant métadonnées et champ `action`
- Réorganisation de l'écran de réglages avec interrupteur de filtrage global, interrupteur contacts uniquement, et élément d'action code entreprise
- Amélioration de l'écran d'accueil avec statistiques de motifs bloqués intégrées, bouton d'info, et titre de carte de screening d'appel mis à jour
- Changement de l'icône de signalement de `Icons.Rounded.Report` à `Icons.Rounded.Campaign`
- Amélioration de la carte de permission de screening d'appel avec le nombre de numéros bloqués
- Suppression du bouton GitHub Sponsors de la feuille de don, et mise en avant du bouton Liberapay en pleine largeur avec icône Euro
- Déplacement de la feuille de don vers le package `com.cbouvat.android.saracroche.ui.sheet` et suppression des imports inutilisés
- Mise à jour de la documentation : suppression de la référence obsolète à JAVA_HOME, mise à jour du lien du dépôt iOS et du lien de don PayPal
- Mise à jour des liens de dépôt vers Codeberg dans CONTRIBUTING.md et SettingsScreen

### Supprimé

- Suppression de la section contact de l'écran de réglages
- Suppression du signalement de bug par e-mail
- Suppression du bouton de don GitHub Sponsors et de `.github/FUNDING.yml`
- Suppression du composant séparé `BlockedPatternsStatsCard` (intégré à la carte de screening d'appel)
- Suppression de `app/src/main/assets/blocked-patterns.json` (remplacé par `french-list-arcep-operators.json`)

## [2.5.0] - 2025-12-25

### Modifié

- Suppression de 23 entrées d'opérateur BICS dans blocked-patterns.json
- Mise à jour du README pour annoncer la disponibilité sur F-Droid avec lien de téléchargement direct
- Remplacement du lien GitHub Sponsors par le lien de soutien saracroche.org dans le README
- Suppression de la section de documentation des sources de données ARCEP du README, remplacée par une référence au fichier JSON local
- Mise à jour de versionCode à 21 et versionName à 2.5.0

## [2.4.0] - 2025-11-11

### Modifié

- Restructuration de blocked-patterns.json avec des champs de plage numérique `start`/`end`, ajout de 26 nouveaux opérateurs, suppression de 3 opérateurs (baisse nette de 859 à 827 entrées)
- Mise à jour des captures d'écran du téléphone
- Mise à jour de versionCode à 20 et versionName à 2.4.0

## [2.3.0] - 2025-11-11

### Modifié

- Migration de l'API de signalement vers `saracroche.org/api` avec un nouveau contrat (`phone: Long`, `device_id`, en-tête `Accept`)
- Mise à jour des URLs du site de `cbouvat.com/saracroche` vers `saracroche.org`
- Changement du lien de don Stripe de `buy.stripe.com` à `donate.stripe.com`
- Ajout d'un lien de don Stripe personnalisé dans `.github/FUNDING.yml`
- Ajout d'une note d'usage France uniquement dans les métadonnées de l'app
- Mise à jour du fichier LICENSE pour corriger le texte de la GNU General Public License v3.0
- Mise à jour de versionCode à 19 et versionName à 2.3.0

### Supprimé

- Suppression de la validation des numéros français à 12 ou 16 chiffres dans `ReportViewModel`

## [2.2.0] - 2025-10-22

### Ajouté

- Ajout du support des numéros français à 12 et 16 caractères (incluant le préfixe `+33`)

### Modifié

- Renommage de `.github/copilot-instructions.md` en `AGENTS.md` avec un titre mis à jour
- Changement de terminologie dans SettingsScreen : « anonymes » devient « masqués » puis « privés »
- Suppression des imports d'icônes inutilisés `AddComment` et `Help`, et reformatage de l'intent de la politique de confidentialité
- Suppression du commentaire TODO du XML des règles d'extraction de données
- Mise à jour de versionCode à 18 et versionName à 2.2.0

## [2.1.0] - 2025-10-22

### Ajouté

- Ajout de liens web externes pour l'aide et la politique de confidentialité dans SettingsScreen (en remplacement de l'écran d'aide intégré HelpScreen)
- Ajout d'une section « Contact » dans SettingsScreen avec liens e-mail et Mastodon
- Ajout de la configuration `dependenciesInfo` pour exclure les dépendances de l'APK et du bundle

### Modifié

- Mise à jour des icônes dans SettingsScreen : blocage d'appel anonyme vers `Icons.Rounded.PhoneDisabled`, contacter le développeur vers `Icons.Rounded.Mail`, nouvelles icônes aide et confidentialité
- Déplacement du texte de pied de page « Bisou » et de la fonction `openPlayStore()` de HelpScreen vers SettingsScreen
- Refactorisation des imports, des espaces et du formatage dans plusieurs fichiers
- Mise à jour de `distributionSha256Sum` dans gradle-wrapper.properties
- Mise à jour de versionCode à 17 et versionName à 2.1.0

### Supprimé

- Suppression du fichier HelpScreen et de l'onglet de navigation « Aide » (passage de 4 à 3 onglets)
- Suppression des imports de coroutines inutilisés dans CallScreeningService

## [2.0.0] - 2025-10-22

### Ajouté

- Ajout du blocage des appels anonymes/privés avec des préférences basées sur DataStore et un interrupteur
- Ajout de `datastore-preferences` 1.1.7 au catalogue de versions

### Modifié

- Retour en arrière de la version d'AGP de 8.13.0 à 8.11.1 dans libs.versions.toml
- Mise à jour de versionCode à 16 et versionName à 2.0.0

### Corrigé

- Ajout du wrapper `platform()` manquant pour la dépendance de test Compose BOM

### Supprimé

- Suppression de la fonction composable `SettingsSection` de SettingsScreen

## [1.9.0] - 2025-10-22

### Modifié

- Migration des options du compilateur Kotlin du bloc `kotlinOptions` vers la syntaxe DSL `kotlin { compilerOptions {} }`
- Mise à jour du wrapper Gradle vers la version 9.0.0 avec de nouvelles propriétés de sécurité (`networkTimeout`, `validateDistributionUrl`, `distributionSha256Sum`)
- Mise à jour de `gradlew` et `gradlew.bat` pour la conformité POSIX et une meilleure gestion des erreurs (dans le cadre de la montée de version Gradle 9.0.0)
- Mise à jour de versionCode à 15 et versionName à 1.9.0

### Supprimé

- Suppression d'`androidx-ui-test-junit4` de la configuration de build

## [1.8.0] - 2025-09-18

### Ajouté

- Ajout de fichiers de métadonnées d'application en anglais et en français (descriptions, titres, captures d'écran)
- Ajout d'informations sur la disponibilité F-Droid dans le README
- Ajout de `blocked-patterns.json` avec un format basé sur des motifs (champs `operator_name`, `pattern` avec jokers `#`)
- Ajout de la classe utilitaire `BlockedPatternManager` pour charger et faire correspondre les motifs bloqués

### Modifié

- Réécriture de la logique de screening d'appel, passant d'une correspondance de préfixe à une correspondance de motifs à joker, avec les méthodes `normalizePhoneNumber()` et `matchesPattern()`
- Changement de `setSkipNotification` de `true` à `false` : les appels bloqués génèrent désormais des notifications
- Remplacement de l'icône Send par l'icône AddAlert dans ReportScreen
- Simplification des instructions de compilation et suppression de la section de configuration dans le README
- Mise à jour des versions de bibliothèques dans libs.versions.toml
- Suppression d'`appVersion` de `ReportRequest` et de la logique associée dans `NetworkService`
- Remplacement du fichier de configuration d'exemple (`Config.kt.example`) par le vrai `Config.kt`, et mise à jour de `.gitignore`
- Mise à jour de l'adresse e-mail de contact dans le Code de conduite, la Politique de sécurité, HelpScreen et SettingsScreen
- Mise à jour de versionCode à 14 et versionName à 1.8.0

### Supprimé

- Suppression de `blocked-prefixes.json` et de `BlockedPrefixManager` (remplacés par le système basé sur les motifs)

## [1.7.0] - 2025-09-18

### Ajouté

- Ajout du bouton de notation « Noter l'application » dans DonationSheet

### Modifié

- Remplacement du composable réutilisable `DonationButton` par des boutons inline utilisant les icônes `CreditCard` et `Wallet`, changement du texte « Carte bancaire » en « Carte bancaire & Google Pay »
- Traduction de tous les commentaires inline de NetworkService, du français vers l'anglais
- Mise à jour de la description des appels bloqués dans HomeScreen au présent
- Mise à jour de versionCode à 13 et versionName à 1.7.0

## [1.6.0] - 2025-09-18

### Ajouté

- Ajout d'une option de paiement carte bancaire/Stripe dans DonationSheet
- Ajout d'un bouton de notation de l'app dans DonationSheet
- Ajout du composable `SupportSection` avec boutons e-mail et issue GitHub dans HelpScreen
- Ajout d'entrées FAQ pour le service 33700, la recherche d'opérateur ARCEP, et le dépannage dans HelpScreen

### Modifié

- Réécriture du système de couleurs dans `Color.kt` avec de nouvelles variables de couleur de base
- Désactivation du thème dynamique Material You (`dynamicColor` mis à `false`)
- Restructuration de la mise en page de HomeScreen et mise à jour du contenu
- Simplification de la mise en page de ReportScreen, suppression des cartes Service 33700 et recherche opérateur ARCEP, changement de l'icône vers `Send`
- Réorganisation des sections de SettingsScreen et renommage des libellés
- Refonte de l'en-tête de DonationSheet, agrandissement de l'icône cœur, et réordonnancement des boutons
- Refonte des éléments FAQ de HelpScreen et remplacement de la section de signalement de bug par SupportSection
- Inlining d'`AppNavigation` dans `SaracrocheApp` et suppression des `WindowInsets` inutilisés
- Mise à jour de versionCode à 12 et versionName à 1.6.0

### Supprimé

- Suppression de la carte Service 33700 et de la carte de recherche opérateur ARCEP dans ReportScreen
- Suppression du composable `AppNavigation` (inliné dans `SaracrocheApp`)

## [1.5.0] - 2025-09-18

### Ajouté

- Activation de la minification du code (`isMinifyEnabled`) et de la réduction des ressources (`isShrinkResources`) pour les builds release
- Ajout de la règle ProGuard `-keep class com.cbouvat.android.** { *; }` pour empêcher la suppression de classes
- Mise à jour de versionCode à 11 et versionName à 1.5.0

### Supprimé

- Suppression d'`applicationIdSuffix = ".debug"` du type de build debug

## [1.4.0] - 2025-09-18

### Ajouté

- Ajout des préfixes bloqués de l'opérateur COCR dans blocked-prefixes.json

### Modifié

- Migration des déclarations de dépendances inline dans `build.gradle.kts` vers des références au catalogue de versions dans `gradle/libs.versions.toml`
- Mise à jour de l'Android Gradle Plugin de 8.11.1 à 8.12.0
- Désactivation de la minification (`isMinifyEnabled`, `isShrinkResources`) et ajout d'une configuration de type de build debug
- Suppression de la ligne commentée pour les configurations sensibles dans Config.kt.example
- Mise à jour de versionCode à 8 et versionName à 1.4.0

## [1.3.3] - 2025-09-18

### Modifié

- Refactorisation du placement de SupportProjectCard dans HomeScreen pour une meilleure lisibilité
- Suppression de l'import `IconButton` inutilisé dans HomeScreen
- Mise à jour de versionCode à 7 et versionName à 1.3.3

## [1.2.0] - 2025-09-18

### Ajouté

- Ajout du composable `SupportProjectCard` dans HomeScreen avec option de don
- Ajout d'un élément d'aide à propos de Sarah (« Pourquoi une patte d'ours ? ») dans HelpScreen
- Ajout d'un point d'interrogation au titre confidentialité dans HelpScreen

### Modifié

- Réduction du padding bas de 64dp à 32dp et ajout d'un spacer de 64dp dans SettingsScreen et HelpScreen pour éviter la coupure de contenu
- Mise à jour du README.md avec le bon lien iOS et suppression des sections françaises pour plus de clarté
- Mise à jour de versionCode à 3 et versionName à 1.2.0

### Supprimé

- Suppression du bouton icône cœur de la barre supérieure de HomeScreen (remplacé par SupportProjectCard)

## [1.1.0] - 2025-09-18

### Ajouté

- Ajout d'une note de disponibilité iOS dans README.md

### Modifié

- Mise à jour du README.md pour refléter le statut de disponibilité actuel sur Google Play Store avec lien de téléchargement
- Mise à jour de versionName à 1.1.0

## [1.0.0] - 2025-09-18

### Ajouté

- Sortie de la première version stable de l'application Android de blocage d'appel Saracroche
- Ajout de `CallScreeningService` avec `BlockedPrefixManager` pour le chargement de préfixes basé sur JSON et la correspondance de motifs
- Ajout des composants UI HomeScreen, SettingsScreen, ReportScreen, HelpScreen et DonationSheet
- Ajout de `NetworkService`, `ApiModels` et `ReportViewModel` pour le signalement de numéros
- Ajout d'un thème Material 3 personnalisé avec intégration de Google Fonts
- Ajout de `PermissionUtils` pour la gestion de la permission de screening d'appel
- Ajout d'un premier jeu de préfixes de démarchage téléphonique français dans `blocked-prefixes.json`
- Ajout de l'infrastructure du projet : LICENSE, README, CODE_OF_CONDUCT, CONTRIBUTING, SECURITY, FUNDING.yml
- Ajout de `CallScreeningLogicTest` pour la validation de la logique de blocage d'appel
- Ajout de `Config.kt.example` pour la gestion de configuration sensible
