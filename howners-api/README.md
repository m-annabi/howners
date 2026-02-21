# Howners Frontend - Application Angular

![Version](https://img.shields.io/badge/version-1.0.0-blue)
![Angular](https://img.shields.io/badge/Angular-15.1.0-red)
![TypeScript](https://img.shields.io/badge/TypeScript-4.9.4-blue)
![License](https://img.shields.io/badge/license-MIT-brightgreen)

**Interface web pour la gestion locative avec signature électronique**

Application Angular moderne et responsive pour gérer propriétés, locations, contrats et signatures électroniques.

---

## 📋 Table des Matières

- [Vue d'Ensemble](#vue-densemble)
- [Fonctionnalités](#fonctionnalités)
- [Technologies](#technologies)
- [Architecture](#architecture)
- [Prérequis](#prérequis)
- [Installation](#installation)
- [Configuration](#configuration)
- [Développement](#développement)
- [Build](#build)
- [Structure](#structure)
- [Composants Principaux](#composants-principaux)
- [Routing](#routing)
- [Guards & Interceptors](#guards--interceptors)
- [État & Services](#état--services)
- [Styling](#styling)
- [Déploiement](#déploiement)

---

## 🎯 Vue d'Ensemble

Application web Angular permettant aux propriétaires de gérer leurs biens immobiliers et aux locataires de signer électroniquement leurs contrats.

**Caractéristiques:**
- 🎨 Interface moderne et responsive (Bootstrap)
- 🔐 Authentification JWT sécurisée
- 📱 Mobile-first design
- ✍️ Signature électronique (DocuSign + Canvas)
- 📄 Visualisation PDF intégrée
- 🔔 Notifications toast temps réel
- 🚀 Lazy loading pour performance optimale
- 🌐 Support multi-langues (préparé)

---

## ✨ Fonctionnalités

### Pour les Propriétaires (OWNER)

#### Tableau de Bord
- Statistiques en temps réel (propriétés, locations, revenus)
- Graphiques (à implémenter)
- Activités récentes

#### Gestion des Propriétés
- Liste avec filtres et recherche
- Fiche détaillée avec galerie photos
- Formulaire de création/édition
- Upload de photos avec réorganisation
- Association de documents

#### Gestion des Locations
- Liste des locations actives/terminées
- Création avec sélection de propriété
- Sélection/création du locataire
- Suivi des dates et montants

#### Gestion des Contrats
- Génération automatique à partir de templates
- Personnalisation avant envoi
- Aperçu PDF
- **Envoi pour signature électronique**
- Suivi du statut en temps réel
- Timeline de signature
- Téléchargement des contrats signés

#### Signature Électronique
- Bouton "Envoyer pour signature"
- Visualisation du statut
- Timeline: Envoyé → Vu → Signé
- Renvoyer l'email
- Annuler la demande
- Notifications email automatiques

### Pour les Locataires (TENANT)

#### Page de Signature Publique
- **Accès sans compte** via token sécurisé
- Visualisation du contrat complet
- Détails de la propriété et location
- Aperçu PDF du contrat
- Bouton "Signer le contrat"
- Redirection vers DocuSign
- Page de confirmation

#### Espace Personnel
- Vue des contrats assignés
- Historique des signatures
- Documents associés

---

## 🛠️ Technologies

### Framework & Core
| Technologie | Version | Usage |
|-------------|---------|-------|
| **Angular** | 15.1.0 | Framework principal |
| **TypeScript** | 4.9.4 | Langage |
| **Angular CLI** | 15.1.5 | Build & dev tools |
| **RxJS** | 7.8.0 | Programmation réactive |
| **Zone.js** | 0.12.0 | Change detection |

### UI & Styling
| Technologie | Version | Usage |
|-------------|---------|-------|
| **Bootstrap** | 5.3.0 | Framework CSS |
| **Bootstrap Icons** | 1.11.0 | Icônes |
| **SCSS** | - | Préprocesseur CSS |

### Build & Dev Tools
| Technologie | Version | Usage |
|-------------|---------|-------|
| **Webpack** | 5.x | Bundler (via Angular CLI) |
| **ESLint** | - | Linting TypeScript |
| **TypeScript** | 4.9.4 | Compilation |

---

## 🏗️ Architecture

### Structure du Projet

```
howners-api/
├── src/
│   ├── app/
│   │   ├── core/                           # Singleton services
│   │   │   ├── auth/
│   │   │   │   └── auth.service.ts         # Authentification JWT
│   │   │   ├── guards/
│   │   │   │   ├── auth.guard.ts          # Protection routes
│   │   │   │   └── role.guard.ts          # Vérification rôles
│   │   │   ├── interceptors/
│   │   │   │   ├── auth.interceptor.ts    # Injection JWT
│   │   │   │   └── error.interceptor.ts   # Gestion erreurs HTTP
│   │   │   ├── models/
│   │   │   │   ├── user.model.ts
│   │   │   │   ├── property.model.ts
│   │   │   │   ├── rental.model.ts
│   │   │   │   ├── contract.model.ts
│   │   │   │   ├── esignature.model.ts    # ⭐ Signature électronique
│   │   │   │   └── ...
│   │   │   └── services/
│   │   │       ├── auth.service.ts
│   │   │       ├── property.service.ts
│   │   │       ├── rental.service.ts
│   │   │       ├── contract.service.ts
│   │   │       ├── esignature.service.ts  # ⭐ E-signature
│   │   │       ├── public-contract.service.ts # ⭐ Public API
│   │   │       ├── notification.service.ts
│   │   │       └── ...
│   │   │
│   │   ├── features/                       # Modules fonctionnels
│   │   │   ├── auth/
│   │   │   │   ├── login/
│   │   │   │   │   ├── login.component.ts
│   │   │   │   │   ├── login.component.html
│   │   │   │   │   └── login.component.scss
│   │   │   │   └── register/
│   │   │   │       └── ...
│   │   │   │
│   │   │   ├── dashboard/
│   │   │   │   ├── dashboard.component.ts  # Tableau de bord
│   │   │   │   ├── dashboard.component.html
│   │   │   │   └── dashboard.component.scss
│   │   │   │
│   │   │   ├── properties/
│   │   │   │   ├── property-list/
│   │   │   │   ├── property-detail/
│   │   │   │   ├── property-form/
│   │   │   │   └── property-photo-upload/
│   │   │   │
│   │   │   ├── rentals/
│   │   │   │   ├── rental-list/
│   │   │   │   ├── rental-detail/
│   │   │   │   └── rental-form/
│   │   │   │
│   │   │   ├── contracts/
│   │   │   │   ├── contract-list/
│   │   │   │   │   ├── contract-list.component.ts
│   │   │   │   │   └── ...
│   │   │   │   ├── contract-detail/        # ⭐ Enrichi avec e-signature
│   │   │   │   │   ├── contract-detail.component.ts
│   │   │   │   │   ├── contract-detail.component.html
│   │   │   │   │   └── contract-detail.component.scss
│   │   │   │   ├── contract-form/
│   │   │   │   │   └── ...
│   │   │   │   └── contract-customize/
│   │   │   │       └── ...
│   │   │   │
│   │   │   └── public-sign/                # ⭐ NOUVEAU: Signature publique
│   │   │       ├── public-sign.component.ts
│   │   │       ├── public-sign.component.html
│   │   │       ├── public-sign.component.scss
│   │   │       ├── public-sign.module.ts
│   │   │       └── public-sign-routing.module.ts
│   │   │
│   │   ├── shared/                         # Composants réutilisables
│   │   │   ├── components/
│   │   │   │   ├── navbar/
│   │   │   │   ├── signature-pad/         # Canvas signature
│   │   │   │   ├── document-upload/
│   │   │   │   ├── property-photo-upload/
│   │   │   │   └── toast/
│   │   │   ├── directives/
│   │   │   └── pipes/
│   │   │
│   │   ├── app.component.ts                # Composant racine
│   │   ├── app.component.html
│   │   ├── app-routing.module.ts           # Routes principales
│   │   └── app.module.ts                   # Module racine
│   │
│   ├── assets/                             # Fichiers statiques
│   │   ├── images/
│   │   └── icons/
│   │
│   ├── environments/                       # Configuration environnements
│   │   ├── environment.ts                 # Développement
│   │   └── environment.prod.ts            # Production
│   │
│   ├── index.html                          # Page HTML principale
│   ├── main.ts                             # Point d'entrée
│   ├── styles.scss                         # Styles globaux
│   └── polyfills.ts                        # Polyfills navigateurs
│
├── angular.json                            # Configuration Angular CLI
├── package.json                            # Dépendances npm
├── tsconfig.json                           # Configuration TypeScript
└── README.md                               # Ce fichier
```

---

## 📦 Prérequis

### Logiciels Requis
- **Node.js** 18.x ou supérieur
- **npm** 9.x ou supérieur
- **Angular CLI** 15.1.5

### Vérifier les Versions
```bash
node --version    # v18.x.x ou supérieur
npm --version     # 9.x.x ou supérieur
ng version        # Angular CLI 15.1.5
```

### Installer Angular CLI
```bash
npm install -g @angular/cli@15.1.5
```

---

## 🚀 Installation

### 1. Cloner le Projet
```bash
git clone https://github.com/votre-repo/howners.git
cd howners/howners-api
```

### 2. Installer les Dépendances
```bash
npm install
```

**Temps d'installation:** ~2-3 minutes

---

## ⚙️ Configuration

### Fichiers d'Environnement

#### Development (`src/environments/environment.ts`)
```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api'
};
```

#### Production (`src/environments/environment.prod.ts`)
```typescript
export const environment = {
  production: true,
  apiUrl: 'https://api.howners.com/api'
};
```

### Modifier l'URL de l'API
```typescript
// Pour pointer vers un backend distant en dev
export const environment = {
  production: false,
  apiUrl: 'https://staging-api.howners.com/api'
};
```

---

## 💻 Développement

### Lancer le Serveur de Développement
```bash
npm start
# ou
ng serve

# L'application sera disponible sur http://localhost:4200
```

**Options utiles:**
```bash
# Port personnalisé
ng serve --port 4201

# Ouvrir automatiquement le navigateur
ng serve --open

# Mode production en dev (minification)
ng serve --configuration production
```

### Hot Reload
Les modifications du code sont automatiquement rechargées dans le navigateur.

### Logs de Développement
- Ouvrir les DevTools du navigateur (F12)
- Onglet Console pour voir les logs
- Onglet Network pour voir les requêtes HTTP

---

## 🔨 Build

### Build de Développement
```bash
npm run build
# ou
ng build

# Output: dist/howners-api/
```

### Build de Production
```bash
npm run build:prod
# ou
ng build --configuration production

# Output: dist/howners-api/
```

**Optimisations en production:**
- Minification du code
- Tree-shaking (suppression du code inutilisé)
- Optimisation des bundles
- Hashing des fichiers pour cache busting
- AOT (Ahead-of-Time) compilation

**Bundle Sizes (production):**
- Main bundle: ~381 KB → 98 KB gzippé
- Polyfills: ~33 KB → 10 KB gzippé
- Styles: ~22 KB → 3 KB gzippé

---

## 📐 Composants Principaux

### Public Sign Component ⭐ NOUVEAU
**Route:** `/sign?token={token}`
**Fonctionnalité:**
- **Accès PUBLIC (sans AuthGuard)**
- Extraction du token depuis URL
- Appel API public
- Affichage détails contrat (lecture seule)
- Informations propriété et location
- Aperçu PDF du contrat
- Bouton "Signer le contrat"
- Redirection vers DocuSign
- Gestion erreurs (token invalide/expiré)

### Contract Detail Component ⭐
**Route:** `/contracts/:id`
**Fonctionnalité:**
- Détails du contrat
- Aperçu PDF
- **Section E-Signature:**
  - Bouton "Envoyer pour signature"
  - Statut avec badge coloré (PENDING, SENT, VIEWED, SIGNED)
  - Timeline de signature
  - Informations signataire
  - Boutons "Renvoyer" et "Annuler"
  - Dates (envoyé, vu, signé)
  - Provider (DocuSign logo)

---

## 🛣️ Routing

### Routes Principales

```typescript
const routes: Routes = [
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },

  // Auth routes (no guard)
  {
    path: 'login',
    component: LoginComponent
  },
  {
    path: 'register',
    component: RegisterComponent
  },

  // Public route (no guard) ⭐
  {
    path: 'sign',
    loadChildren: () => import('./features/public-sign/public-sign.module')
      .then(m => m.PublicSignModule)
  },

  // Protected routes (AuthGuard)
  {
    path: 'dashboard',
    component: DashboardComponent,
    canActivate: [AuthGuard]
  },
  {
    path: 'properties',
    loadChildren: () => import('./features/properties/properties.module')
      .then(m => m.PropertiesModule),
    canActivate: [AuthGuard]
  },
  {
    path: 'rentals',
    loadChildren: () => import('./features/rentals/rentals.module')
      .then(m => m.RentalsModule),
    canActivate: [AuthGuard]
  },
  {
    path: 'contracts',
    loadChildren: () => import('./features/contracts/contracts.module')
      .then(m => m.ContractsModule),
    canActivate: [AuthGuard]
  },

  // 404
  { path: '**', redirectTo: '/dashboard' }
];
```

---

## 🛡️ Guards & Interceptors

### AuthGuard
**Fichier:** `src/app/core/guards/auth.guard.ts`

**Fonctionnalité:**
- Vérifie que l'utilisateur est authentifié (JWT présent)
- Redirige vers `/login` si non authentifié

### AuthInterceptor
**Fichier:** `src/app/core/interceptors/auth.interceptor.ts`

**Fonctionnalité:**
- Ajoute automatiquement le header `Authorization: Bearer {token}` à toutes les requêtes HTTP
- Ignore les endpoints publics

### ErrorInterceptor
**Fichier:** `src/app/core/interceptors/error.interceptor.ts`

**Fonctionnalité:**
- Intercepte les erreurs HTTP
- Affiche des notifications toast
- Déconnecte l'utilisateur si 401

---

## 📊 État & Services

### EsignatureService ⭐
**Fichier:** `src/app/core/services/esignature.service.ts`

**Méthodes:**
```typescript
sendForSignature(contractId: string): Observable<SignatureRequestResponse>
getSignatureStatus(contractId: string): Observable<SignatureRequestResponse>
resendSignatureRequest(contractId: string, signatureRequestId: string): Observable<void>
cancelSignatureRequest(contractId: string, signatureRequestId: string): Observable<void>
```

### PublicContractService ⭐
**Fichier:** `src/app/core/services/public-contract.service.ts`

**Méthodes:**
```typescript
getContractByToken(token: string): Observable<ContractPublicView>
getSigningRedirect(token: string, returnUrl?: string): Observable<SigningRedirectResponse>
```

---

## 🚀 Déploiement

### Build Production
```bash
npm run build:prod
```

**Output:** `dist/howners-api/`

### Déploiement sur Nginx

```nginx
server {
    listen 80;
    server_name howners.com;
    root /var/www/howners/dist/howners-api;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    # Cache des assets statiques
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
}
```

---

## 📖 Documentation Complémentaire

- [Backend README](../howners-back/README.md)
- [WORKFLOW_SIGNATURE.md](../WORKFLOW_SIGNATURE.md)
- [Angular Documentation](https://angular.io/docs)
- [Bootstrap Documentation](https://getbootstrap.com/docs/5.3/)

---

**Version**: 1.0.0
**Date**: 08 Février 2026
**Statut**: ✅ Production Ready
