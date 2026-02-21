# Howners - Plateforme de Gestion Locative

## 📋 Description

Howners est une application web moderne de gestion locative complète, permettant aux propriétaires de gérer leurs biens immobiliers, locations, contrats et documents en toute simplicité.

## ✨ Fonctionnalités

### 🏠 Gestion de Propriétés
- CRUD complet des propriétés
- Support multi-types (appartement, maison, studio, etc.)
- Caractéristiques détaillées (surface, chambres, salles de bain)
- Gestion de documents (photos, factures, diagnostics)

### 📝 Gestion de Locations
- CRUD complet des locations
- Support location courte et longue durée
- Création automatique de comptes locataires
- Gestion des loyers, charges et dépôts de garantie
- Suivi des statuts (PENDING, ACTIVE, TERMINATED, etc.)

### 📄 Système de Contrats
- Génération automatique de contrats depuis templates
- Templates dynamiques avec 30+ variables
- Génération PDF instantanée avec iText
- Versioning complet des contrats
- Hash SHA-256 pour intégrité
- Workflow: DRAFT → SENT → SIGNED → ACTIVE

### ✍️ Signature Électronique
- Signature Canvas HTML5 (souris + tactile)
- Traçabilité complète (IP, User-Agent, timestamp)
- Stockage sécurisé sur MinIO
- Validation stricte des droits de signature

### 📎 Gestion de Documents
- Upload de fichiers (max 10MB)
- 13 types de documents supportés
- Stockage cloud MinIO (S3-compatible)
- Téléchargement sécurisé
- Hash SHA-256 pour vérification d'intégrité

### 📊 Dashboard
- Statistiques en temps réel
- Nombre de propriétés
- Locations actives
- Revenus mensuels
- Activité récente

### 🔐 Authentification & Sécurité
- Authentification JWT (HS512)
- RBAC avec 3 rôles (OWNER, TENANT, ADMIN)
- Permissions granulaires
- Hashage bcrypt des mots de passe
- Guards et interceptors Angular

## 🛠️ Stack Technique

### Backend
- **Framework**: Spring Boot 4.0.2
- **Langage**: Java 21
- **Base de données**: PostgreSQL 16
- **Migrations**: Liquibase
- **Sécurité**: Spring Security + JWT (JJWT 0.12.5)
- **Génération PDF**: iText 8.0.3
- **Stockage**: MinIO (S3-compatible)
- **Build**: Maven

### Frontend
- **Framework**: Angular 15
- **Langage**: TypeScript
- **Formulaires**: Reactive Forms
- **Routage**: Angular Router avec Lazy Loading
- **HTTP**: Interceptors personnalisés
- **Build**: Angular CLI

### Infrastructure
- **Conteneurs**: Docker Compose
- **Base de données**: PostgreSQL 16 (port 5432)
- **Stockage**: MinIO (port 9000/9001)

## 📁 Structure du Projet

```
howners/
├── howners-back/          # Backend Spring Boot
│   ├── src/main/java/
│   │   └── com/howners/gestion/
│   │       ├── config/           # Configuration (Security, S3, Storage)
│   │       ├── controller/       # REST Controllers
│   │       ├── domain/           # Entités JPA
│   │       ├── dto/              # Data Transfer Objects
│   │       ├── repository/       # Repositories JPA
│   │       └── service/          # Business Logic
│   └── src/main/resources/
│       ├── application.yml       # Configuration Spring
│       └── db/changelog/         # Migrations Liquibase
│
├── howners-api/           # Frontend Angular
│   └── src/app/
│       ├── core/                 # Services, Guards, Interceptors
│       │   ├── auth/
│       │   ├── guards/
│       │   ├── interceptors/
│       │   ├── models/
│       │   └── services/
│       ├── features/             # Modules fonctionnels
│       │   ├── auth/
│       │   ├── dashboard/
│       │   ├── properties/
│       │   ├── rentals/
│       │   └── contracts/
│       └── shared/               # Composants partagés
│           └── components/
│               ├── signature-pad/
│               ├── document-upload/
│               └── document-list/
│
└── docker-compose.yml     # Services Docker
```

## 🚀 Installation et Démarrage

### Prérequis

- **Java**: JDK 21
- **Node.js**: v18+ et npm
- **Maven**: 3.8+
- **Docker**: Desktop (pour PostgreSQL et MinIO)
- **Git**: Pour cloner le projet

### 1. Cloner le projet

```bash
git clone <repository-url>
cd howners
```

### 2. Démarrer l'infrastructure Docker

```bash
docker-compose up -d
```

Cela démarre:
- PostgreSQL (port 5432)
- MinIO (port 9000 - API, port 9001 - Console)

Accès MinIO Console: http://localhost:9001
- User: `minioadmin`
- Password: `minioadmin123`

### 3. Configurer et démarrer le Backend

```bash
cd howners-back

# Builder et démarrer le serveur
mvn clean install
mvn spring-boot:run
```

Le backend démarre sur: http://localhost:8080

### 4. Configurer et démarrer le Frontend

```bash
cd ../howners-api

# Installer les dépendances
npm install

# Démarrer le serveur de développement
npm start
```

Le frontend démarre sur: http://localhost:4200

### 5. Accéder à l'application

Ouvrez votre navigateur: http://localhost:4200

#### Créer un compte

1. Cliquez sur "Créer un compte"
2. Remplissez le formulaire
3. Sélectionnez le rôle "Propriétaire" (OWNER)
4. Connectez-vous avec vos identifiants

## 📚 API Documentation

### Endpoints principaux

#### Authentification
- `POST /api/auth/register` - Inscription
- `POST /api/auth/login` - Connexion
- `GET /api/auth/me` - Profil utilisateur

#### Propriétés
- `GET /api/properties` - Liste des propriétés
- `POST /api/properties` - Créer une propriété
- `GET /api/properties/{id}` - Détails d'une propriété
- `PUT /api/properties/{id}` - Modifier une propriété
- `DELETE /api/properties/{id}` - Supprimer une propriété

#### Locations
- `GET /api/rentals` - Liste des locations
- `POST /api/rentals` - Créer une location
- `GET /api/rentals/{id}` - Détails d'une location
- `PUT /api/rentals/{id}` - Modifier une location
- `DELETE /api/rentals/{id}` - Supprimer une location

#### Contrats
- `GET /api/contracts` - Liste des contrats
- `POST /api/contracts` - Créer un contrat
- `GET /api/contracts/{id}` - Détails d'un contrat
- `PUT /api/contracts/{id}` - Modifier un contrat
- `DELETE /api/contracts/{id}` - Supprimer un contrat
- `GET /api/contracts/{id}/versions` - Versions du contrat
- `GET /api/contracts/rental/{rentalId}` - Contrats d'une location

#### Signatures
- `POST /api/signatures` - Signer un contrat
- `GET /api/signatures` - Mes signatures
- `GET /api/signatures/{id}` - Détails d'une signature
- `GET /api/signatures/contract/{contractId}` - Signatures d'un contrat

#### Documents
- `POST /api/documents/upload` - Upload un document
- `GET /api/documents` - Mes documents
- `GET /api/documents/{id}` - Détails d'un document
- `GET /api/documents/property/{propertyId}` - Documents d'une propriété
- `GET /api/documents/rental/{rentalId}` - Documents d'une location
- `GET /api/documents/{id}/download` - Télécharger un document
- `DELETE /api/documents/{id}` - Supprimer un document

#### Dashboard
- `GET /api/dashboard/stats` - Statistiques

## 🗄️ Base de Données

### Tables principales

- **users** - Utilisateurs (propriétaires, locataires, admins)
- **properties** - Biens immobiliers
- **rentals** - Locations
- **contracts** - Contrats de location
- **contract_versions** - Versions des contrats
- **contract_templates** - Templates de contrats
- **signatures** - Signatures électroniques
- **documents** - Documents (photos, PDF, etc.)
- **payments** - Paiements (futur)
- **invoices** - Factures (futur)
- **receipts** - Reçus (futur)
- **expenses** - Dépenses (futur)
- **listings** - Annonces (futur)
- **audit_logs** - Logs d'audit

### Vérifier la base de données

```bash
# Se connecter à PostgreSQL
docker exec -it howners-postgres psql -U howners_user -d howners_db

# Lister les tables
\dt

# Voir la structure d'une table
\d users

# Voir les templates de contrats
SELECT * FROM contract_templates;
```

## 🔧 Configuration

### Variables d'environnement Backend

Fichier: `howners-back/src/main/resources/application.yml`

```yaml
spring:
  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/howners_db}
    username: ${POSTGRES_USER:howners_user}
    password: ${POSTGRES_PASSWORD:howners_pass}

jwt:
  secret: ${JWT_SECRET:change-this-secret-in-production-it-must-be-at-least-256-bits-long-for-hs512-algorithm}
  expiration: ${JWT_EXPIRATION:86400000}

storage:
  s3:
    endpoint: ${MINIO_ENDPOINT:http://localhost:9000}
    access-key: ${MINIO_ROOT_USER:minioadmin}
    secret-key: ${MINIO_ROOT_PASSWORD:minioadmin123}
    bucket: ${MINIO_BUCKET:howners-documents}

app:
  cors:
    allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:4200}
```

### Variables d'environnement Frontend

Fichier: `howners-api/src/environments/environment.ts`

```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api'
};
```

## 🧪 Tests

### Backend

```bash
cd howners-back
mvn test
```

### Frontend

```bash
cd howners-api
npm test
```

## 📦 Build pour Production

### Backend

```bash
cd howners-back
mvn clean package -DskipTests
```

Le JAR est généré dans: `target/demo-0.0.1-SNAPSHOT.jar`

Démarrer:
```bash
java -jar target/demo-0.0.1-SNAPSHOT.jar
```

### Frontend

```bash
cd howners-api
npm run build
```

Les fichiers sont générés dans: `dist/`

## 🐛 Dépannage

### Backend ne démarre pas

```bash
# Vérifier que Docker tourne
docker ps

# Vérifier les logs PostgreSQL
docker logs howners-postgres

# Vérifier les logs MinIO
docker logs howners-minio

# Redémarrer les services
docker-compose restart
```

### Erreurs de migration Liquibase

```bash
# Nettoyer complètement la base
docker-compose down -v
docker-compose up -d

# Attendre que PostgreSQL soit prêt
docker logs -f howners-postgres

# Relancer le backend
cd howners-back
mvn spring-boot:run
```

### Erreurs CORS Frontend

Vérifier que:
1. Le backend tourne sur port 8080
2. `CORS_ALLOWED_ORIGINS` inclut `http://localhost:4200`
3. Le frontend fait des requêtes vers `http://localhost:8080/api`

### MinIO non accessible

```bash
# Vérifier que MinIO tourne
docker ps | grep minio

# Accéder à la console
open http://localhost:9001

# Vérifier que le bucket existe
# Login: minioadmin / minioadmin123
```

## 📈 Utilisation

### Workflow complet

1. **Créer un compte propriétaire**
   - S'inscrire avec rôle OWNER
   - Se connecter

2. **Ajouter une propriété**
   - Aller dans "Propriétés"
   - Cliquer sur "+ Nouvelle Propriété"
   - Remplir le formulaire
   - Uploader des photos/documents

3. **Créer une location**
   - Aller dans "Locations"
   - Cliquer sur "+ Nouvelle Location"
   - Sélectionner une propriété
   - Saisir email du locataire (compte créé automatiquement)
   - Définir loyer et dates
   - Uploader documents (justificatifs)

4. **Générer un contrat**
   - Aller dans "Contrats"
   - Cliquer sur "+ Nouveau Contrat"
   - Sélectionner une location
   - Le PDF est généré automatiquement depuis le template
   - Envoyer au locataire

5. **Signer le contrat (locataire)**
   - Le locataire se connecte
   - Va dans "Contrats"
   - Clique sur le contrat
   - Dessine sa signature sur le Canvas
   - Valide

6. **Gérer les documents**
   - Uploader factures, photos, diagnostics
   - Télécharger les documents
   - Associer aux propriétés ou locations

## 🎯 Statuts de Phase

### ✅ Phase 1 - Infrastructure (100%)
- Docker Compose
- Base de données PostgreSQL
- Migrations Liquibase
- Authentification JWT

### ✅ Phase 2 - MVP Core (100%)
- CRUD Propriétés
- CRUD Locations
- Dashboard avec statistiques
- Création automatique de comptes

### ✅ Phase 3 - Contrats & Signature (100%)
- Templates de contrats
- Génération PDF
- Versioning
- Signature électronique Canvas
- Upload de documents

### 🔜 Phase 4 - Paiements (À venir)
- Intégration Stripe
- Paiements récurrents
- Historique des transactions

### 🔜 Phase 5 - Facturation (À venir)
- Génération de factures
- Gestion des reçus
- Suivi des dépenses

## 🤝 Contribution

Les contributions sont les bienvenues! Veuillez suivre ces étapes:

1. Fork le projet
2. Créez votre branche (`git checkout -b feature/AmazingFeature`)
3. Commit vos changements (`git commit -m 'Add some AmazingFeature'`)
4. Push vers la branche (`git push origin feature/AmazingFeature`)
5. Ouvrez une Pull Request

## 📝 Licence

Ce projet est sous licence MIT.

## 👥 Auteurs

- Développé avec Claude Code

## 🆘 Support

Pour toute question ou problème:
- Ouvrez une issue sur GitHub
- Consultez la documentation

## 🎯 Version Actuelle: 1.0.0

**Date de release**: Février 2026

**Statut**: ✅ Production Ready

---

Fait avec ❤️ par l'équipe Howners
