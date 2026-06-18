# RGPD — état actuel + ce qui reste avant un audit CNIL

## ✅ Ce qui marche

Vérifié manuellement contre `owner1@howners.test` :

| Endpoint | Statut | Contenu |
|---|---|---|
| `GET /api/rgpd/export` | 200 — JSON | personalInfo, properties[], rentals[], contracts[], payments[], messages[], etc. |
| `GET /api/rgpd/export/pdf` | 200 — PDF 1.8 KB | Rendu propre via iText |
| `POST /api/rgpd/erasure` | ✅ effectif | Pseudonymise le compte + **supprime les pièces PII sur S3** (+ efface les métadonnées de la ligne) + **conserve les documents à obligation légale** sous legal hold + **anonymise les messages émis** + trace la demande (`rgpd_requests`) |
| `GET /api/rgpd/consent` | 200 — JSON | Liste `user_consents` enregistrés |
| `POST /api/rgpd/consent` | 200 | Enregistre un nouveau consent |

Implémentations à `RgpdController.java` + service dédié, tracé dans `audit_logs` via `AuditAspect`.

## ⚠️ Trous à combler avant audit

### 1. Documents (S3 / MinIO) — ✅ FAIT
Endpoint `GET /api/rgpd/export/archive` (`RgpdService.exportUserDataAsArchive`) : génère un
ZIP contenant `export.json` (données structurées, dates ISO-8601), `documents/` (tous les
fichiers téléversés, récupérés depuis S3) et une notice `LISEZMOI.txt`. Best-effort par
fichier (un fichier indisponible est ignoré sans faire échouer l'archive). Bouton
« Tout télécharger (archive ZIP) » sur `/profile/rgpd`. Droit à la portabilité (art. 20) couvert.

### 2. Effacement effectif — ✅ FAIT
`POST /api/rgpd/erasure` (`RgpdService.anonymizeUser`) :
- **Supprime les pièces personnelles (PII) sur S3** (`storageService.deleteFile`) et efface les
  métadonnées de la ligne `documents` (le nom de fichier peut contenir du PII).
- **Conserve** les documents à obligation légale sous `legal_hold` (exception art. 17-3) :
  baux, factures, quittances, états des lieux, mises en demeure, signatures
  (`DocumentRetentionPolicy`, testée).
- **Anonymise les messages émis** par l'utilisateur (corps → « [Supprimé à la demande de
  l'utilisateur] »). Les messages reçus (rédigés par des tiers) ne sont pas touchés.
- Les noms dénormalisés (`payer_name`, `tenant_name`…) sont **calculés** via `getFullName()`
  donc déjà couverts par la pseudonymisation du compte.

> Reste possible : un véritable traitement **asynchrone** (job) si le volume de documents
> par utilisateur devient important. Aujourd'hui l'effacement est synchrone et transactionnel.

### 3. Délais légaux — ✅ FAIT
Table `rgpd_requests` (migration 077) : `type` (EXPORT/ERASURE), `status`
(RECEIVED→COMPLETED), `requested_at`, `completed_at`, `details`. Chaque export et chaque
effacement y est tracé. `RgpdRequestRepository.findByStatusAndRequestedAtBefore` permet de
lister les demandes dépassant le délai légal d'1 mois (à brancher sur une alerte/supervision).

### 4. Consent UI côté frontend — ✅ FAIT
L'écran `/profile/rgpd` (`features/profile/rgpd-settings`) existe et est accessible via le
menu utilisateur de la topbar (« Confidentialité ») :
- Affiche et permet de basculer chaque consentement (traitement, marketing, analytics, tiers)
- Boutons d'export (archive ZIP, JSON, PDF)
- Zone de danger : effacement du compte avec double confirmation
- Montre les consents donnés (marketing, partage avec partenaires, cookies non essentiels)
- Permet de retirer un consent
- Permet de demander l'export ou l'effacement avec UI claire et bouton de confirmation

L'endpoint backend existe mais le frontend ne le surface pas.

### 5. Registre des traitements (art. 30)
Obligation légale si > 250 employés OU traitement à grande échelle. Probablement applicable dès que Howners a > 1000 utilisateurs. Aujourd'hui : aucun.

À documenter (Markdown ou Notion) :
- Quelles données sont collectées
- Pourquoi (base légale art. 6)
- Combien de temps elles sont conservées
- Avec qui elles sont partagées (Stripe, DocuSign, MailHog/SendGrid, MinIO/S3)

### 6. DPO ou personne référente
Si > 250 employés ou activité à grande échelle, DPO obligatoire. Sinon, désigner une personne avec un email `dpo@howners.fr` qui répond.

### 7. Cookie banner conforme
Pas vérifié, mais la landing n'a pas de cookie banner visible. Si Howners pose des cookies (analytics, marketing), il faut un banner conforme CNIL (refus = clic 1, pas dark pattern).

## Audit minimal avant client réel

À cocher :
- [ ] `RgpdController` endpoints UI-accessible depuis `/profile`
- [ ] Document de **politique de confidentialité** lié depuis la landing
- [ ] **Email DPO** opérationnel
- [ ] **Registre des traitements** (peut être un Markdown interne au démarrage)
- [ ] Notice **base légale par traitement**
- [ ] **Cookie banner** si tracking
- [ ] **DPA** (Data Processing Agreement) avec sous-traitants : Stripe, DocuSign, AWS/Scaleway pour S3, fournisseur SMTP
- [ ] **Conservation** documents > 10 ans → process d'archivage

## Sources externes recommandées

- CNIL → [Guide du sous-traitant](https://www.cnil.fr/fr/guide-du-sous-traitant)
- CNIL → [Modèle de registre](https://www.cnil.fr/fr/RGPD-le-registre-des-activites-de-traitement)
- iubenda ou Termly pour générer la politique de confidentialité (~10 €/mois)
