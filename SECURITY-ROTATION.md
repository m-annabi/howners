# 🚨 Rotation de credentials requise

Le fichier `howners-back/restart-backend.sh` a été versionné dans Git avec des secrets en clair entre `74017f3` (initial commit) et `f5xxxxx` (commit de rotation). **Ils sont définitivement exposés dans l'historique** — `git filter-repo` ne suffit pas si le repo a été poussé publiquement, il faut les considérer compromis.

## À révoquer dès que possible

### 1. Gmail (compte annabimountassar@gmail.com)

- **Mot de passe d'application Gmail** `***REVOKED***` — visible en clair dans l'historique.
- Aller sur [https://myaccount.google.com/apppasswords](https://myaccount.google.com/apppasswords)
- **Supprimer** ce mot de passe d'application
- Régénérer un nouveau si SMTP Gmail est encore le canal de prod
- Bonne pratique : passer à un fournisseur transactionnel (SendGrid, Postmark, Brevo) avec une vraie clé API séparée

### 2. DocuSign Sandbox

Tous ces identifiants sont exposés dans l'historique :

| Variable | Valeur exposée |
|---|---|
| `DOCUSIGN_INTEGRATION_KEY` | `***REVOKED***` |
| `DOCUSIGN_USER_ID` | `***REVOKED***` |
| `DOCUSIGN_ACCOUNT_ID` | `***REVOKED***` |
| `DOCUSIGN_PRIVATE_KEY` | **Clé RSA privée complète** (PEM 4096 bits) |

**Le plus grave**, c'est la clé RSA. Elle permet à n'importe qui de signer des JWT d'impersonation DocuSign pour ce compte.

Procédure :
1. Connexion à [https://account-d.docusign.com](https://account-d.docusign.com)
2. *Settings* → *Apps and Keys* → trouver l'integration key compromise
3. Soit **delete** la clé d'intégration entière, soit **revoke** la RSA keypair et en générer une nouvelle
4. Mettre la nouvelle valeur uniquement dans `howners-back/.env` (déjà `.gitignore`-ignored)

## Ce qui a été fait dans le repo

- `howners-back/restart-backend.sh` réécrit pour charger `.env` au lieu d'inclure les secrets
- `howners-back/.env.example` créé avec la liste complète des variables et des valeurs neutres
- `.gitignore` durci : `**/.env` est ignoré récursivement avec exception explicite pour `.env.example`
- Aucun `.env` n'apparaît dans `git ls-files`

## Vérification post-rotation

```bash
# 1. Confirmer que les anciennes clés ne fonctionnent plus
curl -sS -X POST https://account-d.docusign.com/oauth/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer&assertion=$JWT_AVEC_ANCIENNE_CLE"
# Doit renvoyer "invalid_grant" ou "consent_required"

# 2. Confirmer que les nouveaux secrets marchent en local
cd howners-back && ./restart-backend.sh
# Vérifier dans les logs : "Started HownersApplication" + pas de "Failed to send welcome owner email: Authentication failed"
```

## Historique git

Si tu veux retirer les secrets de l'historique git lui-même (pas que du HEAD), utilise `git filter-repo` :

```bash
brew install git-filter-repo
git filter-repo --invert-paths --path howners-back/restart-backend.sh
git push --force origin main  # ⚠️ destructif pour les collaborateurs
```

Mais **considère malgré tout les secrets compromis** : ils ont pu être lus par quiconque a eu accès au repo pendant la fenêtre d'exposition.
