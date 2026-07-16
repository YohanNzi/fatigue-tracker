# ADR 0001 — Choix de stack

**Statut** : Acceptée — J0 à J4.

## Contexte

FatigueTracker est un exercice de portfolio technique : une API de suivi de fatigue
structurelle d'une flotte d'appareils (CRUD appareils/relevés, recalcul périodique d'un
indice de fatigue, authentification). L'objectif est de démontrer un niveau de rigueur
"production-like" sur une stack JVM/Spring courante côté back, pas de livrer un produit
opérationnel ni de reproduire une méthode ou des données réelles d'un employeur.

## Décisions

### Spring Boot (Java 21)

Écosystème dominant côté back Java, cohérent avec un objectif de portfolio lisible pour
un recruteur/mainteneur tiers. Java 21 (LTS) pour les *records* (DTOs immuables) et la
maturité de l'écosystème Spring 3.3.x associé.

### PostgreSQL + Flyway

Persistance relationnelle réelle plutôt que H2 en mémoire, y compris en test
(Testcontainers) : les migrations Flyway (`V1`→`V4`) sont la seule source de vérité du
schéma (`ddl-auto: validate`), ce qui écarte toute dérive silencieuse entre entités JPA et
DDL réel. Alternative écartée : H2 seul, plus rapide mais masque des différences de
comportement SQL (contraintes, types) qui ne seraient découvertes qu'en environnement réel.

### Spring Batch

Le recalcul de l'indice de fatigue de toute la flotte est un traitement de masse,
rejouable, idempotent (upsert) — le cas d'usage canonique d'un job chunk-oriented plutôt
que d'une boucle service naïve : reprise/rejeu, métriques d'exécution, découplage
reader/processor/writer. Déclenché à la demande (`POST /api/fatigue/recompute`), pas de
scheduler pour l'instant (voir roadmap).

### Spring Security + JWT stateless

Modèle two-tier volontairement simple (lecture publique, écriture `MAINT`) plutôt qu'une
matrice de permissions fine, suffisant pour démontrer le mécanisme sans sur-ingénierie.
JWT stateless plutôt que sessions HTTP : pas d'état serveur à synchroniser, un jeton
auto-porteur suffit à autoriser une requête, et le modèle s'étend naturellement vers un
futur front Angular (J5) sans renvoyer les identifiants à chaque appel. Alternative
écartée : HTTP Basic, plus simple mais réauthentifie à chaque requête et ne prépare pas la
séparation front/back de la roadmap.

### springdoc-openapi, JaCoCo, Cucumber (J4)

Trois choix de finition, pas d'architecture : documentation API interactive générée depuis
le code (jamais désynchronisée d'un Swagger écrit à la main), mesure de couverture
objective plutôt qu'un chiffre déclaratif, et au moins un scénario BDD lisible par un
non-développeur sur le flow métier central (ingestion → recalcul → alerte). Chacun de ces
trois choix est demandé explicitement par la cible du portfolio (offre citant BDD, tests,
documentation d'API).

## Conséquences

- Le schéma de données est figé par Flyway : toute évolution passe par une nouvelle
  migration versionnée, jamais par une modification rétroactive d'un script déjà appliqué.
- Le recalcul de fatigue reste un traitement manuel/à la demande (pas de `@Scheduled`) :
  acceptable pour un portfolio, documenté comme limite assumée (voir README).
- La formule de fatigue elle-même est générique et illustrative (voir disclaimer README) :
  ce choix de stack ne préjuge en rien d'une méthode de calcul réelle ou certifiée.
