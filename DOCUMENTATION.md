# ELTV — Documentation Technique Détaillée

> **Version :** 1.0 · **Package :** `com.megaiptv.eltv` · **Plateforme :** Android TV (Leanback)

---

## Table des matières

1. [Vue d'ensemble](#1-vue-densemble)
2. [Configuration de build](#2-configuration-de-build)
3. [Architecture générale](#3-architecture-générale)
4. [Cycle de vie de l'application](#4-cycle-de-vie-de-lapplication)
5. [Activités](#5-activités)
6. [Fragments](#6-fragments)
7. [Modèles de données & Base de données](#7-modèles-de-données--base-de-données)
8. [Couche réseau](#8-couche-réseau)
9. [Lecteur média](#9-lecteur-média)
10. [Mini-player](#10-mini-player)
11. [Gestion des thèmes](#11-gestion-des-thèmes)
12. [Parseur M3U & Catégorisation](#12-parseur-m3u--catégorisation)
13. [Présentateurs (Presenters)](#13-présentateurs-presenters)
14. [Permissions & Manifest](#14-permissions--manifest)
15. [Dépendances](#15-dépendances)
16. [Flux de données principaux](#16-flux-de-données-principaux)

---

## 1. Vue d'ensemble

**ELTV** est une application **IPTV native Android TV** développée en Java.  
Elle permet à l'utilisateur de :

| Fonctionnalité | Description |
|---|---|
| 📺 Naviguer les chaînes IPTV | Interface Leanback organisée par catégories |
| 🔍 Rechercher une chaîne | Recherche full-text sur le nom de la chaîne |
| ▶️ Lire un flux vidéo | ExoPlayer avec support HTTP, HTTPS, HLS, RTMP, RTSP |
| ⭐ Gérer les favoris | Ajout/suppression depuis la page de détails |
| ⚙️ Configurer des sources M3U | Ajout d'une URL M3U + synchronisation |
| 🎨 Choisir un thème visuel | Midnight / Forest / Purple |
| 🖥️ Mini-player | Lecture continue lors de la navigation |

L'application est **exclusivement orientée Android TV** (`android.software.leanback` requis) et nécessite **Android 13 (API 33)** minimum.

---

## 2. Configuration de build

### 2.1 Identifiants & versions

| Paramètre | Valeur |
|---|---|
| `applicationId` | `com.megaiptv.eltv` |
| `namespace` | `com.megaiptv.eltv` |
| `minSdk` | **33** (Android 13) |
| `targetSdk` | **36** |
| `compileSdk` | **36.1** (`release(36) { minorApiLevel = 1 }`) |
| `versionCode` | `1` |
| `versionName` | `"1.0"` |

### 2.2 Options de compilation

```kotlin
compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
```

Le code source et la cible sont **Java 11**.

### 2.3 Build Types

| Type | Minification | ProGuard |
|---|---|---|
| `debug` | ❌ Non | — |
| `release` | ❌ Non (`isMinifyEnabled = false`) | `proguard-android-optimize.txt` + `proguard-rules.pro` |

> ⚠️ La minification est désactivée en release. À activer avant une mise en production.

### 2.4 Dépôts (settings.gradle.kts)

```
pluginManagement  → Google, MavenCentral, Gradle Plugin Portal
dependencies      → Google, MavenCentral
```

`RepositoriesMode.FAIL_ON_PROJECT_REPOS` est activé : les dépôts ne peuvent être déclarés qu'au niveau central.

### 2.5 JVM Gradle

```ini
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
```

Heap maximale du daemon Gradle : **2 Go**.

### 2.6 Versions des bibliothèques (libs.versions.toml)

| Bibliothèque | Version |
|---|---|
| Android Gradle Plugin | **9.2.1** |
| Leanback | **1.2.0** |
| Glide | **4.16.0** |
| Room | **2.6.1** |
| Media3 / ExoPlayer | **1.4.1** |
| OkHttp | **4.12.0** |

---

## 3. Architecture générale

```
┌──────────────────────────────────────────────────────────────────┐
│                        ELTVApplication                           │
│  (surveille le cycle de vie global, arrête le stream si bg)      │
└───────────────────┬──────────────────────────────────────────────┘
                    │
        ┌───────────▼─────────────┐
        │       BaseActivity      │  ← mini-player + touches média
        └──┬──────┬───────┬───────┘
           │      │       │
    MainActivity  │  DetailsActivity
    (BrowseFragment) │  (VideoDetailsFragment)
                  │
         SearchActivity   SettingsActivity   PlaybackActivity
         (SearchFragment) (SettingsFragment) (PlaybackVideoFragment)

                    ┌──────────────────────────────┐
                    │     PlayerManager (singleton) │
                    │   ExoPlayer + OkHttp          │
                    └──────────────────────────────┘

                    ┌──────────────────────────────┐
                    │     AppDatabase (Room)        │
                    │  channels · sources           │
                    └──────────────────────────────┘

                    ┌──────────────────────────────┐
                    │     NetworkUtils (OkHttp)     │
                    │  SSL trust-all · timeout 30s  │
                    └──────────────────────────────┘

                    ┌──────────────────────────────┐
                    │     M3UParser                 │
                    │  parse + catégorisation auto  │
                    └──────────────────────────────┘

                    ┌──────────────────────────────┐
                    │     ThemeManager (singleton)  │
                    │  Midnight · Forest · Purple   │
                    └──────────────────────────────┘
```

Le pattern dominant est un **singleton métier** pour les composants transversaux (`PlayerManager`, `ThemeManager`) combiné à un **DAO Room** pour la persistance.

---

## 4. Cycle de vie de l'application

### ELTVApplication

`ELTVApplication` étend `Application` et enregistre un `ActivityLifecycleCallbacks` global.

**Logique de comptage :**

| Événement | `startedCount` | Action |
|---|---|---|
| `onActivityStarted` | +1 | — |
| `onActivityStopped` (sans changement de config) | -1 (min 0) | Si `== 0` → `PlayerManager.release()` |
| Rotation / changement de config | ignoré | — |

**Comportement résultant :**
- Navigation interne (A → B) : compteur passe `1 → 2 → 1` — **le stream continue**
- App mise en arrière-plan (Home, Netflix…) : compteur passe `1 → 0` — **le stream s'arrête immédiatement**
- Retour au premier plan : le mini-player se réattache automatiquement dans `BaseActivity.onResume()`

---

## 5. Activités

### 5.1 MainActivity

```java
extends BaseActivity
```

- Lance le fragment `MainFragment` (BrowseSupportFragment)
- Point d'entrée unique déclaré avec `android.intent.category.LEANBACK_LAUNCHER`
- Orientation forcée : **paysage**
- Banner/Icon : `@drawable/app_icon_your_company`

### 5.2 DetailsActivity

```java
extends BaseActivity
```

- Reçoit un objet `Channel` sérialisé via `Intent.putExtra(CHANNEL, channel)`
- Lance `VideoDetailsFragment`
- Thème : `Theme.ELTV.Details`
- Non exportée

### 5.3 PlaybackActivity

```java
extends FragmentActivity  (pas BaseActivity — pas de mini-player)
```

- Lance `PlaybackVideoFragment`
- Orientation forcée : **paysage**
- Reçoit `CHANNEL_URL` (String) et `CHANNEL_NAME` (String) via Intent
- N'hérite **pas** de `BaseActivity` : le mini-player n'est pas présent en lecture plein écran

### 5.4 SettingsActivity

```java
extends FragmentActivity
```

- Lance `SettingsFragment` (GuidedStepSupportFragment)
- Thème : `Theme.ELTV.GuidedStep`

### 5.5 SearchActivity

```java
extends FragmentActivity
```

- Lance `SearchFragment` (SearchSupportFragment)

### 5.6 BrowseErrorActivity

- Activité d'erreur minimale
- Lance `ErrorFragment`

### 5.7 BaseActivity (classe abstraite)

Toutes les activités (sauf `PlaybackActivity`) héritent de `BaseActivity`.

**Responsabilités :**

| Méthode | Rôle |
|---|---|
| `setupMiniPlayer()` | Instancie `MiniPlayerController` depuis la DecorView |
| `onResume()` | `miniPlayerController.update()` — affiche/rafraîchit le mini-player |
| `onPause()` | `miniPlayerController.detachView()` — détache la PlayerView sans stopper le stream |
| `onDestroy()` | `miniPlayerController.destroy()` — libère les références |
| `dispatchKeyEvent()` | Gère : MEDIA_PLAY_PAUSE, DPAD_DOWN depuis le mini-player |

**Navigation D-PAD :**
- `KEYCODE_MEDIA_PLAY_PAUSE` / `KEYCODE_MEDIA_PLAY` / `KEYCODE_MEDIA_PAUSE` → bascule lecture/pause si un stream est actif
- `KEYCODE_DPAD_DOWN` depuis le mini-player → retourne le focus au contenu principal (`getContentFragmentContainer()`)

---

## 6. Fragments

### 6.1 MainFragment

```java
extends BrowseSupportFragment
implements ThemeManager.ThemeChangeListener
```

**Rôle :** Écran principal — grille de chaînes par catégories + ligne Paramètres.

**Chargement des données (thread background via ExecutorService) :**

1. Obtient la liste des sources depuis Room
2. Si aucune source → insère la source par défaut (`R.string.default_m3u_url`, nom `"IPTV-ORG"`) et synchronise immédiatement
3. Charge les chaînes favorites → ligne "Favoris" en tête de liste
4. Charge les groupes distincts → une ligne par groupe
5. Ajoute la ligne "Paramètres" avec les tuiles `Sources` et `Thème`
6. Poste le rendu sur le thread principal

**Gestion du fond d'écran (BackgroundManager) :**
- À la sélection d'une chaîne → `startBackgroundTimer()` — délai de 300 ms avant chargement
- Chargement du logo via Glide → `BackgroundManager.setDrawable()`
- Fallback sur `R.drawable.default_background`

**Réactivité au thème :**
- Implémente `ThemeManager.ThemeChangeListener.onThemeChanged()`
- Met à jour `setBrandColor()` et `setSearchAffordanceColor()` en temps réel

**Actions utilisateur :**
- Clic sur une chaîne → `DetailsActivity`
- Clic sur une tuile paramètre → `SettingsActivity`
- Clic sur l'icône de recherche → `SearchActivity`

### 6.2 VideoDetailsFragment

```java
extends DetailsSupportFragment
```

**Rôle :** Page de détails d'une chaîne — logo, nom, boutons action.

**Actions disponibles :**

| ID | Libellé | Comportement |
|---|---|---|
| `ACTION_PLAY` (1) | "Lire la chaîne" | Lance `PlaybackActivity` avec URL + nom |
| `ACTION_FAVORITE` (2) | "Ajouter aux favoris" / "Retirer des favoris" | Toggle `isFavorite` + mise à jour Room (thread bg) |

- Logo chargé via **Glide** avec placeholder `R.drawable.default_channel_logo`
- Parallax activé sur le fond (`DetailsSupportFragmentBackgroundController.enableParallax()`)
- Transition partagée avec `FullWidthDetailsOverviewSharedElementHelper` (durée 500 ms)

### 6.3 PlaybackVideoFragment

```java
extends Fragment
```

**Rôle :** Conteneur du `PlayerView` Media3 en plein écran.

**Comportement à `onResume()` :**
- Si `PlayerManager.hasStream() == false` (stream libéré lors du passage en arrière-plan) → relance `startPlaybackFromIntent()`
- Sinon → ré-attache simplement la `PlayerView` au player existant

**`startPlaybackFromIntent()` :**
1. Lit `CHANNEL_URL` et `CHANNEL_NAME` depuis les extras de l'Intent
2. Appelle `PlayerManager.play(context, url, name)`
3. Attache `mPlayerView.setPlayer(player)`

À `onDestroyView()` → détache la PlayerView (`setPlayer(null)`) sans libérer le player.

### 6.4 SearchFragment

```java
extends SearchSupportFragment
implements SearchSupportFragment.SearchResultProvider
```

**Rôle :** Recherche full-text en temps réel dans la base de données.

- `onQueryTextChange()` et `onQueryTextSubmit()` déclenchent `search(query)` sur un thread bg
- Requête SQL : `LOWER(name) LIKE '%' || LOWER(:query) || '%'` (insensible à la casse)
- Résultats affichés en `ListRow` avec `ChannelCardPresenter`
- Clic sur un résultat → `DetailsActivity`

### 6.5 SettingsFragment

```java
extends GuidedStepSupportFragment
```

**Rôle :** Interface de configuration guidée (GuidedStep).

**Actions disponibles :**

| ID | Type | Description |
|---|---|---|
| `ACTION_URL` (1) | Champ de texte éditable (URI) | URL de la playlist M3U |
| `ACTION_SYNC` (2) | Bouton | Synchronise les chaînes depuis l'URL saisie |
| `ACTION_MIDNIGHT` (3) | Radio | Thème Midnight (bleu nuit) |
| `ACTION_FOREST` (4) | Radio | Thème Forest (vert nature) |
| `ACTION_PURPLE` (5) | Radio | Thème Purple (violet) |

**Processus de synchronisation (`doSync()`) :**
1. Lit l'URL depuis `ACTION_URL`
2. Affiche un toast "Synchronisation en cours…"
3. Sur thread background :
   - Insère/met à jour la source dans Room
   - Supprime les chaînes existantes de cette source (`deleteBySource`)
   - Télécharge le contenu via `NetworkUtils.fetchUrl()`
   - Parse le M3U via `M3UParser.parseM3U()`
   - Insère les nouvelles chaînes (`insertAll`)
   - Met à jour `lastSync` de la source
4. Retour sur le thread principal : toast succès (`N chaînes synchronisées`) ou erreur

### 6.6 ErrorFragment

Fragment minimal affiché par `BrowseErrorActivity` en cas d'erreur de chargement.

---

## 7. Modèles de données & Base de données

### 7.1 Channel (entité Room)

**Table :** `channels`

| Champ | Type | Description |
|---|---|---|
| `id` | `Long` (PK auto-généré) | Identifiant unique |
| `sourceId` | `String` | URL de la source M3U parente |
| `name` | `String` | Nom de la chaîne |
| `url` | `String` | URL du flux (HTTP/HTTPS/HLS/RTMP/RTSP) |
| `logo` | `String` | URL du logo (`tvg-logo`) |
| `group` | `String` | Catégorie normalisée (voir M3UParser) |
| `isFavorite` | `boolean` | Marqué comme favori par l'utilisateur |

Implémente `Serializable` pour le passage via `Intent.putExtra()`.

### 7.2 Source (entité Room)

**Table :** `sources`

| Champ | Type | Description |
|---|---|---|
| `url` | `String` (PK, non null) | URL unique de la playlist M3U |
| `name` | `String` | Nom affiché de la source |
| `lastSync` | `long` | Timestamp Unix de la dernière synchronisation |

### 7.3 AppDatabase

```java
@Database(entities = {Channel.class, Source.class}, version = 1, exportSchema = false)
```

**Singleton thread-safe** avec double vérification (DCL) :

```java
Room.databaseBuilder(context, AppDatabase.class, "eliptv_database").build()
```

#### ChannelDao

| Méthode | Requête |
|---|---|
| `getAll()` | `SELECT * FROM channels` |
| `getByGroup(group)` | `SELECT * FROM channels WHERE group = ?` |
| `getGroups()` | `SELECT DISTINCT group … ORDER BY group ASC` |
| `insertAll(channels)` | `INSERT OR REPLACE` |
| `deleteBySource(sourceId)` | `DELETE … WHERE sourceId = ?` |
| `update(channel)` | `UPDATE` |
| `getFavorites()` | `SELECT * … WHERE isFavorite = 1` |
| `searchChannels(query)` | `LOWER(name) LIKE '%' || LOWER(:query) || '%'` |

#### SourceDao

| Méthode | Requête |
|---|---|
| `getAll()` | `SELECT * FROM sources` |
| `insert(source)` | `INSERT OR REPLACE` |
| `delete(url)` | `DELETE … WHERE url = ?` |

---

## 8. Couche réseau

### NetworkUtils

Client OkHttp **singleton** partagé par toute l'application.

**Configuration :**

| Paramètre | Valeur |
|---|---|
| SSL | Trust-All (`X509TrustManager` permissif + `hostnameVerifier` `true`) |
| `connectTimeout` | 30 secondes |
| `readTimeout` | 30 secondes |
| `writeTimeout` | 30 secondes |

> ⚠️ **Sécurité :** Le trust-manager accepte tous les certificats TLS, y compris auto-signés. Cela est intentionnel pour la compatibilité avec les sources IPTV non standard mais constitue un risque en environnement de production.

**`fetchUrl(url)`** :
- Émet une requête GET synchrone (doit être appelé sur un thread background)
- Lève `IOException` si le code HTTP n'est pas 2xx ou si la réponse est vide

### ELTVGlideModule

```java
@GlideModule
public class ELTVGlideModule extends AppGlideModule
```

Intègre **OkHttp3** comme transport réseau de Glide :
- Réutilise le même `OkHttpClient` que le reste de l'application (SSL étendu)
- Désactive la lecture du manifest (`isManifestParsingEnabled = false`)

---

## 9. Lecteur média

### PlayerManager

**Singleton** gérant l'unique instance d'`ExoPlayer`.

**Initialisation du player :**

```
OkHttpDataSource.Factory(NetworkUtils.getClient())
    ↓
DefaultDataSource.Factory(context, okHttpFactory)
    ↓
DefaultMediaSourceFactory(dataSourceFactory)
    ↓
ExoPlayer.Builder(context).setMediaSourceFactory(…).build()
```

Tous les flux passent par le client OkHttp (SSL étendu, timeout 30 s).

**API publique :**

| Méthode | Description |
|---|---|
| `getPlayer(context)` | Retourne ou crée le player singleton |
| `getPlayerIfExists()` | Retourne le player S'IL EXISTE (null sinon) — pour le mini-player |
| `play(context, url, name)` | Stop → clear → setMediaItem → prepare → play |
| `pause()` | Pause sans libérer |
| `resume()` | Reprend la lecture |
| `isStreamActive()` | `STATE_READY` ou `STATE_BUFFERING` |
| `isPlaying()` | Lecture active (pas en pause) |
| `hasStream()` | Une URL a été chargée (même si en pause) |
| `release()` | Libère le player et remet les métadonnées à null |

**Protocoles supportés :** HTTP, HTTPS, HLS (`.m3u8`), RTMP, RTSP (via Media3 + module HLS).

---

## 10. Mini-player

### MiniPlayerController

Contrôle l'overlay mini-player présent dans **toutes les activités sauf `PlaybackActivity`**.

**Vues attendues dans le layout (IDs) :**

| ID | Type | Rôle |
|---|---|---|
| `mini_player_container` | `View` (conteneur) | Visibilité VISIBLE/GONE |
| `mini_player_view` | `PlayerView` | Rendu vidéo miniature |
| `mini_channel_name` | `TextView` | Nom de la chaîne en lecture |
| `mini_play_pause_btn` | `TextView` | Bouton ▶ / ⏸ |
| `mini_expand_btn` | `View` | Ouvre `PlaybackActivity` |
| `mini_stop_btn` | `View` | Arrête le stream (`PlayerManager.release()`) |

**Stratégie de partage du player :**

```
PlaybackActivity au premier plan
    → PlayerView plein écran s'attache au player
    → Media3 détache automatiquement le mini-player

Retour vers une autre activité (BaseActivity)
    → onResume() → miniPlayerController.update()
    → Le mini-player se réattache au même player singleton
```

**`update()`** :
- Si un stream est actif ou chargé → `container.VISIBLE` + `playerView.setPlayer(player)`
- Sinon → `container.GONE` + `playerView.setPlayer(null)`

**`detachView()`** (appelé dans `onPause()`) :
- `playerView.setPlayer(null)` — détache la vue **sans stopper la lecture**
- Permet à `PlaybackActivity` de prendre le contrôle de la même instance ExoPlayer

---

## 11. Gestion des thèmes

### ThemeManager

**Singleton** avec persistance dans `SharedPreferences` (`eltv_prefs`, clé `theme`).

**Thèmes disponibles :**

| Constante | Valeur | Brand Color | Search Affordance Color |
|---|---|---|---|
| `THEME_MIDNIGHT` | 0 | `#1A1A2E` (bleu nuit) | `#E94560` (rouge corail) |
| `THEME_FOREST` | 1 | `#1B4332` (vert foncé) | `#52B788` (vert menthe) |
| `THEME_PURPLE` | 2 | `#2D1B69` (violet profond) | `#9B59B6` (violet clair) |

**Interface de notification :**

```java
public interface ThemeChangeListener {
    void onThemeChanged(int theme);
}
```

- `MainFragment` implémente cette interface → met à jour les couleurs Leanback en temps réel
- `SettingsTilePresenter` utilise `ThemeManager.getBrandColor()` pour la couleur des tuiles

**Persistance :** `SharedPreferences.MODE_PRIVATE`, sauvegarde synchrone via `apply()`.

---

## 12. Parseur M3U & Catégorisation

### M3UParser

#### `parseM3U(content, sourceId)`

Parcourt ligne par ligne un fichier M3U étendu (format EXTM3U) :

1. Ligne `#EXTINF:` → extrait les attributs via regex, puis le nom après la dernière virgule
2. Ligne URL (`http`, `https`, `rtmp`, `rtsp`) → finalise l'objet `Channel` et l'ajoute à la liste

**Attributs extraits :**

| Attribut M3U | Champ Channel |
|---|---|
| `tvg-logo` ou `logo` | `logo` |
| `group-title` | Entrée dans `categorizeChannel()` |
| Nom (après la virgule) | `name` |

#### `categorizeChannel(name, originalGroup)`

Catégorisation automatique par mots-clés (insensible à la casse) :

| Catégorie | Exemples de mots-clés |
|---|---|
| **Sports** | sport, football, soccer, basketball, espn, bein, sky sport, nba, nfl, ufc |
| **News** | news, cnn, bbc, al jazeera, sky news, france 24, info, météo |
| **Movies & Series** | movie, cinema, film, hbo, action, thriller, series, tv show, drama |
| **Kids** | kids, cartoon, disney, nickelodeon, anime, manga, gulli, canal j |
| **Knowledge** | culture, edu, history, discovery, nat geo, science, animal, planet |
| **Music** | music, mtv, vh1, radio, rock, pop, jazz, classical |
| **Lifestyle** | entertainment, reality, lifestyle, food, cooking, travel, fashion |
| **General** | (défaut si aucun mot-clé ne correspond) |

#### `parseAttributes(line)`

Regex `([a-z0-9-]+)=(\"[^\"]*\"|'[^']*'|[^,\s]+)` — supporte les valeurs entre guillemets doubles, simples, ou sans guillemets.

---

## 13. Présentateurs (Presenters)

### ChannelCardPresenter

Présente une chaîne IPTV sous forme de carte Leanback (`ImageCardView`) :
- Image : logo de la chaîne chargé par Glide (placeholder : `default_channel_logo`)
- Titre : `channel.getName()`
- Taille de carte : définie dans les styles/layouts

### DetailsDescriptionPresenter

Présente le titre et éventuellement la description d'une chaîne dans `VideoDetailsFragment` (`AbstractDetailsDescriptionPresenter`).

### SettingsTilePresenter (classe interne de MainFragment)

Présente les tuiles de paramètres :
- `TextView` carré 220×220 dp
- Couleur de fond : `ThemeManager.getBrandColor()` (mise à jour dynamique)
- Texte centré, blanc, 14 sp

### CardPresenter

Présentateur legacy (pour le modèle `Movie`), conservé pour compatibilité.

---

## 14. Permissions & Manifest

### Permissions déclarées

| Permission | Usage |
|---|---|
| `INTERNET` | Téléchargement des playlists M3U et flux vidéo |
| `ACCESS_NETWORK_STATE` | Vérification de la connectivité |

### Fonctionnalités requises

| Feature | `required` | Raison |
|---|---|---|
| `android.hardware.touchscreen` | `false` | Android TV n'a pas d'écran tactile |
| `android.software.leanback` | `true` | Application exclusivement Android TV |

### Activités & leurs attributs

| Activité | Exported | Thème | Orientation |
|---|---|---|---|
| `MainActivity` | `true` (launcher) | `Theme.ELTV` | Paysage |
| `DetailsActivity` | `false` | `Theme.ELTV.Details` | — |
| `PlaybackActivity` | `false` | (par défaut) | Paysage |
| `SettingsActivity` | `false` | `Theme.ELTV.GuidedStep` | — |
| `SearchActivity` | `false` | (par défaut) | — |
| `BrowseErrorActivity` | `false` | (par défaut) | — |

---

## 15. Dépendances

### Bibliothèques de production

| Groupe | Artefact | Version | Rôle |
|---|---|---|---|
| `androidx.leanback` | `leanback` | 1.2.0 | UI Android TV (Browse, Details, Search, GuidedStep) |
| `com.github.bumptech.glide` | `glide` | 4.16.0 | Chargement d'images (logos chaînes, fond d'écran) |
| `com.github.bumptech.glide` | `okhttp3-integration` | 4.16.0 | Transport OkHttp pour Glide |
| `androidx.room` | `room-runtime` | 2.6.1 | Base de données locale SQLite (ORM) |
| `androidx.media3` | `media3-exoplayer` | 1.4.1 | Moteur de lecture vidéo ExoPlayer |
| `androidx.media3` | `media3-exoplayer-hls` | 1.4.1 | Support des flux HLS (.m3u8) |
| `androidx.media3` | `media3-ui` | 1.4.1 | `PlayerView` (interface du lecteur) |
| `androidx.media3` | `media3-datasource-okhttp` | 1.4.1 | Source de données OkHttp pour Media3 |
| `com.squareup.okhttp3` | `okhttp` | 4.12.0 | Client HTTP (playlists + flux + Glide) |

### Annotation Processors

| Artefact | Rôle |
|---|---|
| `glide:compiler` | Génération du `GlideApp` |
| `room:room-compiler` | Génération des implémentations DAO |

---

## 16. Flux de données principaux

### 16.1 Premier démarrage — Chargement de la playlist par défaut

```
MainFragment.loadChannels()
    ↓ [thread bg]
AppDatabase.sourceDao().getAll() → liste vide
    ↓
Créer Source("default_m3u_url", "IPTV-ORG") → sourceDao.insert()
    ↓
NetworkUtils.fetchUrl(defaultUrl) → contenu M3U (String)
    ↓
M3UParser.parseM3U(content, defaultUrl) → List<Channel>
    ↓
channelDao().insertAll(channels)
    ↓
sourceDao.insert(src avec lastSync mis à jour)
    ↓ [thread principal]
buildRows() → ArrayObjectAdapter → setAdapter()
```

### 16.2 Lecture d'une chaîne

```
MainFragment → [clic] → DetailsActivity (Channel sérialisé via Intent)
    ↓
VideoDetailsFragment → [bouton Lire]
    ↓
PlaybackActivity.start(url, name)
    ↓
PlaybackVideoFragment.startPlaybackFromIntent()
    ↓
PlayerManager.play(context, url, name)
    → ExoPlayer.setMediaItem(url)
    → ExoPlayer.prepare()
    → ExoPlayer.play()
    ↓
mPlayerView.setPlayer(player)
```

### 16.3 Mini-player lors de la navigation

```
PlaybackActivity → [Back] → DetailsActivity (ou MainFragment)
    ↓
BaseActivity.onResume()
    → miniPlayerController.update()
    → PlayerManager.getPlayerIfExists() → ExoPlayer (toujours actif)
    → container.VISIBLE
    → miniPlayerView.setPlayer(player)   ← stream continue sans interruption
```

### 16.4 Arrêt automatique (passage en arrière-plan)

```
Utilisateur appuie sur Home
    ↓
MainActivity.onStop() (isChangingConfigurations = false)
    ↓
ELTVApplication → startedCount-- → 0
    ↓
PlayerManager.release()
    → ExoPlayer.release()
    → player = null
    → currentUrl = null
```

### 16.5 Synchronisation manuelle (Settings)

```
SettingsFragment → [bouton Sync]
    ↓ [thread bg]
sourceDao.insert(new Source(url))
channelDao.deleteBySource(url)
NetworkUtils.fetchUrl(url) → String
M3UParser.parseM3U(content, url) → List<Channel>
channelDao.insertAll(channels)
sourceDao.insert(src avec lastSync)
    ↓ [thread principal]
Toast: "N chaînes synchronisées"
```

---

*Documentation générée le 2026-07-19 — ELTV v1.0*

