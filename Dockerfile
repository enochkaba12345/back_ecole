# Étape 1 : Construction de l'application
FROM maven:3.9.9-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# Étape 2 : Création de l'image finale
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Installer le client PostgreSQL pour que la commande pg_isready fonctionne
RUN apt-get update && apt-get install -y postgresql-client && rm -rf /var/lib/apt/lists/*

# Copier le JAR généré
COPY --from=builder /app/target/SYSGESPECOLE-0.0.1-SNAPSHOT.jar app.jar
RUN apk add --no-cache postgresql-client

# Exposer le port de ton application
EXPOSE 8080

# Commande de démarrage
ENTRYPOINT ["java", "-jar", "app.jar"]
