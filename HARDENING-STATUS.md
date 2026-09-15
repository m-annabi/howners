# Sécurisation — branche codex/hardening-audit

## Vérifications effectuées

- Java 21, `./mvnw test` : **263 tests réussis**, 0 échec, 0 erreur ; 2 tests PostgreSQL
  ignorés faute de base de test locale (265 tests recensés).
- `npm run build` et `npm run prerender` : réussis ; avertissements de taille bundle/SCSS
  toujours présents. Le pré-rendu signale aussi l'impossibilité d'extraire automatiquement les
  routes, mais termine le rendu de la route explicitement configurée.
- `npx tsc --project tsconfig.spec.json --noEmit` : réussi (compilation des tests frontend,
  pas leur exécution dans un navigateur).
- `git diff --check` et parsing YAML du workflow/Compose : réussis.
- Tests navigateur non exécutés : téléchargement du navigateur indisponible dans cet environnement.
- Tests de migration PostgreSQL ajoutés à la CI mais non exécutés ici.

## Changements locaux (pas de déploiement)

- Les seeds 027, 049–051 et 054–056 exigent le contexte Liquibase explicitement activé `demo`.
  Les fichiers historiques ne sont pas réécrits : restrictions portées par les includes.
- Le profil prod active le contexte `prod`. La migration 111 désactive les comptes du domaine
  réservé `@howners.test` et incrémente leurs versions de jeton, sans effacer de données.
- HTTP refuse les utilisateurs désactivés. WebSocket refuse les jetons invalides/révoqués,
  restreint les origines, limite les abonnements à `/user/queue/messages` et interdit SEND
  (l'envoi métier passe par REST). La révocation est revérifiée à SUBSCRIBE/SEND ; une
  connexion déjà abonnée n'est pas automatiquement fermée par cette modification.
- Les endpoints Stripe exigent un secret de signature, même en local. Utiliser Stripe CLI
  avec un secret de webhook local, jamais des payloads HTTP non signés.
- Une table transactionnelle déduplique les identifiants d'événements Stripe : l'événement
  n'est enregistré définitivement que si le traitement réussit. Les échecs sont renvoyés en
  HTTP 500 pour reprise par Stripe. Les reprises manuelles se font depuis Stripe.
- Les paiements vérifient montant/devise et le compte connecté lorsqu'il est présent.
  Un verrou optimiste protège des mises à jour concurrentes. Les appels de création Stripe
  utilisent des clés d'idempotence ; les sessions Checkout ouvertes et intents connus sont
  réutilisés. Une session expirée ouvre une nouvelle tentative numérotée.
- CI : tests unitaires frontend et Cypress, PostgreSQL de test, images staging par SHA complet.
  Aucun workflow n'a été lancé à distance. Les tests Cypress sont des tests UI avec fixtures,
  pas une validation complète de Stripe, MinIO et PostgreSQL réunis.
- UX : retour à la page d'origine après reconnexion, messages de récupération plus précis,
  recharge du dashboard et avertissement quand les compteurs d'actions sont incomplets.
  Onboarding prolongé jusqu'à la première quittance.

## Mise en service — à effectuer après validation CI

1. Sauvegarder la base ET les objets MinIO hors de l'hôte ; restaurer ces copies dans un
   environnement isolé et vérifier ouverture des PDFs et correspondance des références.
   Le script db-backup.sh existant ne sauvegarde pas à lui seul les objets MinIO.
2. Vérifier les comptes qui seront désactivés :
   `SELECT id, email, enabled FROM users WHERE lower(email) LIKE '%@howners.test';`
   Ne pas utiliser de comptes de démonstration comme comptes réels de production.
3. Vérifier le profil `prod`, les origines CORS et les deux secrets webhooks Stripe.
4. Exécuter les tests de migration CI : base neuve prod et base demo mise à niveau en prod.
5. Conserver les références des images actuellement en service avant de déployer le nouveau SHA.
6. Vérifier connexion, refus d'un compte désactivé, quittance et paiement Stripe en sandbox.

### Données de démonstration en local uniquement

Activer `SPRING_LIQUIBASE_CONTEXTS=demo` sur une base locale dédiée, sans profil prod.
Le contexte `demo` ne doit jamais être ajouté aux paramètres d'une production.

### Retour arrière applicatif

Réutiliser le SHA complet de la dernière version validée :

```sh
BACKEND_TAG=sha-<SHA_VALIDÉ> FRONTEND_TAG=sha-<SHA_VALIDÉ>-staging \
docker compose --env-file .env.prod -f docker-compose.prod.yml pull backend frontend
BACKEND_TAG=sha-<SHA_VALIDÉ> FRONTEND_TAG=sha-<SHA_VALIDÉ>-staging \
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d backend frontend
```

Ne pas annuler automatiquement la désactivation des comptes de démonstration. Les migrations
de ce lot sont additives ; revenir aux anciennes images ne nécessite pas de supprimer leurs
colonnes. Les versions antérieures à ce lot n'ont pas forcément de tag SHA complet : conserver
alors leur digest constaté, ne pas inventer un tag.

## Points encore ouverts

- Angular 15 : la tentative de migration 16 a échoué sur les contraintes de versions
  Angular/Universal/TypeScript. Aucun changement de package.json ou du lockfile n'a été conservé.
  Converger explicitement les versions, puis migrer par majeure avec tests UI/SSR à chaque étape.
- Import bancaire : demander un export anonymisé et confirmer formats/devise/colonnes ; prévoir
  prévisualisation, dédoublonnage et confirmation explicite avant de solder un loyer. Pas encore implémenté.
- Le tableau de bord d'actions et l'export annuel LMNP existent déjà ; pas de duplication.
- Relecture juridique, restauration réelle hors site et vérification du serveur non effectuées.
- Une file durable de traitement asynchrone/une interface de rejeu, la réconciliation des
  abonnements reçus dans le désordre et une stratégie d'idempotence longue durée restent à prévoir.
- Les tests PostgreSQL nécessitent les variables HOWNERS_TEST_DATABASE_* (renseignées en CI).
  Sans elles, ils sont explicitement ignorés et ne prouvent pas une migration réussie.
