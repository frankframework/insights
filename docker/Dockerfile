FROM aquasec/trivy:0.69.3 AS trivy-downloader

FROM eclipse-temurin:25.0.1_8-jre-jammy AS runner
ARG GID=2000
ARG UID=2000

RUN addgroup --gid ${GID} spring && \
    adduser --uid ${UID} --gid ${GID} --shell /usr/sbin/nologin spring

USER root
RUN apt-get update && apt-get install -y --no-install-recommends ca-certificates && rm -rf /var/lib/apt/lists/*
COPY --from=trivy-downloader /usr/local/bin/trivy /usr/local/bin/trivy

USER spring

COPY --chown=spring --chmod=774 ./docker/scripts/ /scripts/
COPY --chown=spring ./target/insights*.jar /app/insights.jar

EXPOSE 8080

ENTRYPOINT ["/scripts/entrypoint.sh"]
CMD ["java","-jar","/app/insights.jar"]
