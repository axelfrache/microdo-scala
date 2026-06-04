FROM eclipse-temurin:21-jdk-jammy AS builder

ENV SBT_VERSION=1.12.11

RUN apt-get update && apt-get install -y curl && \
    curl -fL "https://github.com/sbt/sbt/releases/download/v${SBT_VERSION}/sbt-${SBT_VERSION}.tgz" \
      -o /tmp/sbt.tgz && \
    tar xzf /tmp/sbt.tgz -C /usr/local && \
    ln -s /usr/local/sbt/bin/sbt /usr/local/bin/sbt && \
    rm /tmp/sbt.tgz

WORKDIR /build
COPY build.sbt .
COPY project/ project/
RUN sbt update

COPY src/ src/
RUN sbt 'set assembly / assemblyJarName := "squirrelvault.jar"' assembly

FROM eclipse-temurin:21-jre-jammy

RUN apt-get update && apt-get install -y ca-certificates curl gnupg lsb-release && \
  mkdir -p /etc/apt/keyrings && \
  curl -fsSL https://www.postgresql.org/media/keys/ACCC4CF8.asc | gpg --dearmor -o /etc/apt/keyrings/postgresql.gpg && \
  echo "deb [signed-by=/etc/apt/keyrings/postgresql.gpg] http://apt.postgresql.org/pub/repos/apt jammy-pgdg main" > /etc/apt/sources.list.d/pgdg.list && \
  apt-get update && apt-get install -y postgresql-client-16 && \
  rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=builder /build/target/scala-3.8.3/squirrelvault.jar .

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "squirrelvault.jar"]
