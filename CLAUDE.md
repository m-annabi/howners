# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository layout

This is a two-app monorepo for **Howners**, a rental-property management platform. Both apps live at the repo root:

- `howners-back/` — Spring Boot 4.0.2 / Java 21 REST API (Maven). Package root `com.howners.gestion`.
- `howners-api/` — Angular 15 frontend (despite the name, this is the **frontend**, not an API).
- `docker-compose.yml` — local infra: PostgreSQL 16, MinIO (S3), MailHog (SMTP capture). `docker-compose.prod.yml` is the production overlay.

The backend serves on `:8080`, the frontend dev server on `:4200`. Backend exposes everything under `/api/...`; the frontend's `environment.ts` points at `http://localhost:8080/api`.

## Common commands

### Bring up the whole stack

```bash
docker-compose up -d                 # postgres :5432, minio :9000/:9001, mailhog :1025/:8025
cd howners-back && ./mvnw spring-boot:run     # use the Maven wrapper, not system mvn
cd howners-api && npm install && npm start    # Angular CLI dev server on :4200
```

`howners-back/start.sh` is a convenience wrapper that sources `.env` and runs `./mvnw spring-boot:run`. **Never inline secrets in scripts or any tracked file** — all credentials (SMTP, DocuSign, Stripe…) belong in `.env`, which is gitignored. (A former `restart-backend.sh` that hardcoded credentials was purged from history after those secrets leaked; it is gitignored to prevent reintroduction. Use `start.sh`.)

For a step-by-step local bootstrap (services, default credentials, URLs to verify), see `SETUP_GUIDE.md` at the repo root. Other top-level docs worth knowing: `OBSERVABILITY.md` (metrics/logging), `RGPD-AUDIT.md` (RGPD/audit obligations), `SECURITY-ROTATION.md` (secret rotation playbook), `NEXT-STEPS.md` (open roadmap items).

### Backend (`howners-back/`)

```bash
./mvnw test                                                  # all tests
./mvnw -Dtest=PropertyServiceTest test                       # single test class
./mvnw -Dtest=PropertyServiceTest#methodName test            # single test method
./mvnw clean package -DskipTests                             # build jar -> target/demo-0.0.1-SNAPSHOT.jar
java -jar target/demo-0.0.1-SNAPSHOT.jar                     # run the built jar
```

Spring Boot 4.0.2 requires **JDK 21**. The Maven wrapper does not provision a JDK — make sure `JAVA_HOME` (or your shell's default `java`) points at a 21 install before running `./mvnw`.

Profiles: `application.yml` (default), `application-dev.yml`, `application-prod.yml`. Activate with `-Dspring.profiles.active=dev`.

### Frontend (`howners-api/`)

```bash
npm start                  # ng serve on :4200
npm run build              # production build into dist/
npm test                   # Karma + Jasmine unit tests
npm run cypress:open       # interactive E2E
npm run cypress:run        # headless E2E (cypress/e2e/)
```

The API base URL is hardcoded in `src/environments/environment.ts` to `http://localhost:8080/api` (and in `environment.prod.ts` for prod builds). If you tunnel the backend onto another host/port, edit those files — there is no runtime override.

### Database / migrations

Schema is owned by **Liquibase** — `spring.jpa.hibernate.ddl-auto: none`. Never let Hibernate auto-generate DDL. Add a new changelog file under `howners-back/src/main/resources/db/changelog/` using the next free number (latest is `066-...`) and register it in `db.changelog-master.xml`. Number `017` is intentionally absent — keep the gap.

To reset the DB completely (destroys all data):

```bash
docker-compose down -v && docker-compose up -d
```

If a Liquibase lock is stuck:

```bash
docker exec -it howners-postgres psql -U howners_user -d howners_db \
  -c "UPDATE databasechangeloglock SET locked=false, lockgranted=null, lockedby=null;"
```

## Architecture

### Backend layering (`com.howners.gestion`)

Standard Spring layered architecture, but several cross-cutting subsystems are worth knowing before editing:

- `controller/` — REST entry points. ~35 controllers grouped by domain (property, rental, contract, document, signature, payment, invoice, listing, application, message, subscription, audit, rgpd, tenant-discovery, etc.). `PublicContractController` is the only auth-bypassed controller — it serves the tokenised `/sign` flow.
- `service/<domain>/` — business logic. Domains mirror the controllers and the `domain/` entity packages.
- `domain/<domain>/` — JPA entities. Each domain has its own subpackage (`property`, `rental`, `contract`, `signature`, `subscription`, `audit`, etc.).
- `repository/` — Spring Data JPA repositories.
- `dto/` — request/response DTOs; never expose entities directly.
- `security/` — JWT (`security/jwt/`), `UserPrincipal`, `RateLimitFilter`, and `ContractTokenProvider` (signed tokens for public sign links).
- `config/` — `SecurityConfig` (Spring Security wiring), `S3Config` + `StorageProperties` (MinIO), `StripeConfig`, `DocuSignProperties`, `RateLimitConfig`, `AuditAspect` (AOP-based audit log writes), `JpaAuditConfig`, `StartupConfigValidator`.
- `exception/` — typed exceptions + `@RestControllerAdvice` global handler.

Key cross-cutting behaviours:
- **AuthN**: JWT (HS512, JJWT 0.12.5). Filter chain in `SecurityConfig`. `UserPrincipal` is the authenticated principal.
- **AuthZ**: RBAC with roles `OWNER`, `TENANT`, `ADMIN` (and references to `CONCIERGE` in route guards). Enforced via Spring `@PreAuthorize` on services/controllers — listings expose public read but owner-only writes are gated this way; do not rely on URL-level rules alone.
- **Storage**: All file uploads go through the `service/storage/` abstraction backed by S3 (`software.amazon.awssdk`) targeting MinIO locally. SHA-256 hashes are stored alongside documents/contracts for integrity checks.
- **PDF generation**: iText (`itext-core` + `html2pdf`) renders contracts from Thymeleaf-style templates with ~30 substitution variables. Contracts are versioned (`contract_versions`) and each version has a stored `file_key` in S3.
- **E-signature**: Two providers coexist. `signature/` (controller + service) handles the in-app HTML5 canvas signature; `esignature/` integrates DocuSign (JWT auth, requires RSA keypair, demo URL by default). The provider per contract is tracked in the `signature_provider` column. Public sign flow: backend mints a `ContractTokenProvider` token, emails it via Thymeleaf template + Spring Mail, recipient lands on `/sign?token=...` (Angular `public-sign` module, no `AuthGuard`).
- **Audit**: `AuditAspect` writes to `audit_logs` via AOP. RGPD endpoints (`RgpdController`) handle export/anonymisation (`anonymized_at` on users, `user_consents` table).
- **Payments / billing**: Stripe SDK (`stripe-java`). Webhooks land on `WebhookController`. Subscriptions, plans, and usage tracking live in `domain/subscription/` (changelogs 040–043).

### Frontend (`howners-api/src/app/`)

Standard Angular module structure, all feature modules **lazy-loaded** from `app-routing.module.ts`:

- `core/` — singletons: `auth/`, `guards/` (`AuthGuard`, `RoleGuard` — reads `data: { roles: [...] }` from routes), `interceptors/` (`auth.interceptor` attaches JWT, `error.interceptor` central error handling), `services/`, `models/`.
- `features/<domain>/` — one lazy module per domain (admin, applications, audit, auth, billing, contracts, dashboard, expenses, financial, inventory, invoices, landing, listings, messages, payments, profile, properties, public-sign, ratings, receipts, referral, rentals, templates, tenant-discovery, tenant-search) plus a `not-found` fallback route.
- `shared/` — reusable components (`signature-pad`, `document-upload`, `document-list`, etc.) re-exported via `shared.module.ts`.

Routing rules to respect when adding features:
- Wire new feature modules into `app-routing.module.ts` with `loadChildren`.
- Protect with `AuthGuard`; add `RoleGuard` + `data: { roles: [...] }` for role-restricted areas.
- `listings` and `sign` are intentionally **not** behind `AuthGuard` — listings have public browse with backend-side `@PreAuthorize` on owner mutations; `sign` is the tokenised public signing flow.

## Environment & configuration

- Root `.env` is the source of truth for the backend (loaded via `spring-dotenv`); `.env.example` documents every variable. Backend also accepts plain Spring env vars (`DATABASE_URL`, `JWT_SECRET`, `MINIO_*`, `STRIPE_*`, `DOCUSIGN_*`, `SMTP_*`, `BACKEND_URL`, `FRONTEND_URL`, `CORS_ALLOWED_ORIGINS`).
- Locally, SMTP is wired to MailHog (`localhost:1025`); read sent emails at http://localhost:8025.
- MinIO console at http://localhost:9001 (login `minioadmin` / `minioadmin123`); the bucket `howners-documents` is auto-created by the `minio-setup` compose service.
- DocuSign defaults to the **demo** environment; without DocuSign credentials, the e-signature send flow will partially fail (email still sent, envelope creation throws). The in-app canvas signature path works without DocuSign.
- Multipart limit is 10MB (both `spring.servlet.multipart.*` and the document service).

## Conventions

- Comments and many identifiers are in **French** — match the existing language when extending a module rather than mixing.
- Use Lombok annotations for entities/DTOs (already on the classpath).
- New endpoints: add controller + service + DTOs; do not return entities. Apply `@PreAuthorize` for role checks at the service or controller level.
- Touching contracts/signatures: preserve the versioning + SHA-256 + S3 `file_key` invariants; a contract is immutable once signed.
