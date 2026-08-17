FROM eclipse-temurin:25.0.3_9-jre-jammy AS base
ARG GID=2000
ARG UID=2000

RUN addgroup --gid ${GID} spring && \
    adduser --uid ${UID} --gid ${GID} --shell /usr/sbin/nologin spring

RUN apt-get update && \
    apt-get install -y --no-install-recommends ca-certificates && \
    rm -rf /var/lib/apt/lists/*

COPY --chown=spring --chmod=774 ./docker/scripts/ /scripts/


FROM base AS webapp

LABEL org.opencontainers.image.title="insights-webapp" \
      org.opencontainers.image.description="Frank!Framework Insights webapp: REST API and Angular frontend" \
      org.opencontainers.image.source="https://github.com/frankframework/insights"

USER spring

COPY --chown=spring ./insights-webapp/target/insights-webapp-*.jar /app/insights-webapp.jar

EXPOSE 8080

ENTRYPOINT ["/scripts/entrypoint.sh"]
CMD ["java","-jar","/app/insights-webapp.jar"]


FROM aquasec/trivy:0.73.0 AS trivy-downloader

FROM base AS data-import

LABEL org.opencontainers.image.title="insights-data-import" \
      org.opencontainers.image.description="Frank!Framework Insights data import service: GitHub ingestion and Trivy vulnerability scanning" \
      org.opencontainers.image.source="https://github.com/frankframework/insights"

USER root
RUN apt-get update && \
    apt-get install -y --no-install-recommends maven && \
    rm -rf /var/lib/apt/lists/*
COPY --from=trivy-downloader /usr/local/bin/trivy /usr/local/bin/trivy

USER spring

COPY --chown=spring ./insights-data-import/target/insights-data-import-*.jar /app/insights-data-import.jar

EXPOSE 8081

ENTRYPOINT ["/scripts/entrypoint.sh"]
CMD ["java","-jar","/app/insights-data-import.jar"]
