# Paiement en ligne — frais Stripe et modèle économique

Note de travail (septembre 2026) en réponse à la question « afficher le surcoût Stripe et étudier
la question pour le business model ».

## Ce que fait la plateforme aujourd'hui

- **Stripe Connect en *direct charge*** : le locataire paie par carte sur une page Stripe ; l'argent
  est encaissé **directement sur le compte Connect du bailleur**. La plateforme ne touche jamais les
  fonds, elle prélève seulement sa commission au passage (`application_fee_amount`).
- **Commission plateforme** : `stripe.platform-fee-percent` (2,5 % par défaut), surchargeable par
  plan d'abonnement (`subscription_plans.platform_fee_percent`).
- **Frais Stripe** : en direct charge, ils sont **supportés par le compte connecté, donc par le
  bailleur** (tarif standard cartes européennes : 1,5 % + 0,25 € ; cartes hors UE : 3,25 % + 0,25 € ;
  plus 2 % si conversion de devise). Ils ne sont pas visibles dans le code : Stripe les déduit du
  versement.
- **Le locataire ne paie aucun frais.** Le paiement hors plateforme (virement, chèque) reste
  gratuit pour tout le monde : le locataire le déclare, le bailleur confirme.

Exemple sur un loyer de 800 € réglé par carte (plan par défaut) :

| Poste | Montant |
|---|---|
| Commission Howners 2,5 % | 20,00 € |
| Frais Stripe 1,5 % + 0,25 € | 12,25 € |
| **Total à la charge du bailleur** | **32,25 €** |
| Versement net | 767,75 € |

Ce détail est maintenant affiché au bailleur dans son profil (réglages de paiement) et sur chaque
échéance non réglée qui peut être payée par carte.

## Le point à trancher : qui supporte 1,5 % + 0,25 € ?

Trois options :

1. **Bailleur (situation actuelle).** Simple, conforme, mais 4 % de frais cumulés sur un loyer
   rendent la carte peu attractive face au virement gratuit : le paiement en ligne restera marginal.
2. **Locataire (surcoût affiché au paiement).** Juridiquement encadré : en France, la
   *surcharge* d'un paiement par carte est **interdite** pour les cartes de particuliers depuis
   la directive DSP2 (art. L112-12 du Code monétaire et financier). Ajouter « + 12,25 € de frais
   de paiement » au locataire n'est donc pas possible tel quel. On peut en revanche facturer un
   **service** distinct (ex. « paiement en ligne avec quittance instantanée ») — à valider
   juridiquement, et le locataire choisira le virement.
3. **Intégré à l'abonnement (recommandé).** La commission variable disparaît pour les plans
   payants : le bailleur PRO/PREMIUM paie un abonnement qui couvre la plateforme, et ne supporte
   que les frais Stripe réels (1,5 % + 0,25 €) affichés clairement. Le plan gratuit garde une
   commission (2,5 % ou plus) qui incite à passer au plan payant.

## Recommandation

- Garder le **direct charge** (pas de fonds sur le compte plateforme, pas d'agrément de
  services de paiement à demander).
- **Aucun frais côté locataire** (contrainte légale + adoption).
- **Commission dégressive par plan** : FREE 2,5 %, PRO 1 %, PREMIUM 0 % — le mécanisme existe déjà
  (`platform_fee_percent` par plan), seules les valeurs sont à renseigner.
- **Afficher les frais avant activation** (fait) et **dans l'export comptable** : les frais Stripe
  sont des charges déductibles (LMNP au réel), les faire remonter dans la comptabilité est un
  vrai argument produit — à prévoir dans le module comptable (relevé des `balance_transactions`
  Stripe).
- **Prélèvement SEPA** : Stripe le propose (0,35 € par prélèvement, plafonné) ; pour des loyers,
  c'est le mode le plus adapté (récurrent, quasi gratuit). À activer sur le compte Connect quand
  les clés de test seront disponibles — c'est probablement ce qui fera décoller le paiement en
  ligne, bien plus que la carte.

## Reste à faire côté technique

- Clés Stripe de test (`STRIPE_SECRET_KEY`, `STRIPE_WEBHOOK_SECRET`) dans `.env` pour vérifier
  checkout, webhook et versement sur un compte Connect Express de test.
- Récupérer les frais réels par paiement (`BalanceTransaction.fee`) après paiement pour remplacer
  l'estimation affichée par le montant exact.
