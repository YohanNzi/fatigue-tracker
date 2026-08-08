# Build multi-stage (J4) : étage de build (Maven + JDK 21) puis image runtime légère
# (JRE 21 uniquement, pas de JDK/Maven dans l'image finale).

# --- Étage 1 : build ---------------------------------------------------------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Copie du wrapper Maven et du pom.xml seuls d'abord : permet à Docker de mettre en
# cache la résolution des dépendances tant que le pom.xml ne change pas, même si le
# code source change ensuite (couche invalidée uniquement sur changement de pom.xml).
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline

COPY src/ src/
# Le front Angular est requis par le profil `fullstack` (frontend-maven-plugin +
# copie de frontend/dist/frontend/browser dans le jar). node_modules/dist exclus
# via .dockerignore : le plugin réinstalle node + npm install + build lui-même.
COPY frontend/ frontend/
# Le jar applicatif ne dépend pas des tests (Testcontainers/Docker-in-Docker non
# disponibles de façon fiable à l'intérieur d'un build Docker) : on saute les tests ici,
# la CI (.github/workflows/ci.yml) reste responsable de lancer ./mvnw verify au complet.
# -Pfullstack : construit le front et l'empaquette dans le jar (mono-artefact servi
# par Spring). SANS ce profil, le jar est backend-only (front absent).
RUN ./mvnw -B -DskipTests -Pfullstack package

# --- Étage 2 : runtime ---------------------------------------------------------
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# Utilisateur non privilégié plutôt que root (bonne pratique de moindre privilège).
RUN groupadd --system fatiguetracker && useradd --system --gid fatiguetracker fatiguetracker

COPY --from=build /workspace/target/fatigue-tracker-*.jar /app/app.jar
RUN chown fatiguetracker:fatiguetracker /app/app.jar
USER fatiguetracker

EXPOSE 8080

# Cap le heap sur la RAM réellement allouée au conteneur (Render free = 512 Mo) :
# évite un OOM-kill silencieux. Le port réel vient de PORT (cf. application.yml).
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70.0"

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
