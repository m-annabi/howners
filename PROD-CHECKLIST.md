# ✅ Checklist de mise en production — Howners

Checklist ordonnée et exécutable pour passer Howners en production. Les phases sont
séquentielles : ne pas démarrer une phase tant que la précédente n'est pas verte.

> Docs complémentaires : `SECURITY-ROTATION.md` (secrets compromis), `OBSERVABILITY.md`
> (monitoring), `RGPD-AUDIT.md` (conformité CNIL), `NEXT-STEPS.md` (backlog),
> `SETUP_GUIDE.md` (bootstrap local).

Légende : 🔴 bloquant · 🟡 important · 🟠 à planifier

---

## Phase 0 — Pré-déploiement (code)

- [x] Travail des 10 chantiers business committé et poussé sur `origin/main`
- [x] `cd howners-back && ./mvnw clean test` → **137 tests verts** (vérifié)
- [x] `cd howners-api && npm run build` → build prod OK (vérifié)
- [ ] 🟡 Relire le diff de `application-prod.yml` : `ddl-auto: none`, swagger désactivé, logs en WARN
- [x] Aucun `.env` suivi par Git (`.gitignore` durci `.env.*`, vérifié)

---

## Phase 1 — 🔴 Sécurité : secrets (BLOQUANT)

### 1.1 Révoquer les secrets compromis (historique Git) — ✅ FAIT
Voir `SECURITY-ROTATION.md`. L'historique Git a aussi été purgé (`restart-backend.sh`
retiré de toutes les branches via `git filter-repo` + force-push).

- [x] **Gmail** : mot de passe d'app révoqué (confirmé). Migrer vers un fournisseur transactionnel reste recommandé.
- [x] **DocuSign** : RSA keypair + integration key révoquées (confirmé).
- [x] Historique Git purgé des secrets (branches `main`, `Olivier`, `claude/…`). Résiduel : `refs/pull/1/head` (géré par GitHub, demander purge au support si besoin).

### 1.2 Générer les secrets de production
- [x] `JWT_SECRET` (72 ch.), `POSTGRES_PASSWORD`, `MINIO_ROOT_PASSWORD` générés dans `.env.prod` (chmod 600, gitignoré)
- [ ] 🔴 Compléter `.env.prod` avec les valeurs **externes** : Stripe live, SMTP, domaine, CORS (cf. 1.3 + Phase 3)

### 1.3 Variables d'environnement à renseigner (`.env`)
| Variable | Valeur prod |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `POSTGRES_DB/USER/PASSWORD` | DB de prod |
| `JWT_SECRET` | secret généré (64+ chars) |
| `JWT_EXPIRATION` | ex. `86400000` (24h) |
| `MINIO_ROOT_USER/PASSWORD`, `MINIO_BUCKET` | identifiants S3/MinIO prod |
| `SMTP_*`, `EMAIL_FROM` | fournisseur transactionnel (nouvelle clé) |
| `STRIPE_SECRET_KEY/PUBLIC_KEY` | **clés live** (`sk_live_…` / `pk_live_…`) |
| `STRIPE_WEBHOOK_SECRET` | secret du webhook prod (cf. Phase 3) |
| `STRIPE_PLATFORM_FEE_PERCENT` | `2.5` (fallback ; la valeur réelle vient du plan) |
| `DOCUSIGN_*` | nouvelles clés (ou laisser vide → signature canvas seule) |
| `BACKEND_URL` | `https://api.howners.com` |
| `FRONTEND_URL` | `https://howners.com` |
| `CORS_ALLOWED_ORIGINS` | `https://howners.com` (domaine prod exact, pas de `*`) |

---

## Phase 2 — 🔴 Infrastructure

### 2.1 HTTPS + reverse proxy ✅ (config fournie)
Le reverse proxy **Caddy** est intégré (`Caddyfile` + service `caddy` dans
`docker-compose.prod.yml`). Caddy est le seul service exposé sur l'hôte (`80`/`443`) ;
`backend` et `frontend` ne sont plus publiés (directive `expose`, réseau interne).
TLS Let's Encrypt automatique, redirection HTTP→HTTPS, HSTS.

- [ ] 🔴 Renseigner `APP_DOMAIN` et `ACME_EMAIL` dans le `.env` (cf. `.env.example`)
- [ ] 🔴 A records DNS : `APP_DOMAIN` **et** `api.APP_DOMAIN` → IP du serveur (les deux requis
  pour l'émission des certificats)
- [ ] 🟡 1er déploiement : décommenter `acme_ca …staging…` dans le `Caddyfile` pour tester
  l'obtention de certificat sans épuiser le quota Let's Encrypt, puis recommenter
- [ ] 🟡 Vérifier que les ports 80 et 443 sont ouverts sur le firewall/cloud du serveur

### 2.2 Frontend : URL d'API
`environment.prod.ts` pointe en dur sur `https://api.howners.com/api`.
- [ ] 🟡 Adapter `howners-api/src/environments/environment.prod.ts` (`apiUrl`, `wsUrl`) au vrai domaine
- [ ] 🟡 Rebuild l'image frontend après modification

### 2.3 Images & registry
La CI (`.github/workflows/ci.yml`) build **et pousse** les images sur **GHCR** à chaque
push sur `main` : `ghcr.io/m-annabi/howners-backend` et `…/howners-frontend`
(tags `latest` + sha court). Sur les PR, les images sont buildées sans push (validation).

- [x] CI pousse les images Docker (GHCR via `GITHUB_TOKEN`, `packages: write`)
- [ ] 🔴 **Premier push** : GitHub crée les packages en **privé**. Sur le serveur de prod,
  s'authentifier à GHCR pour les pull :
  ```bash
  echo "$GHCR_PAT" | docker login ghcr.io -u <user> --password-stdin   # PAT scope read:packages
  ```
  (ou rendre les packages publics : UI GitHub → Packages → Package settings.)
- [ ] 🟡 Déployer depuis le registry : dans `docker-compose.prod.yml`, commenter `build:` et
  décommenter `image: ghcr.io/m-annabi/howners-*:latest`, puis `docker compose pull && up -d`.
- [ ] 🟠 (multi-instance) Externaliser le rate-limiting en Redis — actuellement en mémoire par instance

### 2.4 Sauvegardes
Scripts fournis : `scripts/db-backup.sh` (dump custom + intégrité + rétention) et
`scripts/db-restore.sh` (restauration + mode `--verify-latest` non destructif).
Voir `scripts/README.md`. Testés de bout en bout (dump → restauration scratch → 41 tables).

- [x] Script de **backup** PostgreSQL (`pg_dump -Fc` + contrôle PGDMP + rétention/MIN_KEEP)
- [x] Script de **restauration** + vérification non destructive (`--verify-latest`)
- [ ] 🔴 Installer le **cron quotidien** sur le serveur (ligne dans `scripts/README.md`)
  ```cron
  15 3 * * * BACKUP_DIR=/var/backups/howners /opt/howners/scripts/db-backup.sh >> /var/log/howners-backup.log 2>&1
  0  4 * * 0 BACKUP_DIR=/var/backups/howners /opt/howners/scripts/db-restore.sh --verify-latest >> /var/log/howners-backup.log 2>&1
  ```
- [x] **Copie off-site** intégrée à `db-backup.sh` (best-effort après chaque dump) — reste à
  renseigner `BACKUP_RCLONE_REMOTE` **ou** `BACKUP_S3_URI` côté serveur (un backup sur le
  seul hôte ne protège pas d'une perte de l'hôte)
- [ ] 🟡 Brancher `BACKUP_HEALTHCHECK_URL` (healthchecks.io) pour être alerté si le backup ne tourne pas
- [ ] 🟡 Vérifier la persistance des volumes `postgres_data` et `minio_data`
- [ ] 🟡 Tester une **restauration** au moins une fois — `./scripts/db-restore.sh --verify-latest`

---

## Phase 3 — 🟡 Intégrations externes

### 3.1 Stripe (live)

**Déjà câblé côté code** (rien à développer) : Checkout abonnement (redirection), webhook
`/api/webhooks/stripe` à **signature vérifiée**, routage Connect du loyer (`application_fee`
dégressif par plan + `transfer_data`), et récompense de parrainage des abonnés Stripe (coupon
100 % sur la prochaine échéance = 1 mois offert).

Actions (compte / serveur) :
- [ ] 🔴 Renseigner les clés **live** dans `.env` : `STRIPE_SECRET_KEY`, `STRIPE_PUBLIC_KEY`,
  `STRIPE_PLATFORM_FEE_PERCENT` (repli ; la commission réelle est dégressive par plan).
- [ ] 🔴 Créer le **webhook prod** → `https://api.<DOMAINE>/api/webhooks/stripe`, événements :
  `customer.subscription.created/updated/deleted`, `checkout.session.completed`,
  `payment_intent.succeeded`, `payment_intent.payment_failed`. Copier le signing secret dans
  `STRIPE_WEBHOOK_SECRET` (sans lui, les webhooks signés sont rejetés en 400).
- [ ] 🟡 Créer les **Prices** (PRO, PREMIUM, AGENCE — mensuel + annuel) et renseigner
  `stripe_price_id_*` en base. **Turnkey** (idempotent) :
  ```bash
  STRIPE_SECRET_KEY=sk_live_xxx ./scripts/setup-stripe-prices.sh --dry-run   # aperçu
  STRIPE_SECRET_KEY=sk_live_xxx ./scripts/setup-stripe-prices.sh             # crée + écrit en base
  ```
- [ ] 🟡 Activer **Stripe Connect (Express)** si l'encaissement des loyers est dans le scope du lancement.
- [ ] 🟡 **Tester en mode TEST d'abord** (cartes `4242…`) : abonnement bout-en-bout + un parrainage
  (vérifier que le coupon apparaît sur l'abonnement Stripe), puis basculer en live et redémarrer le backend.

### 3.2 Données métier
- [x] **Indices IRL** : valeurs 2022→2026 T1 seedées et corrigées (changelogs 071 + 078,
  vérifiées sur la table ANIL). Compléter les trimestres suivants via `POST /api/irl-indices`
  (rôle ADMIN) au fil des publications INSEE (série 001515333).
  Sans ça, la révision de loyer échoue avec « indice manquant ».
- [ ] 🟠 Vérifier/retirer les données seed de démo (`054`, `055`, `056`…) si présence non souhaitée en prod

### 3.3 Email & stockage
- [ ] 🔴 SMTP transactionnel réel testé (un email de bienvenue doit partir sans erreur d'auth)
- [ ] 🟡 Bucket S3/MinIO de prod créé, credentials testés (upload d'un document → presigned URL OK)

---

## Phase 4 — 🟡 Observabilité

Voir `OBSERVABILITY.md` pour les procédures détaillées.

- [x] **Sentry câblé** backend (appender Logback) + frontend (`@sentry/angular-ivy`), gated sur DSN
- [x] `/actuator/**` (hors `/health`) **réservé à ADMIN** en prod (vérifié : 401 sans auth)
- [x] Métriques **Prometheus** exposées (`/actuator/prometheus`, ADMIN-gated)
- [ ] 🟡 Créer les projets **Sentry** (back + front), renseigner `SENTRY_DSN` (.env) et
  `sentryDsn` (`environment.prod.ts`). DSN vide ⇒ Sentry off (no-op).
- [ ] 🟡 **Uptime monitoring** sur `https://api.howners.com/actuator/health` (UptimeRobot, etc.)
- [ ] 🟠 Agrégation des logs (les conteneurs loggent sur stdout → brancher journald/CloudWatch/Loki)
- [ ] 🟠 Restreindre `/actuator/prometheus` à une IP interne via le reverse proxy si scrapé

---

## Phase 5 — 🟠 Juridique & RGPD

- [ ] 🔴 **Relecture juridique** des documents générés à portée légale (faits dans les nouveaux chantiers) :
  mise en demeure (art. 24 loi 89-462), courrier de révision IRL, décompte de régularisation
  des charges, disclaimer de l'export 2044.
- [ ] 🟠 RGPD (cf. `RGPD-AUDIT.md`) : suppression S3 à l'anonymisation, UI de consentement,
  suivi du délai légal d'1 mois, registre art. 30, mention DPO + bannière cookies.
  → tolérable sur un lancement bêta fermé, **obligatoire** pour une ouverture publique.
- [ ] 🟠 CGU / CGV / politique de confidentialité publiées

---

## Phase 6 — Déploiement & vérification

### 6.1 Déployer
```bash
# Sur le serveur, avec le .env de prod en place
docker compose -f docker-compose.prod.yml up -d --build
docker compose -f docker-compose.prod.yml ps          # tous "healthy"
docker compose -f docker-compose.prod.yml logs -f backend
# Attendre "Started HownersApplication" + validation StartupConfigValidator OK
```

### 6.2 Vérifications post-déploiement
- [ ] `curl https://api.howners.com/actuator/health` → `{"status":"UP"}`
- [ ] Migrations Liquibase 067→076 appliquées :
  ```sql
  SELECT id FROM databasechangelog WHERE id LIKE '07%' ORDER BY orderexecuted DESC LIMIT 10;
  ```
- [ ] `https://howners.com` se charge en HTTPS (cadenas valide, pas de mixed content)
- [ ] Inscription d'un compte test → login → création d'un bien (parcours golden path)
- [ ] Un email transactionnel arrive réellement
- [ ] CORS : le front prod appelle l'API sans erreur cross-origin
- [ ] Headers de sécurité présents : `curl -I https://howners.com` → HSTS, CSP, X-Frame-Options

### 6.3 Rollback
- [ ] Procédure de rollback documentée :
  ```bash
  # Revenir à l'image précédente
  docker compose -f docker-compose.prod.yml down
  git checkout <commit-précédent> && docker compose -f docker-compose.prod.yml up -d --build
  ```
- [ ] ⚠️ Les migrations Liquibase ne sont **pas auto-réversibles** : prévoir un backup DB
  **avant** chaque déploiement (Phase 2.4) et un rollback SQL manuel si nécessaire.

---

## Récapitulatif des bloquants 🔴 avant ouverture publique

| # | Bloquant | Statut | Qui |
|---|---|---|---|
| 1 | Secrets compromis révoqués (Gmail, DocuSign) + historique purgé | ✅ fait | — |
| 2 | Secrets de prod générés, `.env` hors Git | ✅ fait (infra) — reste à remplir Stripe/SMTP/domaine | toi |
| 3 | HTTPS + reverse proxy (Caddy) | ✅ config faite — reste DNS (2 A records) | toi (DNS) |
| 4 | Backups PostgreSQL + restauration testée | ✅ scripts faits & testés (off-site intégré) — reste cron + 1 var off-site | toi (serveur) |
| 5 | Stripe live + webhook prod | ✅ code complet (Checkout, webhook signé, Connect, coupon parrainage, script Prices) — reste clés live + webhook + Prices (compte Stripe) | toi |
| 6 | SMTP réel fonctionnel | ⛔ compte fournisseur requis | toi |
| 7 | Documents légaux relus par un juriste | ⛔ avocat requis | toi |

**Codable côté Howners : terminé.** Les bloquants restants sont des actions externes
(DNS, serveur, comptes Stripe/SMTP, avocat) que le code ne peut pas déverrouiller.
