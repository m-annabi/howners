#!/usr/bin/env bash
#
# Crée (idempotemment) les Produits + Prix Stripe pour les plans payants de Howners,
# puis écrit les `stripe_price_id_monthly` / `stripe_price_id_annual` dans la base.
#
# Rend "turnkey" le bloquant prod : « Stripe live — créer les Prices (dont AGENCE)
# → stripe_price_id_* en base ». Une fois lancé avec la clé secrète Stripe, les plans
# sont vendables immédiatement (le front lit ces price IDs pour ouvrir le Checkout).
#
# Usage :
#   STRIPE_SECRET_KEY=sk_live_xxx ./scripts/setup-stripe-prices.sh            # crée + écrit en base
#   STRIPE_SECRET_KEY=sk_test_xxx ./scripts/setup-stripe-prices.sh --dry-run  # affiche, ne touche à rien
#                                  ./scripts/setup-stripe-prices.sh --force   # recrée même si déjà configuré
#
# Idempotent : un plan dont les deux price IDs sont déjà en base est ignoré (sauf --force).
# Côté Stripe, Produit réutilisé par id déterministe (howners_<plan>) et Prix réutilisé
# s'il existe déjà un prix actif au bon montant/intervalle → relancer ne duplique rien.
#
# Prérequis : curl, jq, docker (conteneur postgres up). La clé Stripe et les creds DB
# peuvent venir de l'environnement ou d'ENV_FILE (.env). RIEN n'est écrit dans un fichier suivi.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

STRIPE_API="https://api.stripe.com/v1"
CURRENCY="${STRIPE_CURRENCY:-eur}"
POSTGRES_CONTAINER="${POSTGRES_CONTAINER:-howners-postgres}"
ENV_FILE="${ENV_FILE:-$REPO_ROOT/.env}"

log()  { printf '%s [stripe-prices] %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$*"; }
fail() { log "ERREUR: $*"; exit 1; }

DRY_RUN=0; FORCE=0
for arg in "$@"; do
  case "$arg" in
    --dry-run) DRY_RUN=1 ;;
    --force)   FORCE=1 ;;
    -h|--help) sed -n '2,30p' "$0"; exit 0 ;;
    *) fail "option inconnue: $arg (voir --help)" ;;
  esac
done

# --- Secrets : env d'abord, sinon ENV_FILE (sans écraser une valeur déjà exportée) ---
if [ -f "$ENV_FILE" ]; then
  while IFS= read -r line; do
    case "$line" in
      STRIPE_SECRET_KEY=*|POSTGRES_USER=*|POSTGRES_DB=*|POSTGRES_PASSWORD=*)
        key="${line%%=*}"; eval "current=\${$key:-}"
        [ -z "$current" ] && export "$line" ;;
    esac
  done < "$ENV_FILE"
fi

: "${STRIPE_SECRET_KEY:?STRIPE_SECRET_KEY manquant (sk_live_... ou sk_test_..., via env ou .env)}"
: "${POSTGRES_USER:?POSTGRES_USER manquant}"
: "${POSTGRES_DB:?POSTGRES_DB manquant}"
: "${POSTGRES_PASSWORD:?POSTGRES_PASSWORD manquant}"

command -v curl   >/dev/null 2>&1 || fail "curl introuvable"
command -v jq     >/dev/null 2>&1 || fail "jq introuvable"
command -v docker >/dev/null 2>&1 || fail "docker introuvable"
[ "$(docker inspect -f '{{.State.Running}}' "$POSTGRES_CONTAINER" 2>/dev/null || echo false)" = "true" ] \
  || fail "conteneur '$POSTGRES_CONTAINER' non démarré"

case "$STRIPE_SECRET_KEY" in
  sk_live_*) log "Clé Stripe : LIVE (production) — les Prix créés seront réels." ;;
  sk_test_*) log "Clé Stripe : TEST (bac à sable)." ;;
  *) log "ATTENTION: la clé ne ressemble ni à sk_live_ ni à sk_test_." ;;
esac
[ "$DRY_RUN" -eq 1 ] && log "Mode --dry-run : aucun appel Stripe, aucune écriture en base."

# --- Helpers Stripe (curl + jq, contrôle d'erreur) ---
stripe_call() { # $1=METHOD $2=path ; reste = -d ...
  local method="$1" path="$2"; shift 2
  local resp
  resp="$(curl -s -X "$method" -u "$STRIPE_SECRET_KEY:" "$STRIPE_API/$path" "$@")" || fail "appel Stripe échoué ($path)"
  if printf '%s' "$resp" | jq -e 'has("error")' >/dev/null 2>&1; then
    fail "Stripe ($path) : $(printf '%s' "$resp" | jq -r '.error.message')"
  fi
  printf '%s' "$resp"
}

get_or_create_product() { # $1=product_id $2=name -> echoes product_id
  local pid="$1" name="$2" resp
  resp="$(curl -s -u "$STRIPE_SECRET_KEY:" "$STRIPE_API/products/$pid")" || fail "appel Stripe échoué (products/$pid)"
  if printf '%s' "$resp" | jq -e '.id' >/dev/null 2>&1; then
    printf '%s' "$pid"; return
  fi
  stripe_call POST products -d "id=$pid" -d "name=$name" -d "metadata[howners_plan]=$pid" >/dev/null
  printf '%s' "$pid"
}

find_or_create_price() { # $1=product_id $2=interval(month|year) $3=amount_cents $4=lookup_key -> echoes price_id
  local pid="$1" interval="$2" amount="$3" lookup="$4" existing
  existing="$(stripe_call GET "prices?product=$pid&active=true&limit=100" \
    | jq -r --arg i "$interval" --argjson a "$amount" \
        '.data[] | select(.recurring.interval==$i and .unit_amount==$a) | .id' | head -n1)"
  if [ -n "$existing" ]; then
    printf '%s' "$existing"; return
  fi
  stripe_call POST prices \
    -d "product=$pid" -d "unit_amount=$amount" -d "currency=$CURRENCY" \
    -d "recurring[interval]=$interval" -d "lookup_key=$lookup" -d "transfer_lookup_key=true" \
    | jq -r '.id'
}

cents() { awk -v p="$1" 'BEGIN { printf "%.0f", p * 100 }'; }

psql_read() { docker exec -e PGPASSWORD="$POSTGRES_PASSWORD" "$POSTGRES_CONTAINER" \
  psql -tAF'|' -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "$1"; }
psql_exec() { docker exec -e PGPASSWORD="$POSTGRES_PASSWORD" "$POSTGRES_CONTAINER" \
  psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "$1" >/dev/null; }

# ----------------------------------------------------------------------------
log "Plans payants (monthly_price > 0) :"
created=0; skipped=0

while IFS='|' read -r name monthly annual cur_m cur_a; do
  [ -n "$name" ] || continue
  pid="howners_$(printf '%s' "$name" | tr '[:upper:]' '[:lower:]')"
  m_cents="$(cents "$monthly")"; a_cents="$(cents "$annual")"

  if [ -n "$cur_m" ] && [ -n "$cur_a" ] && [ "$FORCE" -ne 1 ]; then
    log "  • $name : déjà configuré (mensuel=$cur_m, annuel=$cur_a) — ignoré (--force pour recréer)"
    skipped=$((skipped + 1)); continue
  fi

  if [ "$DRY_RUN" -eq 1 ]; then
    log "  • $name : créerait produit '$pid' + prix mensuel ${m_cents}c + annuel ${a_cents}c ($CURRENCY), puis MAJ base"
    continue
  fi

  log "  • $name : produit '$pid'…"
  get_or_create_product "$pid" "Howners $name" >/dev/null
  mid="$(find_or_create_price "$pid" month "$m_cents" "${pid}_monthly")"
  aid="$(find_or_create_price "$pid" year  "$a_cents" "${pid}_annual")"
  psql_exec "UPDATE subscription_plans SET stripe_price_id_monthly='$mid', stripe_price_id_annual='$aid' WHERE name='$name';"
  log "    → mensuel=$mid  annuel=$aid  (écrits en base)"
  created=$((created + 1))
done < <(psql_read "SELECT name, monthly_price, annual_price, COALESCE(stripe_price_id_monthly,''), COALESCE(stripe_price_id_annual,'') FROM subscription_plans WHERE monthly_price > 0 ORDER BY monthly_price;")

if [ "$DRY_RUN" -eq 1 ]; then
  log "Terminé (dry-run) — rien créé."
else
  log "Terminé — $created plan(s) configuré(s), $skipped ignoré(s)."
  [ "$created" -gt 0 ] && log "Rappel : activez aussi le webhook prod (events checkout/subscription) dans le Dashboard Stripe."
fi
