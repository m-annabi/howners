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

## À prévoir côté ops (hors scripts)

- **Off-site** : ces dumps restent sur l'hôte. Pousser `$BACKUP_DIR` vers un stockage
  distant (S3/MinIO bucket dédié, `rclone`, `aws s3 sync`) protège d'une perte de l'hôte.
- **Persistance des volumes** : `postgres_data` et `minio_data` (docker-compose.prod.yml)
  doivent être sur un disque persistant et inclus dans la stratégie de sauvegarde de l'hôte.
- **Chiffrement** : si les dumps quittent l'hôte, les chiffrer (`age`, `gpg`) — ils
  contiennent toutes les données clients.
