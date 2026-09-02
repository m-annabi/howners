# Templates de bail — couverture juridique et comparaison avec les modèles PAP

Note de travail (septembre 2026). Aucune compétence juridique automatisée n'était disponible dans
l'environnement de développement : la revue ci-dessous a été faite manuellement, à partir des
textes (loi n° 89-462 du 6 juillet 1989, décret n° 2015-587 « contrat type », décret n° 2015-981
« mobilier du meublé », loi ELAN pour le bail mobilité) et de la structure des modèles PAP.
**Elle ne remplace pas la relecture par un juriste** (item #4 de `NEXT-STEPS.md`), notamment pour
l'encadrement des loyers (zones et décrets annuels) et les mentions locales.

## Ce que fournit la plateforme (changelog 106)

| Template | Régime | Par défaut | Modifiable |
|---|---|---|---|
| Bail d'habitation nu — résidence principale (contrat type) | Loi 89, titre Ier bis, décret 2015-587 | oui (LONG_TERM) | dupliquer puis adapter |
| Bail meublé — résidence principale (1 an / 9 mois étudiant) | Loi 89, titre Ier bis, décret 2015-981 | non | dupliquer puis adapter |
| Bail mobilité — meublé, 1 à 10 mois | Loi 89, art. 25-12 à 25-18 (ELAN) | non | dupliquer puis adapter |
| Contrat de location de parking / garage / box | Code civil (art. 1713 s.) | non | dupliquer puis adapter |
| Bail meublé — courte durée (vacances, mobilité) | Location saisonnière (Code civil) | oui (SHORT_TERM) | dupliquer puis adapter |

Les modèles « Howners » (sans auteur) sont visibles par tous les bailleurs, en lecture seule ; le
bouton **Dupliquer** crée une copie personnelle éditable. Les crochets `[à compléter]` signalent
les mentions que le bailleur doit renseigner avant envoi (elles ne sont pas déductibles des données
du bien : régime de copropriété, période de construction, zone d'encadrement, garanties…).

Variables disponibles dans l'éditeur (bouton « Variables ») : identité des parties, adresse et
consistance du bien, dates, `rental.monthlyRent`, `rental.charges`, `rental.totalMonthly`,
`rental.paymentDay`, `rental.depositAmount`, `rental.endDateClause`, `today`.

## Comparaison avec les modèles PAP (bail nu / meublé)

| Rubrique PAP | Couvert | Remarque |
|---|---|---|
| Désignation des parties (bailleur, mandataire, locataire) | ✅ | Le mandataire (agence) n'est pas modélisé : mention libre. |
| Consistance du logement (type, surface, pièces, annexes, équipements, TIC) | ✅ | Régime juridique et période de construction à compléter à la main. |
| Destination (résidence principale) | ✅ | |
| Prise d'effet, durée, reconduction, durée réduite art. 11 | ✅ | |
| Loyer, encadrement (loyer de référence, complément), loyer du précédent locataire | ✅ | Zone d'encadrement non déduite automatiquement de l'adresse : à cocher. |
| Révision IRL (trimestre de référence) | ✅ | La révision annuelle est automatisée par ailleurs (module révision de loyer). |
| Charges (provisions + régularisation ou forfait) | ✅ | Régularisation calculée puis ajustable avec justificatif (lot C). |
| Modalités de paiement, quittance gratuite | ✅ | |
| Dépôt de garantie (1 mois nu / 2 mois meublé), délais de restitution | ✅ | |
| Travaux réalisés / autorisés | ✅ | à compléter |
| Garanties (caution, Visale, GLI) | ✅ | acte de cautionnement à annexer manuellement |
| Clause de solidarité et fin de solidarité (art. 8-1) | ✅ | |
| Clause résolutoire (art. 24) | ✅ | |
| Obligations des parties (art. 6 et 7) | ✅ | |
| Congé (préavis locataire 3 mois / 1 mois ; bailleur 6 mois / 3 mois meublé) | ✅ | |
| Honoraires de location (plafonds) | ✅ | à compléter si intermédiaire |
| Notice d'information (arrêté du 29 mai 2015) | ⚠️ | Listée en annexe, **le document lui-même n'est pas généré** : à joindre (PDF officiel) — à ajouter dans les documents du bail. |
| Dossier de diagnostic technique (DPE, ERP, plomb, électricité, gaz, bruit, amiante) | ⚠️ | Listé en annexe ; les diagnostics sont à déposer dans les documents du bien. Le DPE du bien est déjà saisi (étiquette). |
| État des lieux d'entrée / sortie | ✅ | Généré par le module états des lieux et rattaché au bail. |
| Inventaire du mobilier (meublé) | ⚠️ | Mentionné ; pas de générateur d'inventaire dédié (à faire dans l'EDL ou en pièce jointe). |
| Extraits du règlement de copropriété | ⚠️ | À déposer dans les documents du bien. |
| Autorisation préalable de mise en location, permis de louer | ⚠️ | Mention seulement. |

## Points à faire valider par un juriste

1. Formulation de la clause d'encadrement des loyers et du complément de loyer (zones tendues :
   Paris, Lille, Lyon, Bordeaux, Montpellier…), qui dépend d'arrêtés préfectoraux annuels.
2. Bail mobilité : justificatifs recevables et mention de la garantie Visale.
3. Colocation à baux multiples (chambre + parties communes) — non couverte.
4. Bail meublé étudiant de 9 mois : absence de reconduction et articulation avec la date de fin
   calculée par la plateforme.
5. Notice d'information : version en vigueur à joindre systématiquement (évolutions 2023-2025 sur
   le DPE et la décence énergétique — interdiction de louer les logements G depuis 2025, F en 2028).

## Suites techniques suggérées

- Joindre automatiquement la notice d'information officielle (PDF) à l'envoi du contrat.
- Cases à cocher dans le formulaire de contrat pour les mentions structurantes (zone
  d'encadrement, copropriété, période de construction, régime des charges) afin de remplacer les
  crochets par des variables.
- Générateur d'inventaire du mobilier pour les meublés (liste du décret 2015-981 pré-remplie).
