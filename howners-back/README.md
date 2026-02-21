# Howners Backend - API REST Spring Boot

![Version](https://img.shields.io/badge/version-1.0.0-blue)
![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.2-green)
![License](https://img.shields.io/badge/license-MIT-brightgreen)

**Plateforme de gestion locative avec signature électronique**

API REST complète pour la gestion de propriétés, locations, contrats et signatures électroniques via DocuSign.

---

## 📋 Table des Matières

- [Vue d'Ensemble](#vue-densemble)
- [Fonctionnalités](#fonctionnalités)
- [Architecture](#architecture)
- [Technologies](#technologies)
- [Prérequis](#prérequis)
- [Installation](#installation)
- [Configuration](#configuration)
- [Lancement](#lancement)
- [API Documentation](#api-documentation)
- [Base de Données](#base-de-données)
- [Sécurité](#sécurité)
- [Tests](#tests)
- [Déploiement](#déploiement)
- [Troubleshooting](#troubleshooting)

---

## 🎯 Vue d'Ensemble

Howners Backend est une API REST Spring Boot permettant aux propriétaires de gérer l'intégralité de leurs locations:

- **Gestion des propriétés** avec photos et documents
- **Gestion des locations** avec suivi des locataires
- **Génération automatique de contrats** à partir de templates personnalisables
- **Signature électronique** via DocuSign avec accès public sécurisé
- **Stockage cloud** des documents (MinIO/S3)
- **Notifications email** avec templates HTML professionnels
- **Audit trail complet** pour conformité légale

**Points forts:**
- 🔐 Sécurité JWT + BCrypt
- 📄 Génération PDF avec iText
- ✍️ Double signature: Canvas HTML5 + DocuSign
- 📧 Emails HTML avec Thymeleaf
- ☁️ Stockage S3-compatible
- 🔄 Webhooks temps réel
- 📊 Dashboard analytics

---

## ✨ Fonctionnalités

### Gestion des Propriétés
- CRUD complet des propriétés
- Types: Appartement, Maison, Studio, etc.
- Upload de photos avec ordre personnalisable
- Géolocalisation et détails techniques
- Documents associés (diagnostics, etc.)

### Gestion des Locations
- Création de locations longue/courte durée
- Statuts: PENDING → ACTIVE → TERMINATED
- Création automatique du compte locataire
- Suivi loyers, charges, caution
- Dates de début/fin et reconduction

### Gestion des Contrats
- **Templates personnalisables** avec 30+ variables
- **Génération PDF automatique** à partir des données
- **Versioning SHA-256** pour intégrité
- **Cycle de vie**: DRAFT → SENT → SIGNED → ACTIVE → TERMINATED
- Prévisualisation avant envoi

### Signature Électronique
- **DocuSign intégré** (SDK 6.5.0)
- **Signature Canvas** HTML5 (fallback)
- **Accès public sans compte** via token sécurisé (BCrypt)
- **Embedded signing** workflow
- **Webhooks temps réel** pour notifications
- **Emails HTML professionnels**
- **Audit trail complet** (IP, User-Agent, timestamps)

### Gestion des Documents
- Upload jusqu'à 10MB
- 13+ types supportés (contrat, diagnostic, état des lieux, etc.)
- Stockage MinIO/S3
- SHA-256 hash pour intégrité
- URLs présignées pour téléchargement sécurisé

### Dashboard & Analytics
- Statistiques temps réel
- Nombre de propriétés/locations actives
- Revenus mensuels
- Activité récente

---

## 🏗️ Architecture

### Structure du Projet

```
howners-back/
├── src/main/java/com/howners/gestion/
│   ├── config/                     # Configuration Spring
│   │   ├── SecurityConfig.java     # JWT, CORS, RBAC
│   │   ├── S3Config.java          # MinIO/S3 client
│   │   ├── DocuSignProperties.java # Config DocuSign
│   │   ├── JacksonConfig.java     # JSON serialization
│   │   └── ...
│   │
│   ├── controller/                 # REST Controllers
│   │   ├── AuthController.java
│   │   ├── PropertyController.java
│   │   ├── RentalController.java
│   │   ├── ContractController.java
│   │   ├── ContractESignatureController.java
│   │   ├── PublicContractController.java
│   │   ├── WebhookController.java
│   │   └── ...
│   │
│   ├── domain/                     # Entités JPA
│   │   ├── user/
│   │   │   ├── User.java          # Utilisateur avec rôles
│   │   │   └── Role.java          # OWNER, TENANT, ADMIN
│   │   ├── property/
│   │   │   ├── Property.java
│   │   │   └── PropertyType.java
│   │   ├── rental/
│   │   │   ├── Rental.java
│   │   │   └── RentalStatus.java
│   │   ├── contract/
│   │   │   ├── Contract.java
│   │   │   ├── ContractVersion.java
│   │   │   ├── ContractTemplate.java
│   │   │   ├── ContractSignatureRequest.java
│   │   │   └── SignatureRequestStatus.java
│   │   └── ...
│   │
│   ├── repository/                 # Spring Data JPA
│   │   ├── UserRepository.java
│   │   ├── PropertyRepository.java
│   │   ├── ContractRepository.java
│   │   ├── ContractSignatureRequestRepository.java
│   │   └── ...
│   │
│   ├── service/                    # Logique métier
│   │   ├── auth/
│   │   │   └── AuthService.java
│   │   ├── property/
│   │   │   └── PropertyService.java
│   │   ├── contract/
│   │   │   ├── ContractService.java
│   │   │   ├── ContractESignatureService.java  # ⭐ Core
│   │   │   └── PdfService.java
│   │   ├── esignature/
│   │   │   ├── ESignatureProvider.java         # Interface
│   │   │   ├── DocuSignProvider.java           # Implémentation
│   │   │   └── ESignatureProviderFactory.java
│   │   ├── email/
│   │   │   ├── EmailService.java               # Interface
│   │   │   └── SmtpEmailService.java
│   │   ├── storage/
│   │   │   └── StorageService.java             # MinIO/S3
│   │   └── ...
│   │
│   ├── security/                   # Sécurité JWT + Tokens
│   │   ├── jwt/
│   │   │   ├── JwtTokenProvider.java
│   │   │   ├── JwtAuthenticationFilter.java
│   │   │   └── JwtAuthenticationEntryPoint.java
│   │   ├── ContractTokenProvider.java          # BCrypt tokens
│   │   └── UserPrincipal.java
│   │
│   ├── dto/                        # Data Transfer Objects
│   │   ├── request/
│   │   ├── response/
│   │   ├── contract/
│   │   ├── esignature/
│   │   ├── email/
│   │   └── ...
│   │
│   └── exception/                  # Exceptions personnalisées
│       └── ...
│
├── src/main/resources/
│   ├── application.yml             # Configuration principale
│   ├── templates/email/            # Templates Thymeleaf
│   │   ├── signature-request.html
│   │   ├── signature-completed.html
│   │   └── signature-declined.html
│   └── db/changelog/               # Migrations Liquibase
│       ├── 001-create-users-table.xml
│       ├── 021-create-contract-signature-requests-table.xml
│       └── ...
│
├── docker-compose.yml              # PostgreSQL + MinIO + MailHog
├── pom.xml                         # Dépendances Maven
└── .env                            # Variables d'environnement
```

---

## 🛠️ Technologies

### Backend Core
| Technologie | Version | Usage |
|-------------|---------|-------|
| **Spring Boot** | 4.0.2 | Framework principal |
| **Java** | 21+ | Langage |
| **Maven** | 3.8+ | Build tool |
| **PostgreSQL** | 16 | Base de données |
| **Liquibase** | Latest | Migrations DB |
| **Lombok** | Latest | Réduction boilerplate |

### Sécurité
| Technologie | Version | Usage |
|-------------|---------|-------|
| **Spring Security** | 7.0.3 | Authentification/autorisation |
| **JJWT** | 0.12.5 | JWT tokens |
| **BCrypt** | - | Hash passwords/tokens |

### Intégrations
| Technologie | Version | Usage |
|-------------|---------|-------|
| **DocuSign SDK** | 6.5.0 | Signature électronique |
| **AWS SDK v2** | 2.21.0 | Stockage S3/MinIO |
| **iText** | 8.0.3 | Génération PDF |
| **JavaMail** | - | Envoi emails SMTP |
| **Thymeleaf** | - | Templates email HTML |

---

## 📦 Prérequis

### Logiciels Requis
- **Java JDK** 21 ou supérieur
- **Maven** 3.8+
- **Docker** & Docker Compose (pour services locaux)
- **Git** (pour clonage)

### Services Externes
- **PostgreSQL** 16 (fourni via Docker Compose)
- **MinIO** (fourni via Docker Compose)
- **MailHog** (fourni via Docker Compose)
- **DocuSign** (compte sandbox pour tests complets)

---

## 🚀 Installation

### 1. Cloner le Projet
```bash
git clone https://github.com/votre-repo/howners.git
cd howners/howners-back
```

### 2. Démarrer l'Infrastructure Docker
```bash
# Depuis le dossier howners-back/
docker-compose up -d

# Vérifier que les services sont UP
docker-compose ps
```

**Services disponibles:**
- PostgreSQL: `localhost:5432`
- MinIO API: `localhost:9000`
- MinIO Console: http://localhost:9001
- MailHog Web UI: http://localhost:8025

### 3. Configuration
Créer un fichier `.env` à la racine du projet:

```bash
# Base de données
DATABASE_URL=jdbc:postgresql://localhost:5432/howners_db
POSTGRES_USER=howners_user
POSTGRES_PASSWORD=howners_pass

# JWT
JWT_SECRET=your-very-long-secret-key-at-least-64-characters-long-for-security
JWT_EXPIRATION=86400000

# MinIO/S3
MINIO_ENDPOINT=http://localhost:9000
MINIO_ROOT_USER=minioadmin
MINIO_ROOT_PASSWORD=minioadmin123
MINIO_BUCKET=howners-documents

# Email (MailHog local)
SMTP_HOST=localhost
SMTP_PORT=1025
EMAIL_FROM=noreply@howners.com

# URLs Application
BACKEND_URL=http://localhost:8080
FRONTEND_URL=http://localhost:4200

# DocuSign (sandbox)
ESIGNATURE_PROVIDER=docusign
DOCUSIGN_INTEGRATION_KEY=your-integration-key
DOCUSIGN_USER_ID=your-user-id
DOCUSIGN_ACCOUNT_ID=your-account-id
DOCUSIGN_PRIVATE_KEY=your-rsa-private-key
DOCUSIGN_BASE_PATH=https://demo.docusign.net/restapi
DOCUSIGN_OAUTH_BASE_PATH=https://account-d.docusign.com

# CORS
CORS_ALLOWED_ORIGINS=http://localhost:4200,http://localhost:4201
```

### 4. Installer les Dépendances
```bash
./mvnw clean install -DskipTests
```

---

## ⚙️ Configuration

### application.yml

Le fichier `src/main/resources/application.yml` contient la configuration principale:

```yaml
spring:
  application:
    name: howners-backend

  datasource:
    url: ${DATABASE_URL}
    username: ${POSTGRES_USER}
    password: ${POSTGRES_PASSWORD}
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: none
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect

  liquibase:
    change-log: classpath:db/changelog/db.changelog-master.xml

  mail:
    host: ${SMTP_HOST}
    port: ${SMTP_PORT}
    username: ${SMTP_USERNAME:}
    password: ${SMTP_PASSWORD:}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true

jwt:
  secret: ${JWT_SECRET}
  expiration: ${JWT_EXPIRATION}

storage:
  endpoint: ${MINIO_ENDPOINT}
  access-key: ${MINIO_ROOT_USER}
  secret-key: ${MINIO_ROOT_PASSWORD}
  bucket: ${MINIO_BUCKET}

esignature:
  provider: ${ESIGNATURE_PROVIDER:docusign}
  docusign:
    integration-key: ${DOCUSIGN_INTEGRATION_KEY}
    user-id: ${DOCUSIGN_USER_ID}
    account-id: ${DOCUSIGN_ACCOUNT_ID}
    base-path: ${DOCUSIGN_BASE_PATH}
    oauth-base-path: ${DOCUSIGN_OAUTH_BASE_PATH}
    private-key: ${DOCUSIGN_PRIVATE_KEY}
  callback-base-url: ${BACKEND_URL}

cors:
  allowed-origins: ${CORS_ALLOWED_ORIGINS}
```

---

## 🏃 Lancement

### Mode Développement
```bash
# Compilation + Démarrage
./mvnw spring-boot:run

# L'application démarre sur http://localhost:8080
```

### Mode Production
```bash
# Build du JAR
./mvnw clean package -DskipTests

# Exécution
java -jar target/demo-0.0.1-SNAPSHOT.jar
```

### Vérifier le Démarrage
```bash
# Health check
curl http://localhost:8080/actuator/health

# Réponse attendue
{"status":"UP"}
```

### Logs de Démarrage
Les logs doivent confirmer:
```
✅ Started HownersApplication in X.XXX seconds
✅ HikariPool-1 - Start completed
✅ Tomcat started on port 8080
✅ Database info: PostgreSQL 16.11
✅ JPA EntityManagerFactory initialized
```

---

## 📚 API Documentation

### Endpoints Principaux

#### Authentication
| Méthode | Endpoint | Description | Auth |
|---------|----------|-------------|------|
| POST | `/api/auth/register` | Inscription utilisateur | ❌ |
| POST | `/api/auth/login` | Connexion (JWT) | ❌ |
| GET | `/api/auth/me` | Profil utilisateur | ✅ |

#### Properties
| Méthode | Endpoint | Description | Auth | Rôles |
|---------|----------|-------------|------|-------|
| GET | `/api/properties` | Liste propriétés | ✅ | OWNER, ADMIN |
| GET | `/api/properties/{id}` | Détail propriété | ✅ | OWNER, ADMIN |
| POST | `/api/properties` | Créer propriété | ✅ | OWNER, ADMIN |
| PUT | `/api/properties/{id}` | Modifier propriété | ✅ | OWNER, ADMIN |
| DELETE | `/api/properties/{id}` | Supprimer propriété | ✅ | OWNER, ADMIN |

#### Rentals
| Méthode | Endpoint | Description | Auth | Rôles |
|---------|----------|-------------|------|-------|
| GET | `/api/rentals` | Liste locations | ✅ | OWNER, TENANT, ADMIN |
| POST | `/api/rentals` | Créer location | ✅ | OWNER, ADMIN |
| PUT | `/api/rentals/{id}` | Modifier location | ✅ | OWNER, ADMIN |

#### Contracts
| Méthode | Endpoint | Description | Auth | Rôles |
|---------|----------|-------------|------|-------|
| GET | `/api/contracts` | Liste contrats | ✅ | OWNER, ADMIN |
| GET | `/api/contracts/{id}` | Détail contrat | ✅ | OWNER, TENANT, ADMIN |
| POST | `/api/contracts` | Générer contrat | ✅ | OWNER, ADMIN |
| PUT | `/api/contracts/{id}` | Modifier contrat | ✅ | OWNER, ADMIN |

#### E-Signature (Authentifié)
| Méthode | Endpoint | Description | Auth | Rôles |
|---------|----------|-------------|------|-------|
| POST | `/api/contracts/{id}/esignature/send` | Envoyer pour signature | ✅ | OWNER, ADMIN |
| GET | `/api/contracts/{id}/esignature/status` | Statut signature | ✅ | OWNER, ADMIN |
| POST | `/api/contracts/{id}/esignature/resend` | Renvoyer email | ✅ | OWNER, ADMIN |
| DELETE | `/api/contracts/{id}/esignature/cancel` | Annuler demande | ✅ | OWNER, ADMIN |

#### E-Signature (Public - Sans Auth)
| Méthode | Endpoint | Description | Auth |
|---------|----------|-------------|------|
| GET | `/api/public/contracts/token/{token}` | Voir contrat par token | ❌ |
| POST | `/api/public/contracts/token/{token}/redirect` | Obtenir URL DocuSign | ❌ |

#### Webhooks
| Méthode | Endpoint | Description | Auth |
|---------|----------|-------------|------|
| POST | `/api/webhooks/docusign` | Webhook DocuSign | ❌ |

#### Documents
| Méthode | Endpoint | Description | Auth |
|---------|----------|-------------|------|
| POST | `/api/documents/upload` | Upload document | ✅ |
| GET | `/api/documents/{id}/download` | Télécharger document | ✅ |

### Exemples de Requêtes

#### 1. Inscription
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "owner@example.com",
    "password": "SecurePass123!",
    "firstName": "Jean",
    "lastName": "Dupont",
    "phone": "0612345678",
    "role": "OWNER"
  }'
```

#### 2. Connexion
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "owner@example.com",
    "password": "SecurePass123!"
  }'

# Réponse
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "type": "Bearer",
  "user": {
    "id": "uuid",
    "email": "owner@example.com",
    "role": "OWNER"
  }
}
```

#### 3. Créer une Propriété
```bash
curl -X POST http://localhost:8080/api/properties \
  -H "Authorization: Bearer {votre-jwt-token}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Appartement T3 Centre-Ville",
    "propertyType": "APARTMENT",
    "addressLine1": "15 Rue de la République",
    "city": "Lyon",
    "postalCode": "69001",
    "surfaceArea": 65.5,
    "bedrooms": 2,
    "bathrooms": 1,
    "description": "Bel appartement rénové"
  }'
```

#### 4. Envoyer un Contrat pour Signature
```bash
curl -X POST http://localhost:8080/api/contracts/{contract-id}/esignature/send \
  -H "Authorization: Bearer {votre-jwt-token}"

# Réponse
{
  "id": "uuid",
  "status": "SENT",
  "signerEmail": "tenant@example.com",
  "sentAt": "2026-02-08T10:00:00Z",
  "tokenExpiresAt": "2026-03-10T10:00:00Z"
}
```

#### 5. Accès Public au Contrat
```bash
curl -X GET http://localhost:8080/api/public/contracts/token/{token}

# Réponse (sans authentification)
{
  "contractId": "uuid",
  "contractNumber": "CONT-2026-001",
  "propertyName": "Appartement T3 Centre-Ville",
  "tenantName": "Marie Martin",
  "monthlyRent": 850.00,
  "status": "SENT"
}
```

---

## 🗄️ Base de Données

### Migrations Liquibase

22 migrations gérées automatiquement au démarrage:

```
001 - Création table users
002 - Création table properties
003 - Création table rentals
004 - Création table contracts
005 - Création table contract_versions
006 - Création table signatures
007-012 - Tables paiements, documents, photos
013-020 - Améliorations et colonnes supplémentaires
021 - Création table contract_signature_requests ⭐
022 - Ajout colonne signature_provider ⭐
```

### Schéma Principal

**Relations clés:**
```
User (OWNER) --< Property --< Rental >-- User (TENANT)
                                |
                            Contract --< ContractVersion
                                |
                      ContractSignatureRequest
```

### Vérifier les Migrations
```bash
# Se connecter à PostgreSQL
docker exec -it howners-postgres psql -U howners_user -d howners_db

# Lister les tables
\dt

# Vérifier les migrations
SELECT * FROM databasechangelog ORDER BY dateexecuted DESC LIMIT 5;
```

---

## 🔐 Sécurité

### JWT Authentication
- **Algorithme**: HS512 (HMAC-SHA512)
- **Expiration**: 24h par défaut
- **Storage**: localStorage (frontend)
- **Header**: `Authorization: Bearer {token}`

### Contract Token Security
- **Génération**: SecureRandom 32 bytes (256 bits)
- **Hashing**: BCrypt (cost 12)
- **Encoding**: Base64 URL-safe
- **Expiration**: 30 jours par défaut
- **Usage**: Accès public aux contrats sans compte

### Password Security
- **Hashing**: BCrypt (Spring Security default)
- **Validation**: Min 8 caractères (recommandé)

### RBAC (Role-Based Access Control)
| Rôle | Permissions |
|------|-------------|
| **OWNER** | CRUD propriétés, locations, contrats, envoi signature |
| **TENANT** | Lecture locations/contrats assignés, signature |
| **ADMIN** | Accès complet |

### Endpoints Publics
Seuls ces endpoints sont accessibles sans JWT:
- `/api/auth/register`
- `/api/auth/login`
- `/api/public/contracts/token/**`
- `/api/webhooks/**`
- `/actuator/health`

### CORS
- Origins autorisées: Configuration via `CORS_ALLOWED_ORIGINS`
- Méthodes: GET, POST, PUT, DELETE, PATCH, OPTIONS
- Headers: All
- Credentials: Enabled

---

## 🧪 Tests

### Tests Manuels (Postman/curl)

#### Workflow Complet
1. Inscription/Connexion
2. Créer propriété
3. Créer location (avec email locataire)
4. Générer contrat
5. Envoyer pour signature
6. Vérifier email dans MailHog (http://localhost:8025)
7. Copier token de l'email
8. Accéder à `/api/public/contracts/token/{token}`
9. Vérifier le statut

#### Vérifier MailHog
```bash
# Ouvrir l'interface web
open http://localhost:8025

# Vérifier les emails envoyés
# Template "Signature de contrat" devrait apparaître
```

### Tests Unitaires (À Implémenter)
```bash
./mvnw test
```

### Tests d'Intégration (À Implémenter)
```bash
./mvnw verify
```

---

## 🚀 Déploiement

### Environnement Staging

```bash
# 1. Build
./mvnw clean package -DskipTests

# 2. Upload JAR sur serveur
scp target/demo-0.0.1-SNAPSHOT.jar user@server:/opt/howners/

# 3. Configurer variables d'environnement sur serveur
export DATABASE_URL=jdbc:postgresql://db-server:5432/howners_db
export JWT_SECRET=production-secret-key
export MINIO_ENDPOINT=https://s3.yourcompany.com
# ...

# 4. Lancer
java -jar /opt/howners/demo-0.0.1-SNAPSHOT.jar
```

### Docker Deployment

```dockerfile
FROM openjdk:21-jdk-slim
WORKDIR /app
COPY target/demo-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
```

```bash
docker build -t howners-backend .
docker run -p 8080:8080 --env-file .env howners-backend
```

### Checklist Production

- [ ] Changer `JWT_SECRET` (256+ bits random)
- [ ] Configurer SMTP production (Gmail/SendGrid)
- [ ] Configurer DocuSign production
- [ ] Activer HTTPS (certificat SSL)
- [ ] Configurer AWS S3 (au lieu de MinIO)
- [ ] Activer logging agrégé (ELK, CloudWatch)
- [ ] Monitoring (Prometheus + Grafana)
- [ ] Backup automatique PostgreSQL
- [ ] Rate limiting API
- [ ] Security audit

---

## 🐛 Troubleshooting

### Problème: Application ne démarre pas

**Erreur: `ObjectMapper bean not found`**
```
Solution: Vérifier que JacksonConfig.java existe
Location: src/main/java/com/howners/gestion/config/JacksonConfig.java
```

**Erreur: `Connection refused to PostgreSQL`**
```bash
# Vérifier que PostgreSQL est UP
docker-compose ps

# Redémarrer
docker-compose restart postgres

# Vérifier les logs
docker-compose logs postgres
```

### Problème: Email non envoyé

```bash
# Vérifier MailHog
curl http://localhost:8025/api/v1/messages

# Vérifier la config SMTP
grep -A 5 "spring.mail" src/main/resources/application.yml

# Vérifier les logs Spring
# Chercher "SmtpEmailService" dans les logs
```

### Problème: DocuSign webhook ne fonctionne pas

```bash
# En développement local, utiliser ngrok
ngrok http 8080

# Configurer l'URL webhook dans DocuSign:
# https://your-ngrok-url.ngrok.io/api/webhooks/docusign
```

### Problème: MinIO inaccessible

```bash
# Vérifier le service
docker-compose ps minio

# Accéder à la console
open http://localhost:9001

# Login: minioadmin / minioadmin123

# Vérifier que le bucket existe
aws --endpoint-url http://localhost:9000 s3 ls
```

### Logs de Débogage

```bash
# Activer logs DEBUG
# Dans application.yml:
logging:
  level:
    com.howners.gestion: DEBUG
    org.springframework.security: DEBUG
```

---

## 📖 Documentation Complémentaire

- [WORKFLOW_SIGNATURE.md](./WORKFLOW_SIGNATURE.md) - Workflow détaillé signature électronique
- [IMPLEMENTATION_COMPLETE.md](./IMPLEMENTATION_COMPLETE.md) - Rapport d'implémentation
- [SETUP_GUIDE.md](./SETUP_GUIDE.md) - Guide de configuration complet
- [TEST_RESULTS.md](./TEST_RESULTS.md) - Résultats des tests

---

## 🤝 Contribution

### Standards de Code
- Java Code Style: Google Java Style Guide
- Commits: Conventional Commits
- Branches: feature/xxx, fix/xxx, docs/xxx

### Ajouter une Feature
1. Fork le projet
2. Créer une branche (`git checkout -b feature/ma-feature`)
3. Commit (`git commit -m 'Add: nouvelle feature'`)
4. Push (`git push origin feature/ma-feature`)
5. Créer une Pull Request

---

## 📄 License

MIT License - Voir fichier [LICENSE](LICENSE)

---

## 👥 Auteurs

- **Équipe Howners** - *Développement initial*
- **Claude Code** - *Assistance implémentation e-signature*

---

## 🙏 Remerciements

- Spring Boot Team
- DocuSign Developer Community
- MinIO Team
- iText Team

---

## 📞 Support

- **Email**: support@howners.com
- **Documentation**: https://docs.howners.com
- **Issues**: https://github.com/votre-repo/howners/issues

---

**Version**: 1.0.0
**Date**: 08 Février 2026
**Statut**: ✅ Production Ready

---
