#!/usr/bin/env bash
#
# Sauvegarde PostgreSQL de Howners (format custom pg_dump, compressé & restaurable
# sélectivement). Conçu pour tourner depuis l'hôte via `docker exec` sur le conteneur
# postgres — aucun client psql/pg_dump requis sur l'hôte.
#
# Usage :
#   scripts/db-backup.sh
#
# Config (variables d'env, valeurs par défaut entre crochets) :
#   POSTGRES_CONTAINER      nom du conteneur postgres        [howners-postgres]
#   BACKUP_DIR              dossier de destination           [/var/backups/howners]
#   BACKUP_RETENTION_DAYS   purge les dumps plus vieux que N [14]
#   BACKUP_MIN_KEEP         garde toujours les N plus récents [3]
#   BACKUP_HEALTHCHECK_URL  ping sur succès (dead-man switch) []  ex: https://hc-ping.com/xxxx
#   ENV_FILE               .env d'où lire POSTGRES_*         [<repo>/.env]
#
# Les identifiants (POSTGRES_USER/DB/PASSWORD) sont lus depuis ENV_FILE, ou depuis
# l'environnement s'ils sont déjà exportés (ces derniers ont priorité).
#
# Cron quotidien à 3h15 (utilisateur ayant accès à docker) :
#   15 3 * * * BACKUP_DIR=/var/backups/howners /chemin/howners/scripts/db-backup.sh >> /var/log/howners-backup.log 2>&1

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

POSTGRES_CONTAINER="${POSTGRES_CONTAINER:-howners-postgres}"
BACKUP_DIR="${BACKUP_DIR:-/var/backups/howners}"
BACKUP_RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-14}"
BACKUP_MIN_KEEP="${BACKUP_MIN_KEEP:-3}"
BACKUP_HEALTHCHECK_URL="${BACKUP_HEALTHCHECK_URL:-}"
# Copie off-site (un backup sur le seul hôte ne protège pas d'une perte de l'hôte).
# Renseigner l'UN des deux :
#   BACKUP_RCLONE_REMOTE  ex: "s3prod:howners-backups/db"   (rclone copy)
#   BACKUP_S3_URI         ex: "s3://howners-backups/db/"     (aws s3 cp)
BACKUP_RCLONE_REMOTE="${BACKUP_RCLONE_REMOTE:-}"
BACKUP_S3_URI="${BACKUP_S3_URI:-}"
ENV_FILE="${ENV_FILE:-$REPO_ROOT/.env}"

log()  { printf '%s [db-backup] %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$*"; }

ping_fail() {
  [ -n "$BACKUP_HEALTHCHECK_URL" ] && command -v curl >/dev/null 2>&1 \
    && curl -fsS -m 10 --retry 2 "${BACKUP_HEALTHCHECK_URL}/fail" >/dev/null 2>&1 || true
}
fail() { log "ERREUR: $*"; ping_fail; exit 1; }

# --- Identifiants : .env d'abord (sans écraser un env déjà fourni) ---
if [ -f "$ENV_FILE" ]; then
  # Ne charge que les variables non déjà définies, pour respecter l'override par env.
  while IFS= read -r line; do
    case "$line" in
      POSTGRES_USER=*|POSTGRES_DB=*|POSTGRES_PASSWORD=*)
        key="${line%%=*}"
        eval "current=\${$key:-}"
        [ -z "$current" ] && export "$line"
        ;;
    esac
  done < "$ENV_FILE"
fi
: "${POSTGRES_USER:?POSTGRES_USER manquant (ENV_FILE=$ENV_FILE ou env)}"
: "${POSTGRES_DB:?POSTGRES_DB manquant}"
: "${POSTGRES_PASSWORD:?POSTGRES_PASSWORD manquant}"

# --- Préconditions ---
command -v docker >/dev/null 2>&1 || fail "docker introuvable"
[ "$(docker inspect -f '{{.State.Running}}' "$POSTGRES_CONTAINER" 2>/dev/null || echo false)" = "true" ] \
  || fail "conteneur '$POSTGRES_CONTAINER' non démarré"

mkdir -p "$BACKUP_DIR" || fail "impossible de créer $BACKUP_DIR"

TS="$(date '+%Y%m%d-%H%M%S')"
DEST="$BACKUP_DIR/howners-${POSTGRES_DB}-${TS}.dump"
TMP="${DEST}.partial"

log "Dump de '$POSTGRES_DB' (conteneur '$POSTGRES_CONTAINER') → $DEST"

# Format custom (-Fc) : compressé, restaurable par table. --no-owner/--no-privileges
# pour pouvoir restaurer dans un cluster aux rôles différents.
if ! docker exec -e PGPASSWORD="$POSTGRES_PASSWORD" "$POSTGRES_CONTAINER" \
      pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc --no-owner --no-privileges > "$TMP"; then
  rm -f "$TMP"
  fail "pg_dump a échoué"
fi

# --- Intégrité : en-tête magique du format custom + taille plausible ---
HEADER="$(head -c 5 "$TMP" 2>/dev/null || true)"
[ "$HEADER" = "PGDMP" ] || { rm -f "$TMP"; fail "format inattendu (en-tête='$HEADER', attendu 'PGDMP')"; }
SIZE="$(wc -c < "$TMP" | tr -d ' ')"
[ "$SIZE" -gt 1024 ] || { rm -f "$TMP"; fail "dump trop petit ($SIZE octets) — base vide ou échec silencieux ?"; }

mv "$TMP" "$DEST"
log "OK — $(du -h "$DEST" | cut -f1) ($SIZE octets). Vérification approfondie : scripts/db-restore.sh --verify-latest"

# --- Copie off-site (best-effort : un échec n'invalide pas le backup local) ---
if [ -n "$BACKUP_RCLONE_REMOTE" ]; then
  if command -v rclone >/dev/null 2>&1; then
    if rclone copy "$DEST" "$BACKUP_RCLONE_REMOTE" 2>&1; then
      log "Off-site OK (rclone) → $BACKUP_RCLONE_REMOTE"
    else
      log "ATTENTION: copie off-site rclone échouée (backup local conservé)"
    fi
  else
    log "ATTENTION: BACKUP_RCLONE_REMOTE défini mais 'rclone' introuvable"
  fi
elif [ -n "$BACKUP_S3_URI" ]; then
  if command -v aws >/dev/null 2>&1; then
    if aws s3 cp "$DEST" "$BACKUP_S3_URI" >/dev/null 2>&1; then
      log "Off-site OK (aws s3) → $BACKUP_S3_URI"
    else
      log "ATTENTION: copie off-site S3 échouée (backup local conservé)"
    fi
  else
    log "ATTENTION: BACKUP_S3_URI défini mais 'aws' introuvable"
  fi
else
  log "Off-site non configuré (BACKUP_RCLONE_REMOTE / BACKUP_S3_URI vides) — backup local uniquement."
fi

# --- Rétention : purge > RETENTION_DAYS, en gardant toujours les MIN_KEEP plus récents ---
n=0
( ls -1t "$BACKUP_DIR"/howners-*.dump 2>/dev/null || true ) | while IFS= read -r f; do
  n=$((n + 1))
  [ "$n" -le "$BACKUP_MIN_KEEP" ] && continue
  if [ -n "$(find "$f" -mtime +"$BACKUP_RETENTION_DAYS" 2>/dev/null || true)" ]; then
    log "Purge (>$BACKUP_RETENTION_DAYS j) : $(basename "$f")"
    rm -f "$f"
  fi
done

REMAIN="$(ls -1 "$BACKUP_DIR"/howners-*.dump 2>/dev/null | wc -l | tr -d ' ')"
log "Sauvegardes conservées dans $BACKUP_DIR : $REMAIN"

# Off-site : pousser $DEST vers S3/MinIO ou un stockage distant est l'étape suivante
# recommandée (un backup sur le même hôte ne protège pas d'une perte de l'hôte).

[ -n "$BACKUP_HEALTHCHECK_URL" ] && command -v curl >/dev/null 2>&1 \
  && curl -fsS -m 10 --retry 2 "$BACKUP_HEALTHCHECK_URL" >/dev/null 2>&1 || true
log "Terminé."
