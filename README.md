<!-- Remplacer OWNER/fatigue-tracker ci-dessous par votre org/user GitHub une fois le repo poussé -->
![CI](https://github.com/OWNER/fatigue-tracker/actions/workflows/ci.yml/badge.svg)

# FatigueTracker

Suivre la fatigue structurelle d'une flotte d'appareils demande d'agréger, appareil par appareil,
des heures de vol et des relevés au fil du temps pour estimer une usure et anticiper la
maintenance. Ce projet est un exercice personnel inspiré d'un vécu dans l'aéronautique : il
réimplémente, en environnement public et générique, l'idée d'une API de suivi de flotte — sans
aucune donnée, algorithme ou nom d'entreprise réels. C'est un portfolio technique, pas un produit
opérationnel.

> **Disclaimer** : la formule de calcul de l'« indice de fatigue » (voir section Batch ci-dessous)
> est **entièrement générique et illustrative** — une accumulation de dommage linéaire façon
> règle de Miner très simplifiée, avec des paramètres arbitraires. **Aucune donnée, seuil,
> constante ou méthode propriétaire d'un employeur réel n'est utilisée dans ce dépôt.** Elle ne
> doit en aucun cas être interprétée comme une méthode de calcul de fatigue structurelle
> aéronautique réelle ou certifiée — c'est un exercice de portfolio technique autour de Spring
> Batch, pas un outil d'ingénierie.

## Stack

- Java 21
- Spring Boot 3.3.x (`web`, `data-jpa`, `validation`, `actuator`, `batch`, `security`)
- Maven (avec wrapper `mvnw` / `mvnw.cmd`)
- **PostgreSQL** (persistance réelle depuis J1), schéma géré par **Flyway**
- **Spring Batch** (depuis J2) : recalcul chunk-oriented de l'indice de fatigue de la flotte
- **Spring Security** (depuis J3) : lecture publique, écriture protégée par JWT stateless (voir
  section Sécurité)
- JUnit 5 + MockMvc + Mockito (tests rapides), **Testcontainers** (tests d'intégration Postgres réel),
  `spring-batch-test` (`JobLauncherTestUtils`), `spring-security-test` (`@WithMockUser`)

## Architecture

Organisation **package-by-feature** : `aircraft` et `reading` regroupent chacun entité, repository,
service, controller et DTOs. Un package `common` porte la gestion d'erreurs transverse
(`@RestControllerAdvice`, corps d'erreur structuré), non spécifique à une feature. Le package
`security` (J3) regroupe l'authentification JWT et le modèle d'autorisation, transverses à toute
l'API.

```
src/main/java/dev/ynzi/fatiguetracker/
├── FatigueTrackerApplication.java
├── aircraft/
│   ├── Aircraft.java                 (entité JPA)
│   ├── AircraftRepository.java
│   ├── AircraftService.java
│   ├── AircraftController.java
│   ├── AircraftNotFoundException.java
│   └── dto/
│       ├── AircraftRequest.java      (record, validation Bean Validation)
│       └── AircraftResponse.java     (record, jamais l'entité exposée)
├── reading/                           (J1)
│   ├── FlightReading.java             (entité JPA, @ManyToOne vers Aircraft)
│   ├── FlightReadingRepository.java
│   ├── FlightReadingService.java
│   ├── FlightReadingController.java
│   └── dto/
│       ├── FlightReadingRequest.java
│       └── FlightReadingResponse.java
├── fatigue/                           (J2)
│   ├── FatigueStatus.java             (entité JPA, @OneToOne vers Aircraft)
│   ├── FatigueStatusRepository.java
│   ├── FatigueProperties.java         (@ConfigurationProperties "fatigue.*")
│   ├── FatigueCalculator.java         (formule illustrative, pure, testée unitairement)
│   ├── FatigueComputationResult.java
│   ├── FatigueService.java            (déclenche le job, expose les lectures)
│   ├── FatigueController.java
│   ├── FatigueRecomputeException.java
│   ├── dto/
│   │   ├── FatigueStatusResponse.java
│   │   ├── FleetFatigueResponse.java
│   │   └── RecomputeResponse.java
│   └── batch/
│       ├── FatigueBatchConfig.java     (Job/Step chunk-oriented, reader appareils)
│       ├── AircraftFatigueProcessor.java (processor : calcul + détection d'alerte)
│       └── FatigueStatusWriter.java    (writer : upsert FatigueStatus)
├── common/
│   ├── ApiError.java                 (corps d'erreur structuré)
│   └── GlobalExceptionHandler.java
└── security/                          (J3)
    ├── SecurityConfig.java            (SecurityFilterChain, PasswordEncoder, AuthenticationManager)
    ├── RestAuthenticationEntryPoint.java (401 au format ApiError)
    ├── RestAccessDeniedHandler.java   (403 au format ApiError)
    ├── jwt/
    │   ├── JwtProperties.java         (@ConfigurationProperties "security.jwt.*")
    │   ├── JwtService.java            (émission/validation HS256)
    │   └── JwtAuthenticationFilter.java (OncePerRequestFilter, peuple le SecurityContext)
    ├── user/
    │   ├── AppUser.java               (entité JPA, mot de passe BCrypt)
    │   ├── Role.java                  (VIEWER, MAINT)
    │   ├── AppUserRepository.java
    │   └── AppUserDetailsService.java (UserDetailsService)
    └── auth/
        ├── AuthController.java        (POST /api/auth/login)
        └── dto/
            ├── LoginRequest.java
            └── LoginResponse.java

src/main/resources/
├── application.yml
└── db/migration/
    ├── V1__init.sql                  (schéma Flyway : tables aircraft + flight_reading)
    ├── V2__batch_schema.sql          (schéma officiel Spring Batch, BATCH_JOB_* / BATCH_STEP_*)
    ├── V3__fatigue_status.sql        (table fatigue_status)
    └── V4__app_user.sql              (table app_user + 2 comptes de démo, J3)
```

## Run

Prérequis : un Postgres accessible (via `docker-compose` ci-dessous, ou une instance existante).

```bash
# Démarre Postgres en local (credentials par défaut de dev, cf. .env.example)
docker-compose up -d

# Optionnel : copier .env.example en .env pour surcharger les identifiants
cp .env.example .env

./mvnw spring-boot:run
```

L'API démarre sur `http://localhost:8080`. Au démarrage, Flyway applique les migrations
(`src/main/resources/db/migration`) sur la base Postgres ; Hibernate est en
`ddl-auto: validate` (aucune génération automatique de schéma, Flyway fait foi).

Connexion configurée via variables d'env (defaults de dev, surchargeables, aucun secret en dur) :
`POSTGRES_HOST`, `POSTGRES_PORT`, `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`. Depuis J3,
`JWT_SECRET` (et optionnellement `JWT_EXPIRATION_MINUTES`) permet de surcharger la signature des
JWT — voir section Sécurité.

Healthcheck :

```bash
curl http://localhost:8080/actuator/health
```

### Endpoints `/api/aircraft`

Lecture publique, écriture réservée au rôle `MAINT` (voir section Sécurité pour obtenir un token) :

```bash
# Créer un appareil (nécessite un token MAINT, voir plus bas)
curl -i -X POST http://localhost:8080/api/aircraft \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"registration":"F-ABCD","model":"Mirage 2000","flightHours":1200.5}'

# Lister tous les appareils (public, aucun token requis)
curl http://localhost:8080/api/aircraft

# Récupérer un appareil par id (public)
curl http://localhost:8080/api/aircraft/1

# Mettre à jour un appareil (MAINT)
curl -i -X PUT http://localhost:8080/api/aircraft/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"registration":"F-ABCD","model":"Mirage 2000","flightHours":1350.0}'

# Supprimer un appareil (MAINT)
curl -i -X DELETE http://localhost:8080/api/aircraft/1 \
  -H "Authorization: Bearer $TOKEN"
```

### Endpoints `/api/aircraft/{aircraftId}/readings` (J1)

```bash
# Ajouter un relevé de vol pour l'appareil 1 (MAINT)
curl -i -X POST http://localhost:8080/api/aircraft/1/readings \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"recordedAt":"2026-01-10T08:00:00Z","cycles":4,"maxLoadFactor":2.1,"flightHours":3.5}'

# Lister les relevés de l'appareil 1 (public)
curl http://localhost:8080/api/aircraft/1/readings
```

## Batch : recalcul de l'indice de fatigue (J2)

### Ce que fait le job

`fatigueRecomputeJob` (Spring Batch, un seul step chunk-oriented `recomputeFatigueStep`) recalcule
l'indice de fatigue de **toute la flotte** à chaque exécution :

- **Reader** (`RepositoryItemReader<Aircraft>`) : parcourt tous les appareils (`AircraftRepository
  ::findAll`, triés par id, pagination interne = taille de chunk).
- **Processor** (`AircraftFatigueProcessor`) : charge les relevés de l'appareil et délègue le calcul
  à `FatigueCalculator` (formule pure, voir plus bas), produit un `FatigueStatus` transitoire avec
  détection d'alerte.
- **Writer** (`FatigueStatusWriter`) : **upsert** — une seule ligne `fatigue_status` par appareil,
  mise à jour en place (pas d'historique conservé ; le calcul est déterministe et rejouable à tout
  instant à partir des relevés bruts).

Le job n'est **jamais lancé automatiquement au démarrage** (`spring.batch.job.enabled: false`) :
uniquement à la demande via `POST /api/fatigue/recompute`. Le schéma de métadonnées Spring Batch
(`BATCH_JOB_*`, `BATCH_STEP_*`) est géré par Flyway (`V2__batch_schema.sql`, script officiel du
projet Spring Batch), pas par l'auto-initialisation JDBC de Spring Boot
(`spring.batch.jdbc.initialize-schema: never`) — cohérent avec le choix J1 de faire de Flyway la
seule source de vérité du DDL.

### Formule illustrative (voir disclaimer en tête de README)

```
dommage_brut = Σ cycles_i × (maxLoadFactor_i / referenceLoadFactor) ^ exponent
indice       = dommage_brut / normalizationFactor
alerte       = indice ≥ alertThreshold
```

Paramètres configurables (`application.yml`, préfixe `fatigue.*`) :

| Propriété                     | Rôle                                                        | Défaut |
|--------------------------------|--------------------------------------------------------------|--------|
| `fatigue.reference-load-factor`| Facteur de charge "nominal" de référence (dénominateur ratio) | `1.0`  |
| `fatigue.exponent`              | Exposant appliqué au ratio de charge                         | `3.0`  |
| `fatigue.normalization-factor`  | Diviseur du dommage brut cumulé (ordre de grandeur lisible)  | `1000.0` |
| `fatigue.alert-threshold`       | Seuil au-delà duquel `maintenanceAlert` est levé             | `80.0` |
| `fatigue.chunk-size`            | Taille de chunk du step Spring Batch                         | `20`   |

**Encore une fois : ces valeurs et cette formule sont arbitraires et illustratives**, choisies pour
donner un comportement démontrable (0 relevé → index 0, relevés "lourds" → alerte), pas pour
représenter une réalité physique ou une méthode d'un employeur réel.

### Déclencher le recalcul et consulter l'état de fatigue

`POST /api/fatigue/recompute` est protégé (rôle `MAINT`, voir section Sécurité) ; les deux `GET`
restent publics :

```bash
# Lancer une exécution du job (nécessite un token MAINT ; retourne un résumé de l'exécution Spring Batch)
curl -i -X POST http://localhost:8080/api/fatigue/recompute \
  -H "Authorization: Bearer $TOKEN"

# Indice de fatigue + alerte d'un appareil donné (public)
curl http://localhost:8080/api/aircraft/1/fatigue

# État de fatigue de toute la flotte (public, avec les appareils en alerte de maintenance isolés)
curl http://localhost:8080/api/fatigue
```

`GET /api/aircraft/{id}/fatigue` renvoie `"computed": false` (indice à 0, `computedAt` nul) tant que
le job n'a pas encore tourné pour cet appareil ; 404 si l'appareil lui-même n'existe pas.

Planification périodique (ex. `@Scheduled` déclenchant `POST /api/fatigue/recompute`, ou un
scheduler externe type cron/Quartz) : **non implémentée en J2**, next step documenté en roadmap —
le déclenchement reste manuel/à la demande pour l'instant.

## Sécurité (J3)

### Modèle d'autorisation

**Lecture publique, écriture protégée** : tous les `GET` de l'API, `/actuator/health` et
`POST /api/auth/login` sont accessibles sans authentification ; toute autre requête (CRUD
`aircraft`, ajout de relevés, `POST /api/fatigue/recompute`) exige un JWT valide **et** le rôle
`MAINT`. Le rôle `VIEWER` existe pour la cohérence du modèle (deux rôles distincts, évolution
future vers de la lecture différenciée) mais n'apporte aujourd'hui aucun droit de plus qu'un appel
anonyme, la lecture étant déjà entièrement publique.

La règle par défaut (`SecurityConfig`) est volontairement `anyRequest().hasRole("MAINT")` : une
route future non explicitement listée en lecture publique sera donc protégée par défaut, plutôt
que de fuiter accidentellement en écriture libre.

### Mécanisme : JWT stateless (HS256)

Choix retenu plutôt que HTTP Basic : pas de session serveur à maintenir, un jeton auto-porteur
(subject + rôle en claims) suffit à autoriser une requête sans round-trip base à chaque appel,
expiration native côté token, et un modèle qui s'étend naturellement vers un futur front Angular
(J5) sans renvoyer les identifiants à chaque appel.

- `POST /api/auth/login` vérifie les identifiants (mot de passe **BCrypt**, jamais en clair) via
  `AuthenticationManager` + `UserDetailsService`, et retourne un JWT signé HS256 si valides.
- Chaque appel protégé fournit ce token en en-tête `Authorization: Bearer <token>` ; un filtre
  (`JwtAuthenticationFilter`) le valide et peuple le contexte de sécurité à partir de ses claims
  (aucun accès base nécessaire par requête, le rôle est porté par le token).
- **Secret de signature** : variable d'environnement `JWT_SECRET` (base64, ≥ 256 bits décodés).
  `application.yml` fournit un default **explicitement marqué dev only**, versionné donc public —
  à ne **jamais** utiliser tel quel en dehors d'un poste de développement local. `JWT_EXPIRATION_MINUTES`
  (défaut `60`) contrôle la durée de validité.

### Comptes de démonstration

Deux comptes sont seedés par la migration Flyway `V4__app_user.sql` (hash BCrypt en migration —
acceptable ici car ce sont des identifiants de **démo publics et assumés**, jamais de vrai secret) :

| Utilisateur    | Mot de passe | Rôle     |
|----------------|--------------|----------|
| `demo.viewer`  | `viewer123`  | `VIEWER` |
| `demo.maint`   | `maint123`   | `MAINT`  |

### Exemples curl

```bash
# Login (compte MAINT) : récupère un JWT
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"demo.maint","password":"maint123"}'
# {"accessToken":"eyJ...","tokenType":"Bearer","expiresInSeconds":3600,"role":"MAINT"}

# Réutiliser le token pour un appel protégé (jq, ou copier "accessToken" manuellement sinon)
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"demo.maint","password":"maint123"}' | jq -r .accessToken)

curl -i -X POST http://localhost:8080/api/fatigue/recompute \
  -H "Authorization: Bearer $TOKEN"

# Sans token : 401 ; avec un token VIEWER : 403 (rôle insuffisant)
curl -i -X POST http://localhost:8080/api/fatigue/recompute
```

## Endpoints

| Méthode | Path                                   | Auth           | Succès         | Erreurs                              |
|---------|-----------------------------------------|----------------|----------------|----------------------------------------|
| GET     | `/api/aircraft`                         | public         | 200            | —                                     |
| GET     | `/api/aircraft/{id}`                    | public         | 200            | 404 si absent                         |
| POST    | `/api/aircraft`                         | `MAINT`        | 201 + Location | 401 sans token, 403 si rôle insuffisant, 400 si corps invalide |
| PUT     | `/api/aircraft/{id}`                    | `MAINT`        | 200            | 401/403, 404 si absent, 400 si corps invalide |
| DELETE  | `/api/aircraft/{id}`                    | `MAINT`        | 204            | 401/403, 404 si absent                |
| GET     | `/api/aircraft/{aircraftId}/readings`   | public         | 200            | 404 si appareil absent                |
| POST    | `/api/aircraft/{aircraftId}/readings`   | `MAINT`        | 201 + Location | 401/403, 404 si appareil absent, 400 si invalide |
| POST    | `/api/fatigue/recompute`                | `MAINT`        | 200            | 401/403, 409 si le job ne peut pas être lancé |
| GET     | `/api/aircraft/{id}/fatigue`            | public         | 200            | 404 si appareil absent                |
| GET     | `/api/fatigue`                          | public         | 200            | —                                     |
| POST    | `/api/auth/login`                       | public         | 200            | 401 si identifiants invalides, 400 si corps invalide |

## Persistance : PostgreSQL + Flyway

- `docker-compose.yml` (racine) démarre un service `postgres:16-alpine`, credentials via
  variables d'env (defaults de dev dans `.env.example`, jamais commités s'ils sont modifiés —
  `.env` est gitignoré).
- Schéma versionné par Flyway : `src/main/resources/db/migration/V1__init.sql` crée `aircraft`
  et `flight_reading` (FK `flight_reading.aircraft_id -> aircraft.id`, `ON DELETE CASCADE`, index
  sur la FK). `V2__batch_schema.sql` ajoute le schéma de métadonnées Spring Batch. `V3__fatigue_status.sql`
  crée `fatigue_status` (FK unique vers `aircraft`, `ON DELETE CASCADE`). `V4__app_user.sql` (J3)
  crée `app_user` et seed les deux comptes de démo (voir section Sécurité).
- `spring.jpa.hibernate.ddl-auto: validate` : Hibernate ne modifie jamais le schéma, il vérifie
  seulement sa cohérence avec les entités — Flyway est la seule source de vérité du DDL.

## Tests

```bash
./mvnw test      # unitaires/rapides uniquement (aucune dépendance Docker)
./mvnw verify    # inclut les tests d'intégration Testcontainers (nécessite Docker)
```

Deux familles de tests :

- **Rapides, sans DB** (`@WebMvcTest` + MockMvc, service mocké) : `AircraftControllerTest`
  (8 tests — CRUD, 401 sans token, 403 rôle `VIEWER`), `FlightReadingControllerTest` (7 tests —
  création 201, 404 appareil inconnu, validation 400, liste 200/404, 401, 403), `FatigueControllerTest`
  (7 tests — recompute, indice calculé, indice "non calculé", 404 appareil inconnu, flotte avec
  alertes filtrées, 401, 403), `AuthControllerTest` (3 tests — login OK avec JWT mocké, 401
  identifiants invalides, 400 corps invalide). Ces slices `@WebMvcTest` importent explicitement
  `SecurityConfig` (non auto-détecté par la slice) pour exercer les vraies règles d'autorisation
  plutôt que la sécurité par défaut de Spring Boot ; `JwtService`/`UserDetailsService` mockés,
  `@WithMockUser` pour simuler les rôles.
- **Unitaire pur, sans Spring** : `FatigueCalculatorTest` (4 tests) sur la formule illustrative de
  `FatigueCalculator` ; `JwtServiceTest` (3 tests — round-trip subject/rôle, rejet signature
  étrangère, rejet token expiré) sur `JwtService`.
- **Intégration, Postgres réel via Testcontainers** (`AbstractIntegrationTest`, base commune
  `@Testcontainers(disabledWithoutDocker = true)` — skip propre si aucun daemon Docker n'est
  disponible, jamais d'échec) :
  - `FatigueTrackerApplicationTests` — contexte Spring complet + migrations Flyway.
  - `FlightReadingIntegrationTest` — ingestion bout en bout (contrôleur → service → repository →
    Postgres), 4 scénarios (création + liste, 404 appareil inconnu en création et en liste,
    validation 400), écritures authentifiées `@WithMockUser(roles = "MAINT")`.
  - `FlightReadingRepositoryTest` — `FlightReadingRepository` (`@DataJpaTest`, requête filtrée/
    triée par appareil, contrainte NOT NULL sur la FK).
  - `FatigueBatchJobIntegrationTest` — job `fatigueRecomputeJob` lancé réellement via
    `JobLauncherTestUtils` (`@SpringBatchTest`) : insertion d'appareils + relevés (dont un sans
    aucun relevé), vérification de l'indice et de l'alerte persistés par appareil, et qu'un second
    passage fait un upsert (pas de doublon de `fatigue_status`).
  - `SecurityIntegrationTest` (J3) — flow bout-en-bout réel : login avec les comptes de démo
    (seedés par `V4__app_user.sql`) → JWT → appel protégé, 401 sans token, 403 avec un compte
    `VIEWER`, 401 sur mot de passe invalide, lecture publique sans token (5 tests, vrai
    `SecurityFilterChain`, pas une slice).

  Ces deux dernières classes partagent une combinaison d'annotations identique
  (`@SpringBootTest` + `@AutoConfigureMockMvc`) : chacune porte `@DirtiesContext(classMode =
  AFTER_CLASS)` pour empêcher Spring Test de réutiliser en cache l'`ApplicationContext` (et son
  pool JDBC) de l'une pour l'autre alors que chaque classe démarre/arrête son propre conteneur
  Testcontainers — sans quoi le pool réutilisé pointerait vers un conteneur déjà arrêté.

**Statut réel de la dernière exécution dans cet environnement** : Docker disponible → les 46
tests (dont 14 tests d'intégration Testcontainers) ont tourné **et sont verts** (`BUILD
SUCCESS`, 0 échec, 0 erreur, 0 skip). Note technique : la version de Testcontainers gérée par
défaut par `spring-boot-dependencies:3.3.4` (1.19.8) échoue contre les daemons Docker récents
(négociation d'API rejetée, minimum 1.40 requis) ; le `pom.xml` fixe explicitement
`testcontainers.version` à `1.21.4` pour lever ce problème. Sans Docker, ces mêmes tests sont
skippés proprement (`disabledWithoutDocker = true`), jamais en échec.

## CI

`.github/workflows/ci.yml` : sur push et pull request, `actions/setup-java` (Temurin 21) puis
`./mvnw -B verify`. Les runners GitHub-hosted (`ubuntu-latest`) embarquent un daemon Docker actif :
les tests Testcontainers s'y exécutent donc réellement, pas seulement les tests rapides.

## Roadmap

**v0 complète** : Spring Boot (CRUD `aircraft`/`reading`) + Spring Batch (recalcul de fatigue) +
Spring Security (JWT, lecture publique / écriture `MAINT`) — J0 à J3 faits.

Next steps (pas encore faits) :

- **J4** — Finition : documentation API interactive (springdoc-openapi/Swagger UI), couverture de
  tests (JaCoCo + seuil), Dockerfile multi-stage pour packager l'API, éventuellement un
  `docker-compose` complet app + Postgres.
- **J5** (optionnel) — Persistance MongoDB pour les relevés de vol volumineux/semi-structurés,
  front Angular consommant l'API (avec le flow JWT déjà en place côté back).
- Planification périodique du recalcul de fatigue (`@Scheduled` ou scheduler externe) — non fait,
  déclenchement resté manuel via `POST /api/fatigue/recompute` (MAINT).

---

Projet développé avec l'assistance d'un système multi-agents (orchestration IA / prompt engineering).
