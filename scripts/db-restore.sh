#!/usr/bin/env bash
#
# Restauration / vérification d'une sauvegarde PostgreSQL de Howners.
# Opère via `docker exec` sur le conteneur postgres (pas de client requis sur l'hôte).
#
# Modes :
#   scripts/db-restore.sh --verify-latest
#       NON destructif. Restaure le dernier dump dans une base jetable
#       (howners_restore_verify), vérifie qu'elle contient des données, puis la supprime.
#       À lancer périodiquement : un backup jamais restauré n'est pas un backup.
#
#   scripts/db-restore.sh <fichier.dump>          (ou --latest)
#       DESTRUCTIF. Restaure dans la base de production (--clean --if-exists).
#       Demande une confirmation explicite, sauf si --force est passé.
#
# Config (mêmes variables que db-backup.sh) :
#   POSTGRES_CONTAINER [howners-postgres]  BACKUP_DIR [/var/backups/howners]  ENV_FILE [<repo>/.env]

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

POSTGRES_CONTAINER="${POSTGRES_CONTAINER:-howners-postgres}"
BACKUP_DIR="${BACKUP_DIR:-/var/backups/howners}"
ENV_FILE="${ENV_FILE:-$REPO_ROOT/.env}"
SCRATCH_DB="howners_restore_verify"

log()  { printf '%s [db-restore] %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$*"; }
fail() { log "ERREUR: $*"; exit 1; }

if [ -f "$ENV_FILE" ]; then
  while IFS= read -r line; do
    case "$line" in
      POSTGRES_USER=*|POSTGRES_DB=*|POSTGRES_PASSWORD=*)
        key="${line%%=*}"; eval "current=\${$key:-}"
        [ -z "$current" ] && export "$line" ;;
    esac
  done < "$ENV_FILE"
fi
: "${POSTGRES_USER:?POSTGRES_USER manquant}"
: "${POSTGRES_DB:?POSTGRES_DB manquant}"
: "${POSTGRES_PASSWORD:?POSTGRES_PASSWORD manquant}"

command -v docker >/dev/null 2>&1 || fail "docker introuvable"
[ "$(docker inspect -f '{{.State.Running}}' "$POSTGRES_CONTAINER" 2>/dev/null || echo false)" = "true" ] \
  || fail "conteneur '$POSTGRES_CONTAINER' non démarré"

# psql/pg_restore exécutés dans le conteneur avec le mot de passe
dexec() { docker exec -i -e PGPASSWORD="$POSTGRES_PASSWORD" "$POSTGRES_CONTAINER" "$@"; }
psql_db() { dexec psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$1" -tAc "$2"; }

latest_dump() {
  ls -1t "$BACKUP_DIR"/howners-*.dump 2>/dev/null | head -n 1
}

# ----------------------------------------------------------------------------
mode="${1:-}"

if [ "$mode" = "--verify-latest" ]; then
  FILE="$(latest_dump || true)"
  [ -n "$FILE" ] || fail "aucun dump trouvé dans $BACKUP_DIR"
  log "Vérification de restauration : $(basename "$FILE")"

  log "Création de la base jetable '$SCRATCH_DB'…"
  psql_db postgres "DROP DATABASE IF EXISTS $SCRATCH_DB;" >/dev/null
  psql_db postgres "CREATE DATABASE $SCRATCH_DB OWNER $POSTGRES_USER;" >/dev/null

  cleanup() { psql_db postgres "DROP DATABASE IF EXISTS $SCRATCH_DB;" >/dev/null 2>&1 || true; }
  trap cleanup EXIT

  log "Restauration dans '$SCRATCH_DB' (strict, --exit-on-error)…"
  if ! dexec pg_restore -U "$POSTGRES_USER" -d "$SCRATCH_DB" \
        --no-owner --no-privileges --exit-on-error < "$FILE"; then
    fail "pg_restore a échoué — la sauvegarde n'est PAS restaurable !"
  fi

  TABLES="$(psql_db "$SCRATCH_DB" "SELECT count(*) FROM information_schema.tables WHERE table_schema='public';" | tr -d '[:space:]')"
  # Table métier connue comme sanity-check de contenu (4 plans seedés attendus)
  PLANS="$(psql_db "$SCRATCH_DB" "SELECT count(*) FROM subscription_plans;" 2>/dev/null | tr -d '[:space:]' || echo '0')"

  [ "${TABLES:-0}" -gt 0 ] || fail "la base restaurée ne contient aucune table"
  log "OK ✅ — $TABLES tables restaurées, subscription_plans=$PLANS"
  log "Base jetable supprimée. La sauvegarde est restaurable."
  exit 0
fi

# ---- Restauration destructive en production ----
FORCE=0
FILE=""
for arg in "$@"; do
  case "$arg" in
    --force) FORCE=1 ;;
    --latest) FILE="$(latest_dump || true)" ;;
    --verify-latest) ;; # déjà traité
    -*) fail "option inconnue: $arg" ;;
    *) FILE="$arg" ;;
  esac
done

[ -n "$FILE" ] || fail "usage: db-restore.sh <fichier.dump | --latest | --verify-latest> [--force]"
[ -f "$FILE" ] || fail "fichier introuvable: $FILE"

if [ "$FORCE" -ne 1 ]; then
  log "⚠️  RESTAURATION DESTRUCTIVE dans la base '$POSTGRES_DB' (conteneur '$POSTGRES_CONTAINER')."
  log "    Source : $FILE"
  printf "    Tapez 'restore' pour confirmer : "
  read -r confirm
  [ "$confirm" = "restore" ] || fail "annulé."
fi

log "Restauration de '$(basename "$FILE")' dans '$POSTGRES_DB'…"
# --clean --if-exists : remet la base à l'état du dump (drop des objets existants).
dexec pg_restore -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
  --clean --if-exists --no-owner --no-privileges < "$FILE" \
  || log "pg_restore a renvoyé des avertissements (voir ci-dessus)."

log "Terminé. Vérifiez l'application."
