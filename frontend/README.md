# FatigueTracker — Front Angular (J5)

Front Angular 18 (standalone, Angular Material) qui consomme l'API Spring Boot du
projet parent. Objectif : preuve **full-stack Angular/Spring** + démo cliquable.

Généré avec Angular CLI 18.2. Mono-repo : ce dossier `frontend/` vit dans le repo
`fatigue-tracker`.

## Prérequis

- Node **20.11+** (Angular 18) — testé sur 20.20.
- Le back démarré sur `http://localhost:8080` (voir le README racine). CORS y est
  déjà activé pour `http://localhost:4200`.

## Démarrer

```bash
npm install
npm start          # ng serve -> http://localhost:4200
```

La base de l'API est `http://localhost:8080` (voir `src/app/core/api.config.ts`).
Surcharger cette constante — ou basculer sur des chemins relatifs — quand le front
sera servi par Spring (jalon J5.4, même origin).

## Build

```bash
npm run build      # sortie dans dist/frontend
```

> **Note WSL / disque Windows** : le cache de build persistant d'Angular
> (`.angular/cache`, backend LMDB en mmap) échoue sur un montage `drvfs` (`/mnt/c`)
> avec `Operation not permitted`. Le cache est donc **désactivé** dans `angular.json`
> (`cli.cache.enabled: false`). Sur un système de fichiers natif (CI Linux, macOS,
> Windows natif) le cache fonctionne ; le réactiver là-bas est sans risque.

## État (jalons J5)

- **J5.1 (fait)** — vue **Flotte** : tableau des appareils (immatriculation, modèle,
  indice de fatigue, relevés, dernier calcul, statut), appareils en **alerte de
  maintenance** mis en tête et surlignés. Croise `GET /api/aircraft` et `GET /api/fatigue`.
- **J5.2** — vue **détail appareil** : ses relevés paginés (`GET /api/aircraft/{id}/readings`).
- **J5.3** — **login JWT** + intercepteur Bearer + bouton **Recalculer** protégé (`POST /api/fatigue/recompute`, rôle `MAINT`).
- **J5.4** (option) — servir le build front depuis Spring (`static/`) → un seul jar, une seule URL.

## Structure

```
src/app/
  core/api.config.ts            base URL de l'API
  models/fatigue.models.ts      miroirs TS des DTO
  services/fatigue-api.service.ts  accès HTTP (forkJoin aircraft + fatigue)
  fleet/                        vue Flotte (composant standalone + Material table)
  app.component.*               shell (toolbar) + router-outlet
  app.routes.ts                 routes (/fleet par défaut)
```
