# Observability — current state and how to plug Sentry/Datadog

## What's exposed today (no external account)

Spring Boot Actuator endpoints, available at `http://localhost:8080/actuator/*`:

| Endpoint | Auth | Purpose |
|---|---|---|
| `/actuator/health` | **public** | liveness + readiness (`/health/liveness`, `/health/readiness`) |
| `/actuator/info` | **ADMIN** | git commit (when `info.git.mode=simple` is in classpath) |
| `/actuator/metrics` | **ADMIN** | JVM, HTTP, DB pool, Liquibase, etc. |
| `/actuator/prometheus` | **ADMIN** | Prometheus scrape format (micrometer registry) |

`SecurityConfig` permits only `/actuator/health`, then gates the rest behind `hasRole("ADMIN")`
(`/actuator/**`). Exposure is controlled by `management.endpoints.web.exposure.include` in
`application.yml` (`health,info,metrics,prometheus`).

**In prod**, a Prometheus scraper hitting `/actuator/prometheus` must authenticate as ADMIN —
prefer to additionally restrict it to an internal IP via the reverse proxy / load balancer, or
scrape over the Docker-internal network only.

## Sentry (backend) — wired ✅

Branché via l'**appender Logback** (`io.sentry:sentry-logback`), pas le starter Spring Boot.
Raison : le starter cible l'auto-config Spring Boot 3.x ; l'appender Logback est indépendant de
la version de Boot (ici 4.0.2) et ne dépend que de Logback (déjà présent).

- Dépendance : `io.sentry:sentry-logback` (`${sentry.version}` dans `pom.xml`).
- Config : `howners-back/src/main/resources/logback-spring.xml` — appender `SENTRY`
  (ERROR → événements, INFO → breadcrumbs) ajouté à côté de la console.
- Piloté par `application.yml` → `sentry.dsn / sentry.environment` (= `SENTRY_*` du `.env`).

Activer en prod : renseigner dans `.env`
```
SENTRY_DSN=https://your-key@sentry.io/your-project-id
SENTRY_ENVIRONMENT=prod
SENTRY_TRACES_SAMPLE_RATE=0.1
```
DSN vide ⇒ Sentry démarre désactivé (aucun envoi réseau). C'est l'état par défaut en dev/CI.

## Sentry (frontend Angular) — wired ✅

Package `@sentry/angular-ivy@7` (compatible Angular 15 ; `@sentry/angular` v8 vise Angular 14+
avec des APIs différentes).

- `src/main.ts` : `Sentry.init(...)` exécuté **uniquement si** `environment.sentryDsn` est non vide.
- `src/app/app.module.ts` : `ErrorHandler` Sentry fourni conditionnellement (DSN présent) ;
  sinon le `ErrorHandler` Angular par défaut est conservé (logs console en dev).
- `sentryDsn` (vide par défaut) dans `src/environments/environment*.ts` — renseigner le DSN du
  projet front au build pour la prod.

## Datadog (alternative)

For backend, dropin via the Datadog Java agent — no code change:

```bash
java -javaagent:/opt/dd-java-agent.jar -Ddd.service=howners-back -jar target/demo-*.jar
```

For frontend, Datadog RUM:

```bash
npm install @datadog/browser-rum
```

```ts
datadogRum.init({ applicationId, clientToken, site: 'datadoghq.eu', service: 'howners-frontend' });
```

## Logs aggregation

Spring Boot's default logback writes to stdout. For prod, prefer to ship via Vector or Fluent Bit:

- **Easy**: Datadog agent's log harvester
- **OSS**: Vector → Loki → Grafana (free)
- **Cheap**: BetterStack Logtail

## Minimum acceptable for a paying customer

If you can't afford a full observability stack, ship at least:

1. **Sentry free tier** (5k events/mo) on backend + frontend — catches every uncaught exception.
2. **UptimeRobot** hitting `/actuator/health` every minute — alerts on downtime.
3. **A way to read logs** (papertrail, datadog free, or even `journalctl` if VPS) — the prod-incident postmortem path.

Without these, you'll learn about every outage from your customers.
