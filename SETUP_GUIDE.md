# Guide de Configuration - Système de Signature Électronique

## 🚀 Démarrage Rapide (5 minutes)

### 1. Démarrer l'Infrastructure

```bash
# Dans le dossier racine howners/
docker-compose up -d
```

**Services démarrés :**
- ✅ PostgreSQL (port 5432)
- ✅ MinIO S3 (port 9000, console 9001)
- ✅ MailHog SMTP + Web UI (ports 1025, 8025)

**Vérification :**
```bash
docker-compose ps
# Tous les services doivent être "Up"
```

**Interfaces Web :**
- 📧 MailHog: http://localhost:8025 (voir les emails)
- 📦 MinIO Console: http://localhost:9001 (admin/minioadmin123)
- 🗄️ PostgreSQL: localhost:5432 (howners_user/howners_pass)

---

### 2. Configurer les Variables d'Environnement

Le fichier `.env` est déjà configuré avec des valeurs par défaut pour les tests locaux.

**Configuration actuelle :**
```env
# SMTP → MailHog (local)
SMTP_HOST=localhost
SMTP_PORT=1025

# Base de données
POSTGRES_DB=howners_db
DATABASE_URL=jdbc:postgresql://localhost:5432/howners_db

# MinIO/S3
MINIO_ENDPOINT=http://localhost:9000
MINIO_BUCKET=howners-documents

# URLs
BACKEND_URL=http://localhost:8080
FRONTEND_URL=http://localhost:4200
```

**⚠️ DocuSign (optionnel pour tests initiaux) :**
Pour tester la signature électronique complète, vous devez créer un compte DocuSign Developer :

1. Créer compte: https://developers.docusign.com
2. Créer une application (Integration Key)
3. Générer RSA keypair
4. Ajouter les credentials dans `.env`

**Sans DocuSign :** Vous pouvez tester tout sauf la signature DocuSign elle-même.

---

### 3. Démarrer le Backend

```bash
cd howners-back
./mvnw spring-boot:run
```

**Au premier démarrage :**
- ✅ Liquibase crée les tables automatiquement
- ✅ Migrations 001 à 022 s'exécutent
- ✅ Tables créées : users, properties, rentals, contracts, contract_signature_requests, etc.

**Vérification :**
```bash
# Health check
curl http://localhost:8080/actuator/health

# Expected: {"status":"UP"}
```

**Logs attendus :**
```
Started HownersApplication in X.XXX seconds
Liquibase: Successfully acquired change log lock
Liquibase: Update summary:
  - 22 changesets
  - 0 previously run
```

---

### 4. Démarrer le Frontend

```bash
cd howners-api
npm install  # Première fois seulement
npm start
```

**Accessible sur :** http://localhost:4200

**Vérification :**
- Page de login charge
- Pas d'erreurs dans la console browser
- API calls vers http://localhost:8080/api

---

## 🧪 Tests de Fonctionnement

### Test 1 : Infrastructure Docker

```bash
# Vérifier que tous les services sont UP
docker-compose ps

# Logs PostgreSQL
docker-compose logs postgres

# Logs MailHog
docker-compose logs mailhog

# Tester la connexion PostgreSQL
docker exec -it howners-postgres psql -U howners_user -d howners_db -c "\dt"
```

**Résultat attendu :** Liste des 15+ tables créées par Liquibase.

---

### Test 2 : Backend APIs

```bash
# Health check
curl http://localhost:8080/actuator/health

# Créer un utilisateur (exemple)
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "owner@test.com",
    "password": "test123",
    "firstName": "John",
    "lastName": "Doe",
    "role": "OWNER"
  }'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "owner@test.com",
    "password": "test123"
  }'
```

**Résultat attendu :** Token JWT retourné.

---

### Test 3 : Service Email (MailHog)

**Tester l'envoi d'email :**

1. Créer un contrat dans l'application
2. Cliquer "Envoyer pour signature électronique"
3. Ouvrir MailHog: http://localhost:8025
4. Vérifier qu'un email est arrivé avec le design HTML

**Email attendu :**
- Sujet: "Signature de votre contrat de location - [Propriété]"
- Contenu: Template HTML professionnel
- Bouton: "✍️ Signer mon contrat"
- Lien avec token

---

### Test 4 : Accès Public (Sans Auth)

```bash
# Simuler un token (remplacer par un vrai token après envoi)
curl http://localhost:8080/api/public/contracts/token/test-token-here
```

**Ou dans le browser :**
```
http://localhost:4200/sign?token=test-token-here
```

**Résultat attendu :**
- Page charge sans redirect vers login
- Message d'erreur "Token invalide" (normal sans vrai token)

---

## 🔧 Workflow Complet de Test

### Scénario : Créer et Envoyer un Contrat pour Signature

**1. Créer des utilisateurs (via Postman/curl) :**

```bash
# Propriétaire
POST /api/auth/register
{
  "email": "owner@test.com",
  "password": "Test123!",
  "firstName": "Jean",
  "lastName": "Dupont",
  "role": "OWNER"
}

# Locataire
POST /api/auth/register
{
  "email": "tenant@test.com",
  "password": "Test123!",
  "firstName": "Marie",
  "lastName": "Martin",
  "role": "TENANT"
}
```

**2. Créer une propriété (authentifié comme OWNER) :**

```bash
POST /api/properties
Authorization: Bearer {owner-jwt-token}
{
  "name": "Appartement Centre-ville",
  "addressLine1": "15 Rue de la République",
  "city": "Paris",
  "postalCode": "75001",
  "country": "France",
  "propertyType": "APARTMENT",
  "surfaceArea": 45,
  "bedrooms": 2
}
```

**3. Créer une location :**

```bash
POST /api/rentals
Authorization: Bearer {owner-jwt-token}
{
  "propertyId": "{property-id}",
  "tenantId": "{tenant-id}",
  "startDate": "2025-03-01",
  "monthlyRent": 850.00,
  "depositAmount": 1700.00,
  "rentalType": "LONG_TERM"
}
```

**4. Créer un contrat :**

```bash
POST /api/contracts
Authorization: Bearer {owner-jwt-token}
{
  "rentalId": "{rental-id}",
  "templateId": "{template-id}",  # Ou null pour template par défaut
  "customContent": null
}
```

**5. Envoyer pour signature :**

```bash
POST /api/contracts/{contract-id}/esignature/send
Authorization: Bearer {owner-jwt-token}
```

**6. Vérifier dans MailHog :**
- Ouvrir http://localhost:8025
- Email avec lien de signature visible
- Copier le token du lien

**7. Accéder à la page de signature :**
```
http://localhost:4200/sign?token={le-token-copié}
```

**8. Vérifier le statut :**
```bash
GET /api/contracts/{contract-id}/esignature/status
Authorization: Bearer {owner-jwt-token}
```

---

## 🐛 Dépannage

### Problème : Backend ne démarre pas

**Erreur : "Connection refused to localhost:5432"**
```bash
# Vérifier que PostgreSQL tourne
docker-compose ps postgres

# Redémarrer PostgreSQL
docker-compose restart postgres
```

**Erreur : "Liquibase lock"**
```bash
# Libérer le lock
docker exec -it howners-postgres psql -U howners_user -d howners_db \
  -c "UPDATE databasechangeloglock SET locked=false, lockgranted=null, lockedby=null;"
```

---

### Problème : Emails n'arrivent pas dans MailHog

**Vérification :**
```bash
# MailHog est accessible ?
curl http://localhost:8025

# Logs MailHog
docker-compose logs mailhog

# Configuration SMTP dans application.yml correcte ?
SMTP_HOST=localhost
SMTP_PORT=1025
```

---

### Problème : Frontend ne compile pas

**Erreur : "Cannot find module"**
```bash
cd howners-api
rm -rf node_modules package-lock.json
npm install
npm start
```

---

### Problème : DocuSign erreurs

**Sans DocuSign configuré :**
- L'envoi pour signature fonctionnera partiellement
- L'email sera envoyé
- La création d'enveloppe DocuSign échouera

**Solution temporaire :** Commenter le code DocuSign dans `ContractESignatureService.sendContractForSignature()` pour tester le reste.

---

## 📊 Checklist de Configuration

### Infrastructure (Docker)
- [ ] PostgreSQL accessible (port 5432)
- [ ] MinIO accessible (port 9000)
- [ ] MailHog Web UI accessible (port 8025)
- [ ] Bucket MinIO créé (`howners-documents`)

### Backend
- [ ] Application démarre sans erreur
- [ ] Migrations Liquibase exécutées (22 changesets)
- [ ] Health check retourne UP
- [ ] Connexion DB établie
- [ ] Connexion MinIO établie

### Frontend
- [ ] npm install réussi
- [ ] npm start démarre le serveur
- [ ] Application charge sur port 4200
- [ ] Pas d'erreurs console

### Tests Fonctionnels
- [ ] Création utilisateur fonctionne
- [ ] Login retourne JWT
- [ ] Création contrat fonctionne
- [ ] Envoi email arrive dans MailHog
- [ ] Page publique `/sign` charge

---

## 🎯 Configuration Production

### Variables à changer pour PROD

```env
# SMTP Réel (Gmail/SendGrid)
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=noreply@howners.com
SMTP_PASSWORD=app-specific-password

# DocuSign Production
DOCUSIGN_BASE_PATH=https://www.docusign.net/restapi
DOCUSIGN_OAUTH_BASE_PATH=https://account.docusign.com

# URLs Production
BACKEND_URL=https://api.howners.com
FRONTEND_URL=https://app.howners.com

# JWT Secret (min 64 chars)
JWT_SECRET=change-this-to-a-real-random-secret-min-64-characters-long

# Database (hébergée)
DATABASE_URL=jdbc:postgresql://prod-db.example.com:5432/howners_prod
```

---

## 📝 Commandes Utiles

```bash
# Arrêter tous les services Docker
docker-compose down

# Supprimer les données (reset complet)
docker-compose down -v

# Logs en temps réel
docker-compose logs -f

# Rebuild backend après changements
cd howners-back
./mvnw clean install
./mvnw spring-boot:run

# Rebuild frontend
cd howners-api
npm run build
```

---

**Prochaine étape :** Tests d'intégration E2E ! 🚀
