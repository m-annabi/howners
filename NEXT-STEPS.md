# Next steps — what's left and why I can't ship it from here

Cette session a traité 11 items du backlog d'audit (#1, #6, #8, #9, #10, #16, #17, #18, #19, #20, #5/RGPD). Les 5 suivants nécessitent des actions externes — du code seul ne les déverrouille pas.

## #2 — Routing Stripe Connect des paiements

**Quoi** : Aujourd'hui, `StripeConnectService` crée un compte Express mais les `PaymentIntent` créés par `PaymentService` n'utilisent pas `transfer_data[destination]`. Les loyers ne sont pas routés vers les comptes Connect des bailleurs.

**Pourquoi je n'ai pas livré** :
- Stripe Connect doit être activé sur le compte plateforme (depuis le dashboard Stripe → Connect → Get started).
- Le modèle de plateforme doit être déclaré : Direct charges vs Destination charges vs Separate charges and transfers. Chacun a des implications différentes pour le KYC, l'apparence sur le relevé bancaire du tenant, la responsabilité PCI.
- En France, agir comme prestataire de services de paiement nécessite **soit** un agrément ACPR, **soit** d'utiliser un partenaire agréé (Stripe, MangoPay, Lemonway). Stripe couvre via leur licence Irlande.
- La logique fee plateforme doit être définie : 1 % par paiement ? Abonnement avec fee = 0 ? Mixte ?

**Quand ce sera fait côté business** : ajouter dans `PaymentService.createPaymentIntent` :
```java
PaymentIntentCreateParams.builder()
  .setAmount(amount)
  .setCurrency("eur")
  .setOnBehalfOf(landlord.getStripeConnectAccountId())
  .setTransferData(PaymentIntentCreateParams.TransferData.builder()
    .setDestination(landlord.getStripeConnectAccountId())
    .build())
  .setApplicationFeeAmount(platformFeeAmount)
  .build();
```

## #3 — ~~OpenAI live wiring~~ — SUPPRIMÉ

La fonctionnalité de brouillon de bail assisté par IA (`/api/ai/draft-lease`, `AiLeaseService`)
a été **retirée** : jugée superflue, jamais exposée dans le frontend, et porteuse d'un risque
juridique (génération de clauses potentiellement non conformes sans relecture d'avocat).

## #4 — Validation juridique des templates de contrat

**Quoi** : Les templates seedés (`Bail d'habitation vide` + `Bail meublé`) ont été relus en surface ce soir (j'ai fixé `{{property.rooms}}` et `{{rental.endDateClause}}`) mais **personne de qualifié** n'a fait une revue clause par clause.

**Pourquoi je ne peux pas livrer** : Pas avocat. Toute clause manquante ou abusive est :
- Réputée non écrite (locataire bien défendu)
- Pénalité administrative jusqu'à 15 k€ (loi ALUR)

**Action** : faire reviewer les deux templates par un cabinet (Captain Contrat, Eurojuris, ou avocat indépendant immobilier). Coût ~500-1500 €.

## #7 — Angular Universal (SSR) — ✅ FAIT (pre-rendering)

Pre-rendering `@nguniversal/builders` en place : la landing `/` est rendue en HTML statique
à la build (`npm run prerender`), servie telle quelle par nginx (pas de SSR runtime / pas
d'infra Node supplémentaire). Le `<app-root>` n'est plus vide pour les crawlers.

Correctifs SSR appliqués (globals navigateur) : `StorageService` (localStorage), 
`InAppNotificationService` (polling désactivé en SSR), `LandingComponent` (DOCUMENT injecté).
Le JSON-LD SoftwareApplication est inclus dans le HTML pré-rendu.

**Reste possible** : prérendre d'autres routes publiques (`/auth/login`, `/auth/register`) en
les ajoutant à la liste `routes` du target `prerender` (angular.json) après avoir vérifié leur
SSR-safety ; ou passer au SSR runtime (`serve:ssr`, déjà scaffoldé via `server.ts`).

## #11-15 — Stratégique (out of code scope)

| Item | Bloqueur business |
|---|---|
| **APL/CAF intégration** | Pas d'API publique CAF pour bailleurs. Génération CERFA + dépôt automatique = partenariat ou prestataire (Garantme, SmartLoc). |
| **Assurance bailleur affiliée** | Nécessite un contrat partenaire signé (Luko, April, Solly Azar). Affiliate cut typique 5-10 % de la prime. |
| **Recouvrement loyers in-app** | Voir #2. + KYC SEPA niveau plateforme. |
| **Expansion BE/CH/LU** | Localisation copy + recherche légale par pays (Belgique loi de 91, Suisse code des obligations, Luxembourg loi sur le bail). Partenaire bancaire local pour SEPA. |
| **Marketplace gestionnaires** | Modèle ops (vetting des gestionnaires, SLA, dispute resolution). Embauche dédiée. |

## Priorité court terme suggérée

1. **#4** — relecture juridique des templates (le plus risqué légalement)
2. **#7** pre-rendering — gain SEO direct
3. **#2** Stripe routing si modèle business prévoit du fee paiement
4. **#11-15** quand un partenaire signe

## Reliquats de l'audit qualité (septembre 2026)

Traités : preuve IP non forgeable, réconciliation des deux systèmes de signature, retour DocuSign,
guards de rôle, tokens CSS, parcours (returnUrl, CTA anonyme, rôle obligatoire, bien → annonce,
candidature acceptée → location, relances dans le détail paiement, onboarding), SEO (sitemap
proxifié, domaine unifié `howners.com`, JSON-LD sur la fiche annonce), dédoublonnage frontend
(`downloadBlob`/`formatFileSize`, boîte de confirmation HTML) et backend (`RentalAccessService`,
`NotificationDispatcher`, `GeneratedDocumentService`).

Volontairement non traités — à arbitrer :

| Item | Pourquoi |
|---|---|
| **SSR runtime en prod** | Le pre-rendering couvre `/` et `/listings` ; les fiches annonces (`/listings/:id`) sont rendues côté client (les méta + JSON-LD sont posés au chargement). Passer à `serve:ssr` = un conteneur Node à la place de nginx (`Dockerfile`, `docker-compose.prod.yml`, Caddyfile). À faire quand le trafic organique le justifie. |
| **`CrudService<T>` générique** | Les services HTTP ont chacun 3-6 méthodes spécifiques ; un générique n'en factoriserait qu'une ou deux au prix d'une indirection. |
| **Adoption de `data-table`** | Les listes ont des colonnes/actions très différentes ; migrer 8 tableaux vers le composant générique est un chantier UI à part entière (à coupler avec une refonte des filtres). |
| **Fusion des uploads de photos (bien / annonce)** | Deux API distinctes (`/properties/:id/photos`, `/listings/:id/photos`) avec des règles de couverture différentes ; fusionner impose d'abord d'unifier le backend. |
| **Jumeaux `TenantRating` / `OwnerRating`** | Entités et tables distinctes (changelog 089) : une abstraction commune exigerait une migration de schéma. |
| **Vocabulaire bail / contrat** | Choix éditorial (le « contrat » couvre aussi les baux commerciaux et les avenants) — à trancher avec la relecture juridique (#4). |
| **Notice d'information + inventaire du mobilier** | Les templates de bail (Liquibase 106, voir `GUIDE-BAUX.md`) les listent en annexe mais ne les génèrent pas : joindre le PDF officiel de la notice à l'envoi du contrat et proposer un inventaire pré-rempli (décret 2015-981) pour les meublés. |
| **Paiement en ligne : SEPA et frais réels** | Clés Stripe de test à fournir ; ensuite activer le prélèvement SEPA et remonter les frais réels (`BalanceTransaction.fee`) à la place de l'estimation affichée (voir `STRIPE-FEES.md`). |

## Documents produits cette session

- `SECURITY-ROTATION.md` — quels secrets révoquer
- `OBSERVABILITY.md` — comment plug Sentry/Datadog
- `RGPD-AUDIT.md` — état CNIL + trous à combler
- `NEXT-STEPS.md` — ce fichier
