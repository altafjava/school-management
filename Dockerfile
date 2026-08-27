# syntax=docker/dockerfile:1.7
#
# school-saas is the one runnable app in this workspace — platform-saas is a library with no
# Dockerfile of its own (see platform-saas/docker-compose.yml). This was a deliberately deferred
# gap (CLAUDE.md: "No JVM/GC/CDS tuning in-repo — no Dockerfile yet; fix the deployment pipeline
# first") — this file is that pipeline groundwork. Consistent with that same note, it stays
# unopinionated about JVM/GC/CDS tuning: JAVA_OPTS is empty by default and fully
# environment-overridable, not baked in here.
#
# ---------------------------------------------------------------------------
# Build stage — compiles school-saas and produces the Spring Boot executable jar.
#
# com.altafjava.platform:* resolves from mavenLocal() first (see settings.gradle). The
# "platform-m2" build context mounts the runner's ~/.m2/repository (populated by platform-saas's
# publishToMavenLocal on the same machine) read-only into the container. GitHub Packages
# (github_actor/platform_saas_token secrets) is only a fallback.
#
#   DOCKER_BUILDKIT=1 docker build \
#     --build-context platform-m2="$HOME/.m2/repository" \
#     --secret id=github_actor,env=GITHUB_ACTOR \
#     --secret id=platform_saas_token,env=PLATFORM_SAAS_TOKEN \
#     -t school-saas:local .
#
# CI (.github/workflows/docker-build.yml) passes the same context and secrets.
# ---------------------------------------------------------------------------
FROM eclipse-temurin:25-jdk-noble AS build
WORKDIR /workspace

COPY . .
RUN chmod +x gradlew

RUN --mount=type=secret,id=github_actor \
    --mount=type=secret,id=platform_saas_token \
    --mount=type=cache,target=/root/.gradle,sharing=locked \
    --mount=type=bind,from=platform-m2,target=/root/.m2/repository,ro \
    GITHUB_ACTOR="$(cat /run/secrets/github_actor 2>/dev/null || true)" \
    PLATFORM_SAAS_TOKEN="$(cat /run/secrets/platform_saas_token 2>/dev/null || true)" \
    ./gradlew :app:bootJar --no-daemon

# ---------------------------------------------------------------------------
# Runtime stage — minimal JRE, non-root, no build tooling, no Gradle cache.
# ---------------------------------------------------------------------------
FROM eclipse-temurin:25-jre-noble AS runtime

RUN groupadd --system school-saas && useradd --system --gid school-saas --home-dir /app school-saas
WORKDIR /app

COPY --from=build --chown=school-saas:school-saas /workspace/app/build/libs/*.jar app.jar

USER school-saas
EXPOSE 8080

# No JVM/GC/CDS tuning baked in — see file header. Override JAVA_OPTS per environment
# (e.g. -XX:MaxRAMPercentage, container-aware GC choice) at the deployment layer, not here.
ENV JAVA_OPTS=""
ENV SPRING_PROFILES_ACTIVE=prod

# Intentionally no Docker-level HEALTHCHECK: this image targets orchestrated deployment (K8s,
# ECS, ...), whose own liveness/readiness probes are the correct place to call the app's existing
# /actuator/health/liveness and /actuator/health/readiness endpoints (already exposed in
# application-prod.yml) — a second, Docker-native healthcheck would either duplicate that or,
# lacking curl/wget in this minimal JRE base, silently fail. Local `docker run` users can curl
# those endpoints directly.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
