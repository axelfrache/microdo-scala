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

WORKDIR /app
COPY --from=builder /build/target/scala-3.8.3/squirrelvault.jar .

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "squirrelvault.jar"]
