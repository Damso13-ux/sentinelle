# Sentinelle

## Description

Sentinelle est une application Android qui vous protège des appels et SMS indésirables. Le projet a démarré comme un fork de [Saracroche](https://codeberg.org/cbouvat/saracroche-android) et évolue vers une application de protection locale plus large : blocage des appels/SMS indésirables, tableau de bord statistique & historique local, et un moteur de scoring de spam heuristique (à base de règles) fonctionnant entièrement sur l'appareil, conçu pour accueillir plus tard un modèle de machine learning entraîné localement.

## Origine & Remerciements

Sentinelle est une œuvre dérivée de **Saracroche**, créée par [cbouvat](https://codeberg.org/cbouvat) et publiée sous licence GNU General Public License v3.0. Le projet original est disponible sur [codeberg.org/cbouvat/saracroche-android](https://codeberg.org/cbouvat/saracroche-android) (également sur [Google Play](https://play.google.com/store/apps/details?id=com.cbouvat.android.saracroche) et [F-Droid](https://f-droid.org/en/packages/com.cbouvat.android.saracroche/)).

Sentinelle a forké le code de Saracroche à la version 5.1.1. Voir le fichier [NOTICE](NOTICE) pour un résumé des changements apportés depuis le fork. Un grand merci à cbouvat et aux contributeurs de Saracroche pour le travail original sur lequel ce projet s'appuie.

## Fonctionnalités

- 🛡️ Bloque automatiquement les numéros indésirables
- 💬 Bloque les SMS indésirables
- 📊 Tableau de bord statistique & historique local des appels/SMS bloqués
- 🧠 Scoring de spam heuristique sur l'appareil (aucune donnée ne quitte l'appareil)
- 📱 Application Android native
- 🔒 Respect de la vie privée : rien n'est envoyé hors de l'appareil ; tout historique local utilisé pour le scoring est opt-in et reste sur l'appareil
- 🔄 Mises à jour régulières de la base de numéros

## Installation

Sentinelle n'est pas encore publiée sur un store d'applications. Pour l'instant, il faut la compiler depuis les sources :

### Compilation depuis les sources

1. Cloner le dépôt
2. Ouvrir le projet dans Android Studio
3. Synchroniser le projet avec les fichiers Gradle
4. Compiler et lancer le projet sur votre appareil ou un émulateur

**Prérequis :**

- Android Studio
- Android SDK niveau API 29 ou supérieur
- Gradle

## Stack technique

- **Kotlin** - Langage de programmation principal
- **Jetpack Compose** - Boîte à outils UI moderne
- **Architecture MVVM** - Séparation claire des responsabilités
- **Android Call Screening API** - Pour le blocage d'appels
- **Room** - Bibliothèque de persistance
- **WorkManager** - Planification des tâches en arrière-plan
- **DataStore** - Stockage des données
- **Gson** - Parsing JSON

## Contribuer

Les contributions sont les bienvenues ! Voici comment participer :

1. Forker le dépôt
2. Créer une nouvelle branche (`git checkout -b feature/ma-super-fonctionnalite`)
3. Commiter vos changements (`git commit -m "Ajout d'une super fonctionnalité"`)
4. Pousser la branche (`git push origin feature/ma-super-fonctionnalite`)
5. Ouvrir une Pull Request

## Licence

Ce projet est distribué sous licence GNU General Public License v3.0 - voir le fichier [LICENSE](LICENSE) pour les détails. En tant qu'œuvre dérivée de Saracroche, il reste intégralement sous licence GPLv3 ; voir [NOTICE](NOTICE) pour les détails d'attribution.
