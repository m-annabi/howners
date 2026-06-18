# ✅ Checklist de mise en production — Howners

Checklist ordonnée et exécutable pour passer Howners en production. Les phases sont
séquentielles : ne pas démarrer une phase tant que la précédente n'est pas verte.

> Docs complémentaires : `SECURITY-ROTATION.md` (secrets compromis), `OBSERVABILITY.md`
> (monitoring), `RGPD-AUDIT.md` (conformité CNIL), `NEXT-STEPS.md` (backlog),
> `SETUP_GUIDE.md` (bootstrap local).

Légende : 🔴 bloquant · 🟡 important · 🟠 à planifier

---

## Phase 0 — Pré-déploiement (code)

- [x] Travail des 10 chantiers business committé et poussé (`13eda13` sur `origin/main`)
- [ ] 🔴 `cd howners-back && ./mvnw clean test` → **132 tests verts**
- [ ] 🔴 `cd howners-api && npm run build` → build prod sans erreur (warnings de budget tolérés)
- [ ] 🟡 Relire le diff de `application-prod.yml` : `ddl-auto: none`, swagger désactivé, logs en WARN
- [ ] 🟡 Confirmer qu'aucun `.env` n'est suivi par Git : `git ls-files | grep -E '\.env$'` → **vide**

---

## Phase 1 — 🔴 Sécurité : secrets (BLOQUANT)

### 1.1 Révoquer les secrets compromis (historique Git)
Voir `SECURITY-ROTATION.md` pour le détail. Ces secrets sont **définitivement exposés**.

- [ ] 🔴 **Gmail** : supprimer le mot de passe d'app `***REVOKED***` sur
  https://myaccount.google.com/apppasswords. Idéalement migrer vers SendGrid/Brevo/Postmark.
- [ ] 🔴 **DocuSign** : révoquer la RSA keypair + integration key compromise sur
  https://account-d.docusign.com (Settings → Apps and Keys). La clé RSA permet de
  signer des JWT d'impersonation → priorité absolue.
- [ ] 🔴 Vérifier que les anciennes clés ne fonctionnent plus (procédure curl dans `SECURITY-ROTATION.md`)

### 1.2 Générer les secrets de production
- [ ] 🔴 `JWT_SECRET` — min 64 caractères, aléatoire :
  ```bash
  openssl rand -base64 64 | tr -d '\n'
  ```
  (`StartupConfigValidator` refuse de démarrer en prod si le secret par défaut est utilisé.)
- [ ] 🔴 `POSTGRES_PASSWORD` et `MINIO_ROOT_PASSWORD` — forts et uniques :
  ```bash
  openssl rand -base64 32
  ```
- [ ] 🔴 Créer le `.env` de prod à partir de `.env.example`, **hors Git**, permissions `600` :
  ```bash
  cp .env.example .env && chmod 600 .env
  # puis remplir chaque variable (cf. liste ci-dessous)
  ```

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
La CI (`.github/workflows/ci.yml`) build les images mais ne les pousse pas.
- [ ] 🟠 Configurer un registry (GHCR/DockerHub/ECR) et ajouter le `docker push` à la CI
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
- [ ] 🔴 **Copie off-site** des dumps (S3/MinIO/`rclone`) — un backup sur le seul hôte ne
  protège pas d'une perte de l'hôte
- [ ] 🟡 Brancher `BACKUP_HEALTHCHECK_URL` (healthchecks.io) pour être alerté si le backup ne tourne pas
- [ ] 🟡 Vérifier la persistance des volumes `postgres_data` et `minio_data`
- [ ] 🟡 Tester une **restauration** au moins une fois — `./scripts/db-restore.sh --verify-latest`

---

## Phase 3 — 🟡 Intégrations externes

### 3.1 Stripe (live)
- [ ] 🔴 Basculer en clés live dans `.env`
- [ ] 🔴 Créer le **webhook prod** Stripe → `https://api.howners.com/api/webhooks/stripe`,
  événements : `customer.subscription.*`, `payment_intent.succeeded`, `payment_intent.payment_failed`.
  Copier le signing secret dans `STRIPE_WEBHOOK_SECRET`.
- [ ] 🟡 Créer les **Prices** des plans payants (mensuel + annuel) côté Stripe, puis renseigner
  `stripe_price_id_monthly` / `stripe_price_id_annual` en base — **notamment le plan AGENCE**
  (inséré par le changelog 068 avec des `stripe_price_id` à `NULL`) :
  ```sql
  UPDATE subscription_plans SET stripe_price_id_monthly='price_xxx', stripe_price_id_annual='price_yyy' WHERE name='AGENCE';
  ```
- [ ] 🟡 Activer Stripe Connect (Express) si l'encaissement des loyers est dans le scope du lancement
- [ ] 🟡 Test de bout en bout : abonnement réel en mode test → vérifier conversion + récompense parrainage + email (MailHog/prod)

### 3.2 Données métier
- [ ] 🟡 **Indices IRL 2025-2026** : le seed (changelog 071) s'arrête à T4 2024. Compléter via
  `POST /api/irl-indices` (rôle ADMIN) avec les valeurs publiées sur insee.fr (série 001515333).
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

1. Secrets compromis révoqués (Gmail, DocuSign)
2. Secrets de prod générés, `.env` hors Git
3. HTTPS + reverse proxy en place
4. Backups PostgreSQL automatiques + restauration testée
5. Stripe live + webhook prod configurés
6. SMTP réel fonctionnel
7. Documents légaux relus par un juriste
