#!/usr/bin/env bash
#
# Gestion des secrets de Howners — générer, roter, valider.
#
# Principes de sûreté :
#   - n'AFFICHE JAMAIS la valeur d'un secret existant (doctor ne montre que longueur + verdict) ;
#   - n'opère que sur des fichiers .env, et REFUSE d'écrire dans un fichier suivi par git ;
#   - `generate` imprime des valeurs NEUVES (aléatoires) destinées à être collées — pas des secrets existants.
#
# Usage :
#   scripts/manage-secrets.sh generate [jwt|password|all]   # imprime des secrets forts à coller
#   scripts/manage-secrets.sh rotate <VAR> [envfile]        # remplace VAR par un secret neuf (def .env.prod) + backup
#   scripts/manage-secrets.sh doctor [envfile]              # contrôle l'env (manquant/faible/exemple/court) sans rien afficher
#
# Complète SECURITY-ROTATION.md (playbook) et StartupConfigValidator (refus de démarrage en prod).

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

log()  { printf '%s\n' "$*"; }
fail() { printf 'ERREUR: %s\n' "$*" >&2; exit 1; }

command -v openssl >/dev/null 2>&1 || fail "openssl introuvable (requis pour générer des secrets)"

# Valeurs par défaut/exemple interdites (miroir de StartupConfigValidator).
WEAK_VALUES="howners_pass postgres password admin root minioadmin minioadmin123"
PLACEHOLDER_MARKERS="changeme change-me change-this placeholder example your- xxxxx todo a-changer secret-in-production sk_test_ pk_test_ whsec_... ..."

gen_hex()    { openssl rand -hex "${1:-48}"; }                                   # 2*n caractères hex
gen_b64url() { openssl rand -base64 "${1:-24}" | tr '+/' '-_' | tr -d '=\n'; }   # ~4/3*n, URL-safe

is_weak() { # $1=value -> 0 si faible/exemple
  local v; v="$(printf '%s' "$1" | tr '[:upper:]' '[:lower:]')"
  local w
  for w in $WEAK_VALUES;        do [ "$v" = "$w" ] && return 0; done
  for w in $PLACEHOLDER_MARKERS; do case "$v" in *"$w"*) return 0;; esac; done
  return 1
}

# Lit la valeur d'une clé dans un fichier .env, sans l'imprimer (retournée à l'appelant).
read_env_value() { # $1=key $2=file
  local line; line="$(grep -E "^$1=" "$2" 2>/dev/null | head -1 || true)"
  [ -z "$line" ] && return 1
  local val="${line#*=}"
  val="${val%\"}"; val="${val#\"}"      # retire d'éventuels guillemets
  printf '%s' "$val"
}

refuse_if_tracked() { # $1=file — sécurité : ne jamais écrire de secret dans un fichier versionné
  if git -C "$REPO_ROOT" ls-files --error-unmatch "$1" >/dev/null 2>&1; then
    fail "$1 est suivi par git — refus d'y écrire un secret. Utilise un fichier .env gitignoré."
  fi
}

# ---------------------------------------------------------------------------
cmd_generate() {
  case "${1:-all}" in
    jwt)      log "JWT_SECRET=$(gen_hex 48)" ;;
    password) log "$(gen_b64url 24)" ;;
    all)
      log "# Secrets générés — à coller dans .env.prod (puis: chmod 600 .env.prod)."
      log "# Stripe/SMTP/webhook ne se génèrent pas ici : ils proviennent des fournisseurs."
      log "JWT_SECRET=$(gen_hex 48)"
      log "POSTGRES_PASSWORD=$(gen_b64url 24)"
      log "MINIO_ROOT_PASSWORD=$(gen_b64url 24)"
      ;;
    *) fail "type inconnu: ${1}. Attendu: jwt | password | all" ;;
  esac
}

cmd_rotate() {
  local var="${1:-}"; local file="${2:-$REPO_ROOT/.env.prod}"
  [ -n "$var" ] || fail "usage: rotate <VAR> [envfile]"
  [ -f "$file" ] || fail "fichier introuvable: $file"
  refuse_if_tracked "$file"

  local newval
  case "$var" in
    JWT_SECRET) newval="$(gen_hex 48)" ;;
    *PASSWORD|*SECRET|*KEY) newval="$(gen_b64url 24)" ;;
    *) newval="$(gen_b64url 24)" ;;
  esac

  local backup="$file.bak"
  cp "$file" "$backup"; chmod 600 "$backup"

  local tmp; tmp="$(mktemp)"
  if grep -qE "^$var=" "$file"; then
    # remplace la ligne existante (sans interpréter la valeur via sed)
    while IFS= read -r line || [ -n "$line" ]; do
      case "$line" in
        "$var="*) printf '%s=%s\n' "$var" "$newval" ;;
        *)        printf '%s\n' "$line" ;;
      esac
    done < "$file" > "$tmp"
  else
    cat "$file" > "$tmp"; printf '%s=%s\n' "$var" "$newval" >> "$tmp"
  fi
  mv "$tmp" "$file"; chmod 600 "$file"
  log "✅ $var roté dans $file (sauvegarde: $backup). Redémarre le backend pour appliquer."
  log "   Pense à révoquer l'ancienne valeur côté service si c'était une clé externe."
}

cmd_doctor() {
  local file="${1:-$REPO_ROOT/.env.prod}"
  [ -f "$file" ] || fail "fichier introuvable: $file (précise: doctor <envfile>)"
  log "Contrôle des secrets dans $(basename "$file") — les valeurs ne sont jamais affichées."

  local fails=0
  # clé:longueur_min:obligatoire(1/0)
  for spec in "JWT_SECRET:64:1" "POSTGRES_PASSWORD:12:1" "MINIO_ROOT_PASSWORD:12:1" \
              "STRIPE_SECRET_KEY:0:0" "STRIPE_WEBHOOK_SECRET:0:0" "SMTP_PASSWORD:0:0"; do
    local key="${spec%%:*}"; local rest="${spec#*:}"; local minlen="${rest%%:*}"; local required="${rest##*:}"
    local val; val="$(read_env_value "$key" "$file" || true)"

    if [ -z "$val" ]; then
      if [ "$required" = "1" ]; then log "  ✗ $key : manquant"; fails=$((fails+1)); else log "  • $key : absent (optionnel)"; fi
      continue
    fi
    if is_weak "$val"; then
      log "  ✗ $key : valeur par défaut / d'exemple (à remplacer)"; fails=$((fails+1)); continue
    fi
    if [ "$minlen" -gt 0 ] && [ "${#val}" -lt "$minlen" ]; then
      log "  ✗ $key : trop court (${#val} < $minlen)"; fails=$((fails+1)); continue
    fi
    if [ "$key" = "STRIPE_SECRET_KEY" ]; then
      case "$val" in sk_test_*) log "  ⚠ $key : clé de TEST (sk_test_) — OK en staging, pas en prod"; continue;; esac
    fi
    log "  ✓ $key : OK (longueur ${#val})"
  done

  log ""
  if [ "$fails" -eq 0 ]; then log "Résultat : OK ✅"; else log "Résultat : $fails problème(s) ❌"; return 1; fi
}

# ---------------------------------------------------------------------------
case "${1:-}" in
  generate) shift; cmd_generate "$@" ;;
  rotate)   shift; cmd_rotate   "$@" ;;
  doctor)   shift; cmd_doctor   "$@" ;;
  -h|--help|"") sed -n '2,20p' "$0" ;;
  *) fail "commande inconnue: $1 (generate | rotate | doctor)" ;;
esac
