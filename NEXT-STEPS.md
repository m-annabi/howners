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

## Documents produits cette session

- `SECURITY-ROTATION.md` — quels secrets révoquer
- `OBSERVABILITY.md` — comment plug Sentry/Datadog
- `RGPD-AUDIT.md` — état CNIL + trous à combler
- `NEXT-STEPS.md` — ce fichier
