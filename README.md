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
- Spring Boot 3.3.x (`web`, `data-jpa`, `validation`, `actuator`, `batch`)
- Maven (avec wrapper `mvnw` / `mvnw.cmd`)
- **PostgreSQL** (persistance réelle depuis J1), schéma géré par **Flyway**
- **Spring Batch** (depuis J2) : recalcul chunk-oriented de l'indice de fatigue de la flotte
- JUnit 5 + MockMvc + Mockito (tests rapides), **Testcontainers** (tests d'intégration Postgres réel),
  `spring-batch-test` (`JobLauncherTestUtils`)

## Architecture

Organisation **package-by-feature** : `aircraft` et `reading` regroupent chacun entité, repository,
service, controller et DTOs. Un package `common` porte la gestion d'erreurs transverse
(`@RestControllerAdvice`, corps d'erreur structuré), non spécifique à une feature.

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
└── common/
    ├── ApiError.java                 (corps d'erreur structuré)
    └── GlobalExceptionHandler.java

src/main/resources/
├── application.yml
└── db/migration/
    ├── V1__init.sql                  (schéma Flyway : tables aircraft + flight_reading)
    ├── V2__batch_schema.sql          (schéma officiel Spring Batch, BATCH_JOB_* / BATCH_STEP_*)
    └── V3__fatigue_status.sql        (table fatigue_status)
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
`POSTGRES_HOST`, `POSTGRES_PORT`, `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`.

Healthcheck :

```bash
curl http://localhost:8080/actuator/health
```

### Endpoints `/api/aircraft`

```bash
# Créer un appareil
curl -i -X POST http://localhost:8080/api/aircraft \
  -H "Content-Type: application/json" \
  -d '{"registration":"F-ABCD","model":"Mirage 2000","flightHours":1200.5}'

# Lister tous les appareils
curl http://localhost:8080/api/aircraft

# Récupérer un appareil par id
curl http://localhost:8080/api/aircraft/1

# Mettre à jour un appareil
curl -i -X PUT http://localhost:8080/api/aircraft/1 \
  -H "Content-Type: application/json" \
  -d '{"registration":"F-ABCD","model":"Mirage 2000","flightHours":1350.0}'

# Supprimer un appareil
curl -i -X DELETE http://localhost:8080/api/aircraft/1
```

### Endpoints `/api/aircraft/{aircraftId}/readings` (J1)

```bash
# Ajouter un relevé de vol pour l'appareil 1
curl -i -X POST http://localhost:8080/api/aircraft/1/readings \
  -H "Content-Type: application/json" \
  -d '{"recordedAt":"2026-01-10T08:00:00Z","cycles":4,"maxLoadFactor":2.1,"flightHours":3.5}'

# Lister les relevés de l'appareil 1
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

```bash
# Lancer une exécution du job (retourne un résumé de l'exécution Spring Batch)
curl -i -X POST http://localhost:8080/api/fatigue/recompute

# Indice de fatigue + alerte d'un appareil donné
curl http://localhost:8080/api/aircraft/1/fatigue

# État de fatigue de toute la flotte (avec les appareils en alerte de maintenance isolés)
curl http://localhost:8080/api/fatigue
```

`GET /api/aircraft/{id}/fatigue` renvoie `"computed": false` (indice à 0, `computedAt` nul) tant que
le job n'a pas encore tourné pour cet appareil ; 404 si l'appareil lui-même n'existe pas.

Planification périodique (ex. `@Scheduled` déclenchant `POST /api/fatigue/recompute`, ou un
scheduler externe type cron/Quartz) : **non implémentée en J2**, next step documenté en roadmap —
le déclenchement reste manuel/à la demande pour l'instant.

## Endpoints

| Méthode | Path                                   | Succès         | Erreurs                              |
|---------|-----------------------------------------|----------------|----------------------------------------|
| GET     | `/api/aircraft`                         | 200            | —                                     |
| GET     | `/api/aircraft/{id}`                    | 200            | 404 si absent                         |
| POST    | `/api/aircraft`                         | 201 + Location | 400 si corps invalide                 |
| PUT     | `/api/aircraft/{id}`                    | 200            | 404 si absent, 400 si corps invalide  |
| DELETE  | `/api/aircraft/{id}`                    | 204            | 404 si absent                         |
| GET     | `/api/aircraft/{aircraftId}/readings`   | 200            | 404 si appareil absent                |
| POST    | `/api/aircraft/{aircraftId}/readings`   | 201 + Location | 404 si appareil absent, 400 si invalide |
| POST    | `/api/fatigue/recompute`                | 200            | 409 si le job ne peut pas être lancé   |
| GET     | `/api/aircraft/{id}/fatigue`            | 200            | 404 si appareil absent                |
| GET     | `/api/fatigue`                          | 200            | —                                     |

## Persistance : PostgreSQL + Flyway

- `docker-compose.yml` (racine) démarre un service `postgres:16-alpine`, credentials via
  variables d'env (defaults de dev dans `.env.example`, jamais commités s'ils sont modifiés —
  `.env` est gitignoré).
- Schéma versionné par Flyway : `src/main/resources/db/migration/V1__init.sql` crée `aircraft`
  et `flight_reading` (FK `flight_reading.aircraft_id -> aircraft.id`, `ON DELETE CASCADE`, index
  sur la FK). `V2__batch_schema.sql` ajoute le schéma de métadonnées Spring Batch. `V3__fatigue_status.sql`
  crée `fatigue_status` (FK unique vers `aircraft`, `ON DELETE CASCADE`).
- `spring.jpa.hibernate.ddl-auto: validate` : Hibernate ne modifie jamais le schéma, il vérifie
  seulement sa cohérence avec les entités — Flyway est la seule source de vérité du DDL.

## Tests

```bash
./mvnw test      # unitaires/rapides uniquement (aucune dépendance Docker)
./mvnw verify    # inclut les tests d'intégration Testcontainers (nécessite Docker)
```

Deux familles de tests :

- **Rapides, sans DB** (`@WebMvcTest` + MockMvc, service mocké) : `AircraftControllerTest`
  (4 tests), `FlightReadingControllerTest` (5 tests — création 201, 404 appareil inconnu,
  validation 400, liste 200, liste 404), `FatigueControllerTest` (5 tests — recompute, indice
  calculé, indice "non calculé", 404 appareil inconnu, flotte avec alertes filtrées).
- **Unitaire pur, sans Spring** : `FatigueCalculatorTest` (4 tests — 0 relevé, sous le seuil,
  au-dessus du seuil, exactement au seuil) sur la formule illustrative de `FatigueCalculator`.
- **Intégration, Postgres réel via Testcontainers** (`AbstractIntegrationTest`, base commune
  `@Testcontainers(disabledWithoutDocker = true)` — skip propre si aucun daemon Docker n'est
  disponible, jamais d'échec) :
  - `FatigueTrackerApplicationTests` — contexte Spring complet + migrations Flyway.
  - `FlightReadingIntegrationTest` — ingestion bout en bout (contrôleur → service → repository →
    Postgres), 4 scénarios (création + liste, 404 appareil inconnu en création et en liste,
    validation 400).
  - `FlightReadingRepositoryTest` — `FlightReadingRepository` (`@DataJpaTest`, requête filtrée/
    triée par appareil, contrainte NOT NULL sur la FK).
  - `FatigueBatchJobIntegrationTest` — job `fatigueRecomputeJob` lancé réellement via
    `JobLauncherTestUtils` (`@SpringBatchTest`) : insertion d'appareils + relevés (dont un sans
    aucun relevé), vérification de l'indice et de l'alerte persistés par appareil, et qu'un second
    passage fait un upsert (pas de doublon de `fatigue_status`).

**Statut réel de la dernière exécution dans cet environnement** : Docker disponible → les 27
tests (dont 9 tests d'intégration Testcontainers) ont tourné **et sont verts** (`BUILD
SUCCESS`, 0 échec, 0 erreur, 0 skip). Note technique : la version de Testcontainers gérée par
défaut par `spring-boot-dependencies:3.3.4` (1.19.8) échoue contre les daemons Docker récents
(négociation d'API rejetée, minimum 1.40 requis) ; le `pom.xml` fixe explicitement
`testcontainers.version` à `1.21.4` pour lever ce problème. Sans Docker, ces mêmes tests sont
skippés proprement (`disabledWithoutDocker = true`), jamais en échec.

## CI

`.github/workflows/ci.yml` : sur push et pull request, `actions/setup-java` (Temurin 21) puis
`./mvnw -B verify`. Les runners GitHub-hosted (`ubuntu-latest`) embarquent un daemon Docker actif :
les tests Testcontainers s'y exécutent donc réellement, pas seulement les tests rapides.

## Roadmap (next steps, pas encore faits)

- **J3** — Sécurité / authentification (Spring Security, JWT ou OAuth2) : protéger les endpoints
  d'écriture (`POST`/`PUT`/`DELETE`, `POST /api/fatigue/recompute`), a minima une lecture
  publique/authentifiée à définir.
- Planification périodique du recalcul de fatigue (`@Scheduled` ou scheduler externe) — non fait
  en J2, déclenchement resté manuel via `POST /api/fatigue/recompute`.
- Persistance MongoDB pour les relevés de vol volumineux/semi-structurés.
- Front Angular consommant l'API.

---

Projet développé avec l'assistance d'un système multi-agents (orchestration IA / prompt engineering).
