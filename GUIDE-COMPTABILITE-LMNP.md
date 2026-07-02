# Guide — Module comptabilité LMNP (location meublée au réel)

Ce guide explique, pas à pas, comment produire votre comptabilité et vos documents
fiscaux de **loueur en meublé non professionnel (LMNP) au régime réel** depuis Howners :
compte de résultat, bilan, tableau des amortissements, détermination du résultat fiscal
et **FEC**, réunis dans une « liasse » téléchargeable.

> ⚠️ **Document d'aide, pas un conseil comptable.** Howners calcule et met en forme vos
> chiffres à partir de vos données ; il ne remplace ni la liasse fiscale officielle
> (formulaires 2031 et 2033) ni l'avis d'un expert-comptable. En cas de doute, faites
> relire vos déclarations.

---

## Sommaire

1. [À qui s'adresse ce module](#1-à-qui-sadresse-ce-module)
2. [Prérequis](#2-prérequis)
3. [Configurer votre activité](#3-configurer-votre-activité)
4. [Les immobilisations (ce que vous amortissez)](#4-les-immobilisations-ce-que-vous-amortissez)
5. [Les emprunts](#5-les-emprunts)
6. [Lire votre résultat fiscal](#6-lire-votre-résultat-fiscal)
7. [Lire votre bilan](#7-lire-votre-bilan)
8. [Télécharger et déposer la liasse](#8-télécharger-et-déposer-la-liasse)
9. [Points d'attention](#9-points-dattention)
10. [Ce que le module ne fait pas (encore)](#10-ce-que-le-module-ne-fait-pas-encore)
11. [FAQ](#11-faq)

---

## 1. À qui s'adresse ce module

Il s'adresse au bailleur qui loue **en meublé** et a opté (ou est soumis) au **régime
réel** des BIC (bénéfices industriels et commerciaux). C'est le régime qui permet de
**déduire les charges réelles et d'amortir** le bien, le mobilier et les travaux — et
donc, très souvent, de réduire fortement voire d'annuler l'impôt sur les loyers.

Il **ne concerne pas** :

- la location **nue** (revenus fonciers) → utilisez plutôt l'aide à la **déclaration 2044** ;
- le régime **micro-BIC** (abattement forfaitaire de 50 %), qui ne nécessite aucune comptabilité.

---

## 2. Prérequis

| Prérequis | Détail |
|---|---|
| **Plan PRO ou supérieur** | Le module est réservé aux abonnements PRO, PREMIUM et AGENCE. |
| **Au moins un bien meublé** | Le bien doit être marqué « meublé » dans sa fiche (`Meublé = oui`). |
| **Vos loyers saisis/encaissés** | Le calcul se fait en **base d'encaissement** : seuls les loyers et provisions **effectivement encaissés** dans l'année comptent. |
| **Vos dépenses catégorisées** | Assurances, taxe foncière, entretien, copropriété… saisies avec leur catégorie et rattachées au bon bien. |

Accès : menu latéral **Finances → Comptabilité**.

---

## 3. Configurer votre activité

Au premier accès, renseignez le **bilan d'ouverture** de votre activité. Une seule
activité par compte, elle regroupe **tous vos biens meublés**.

| Champ | À renseigner |
|---|---|
| **Début d'activité** | Date de mise en location meublée (démarrage de l'activité LMNP). |
| **Trésorerie d'ouverture** | Solde de trésorerie dédié à l'activité au démarrage (apport de départ). Sert à équilibrer le premier bilan. |
| **SIRET** | Numéro à 14 chiffres de votre activité de loueur en meublé. **Obligatoire sur la déclaration 2031** et il donne son nom réglementaire au fichier FEC. |

> Vous n'avez **pas** de SIRET ? Il s'obtient en déclarant le début d'activité de loueur
> en meublé auprès de l'INPI (guichet unique des formalités des entreprises). Vous pouvez
> configurer l'activité sans, mais la liasse le signalera comme manquant.

Le **capital de l'exploitant** (ce que vous avez apporté) n'est **pas** saisi à la main :
Howners le calcule automatiquement à partir de votre trésorerie d'ouverture, de vos
immobilisations et de vos emprunts, pour que le bilan soit toujours équilibré.

---

## 4. Les immobilisations (ce que vous amortissez)

Une **immobilisation** est un bien durable dont le coût se déduit **étalé dans le temps**
(l'amortissement), et non en une seule fois. Howners pratique un **amortissement linéaire**
(part égale chaque année), avec la 1re année **au prorata** de la date de mise en service.

### Les quatre natures et leurs durées par défaut

| Nature | Durée par défaut | Exemples |
|---|---|---|
| **Immeuble (bâti)** | 30 ans | Valeur de la construction (**hors terrain**, non amortissable) |
| **Mobilier et équipements** | 7 ans | Lit, canapé, électroménager, cuisine équipée |
| **Travaux et agencements** | 12 ans | Rénovation, aménagements |
| **Frais d'acquisition** | 5 ans | Frais de notaire, droits d'enregistrement |

La durée par défaut est modifiable à la saisie si votre situation le justifie.

### Deux façons d'ajouter une immobilisation

**a) Import automatique (recommandé).** Howners détecte des immobilisations à partir de
vos données déjà saisies et les propose dans « Immobilisations détectées » :

- dépenses **Mobilier** → Mobilier et équipements ;
- dépenses **Rénovation** → Travaux et agencements ;
- **prix d'achat − valeur du terrain** de vos biens → Immeuble (bâti) ;
- **frais de notaire** de vos biens → Frais d'acquisition.

Vérifiez les montants, puis cliquez sur **Importer** (à l'unité ou « Tout importer »).
Une même source n'est jamais importée deux fois.

**b) Saisie manuelle.** Renseignez la nature, un libellé, la **base** (montant à amortir),
la **date de mise en service** et, si besoin, la durée.

> 💡 **Terrain.** Le terrain ne s'amortit jamais. Renseignez la **valeur du terrain** dans
> la fiche du bien : Howners l'exclut automatiquement de la base amortissable du bâti.

> 💡 **Immobilisation antérieure au début d'activité.** Si un bien ou du mobilier a été
> acquis **avant** votre début d'activité, l'amortissement démarre au **début d'activité**
> (mise en location), pas à la date d'achat.

---

## 5. Les emprunts

Modélisez votre **prêt d'acquisition** (ou de travaux) pour déduire correctement son coût.
En LMNP réel :

- les **intérêts d'emprunt** et l'**assurance emprunteur** sont des **charges déductibles** ;
- le **remboursement du capital** n'est **pas** une charge (il diminue votre dette au bilan).

### Ajouter un emprunt

| Champ | Détail |
|---|---|
| **Libellé** | Ex. « Prêt acquisition appartement Lyon ». |
| **Capital** | Montant emprunté. |
| **Taux annuel (%)** | Taux nominal (hors assurance). |
| **Durée (mois)** | Ex. 240 pour 20 ans. |
| **Date de déblocage** | Date de mise à disposition des fonds. |
| **Assurance / mois** | Prime d'assurance emprunteur mensuelle (facultatif). |

Howners construit un **tableau d'amortissement à échéance constante** et ventile, année par
année, intérêts / capital / assurance. Dépliez la ligne d'un emprunt (icône chevron) pour
voir l'**échéancier annuel** : intérêts (déductibles), assurance (déductible), capital
remboursé et **capital restant dû**.

> ⚠️ **Ne saisissez pas deux fois vos intérêts.** Si vous modélisez l'emprunt ici, ne
> saisissez **pas** en plus les intérêts ou l'assurance en tant que dépenses : ils seraient
> déduits deux fois. Howners vous le rappelle dans les « Points d'attention ».

---

## 6. Lire votre résultat fiscal

Choisissez l'**exercice** (année civile) en haut de l'écran. La carte « Résultat fiscal »
enchaîne le calcul :

1. **Recettes locatives** — loyers **et** provisions pour charges **encaissés** dans l'année,
   pour vos biens meublés uniquement.
2. **− Charges déductibles** — assurances, taxe foncière, entretien, gestion, copropriété,
   énergie…, **plus** intérêts et assurance d'emprunt.
3. **= Résultat avant amortissements**.
4. **− Amortissements déductibles** — dotations de l'année, dans la limite de la règle
   ci-dessous.
5. **− Déficits antérieurs imputés** — le cas échéant (voir plus bas).
6. **= Résultat fiscal (base imposable BIC)** — le montant à reporter sur votre déclaration.

### La règle clé : l'amortissement ne peut pas créer de déficit

En LMNP, l'amortissement n'est déductible **qu'à hauteur du bénéfice avant amortissement**.
La fraction non déduite devient un **amortissement différé**, **reporté sans limite de durée**
(art. 39 C du CGI) et déductible les années suivantes. Résultat : l'amortissement peut
ramener votre résultat imposable à **zéro**, jamais en dessous.

- **Amortissements différés reportés** : le stock d'amortissements non encore déduits.

### Les déficits (issus des charges, hors amortissements)

Si vos **charges** (hors amortissements) dépassent vos recettes, vous créez un **déficit
BIC**. Ce déficit est **reportable 10 ans** et s'impute automatiquement sur vos bénéfices
des années suivantes (au plus ancien d'abord). L'écran affiche :

- **Déficits antérieurs imputés** : ce qui a été déduit du bénéfice de l'exercice ;
- **Déficits BIC reportables** : le solde encore disponible pour l'avenir.

---

## 7. Lire votre bilan

Le **bilan** photographie votre patrimoine au 31/12. Il est toujours **équilibré**
(un badge « Bilan équilibré » le confirme).

| ACTIF (ce que vous possédez) | PASSIF (comment c'est financé) |
|---|---|
| Immobilisations en **valeur nette** (base − amortissements cumulés) | **Capital de l'exploitant** (vos apports) |
| **Trésorerie** | **Report à nouveau** (résultats des exercices passés) |
| | **Résultat de l'exercice** |
| | **Emprunts** (capital restant dû) |

> Un bilan **déséquilibré** signale une incohérence dans les données d'ouverture
> (trésorerie initiale) ou dans les dates d'immobilisations/emprunts : vérifiez ces
> saisies. En usage normal, il reste équilibré par construction.

---

## 8. Télécharger et déposer la liasse

Bouton **« Télécharger la liasse (PDF + FEC) »**. Vous obtenez un fichier **ZIP** contenant :

### a) Le PDF de la liasse

Un document lisible reprenant :

- **Compte de résultat** (façon **2033-B**) ;
- **Détermination du résultat fiscal** (règle de non-déficit, déficits) ;
- **Tableau des amortissements** (façon **2033-C**) : base, dotation, cumul, valeur nette ;
- **Bilan simplifié** (façon **2033-A**) ;
- une section **« Où reporter ces montants »** qui indique, pour chaque total, la case
  correspondante des formulaires **2031 / 2033** et le report sur la **2042-C-PRO**
  (bénéfice **case 5NA**, déficit **case 5NY**, selon le déclarant).

### b) Le fichier FEC

Le **Fichier des Écritures Comptables** au format normalisé DGFiP (18 colonnes,
séparateur `|`), nommé réglementairement **`SIRENFEC20241231.txt`** lorsque le SIRET est
renseigné. Total débit = total crédit par construction. C'est le fichier à fournir en cas
de contrôle et le format attendu par la plupart des logiciels comptables.

### Comment déposer

1. **Reportez** les montants indiqués par la section « Où reporter » sur votre déclaration
   de résultat **2031** et ses annexes **2033**, puis le résultat sur la **2042-C-PRO** ;
2. ou **transmettez la liasse à votre comptable**, qui la télétransmet (EDI-TDFC).

> Les numéros de case (5NA, 5NY…) valent pour le premier déclarant : vérifiez-les sur le
> **millésime** de votre formulaire.

---

## 9. Points d'attention

L'écran affiche un encadré **« Points d'attention »** selon votre situation :

- **Biens non classés meublé / nu.** Si le caractère meublé d'un bien n'est pas renseigné,
  il est **inclus** dans le calcul BIC — classez chaque bien (`Meublé = oui/non`) pour
  éviter tout **double emploi** avec la déclaration 2044 (location nue).
- **Seuil LMP (23 000 €).** Si vos recettes dépassent **23 000 €** et excèdent aussi vos
  autres revenus d'activité, vous pouvez basculer en **loueur professionnel (LMP)** —
  cotisations sociales et régime des plus-values différents. Rapprochez-vous d'un conseil.
- **Double déduction des intérêts** (voir §5).
- **CFE.** La cotisation foncière des entreprises reste due (sauf exonération la 1re année
  et cas de l'art. 1459 CGI). Elle est appelée directement par le fisc et n'apparaît dans
  la liasse que si vous l'avez saisie en dépense.

---

## 10. Ce que le module ne fait pas (encore)

- **Pas de télétransmission EDI-TDFC** : la liasse se dépose manuellement ou via votre
  comptable.
- **Amortissement linéaire par catégorie**, pas par composants (décomposition du bâti en
  toiture, façade… avec durées distinctes).
- **Pas de gestion de la TVA** (la location meublée d'habitation en est exonérée).
- **Pas de calcul des plus-values** de cession.
- **Pas de conseil fiscal** : les cases proposées sont une aide au report, pas une garantie
  d'exhaustivité de votre situation.

---

## 11. FAQ

**Le module a-t-il besoin d'un comptable ?**
Non pour produire les documents. Mais une **relecture** par un expert-comptable est
recommandée la première année, surtout pour valider vos amortissements.

**Pourquoi mon résultat fiscal est-il à zéro alors que j'ai des loyers ?**
C'est l'effet recherché du réel : les charges + amortissements couvrent les loyers.
L'amortissement non utilisé est **reporté** (il n'est pas perdu).

**J'ai un bien meublé ET un bien nu, que faire ?**
Le module ne prend que les **meublés** ; le bien **nu** relève de l'aide **2044**
(location nue). Assurez-vous que chaque bien est correctement marqué.

**Mes provisions pour charges comptent-elles dans les recettes ?**
Oui. En BIC, les loyers **charges comprises** sont imposables : loyers **et** provisions
pour charges encaissés entrent dans les recettes.

**Puis-je changer la durée d'amortissement d'un bien ?**
Oui, à la saisie de l'immobilisation. Les durées par défaut (§4) suivent les usages LMNP.

**Que se passe-t-il si je supprime un emprunt ou une immobilisation ?**
Le résultat et le bilan sont **recalculés** immédiatement pour l'exercice affiché.

---

> **Rappel.** Ce module est une aide à la tenue de votre comptabilité LMNP et à la
> préparation de vos déclarations. Il n'engage pas Howners sur l'exactitude fiscale de
> votre situation particulière. Pour toute décision à enjeu, consultez un professionnel.
