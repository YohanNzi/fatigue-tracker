<!-- Remplacer OWNER/fatigue-tracker ci-dessous par votre org/user GitHub une fois le repo poussé -->
![CI](https://github.com/OWNER/fatigue-tracker/actions/workflows/ci.yml/badge.svg)

# FatigueTracker

Suivre la fatigue structurelle d'une flotte d'appareils demande d'agréger, appareil par appareil,
des heures de vol et des relevés au fil du temps pour estimer une usure et anticiper la
maintenance. Ce projet est un exercice personnel inspiré d'un vécu dans l'aéronautique : il
réimplémente, en environnement public et générique, l'idée d'une API de suivi de flotte — sans
aucune donnée, algorithme ou nom d'entreprise réels. C'est un portfolio technique, pas un produit
opérationnel.

> Disclaimer : la formule/l'algorithme de calcul d'un « indice de fatigue » (prévu en J1+) sera
> générique et illustratif. Aucune donnée ni méthode propriétaire d'un employeur réel n'est
> utilisée dans ce dépôt.

## Stack

- Java 21
- Spring Boot 3.3.x (`web`, `data-jpa`, `validation`, `actuator`)
- Maven (avec wrapper `mvnw` / `mvnw.cmd`)
- H2 en mémoire (persistance J0)
- JUnit 5 + MockMvc + Mockito pour les tests

## Architecture

Organisation **package-by-feature** : le domaine `aircraft` regroupe entité, repository, service,
controller et DTOs. Un package `common` porte la gestion d'erreurs transverse (`@RestControllerAdvice`,
corps d'erreur structuré), non spécifique à une feature.

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
└── common/
    ├── ApiError.java                 (corps d'erreur structuré)
    └── GlobalExceptionHandler.java
```

## Run

```bash
./mvnw spring-boot:run
```

L'API démarre sur `http://localhost:8080`. Base H2 en mémoire (réinitialisée à chaque démarrage),
console H2 activée sur `/h2-console` (JDBC URL `jdbc:h2:mem:fatiguetracker`).

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

## Endpoints

| Méthode | Path                 | Succès       | Erreurs                              |
|---------|----------------------|--------------|---------------------------------------|
| GET     | `/api/aircraft`      | 200          | —                                     |
| GET     | `/api/aircraft/{id}` | 200          | 404 si absent                         |
| POST    | `/api/aircraft`      | 201 + Location | 400 si corps invalide               |
| PUT     | `/api/aircraft/{id}` | 200          | 404 si absent, 400 si corps invalide  |
| DELETE  | `/api/aircraft/{id}` | 204          | 404 si absent                         |

## Tests

```bash
./mvnw test
```

- 1 test de contexte (`@SpringBootTest`)
- 4 tests contrôleur (`@WebMvcTest` + MockMvc, service mocké) : création 201, validation 400,
  GET introuvable 404, DELETE introuvable 404

## CI

`.github/workflows/ci.yml` : sur push et pull request, `actions/setup-java` (Temurin 21) puis
`./mvnw -B verify`.

## Roadmap (next steps, pas encore faits)

- **J1** — Batch de calcul d'un indice de fatigue à partir de relevés de vol (générique/illustratif).
- Sécurité / authentification (Spring Security, JWT ou OAuth2).
- Persistance MongoDB pour les relevés de vol volumineux/semi-structurés.
- Front Angular consommant l'API.

---

Projet développé avec l'assistance d'un système multi-agents (orchestration IA / prompt engineering).
