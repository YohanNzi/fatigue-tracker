<!-- Remplacer OWNER/fatigue-tracker ci-dessous par votre org/user GitHub une fois le repo poussé -->
![CI](https://github.com/OWNER/fatigue-tracker/actions/workflows/ci.yml/badge.svg)

# FatigueTracker

Suivre la fatigue structurelle d'une flotte d'appareils demande d'agréger, appareil par appareil,
des heures de vol et des relevés au fil du temps pour estimer une usure et anticiper la
maintenance. Ce projet est un exercice personnel inspiré d'un vécu dans l'aéronautique : il
réimplémente, en environnement public et générique, l'idée d'une API de suivi de flotte — sans
aucune donnée, algorithme ou nom d'entreprise réels. C'est un portfolio technique, pas un produit
opérationnel.

> Disclaimer : la formule/l'algorithme de calcul d'un « indice de fatigue » (prévu en J2) sera
> générique et illustratif. Aucune donnée ni méthode propriétaire d'un employeur réel n'est
> utilisée dans ce dépôt.

## Stack

- Java 21
- Spring Boot 3.3.x (`web`, `data-jpa`, `validation`, `actuator`)
- Maven (avec wrapper `mvnw` / `mvnw.cmd`)
- **PostgreSQL** (persistance réelle depuis J1), schéma géré par **Flyway**
- JUnit 5 + MockMvc + Mockito (tests rapides), **Testcontainers** (tests d'intégration Postgres réel)

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
└── common/
    ├── ApiError.java                 (corps d'erreur structuré)
    └── GlobalExceptionHandler.java

src/main/resources/
├── application.yml
└── db/migration/
    └── V1__init.sql                  (schéma Flyway : tables aircraft + flight_reading)
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

## Persistance : PostgreSQL + Flyway

- `docker-compose.yml` (racine) démarre un service `postgres:16-alpine`, credentials via
  variables d'env (defaults de dev dans `.env.example`, jamais commités s'ils sont modifiés —
  `.env` est gitignoré).
- Schéma versionné par Flyway : `src/main/resources/db/migration/V1__init.sql` crée `aircraft`
  et `flight_reading` (FK `flight_reading.aircraft_id -> aircraft.id`, `ON DELETE CASCADE`, index
  sur la FK).
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
  validation 400, liste 200, liste 404).
- **Intégration, Postgres réel via Testcontainers** (`AbstractIntegrationTest`, base commune
  `@Testcontainers(disabledWithoutDocker = true)` — skip propre si aucun daemon Docker n'est
  disponible, jamais d'échec) :
  - `FatigueTrackerApplicationTests` — contexte Spring complet + migrations Flyway.
  - `FlightReadingIntegrationTest` — ingestion bout en bout (contrôleur → service → repository →
    Postgres), 4 scénarios (création + liste, 404 appareil inconnu en création et en liste,
    validation 400).
  - `FlightReadingRepositoryTest` — `FlightReadingRepository` (`@DataJpaTest`, requête filtrée/
    triée par appareil, contrainte NOT NULL sur la FK).

**Statut réel de la dernière exécution dans cet environnement** : Docker disponible → les 16
tests (dont les 7 tests d'intégration Testcontainers) ont tourné **et sont verts** (`BUILD
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

- **J2** — Batch Spring Batch pour recalculer un indice de fatigue à partir des relevés de vol
  (formule générique/illustrative).
- Sécurité / authentification (Spring Security, JWT ou OAuth2).
- Persistance MongoDB pour les relevés de vol volumineux/semi-structurés.
- Front Angular consommant l'API.

---

Projet développé avec l'assistance d'un système multi-agents (orchestration IA / prompt engineering).
