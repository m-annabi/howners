# Observability — current state and how to plug Sentry/Datadog

## What's exposed today (no external account)

Spring Boot Actuator endpoints, available at `http://localhost:8080/actuator/*`:

| Endpoint | Auth | Purpose |
|---|---|---|
| `/actuator/health` | public | liveness + readiness (`/health/liveness`, `/health/readiness`) |
| `/actuator/info` | public | git commit (when `info.git.mode=simple` is in classpath) |
| `/actuator/metrics` | public | JVM, HTTP, DB pool, Liquibase, etc. |
| `/actuator/prometheus` | public | Prometheus scrape format |

The path matchers `permitAll` for `/actuator/**` in `SecurityConfig` so a Prometheus or Kubernetes probe can hit them without auth. **In prod**, lock `/actuator/prometheus` to an internal IP via the load balancer.

## Sentry (backend) — to plug

Add to `howners-back/pom.xml`:

```xml
<dependency>
    <groupId>io.sentry</groupId>
    <artifactId>sentry-spring-boot-starter-jakarta</artifactId>
    <version>7.x.x</version>
</dependency>
```

Set the DSN in `.env`:
```
SENTRY_DSN=https://your-key@sentry.io/your-project-id
SENTRY_TRACES_SAMPLE_RATE=0.1   # 10% of transactions, adjust by traffic
SENTRY_ENVIRONMENT=prod         # or staging, dev
```

`application.yml` will pick them up automatically (Sentry starter reads `SENTRY_*`).

## Sentry (frontend Angular)

```bash
cd howners-api
npm install @sentry/angular
```

In `src/main.ts`:
```ts
import * as Sentry from '@sentry/angular';
Sentry.init({
  dsn: environment.sentryDsn,
  environment: environment.production ? 'prod' : 'dev',
  tracesSampleRate: 0.1,
  integrations: [Sentry.browserTracingIntegration(), Sentry.replayIntegration()],
});
```

Set `sentryDsn` in `src/environments/environment*.ts`.

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
