# 🚀 Déployer Howners sur AWS (staging)

Runbook pour monter un environnement de test sur AWS avec la stack Docker du repo
(`docker-compose.prod.yml` : Caddy TLS + backend + frontend + Postgres + MinIO).
La même procédure vaut pour un VPS quelconque ; seule la création de la machine change.

> Prérequis côté code : rien — le build du frontend accepte `APP_DOMAIN` (build-arg),
> Caddy expose l'API sur `api.<APP_DOMAIN>` et MinIO sur `s3.<APP_DOMAIN>`.
> Pour la mise en production réelle, suivre `PROD-CHECKLIST.md`.

## 1. Créer la machine (Lightsail, ~10 min)

1. Console AWS → **Lightsail** → *Create instance* → Linux, **Ubuntu 24.04**.
2. Taille : **4 GB RAM / 2 vCPU** (~24 $/mois). 2 GB fonctionne mais le build
   Docker du frontend (prerender Angular) y est lent et serré.
3. *Networking* → attacher une **IP statique**.
4. Firewall Lightsail : n'ouvrir que **22 (SSH), 80 et 443** (TCP). Caddy est le
   seul service publié par le compose ; Postgres/MinIO restent internes.

EC2 marche aussi (t3.medium + security group équivalent) si tu préfères le VPC classique.

## 2. DNS (~5 min)

Chez ton registrar, créer **3 entrées A** vers l'IP statique :

| Entrée | Rôle |
|---|---|
| `staging.mondomaine.fr` | frontend |
| `api.staging.mondomaine.fr` | API backend |
| `s3.staging.mondomaine.fr` | MinIO (URLs présignées des documents/photos) |

## 3. Installer Docker sur l'instance

```bash
ssh ubuntu@<IP>
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker ubuntu && exit   # se reconnecter ensuite
```

## 4. Déployer

```bash
git clone git@github.com:m-annabi/howners.git && cd howners
cp .env.staging.example .env.prod
vim .env.prod        # remplir chaque valeur "CHANGER" (secrets via: openssl rand -base64 72)
chmod 600 .env.prod

docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --build
```

Premier lancement : ~5-10 min (builds + certificats Let's Encrypt automatiques).
Pendant les essais, décommenter `acme_ca …staging…` dans le `Caddyfile` pour ne pas
consommer le quota Let's Encrypt (certificat non reconnu par le navigateur, c'est normal).

Vérifier :

```bash
docker compose -f docker-compose.prod.yml ps          # tous "running (healthy)"
curl https://api.staging.mondomaine.fr/actuator/health # {"status":"UP"}
```

puis ouvrir `https://staging.mondomaine.fr`.

## 5. Mettre à jour le staging

Le déploiement est automatique à chaque push sur `main` : la CI construit les
images (dont la variante frontend `:staging`, avec le domaine du staging figé
au build), puis le serveur fait un simple `pull` + `up` (~1 min) — il ne
construit jamais rien lui-même. Manuellement si besoin :

```bash
sudo docker login ghcr.io   # jeton GitHub avec read:packages
sudo FRONTEND_TAG=staging docker compose --env-file .env.prod -f docker-compose.prod.yml pull backend frontend
sudo FRONTEND_TAG=staging docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --remove-orphans
```

(Liquibase applique les migrations tout seul au démarrage du backend.)

## 6. Mettre le staging en pause (arrêter les frais, garder la configuration)

Seul AWS génère des frais récurrents : l'instance Lightsail (~24 $/mois, facturée
**même arrêtée**), l'IP statique si elle n'est plus attachée à une instance
(~3,6 $/mois), la zone hébergée Route 53 (0,50 $/mois) et le domaine
`howners-app.com` (renouvellement annuel chez Amazon Registrar). Mailtrap (plan
gratuit), Stripe (mode test), DocuSign (démo) et Sentry ne facturent rien.

Le job `deploy-staging` de la CI ne tourne que si la variable de dépôt GitHub
`STAGING_DEPLOY_ENABLED` vaut `true` : sans elle, un push sur `main` ne tente
plus de joindre le serveur et la CI reste verte. Les secrets (`STAGING_SSH_KEY`)
et le workflow sont conservés.

1. **Sauvegarder la configuration du serveur** (le seul fichier qui n'est pas
   dans Git) :
   ```bash
   scp ubuntu@51.44.0.177:~/howners/.env.prod ./howners-staging.env.prod   # à garder hors Git
   ```
   Optionnel, si les données de test ont de la valeur : `scripts/db-backup.sh`.
2. **Snapshot de l'instance** : console Lightsail → instance → *Snapshots* →
   *Create snapshot*. Le snapshot conserve le disque complet (Docker, volumes
   Postgres/MinIO, `.env.prod`) pour ~0,05 $/Go/mois (≈ 1-2 $/mois).
3. **Supprimer l'instance** (pas seulement l'arrêter) : *Delete*.
4. **Libérer l'IP statique** : *Networking* → IP statique → *Delete* (sinon
   elle est facturée dès qu'elle n'est plus attachée). Retenir que l'IP figure
   dans `.github/workflows/ci.yml` et dans les 3 entrées A de Route 53 : elle
   changera à la reprise.
5. **Domaine `howners-app.com`** (décision : ne pas le conserver) : *Route 53 →
   Registered domains → howners-app.com → Auto-renew → Disable*. Le domaine
   reste actif jusqu'à son échéance (août 2027, déjà payé) puis expire sans
   nouveau prélèvement ; il ne peut pas être remboursé avant. Il redevient
   ensuite disponible pour n'importe qui.
6. **Zone hébergée Route 53** : *Hosted zones → howners-app.com* → supprimer
   d'abord les 3 entrées A (`staging`, `api.staging`, `s3.staging`), puis
   *Delete hosted zone* (les entrées NS/SOA partent avec). Fin des 0,50 $/mois.
7. Ne **pas** activer `STAGING_DEPLOY_ENABLED` : le déploiement reste en pause.

Vérifier le lendemain dans *Billing → Bills* que seule la ligne du snapshot
subsiste (l'alerte « My Monthly Cost Budget » à 10 $ ne doit plus partir).

> Le domaine `howners.com` (cible de `environment.prod.ts`, du `Caddyfile` et de
> la demande d'approbation Mailtrap) n'est pas enregistré sur ce compte AWS ; s'il
> est déposé chez un autre registrar, son renouvellement se gère là-bas.

### Reprendre

1. Lightsail → *Snapshots* → *Create new instance* depuis le snapshot (même taille
   ou plus grande), attacher une nouvelle IP statique, ouvrir 22/80/443.
2. DNS : sans domaine, choisir un nouveau nom (ou re-déposer `howners-app.com`
   s'il est encore libre), recréer une zone hébergée et les 3 entrées A vers la
   nouvelle IP ; remplacer l'ancienne IP dans `.github/workflows/ci.yml` (deux
   occurrences) et, si le nom change, `APP_DOMAIN` dans le workflow (build-arg
   de l'image `:staging` et URLs du contrôle de santé) et dans `.env.prod`.
3. Vérifier que `~/howners/.env.prod` est bien présent sur l'instance restaurée
   (sinon le recopier depuis la sauvegarde de l'étape 1).
4. Créer la variable de dépôt `STAGING_DEPLOY_ENABLED=true`, puis relancer le
   dernier workflow de `main` (ou pousser) : la CI redéploie les images.

## Variante : vrai S3 AWS au lieu de MinIO

Le backend parle au stockage via le SDK AWS — MinIO n'est qu'un S3 local. Pour
utiliser S3 :

1. Créer un **bucket privé** (ex. `howners-staging-documents`, région `eu-west-3`)
   et un **utilisateur IAM** dont la policy est limitée à ce bucket
   (`s3:GetObject`, `s3:PutObject`, `s3:DeleteObject`, `s3:ListBucket`).
2. Dans `.env.prod` : `MINIO_ENDPOINT=https://s3.eu-west-3.amazonaws.com`,
   `MINIO_ROOT_USER`/`MINIO_ROOT_PASSWORD` = access/secret key IAM,
   `MINIO_BUCKET` = nom du bucket.
3. Supprimer (ou ignorer) les services `minio`/`minio-setup` du compose et
   l'entrée DNS `s3.…` — l'endpoint S3 est déjà public.

Avantages : durabilité gérée, pas de volume à sauvegarder, et pas de dépendance
au NAT « hairpin » (conteneur → IP publique de sa propre machine) dont certains
hôtes s'accommodent mal avec l'option MinIO public.

## Points de vigilance

- **`APP_DOMAIN` change ⇒ rebuilder le frontend** (`up -d --build`) : l'URL de
  l'API est figée dans le bundle Angular au build.
- **Stripe** : clés `sk_test_`/`pk_test_` uniquement ; créer un webhook de test
  pointant sur `https://api.staging.mondomaine.fr/api/webhooks/stripe` et
  reporter son `whsec_…`.
- **Emails** : brancher Mailtrap (ou équivalent) pour capturer les emails —
  jamais de SMTP réel sur un staging rempli de données de test.
- **Données** : le volume `postgres_data` persiste tant qu'on ne fait pas
  `docker compose down -v`. Sauvegardes : `scripts/db-backup.sh`.
- **Coût total** : ~24 $/mois (Lightsail 4 GB) + quelques centimes de S3.
  Penser à supprimer l'instance quand le staging ne sert plus (procédure §6).
