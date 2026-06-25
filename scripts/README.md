# scripts/ — opérations Howners

Scripts d'exploitation, conçus pour tourner depuis l'hôte via `docker exec` sur le
conteneur PostgreSQL (`howners-postgres`). Aucun client `psql`/`pg_dump` requis sur l'hôte.

## Sauvegarde — `db-backup.sh`

`pg_dump` au format custom (compressé, restaurable sélectivement) + contrôle d'intégrité
+ rétention.

```bash
# Manuel
BACKUP_DIR=/var/backups/howners ./scripts/db-backup.sh

# Cron quotidien (utilisateur ayant accès à docker), 3h15
15 3 * * * BACKUP_DIR=/var/backups/howners /opt/howners/scripts/db-backup.sh >> /var/log/howners-backup.log 2>&1
```

Variables (défauts) : `POSTGRES_CONTAINER` [howners-postgres] · `BACKUP_DIR`
[/var/backups/howners] · `BACKUP_RETENTION_DAYS` [14] · `BACKUP_MIN_KEEP` [3] ·
`BACKUP_HEALTHCHECK_URL` [] (ping de succès → dead-man switch, ex. healthchecks.io) ·
`ENV_FILE` [`<repo>/.env`] d'où sont lus `POSTGRES_USER/DB/PASSWORD`.

**Copie off-site** (recommandée — un backup sur le seul hôte ne protège pas d'une perte
de l'hôte). Renseigner l'UN des deux ; la copie est best-effort (un échec n'invalide pas
le backup local) :
- `BACKUP_RCLONE_REMOTE` — ex. `s3prod:howners-backups/db` (nécessite `rclone` configuré)
- `BACKUP_S3_URI` — ex. `s3://howners-backups/db/` (nécessite l'AWS CLI configurée)

`BACKUP_MIN_KEEP` est un garde-fou : on conserve toujours les N dumps les plus récents,
même au-delà de la rétention — ainsi un cron en panne ne purge jamais tout à zéro.

## Restauration & vérification — `db-restore.sh`

```bash
# Vérifier que le DERNIER backup est restaurable (NON destructif) :
# restaure dans une base jetable, compte les tables, puis la supprime.
BACKUP_DIR=/var/backups/howners ./scripts/db-restore.sh --verify-latest

# Restauration RÉELLE en production (DESTRUCTIF, confirmation demandée) :
./scripts/db-restore.sh --latest
./scripts/db-restore.sh /var/backups/howners/howners-howners_db-20260618-031500.dump
```

> Un backup jamais restauré n'est pas un backup. Lancez `--verify-latest` régulièrement
> (idéalement en cron hebdomadaire ou juste après le backup).

```bash
# Exemple : backup quotidien + vérif de restauration hebdo (dimanche 4h)
0 4 * * 0 BACKUP_DIR=/var/backups/howners /opt/howners/scripts/db-restore.sh --verify-latest >> /var/log/howners-backup.log 2>&1
```

## Prix Stripe — `setup-stripe-prices.sh`

Crée (idempotemment) les Produits + Prix Stripe pour les plans payants (PRO, PREMIUM,
AGENCE) et écrit les `stripe_price_id_monthly` / `stripe_price_id_annual` en base — l'étape
qui rend les abonnements vendables (le front lit ces price IDs pour ouvrir le Checkout).

```bash
# Aperçu sans rien créer (lit la base, n'appelle pas Stripe) :
STRIPE_SECRET_KEY=sk_test_xxx ./scripts/setup-stripe-prices.sh --dry-run

# Création réelle (clé LIVE) + écriture en base :
STRIPE_SECRET_KEY=sk_live_xxx ./scripts/setup-stripe-prices.sh

# Recréer même si déjà configuré :
STRIPE_SECRET_KEY=sk_live_xxx ./scripts/setup-stripe-prices.sh --force
```

Idempotent : un plan déjà renseigné en base est ignoré (sauf `--force`) ; côté Stripe le
Produit est réutilisé (id `howners_<plan>`) et un Prix actif au bon montant/intervalle
n'est jamais dupliqué. `STRIPE_SECRET_KEY` peut aussi venir de `.env`. Le webhook prod
(events checkout/subscription) reste à activer dans le Dashboard Stripe.

## Secrets — `manage-secrets.sh`

Générer, roter et valider les secrets, en complément de `SECURITY-ROTATION.md` (playbook)
et de `StartupConfigValidator` (le backend refuse de démarrer en prod sur un secret faible).

```bash
# Générer des secrets forts à coller dans .env.prod (valeurs neuves, jamais committées) :
./scripts/manage-secrets.sh generate all

# Roter une variable (secret neuf + backup, refuse d'écrire dans un fichier git-tracké) :
./scripts/manage-secrets.sh rotate POSTGRES_PASSWORD .env.prod

# Contrôler un fichier d'env sans afficher les valeurs (manquant / faible / exemple / court) :
./scripts/manage-secrets.sh doctor .env.prod
```

Le script n'**affiche jamais** la valeur d'un secret existant (`doctor` ne montre que
longueur + verdict) et refuse d'écrire dans un fichier suivi par git. Stripe/SMTP/webhook
ne se génèrent pas (ils viennent des fournisseurs). Après une rotation : redémarrer le
backend et **révoquer** l'ancienne valeur côté service si c'était une clé externe.

## À prévoir côté ops (hors scripts)

- **Off-site** : intégré à `db-backup.sh` — renseigner `BACKUP_RCLONE_REMOTE` **ou**
  `BACKUP_S3_URI` (voir plus haut). Sans l'une de ces variables, les dumps restent sur
  l'hôte et ne survivent pas à sa perte.
- **Persistance des volumes** : `postgres_data` et `minio_data` (docker-compose.prod.yml)
  doivent être sur un disque persistant et inclus dans la stratégie de sauvegarde de l'hôte.
- **Chiffrement** : si les dumps quittent l'hôte, les chiffrer (`age`, `gpg`) — ils
  contiennent toutes les données clients.
