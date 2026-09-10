# Audit de conformité — Modèles de bail

Relecture de conformité des 4 modèles de bail générés par la plateforme (changelog 106/107),
au regard des obligations documentées du droit locatif français. Complète la note de travail
[`GUIDE-BAUX.md`](GUIDE-BAUX.md) avec une hiérarchisation par gravité et une checklist juriste.

**⚠️ Ceci n'est pas un avis juridique.** Analyse technique produite pour *préparer* la relecture
par un professionnel (avocat / notaire / ADIL), indispensable avant mise en production réelle.
Version consultable (mise en forme) : artifact `d9287488-9a3b-48f5-a5f2-1cf05543eadb`.

Établi le 10/09/2026 · Réf. : loi 89-462, décrets 2015-587 & 2015-981, loi ELAN, loi Climat.

## Verdict d'ensemble

Le **texte** des 4 modèles est substantiellement complet et bien structuré : les clauses de
fond obligatoires (durée & reconduction, loyer, révision IRL, charges, dépôt, solidarité art. 8-1,
clause résolutoire art. 24, obligations art. 6-7, congés) sont présentes et correctement
formulées. Les défauts se concentrent sur **deux axes** : des **annexes obligatoires non produites**
par la plateforme, et des **mentions structurantes en texte libre** (`[à compléter]`) alors
qu'elles engagent la validité du bail.

Bilan : **2 bloquants · 4 élevés · 4 moyens** — clauses de fond conformes.

## Constats transversaux

| ID | Constat | Gravité | Concerne |
|---|---|---|---|
| T-01 | **Notice d'information** (arrêté 29/05/2015) listée mais **jamais jointe** au PDF — annexe obligatoire | 🔴 Bloquant | bail nu, meublé |
| T-02 | **Inventaire du mobilier** du meublé (décret 2015-981) **non généré** → risque de requalification en location nue | 🔴 Bloquant | meublé, mobilité |
| T-03 | **Diagnostics (DDT)** non contrôlés à l'envoi ; le DPE est opposable (loi Climat) | 🟠 Élevé | tous baux habitation |
| T-04 | **Encadrement des loyers** en texte libre — dépend d'arrêtés préfectoraux annuels par commune | 🟠 Élevé | nu, meublé, mobilité |
| T-05 | **Contraintes loi Climat** non remontées : gel loyer F/G, interdiction location G (2025), F (2028), E (2034) | 🟠 Élevé | baux habitation + révision |
| T-06 | **Mentions structurantes en texte libre** (copropriété, période de construction, régime des charges…) → risque d'oubli | 🟡 Moyen | tous |
| T-07 | **Signature électronique** « simple » (eIDAS) à qualifier — force probante / dossier de preuve | 🟡 Moyen | flux signature |
| T-08 | **Grille de vétusté** (décret 2016-382) et **mandataire/agence** non modélisés | 🟡 Moyen | nu, meublé, EDL |
| T-09 | **Colocation à baux multiples** et **bail étudiant 9 mois** à valider | 🟡 Moyen | — |

## Couverture par modèle

- **Bail nu (résidence principale)** — clauses de fond complètes (durée 3/6 ans, art. 11, IRL,
  charges, dépôt 1 mois, solidarité, art. 24, congés 3/6 mois). ▲ encadrement/copro/construction
  en `[à compléter]`. ✕ notice non jointe (T-01), diagnostics non contrôlés (T-03). La mention
  « Lu et approuvé » est sans valeur et inutile (bénin).
- **Bail meublé (1 an / 9 mois étudiant)** — liste mobilier réglementaire citée, durée & congés
  meublé, dépôt 2 mois : conformes. ✕ inventaire non généré (T-02), notice non jointe (T-01).
  ▲ bail étudiant 9 mois par simple mention (T-09).
- **Bail mobilité (ELAN)** — motif justificatif, 1-10 mois non renouvelable, bascule en meublé
  au-delà, sans dépôt, pas de solidarité, Visale : conformes. ✕ inventaire non généré (T-02).
- **Parking / garage / box** — le plus complet et le moins risqué (Code civil art. 1713 s.).
  ▲ vérifier la base de révision retenue.

## Checklist à remettre au juriste

1. **Notice d'information** : version en vigueur à joindre systématiquement (nu + meublé).
2. **Inventaire meublé** : valider une trame conforme au décret 2015-981.
3. **Encadrement des loyers** : formulation de la clause + périmètre communes + source des loyers de référence.
4. **Loi Climat** : règles à bloquer/alerter selon l'étiquette DPE et leur formulation.
5. **Diagnostics** : lesquels rendre obligatoires avant envoi ; blocage vs avertissement.
6. **Signature électronique** : signature « simple » suffisante pour un bail, ou viser « avancée » ?
7. **Bail étudiant 9 mois** : absence de reconduction + articulation avec la date de fin calculée.
8. **Colocation à baux multiples** : à couvrir ? Si oui, trame dédiée.
9. **Grille de vétusté** : valider une grille (décret 2016-382) pour les retenues sur dépôt.
10. **Mentions locales** : permis de louer / autorisation préalable de mise en location.

## Suites techniques

| Gap | Action produit | Effort | État |
|---|---|---|---|
| T-01 Notice | Notice d'information annexée au PDF du contrat (`LegalNoticeService` + `legal/notice-information.html`) | Faible | ✅ **Fait** — version officielle à valider (checklist #1) |
| T-03 Diagnostics | Blocage d'envoi si DPE absent/périmé (`DpeComplianceService`) | Faible | ✅ **Fait** |
| T-05 Loi Climat | Blocage envoi si logement indécent (G) + gel de la révision F/G, selon étiquette DPE | Faible | ✅ **Fait** |
| T-02 Inventaire | Inventaire du mobilier intégré à l'EDL, pré-rempli avec la liste du décret 2015-981, signé par les 2 parties (changelog 109) | Moyen | ✅ **Fait** |
| T-06 Mentions libres | Formulaire de contrat → variables au lieu des crochets | Moyen | À faire |
| T-04 Encadrement | Case « zone encadrée » + champs + données loyers de référence | Élevé | À faire (validation juriste) |

**Implémenté (10/09/2026)** — T-01, T-03 et T-05, sans arbitrage juridique de fond : la notice
voyage désormais avec le contrat (PDF envoyé et PDF signé archivé), et un bail ne peut plus partir
en signature sans DPE valide ni pour un logement classé G ; la révision de loyer est bloquée pour
les passoires F/G. Le calendrier d'interdiction (G→2025, F→2028, E→2034) est appliqué selon l'année.
Reste à valider par le juriste : la **version officielle exacte** de la notice (checklist #1).

**Note** : T-01, T-03 et T-05 ne nécessitent aucun arbitrage juridique de fond (hors version
exacte de la notice) et peuvent être implémentés sans risque de contredire le juriste.
