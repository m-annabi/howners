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

```bash
git pull
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --build
```

(Liquibase applique les migrations tout seul au démarrage du backend.)

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
  Penser à supprimer l'instance quand le staging ne sert plus.
