FROM eclipse-temurin:21-jre-jammy AS runner
ARG GID=2000
ARG UID=2000

RUN addgroup --gid ${GID} spring && \
    adduser --uid ${UID} --gid ${GID} --shell /usr/sbin/nologin spring

USER spring

COPY --chown=spring --chmod=774 ./docker/scripts/ /scripts/
COPY --chown=spring ./target/insights*.jar /app/insights.jar

EXPOSE 8080

ENTRYPOINT ["/scripts/entrypoint.sh"]
CMD ["java","-jar","/app/insights.jar"]
