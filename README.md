# DashCam View — Application Android

Application Android native pour accéder à votre dashcam via WiFi,
visualiser le flux en direct et enregistrer les vidéos localement sur votre téléphone.

---

## Fonctionnalités

| Fonction | Description |
|---|---|
| **Vue en direct** | Flux live RTSP / HTTP MJPEG / HLS (m3u8) via ExoPlayer |
| **Enregistrement local** | Sauvegarde du flux en MP4 dans `Vidéos/Dashcam/` sur le téléphone |
| **Galerie SD** | Parcourir et télécharger les vidéos de la carte SD de la caméra |
| **Galerie locale** | Lire, partager et supprimer les enregistrements sur le téléphone |
| **Presets dashcam** | Viofo, BlackVue, modèles génériques pré-configurés |
| **Détection WiFi** | Détecte automatiquement le réseau et suggère l'IP de la caméra |

---

## Structure du projet

```
DashcamApp/
├── app/src/main/
│   ├── java/com/dashcam/app/
│   │   ├── activities/
│   │   │   ├── SplashActivity.java         Écran de démarrage
│   │   │   ├── MainActivity.java           Connexion WiFi + accueil
│   │   │   ├── LiveViewActivity.java       Vue en direct + enregistrement
│   │   │   ├── GalleryActivity.java        Vidéos sur carte SD caméra
│   │   │   ├── LocalGalleryActivity.java   Enregistrements locaux téléphone
│   │   │   └── SettingsActivity.java       Paramètres avancés
│   │   ├── models/
│   │   │   ├── DashcamConfig.java          Configuration caméra
│   │   │   └── VideoFile.java              Modèle fichier vidéo SD
│   │   ├── services/
│   │   │   ├── RecordingService.java       Service enregistrement (foreground)
│   │   │   └── DashcamConnectionService.java
│   │   └── utils/
│   │       ├── DashcamApiClient.java       Client HTTP multi-protocole
│   │       ├── PrefsManager.java           Persistance configuration
│   │       ├── WifiHelper.java             Utilitaires réseau WiFi
│   │       ├── FileDownloadManager.java    Téléchargements avec progress
│   │       ├── StreamRecorder.java         Enregistrement flux HTTP
│   │       └── ConnectionStatusView.java   Vue indicateur signal
│   └── res/
│       ├── layout/    7 fichiers XML
│       ├── drawable/  15 icônes vectorielles
│       ├── values/    colors, strings, themes, arrays
│       └── xml/       network_security_config.xml
```

---

## Installation

### Prérequis
- **Android Studio** Hedgehog (2023.1) ou supérieur
- **JDK 8** ou supérieur
- Android SDK API 21+ (Android 5.0 Lollipop minimum)

### Étapes

1. **Ouvrir le projet**
   ```
   Android Studio → File → Open → sélectionner le dossier DashcamApp/
   ```

2. **Synchroniser Gradle**
   Android Studio télécharge automatiquement les dépendances.
   Si erreur : `File → Sync Project with Gradle Files`

3. **Compiler et installer**
   - Brancher le téléphone en USB avec le débogage USB activé
   - Cliquer `▶ Run` ou `Shift+F10`

4. **Générer un APK**
   ```
   Build → Build Bundle(s) / APK(s) → Build APK(s)
   ```
   L'APK se trouve dans : `app/build/outputs/apk/debug/app-debug.apk`

---

## Configuration selon votre dashcam

### Étape 1 — Connecter le WiFi
Sur Android : **Paramètres → WiFi → sélectionner le réseau de la dashcam**
(Le SSID contient souvent le nom de la marque, ex: `VIOFO_A119`, `BlackVue_XXXX`)

### Étape 2 — Trouver l'IP et le flux

| Marque | IP par défaut | Port RTSP | Chemin flux |
|---|---|---|---|
| **Viofo** (A119, A229…) | `192.168.1.254` | `554` | `/live` |
| **BlackVue** (DR900X…) | `10.99.77.1` | `7070` | `/blackvue_live.m3u8` |
| **70mai** | `192.168.0.1` | `554` | `/live/ch00_0` |
| **Nextbase** | `192.168.0.1` | `554` | `/live` |
| **Thinkware** | `192.168.0.1` | `554` | `/stream1` |
| **DDPai** | `192.168.0.1` | `8080` | `/` (HTTP MJPEG) |
| **Générique** | `192.168.1.1` | `554` | `/live/channel0` |

### Étape 3 — Utiliser l'URL personnalisée (si les presets ne fonctionnent pas)
Dans `Paramètres avancés`, entrez l'URL complète, exemples :
```
rtsp://192.168.1.254:554/live
rtsp://admin:admin@192.168.1.1:554/stream
http://192.168.0.1:8080/?action=stream
http://192.168.1.1/live.m3u8
```

---

## Protocoles supportés

| Protocole | Usage | Latence |
|---|---|---|
| **RTSP** | Dashcams standard (Viofo, 70mai…) | ~1-3 sec |
| **HTTP MJPEG** | Vieilles dashcams, DDPai | ~0.5-2 sec |
| **HLS (m3u8)** | BlackVue, certains modèles récents | ~3-10 sec |

---

## Enregistrement local

- Les vidéos sont sauvegardées dans **`Vidéos/Dashcam/`** sur le téléphone
- Nommage automatique : `DASHCAM_YYYYMMDD_HHMMSS.mp4`
- Une notification persistante s'affiche pendant l'enregistrement
- L'enregistrement continue en arrière-plan si vous minimisez l'app
- Accès via `Mes enregistrements locaux` dans l'écran d'accueil

---

## Dépendances principales

```gradle
androidx.media3:media3-exoplayer         1.3.1   Lecture flux vidéo
androidx.media3:media3-exoplayer-rtsp    1.3.1   Support RTSP
androidx.media3:media3-exoplayer-hls     1.3.1   Support HLS
com.squareup.okhttp3:okhttp              4.12.0  Requêtes HTTP
com.github.bumptech.glide:glide          4.16.0  Miniatures vidéo
com.google.code.gson:gson               2.10.1  Config JSON
```

---

## Permissions requises

| Permission | Raison |
|---|---|
| `INTERNET` | Connexion réseau WiFi |
| `ACCESS_WIFI_STATE` | Détection réseau dashcam |
| `WRITE_EXTERNAL_STORAGE` | Sauvegarde vidéos (Android ≤9) |
| `READ_MEDIA_VIDEO` | Accès galerie (Android 13+) |
| `FOREGROUND_SERVICE` | Enregistrement en arrière-plan |
| `POST_NOTIFICATIONS` | Notification d'enregistrement |

---

## Dépannage

**Connexion impossible**
- Vérifiez que le WiFi du téléphone est connecté au réseau de la dashcam
- Désactivez les données mobiles temporairement
- Essayez de pinger l'IP depuis un navigateur : `http://192.168.1.1`

**Flux vidéo qui ne s'affiche pas**
- Essayez un autre protocole dans Paramètres (RTSP → HTTP → HLS)
- Entrez l'URL complète dans le champ "URL personnalisée"
- Consultez le manuel de votre dashcam pour l'URL exacte du flux

**Enregistrement vide ou corrompu**
- Les flux RTSP ne peuvent pas toujours être copiés directement en HTTP
- Utilisez le protocole HTTP MJPEG si disponible pour l'enregistrement

---

## Licence
Projet à usage personnel. Toutes les marques citées (Viofo, BlackVue, etc.)
appartiennent à leurs propriétaires respectifs.
