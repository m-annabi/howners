# RGPD — état actuel + ce qui reste avant un audit CNIL

## ✅ Ce qui marche

Vérifié manuellement contre `owner1@howners.test` :

| Endpoint | Statut | Contenu |
|---|---|---|
| `GET /api/rgpd/export` | 200 — JSON | personalInfo, properties[], rentals[], contracts[], payments[], messages[], etc. |
| `GET /api/rgpd/export/pdf` | 200 — PDF 1.8 KB | Rendu propre via iText |
| `POST /api/rgpd/erasure` | défini | Anonymise `users.anonymized_at` + flag `is_anonymized` |
| `GET /api/rgpd/consent` | 200 — JSON | Liste `user_consents` enregistrés |
| `POST /api/rgpd/consent` | 200 | Enregistre un nouveau consent |

Implémentations à `RgpdController.java` + service dédié, tracé dans `audit_logs` via `AuditAspect`.

## ⚠️ Trous à combler avant audit

### 1. Documents (S3 / MinIO)
L'export JSON ne télécharge **pas** les fichiers binaires stockés sur S3 (`documents.file_key`). Pour être pleinement portable (droit à la portabilité art. 20), il faudrait soit :
- Inclure les URLs présignées dans le JSON (valable 7 jours, lisible par l'utilisateur).
- Bundler les fichiers dans un ZIP qui contient JSON + PDFs + photos.

**Recommandation** : générer un ZIP `export-{userId}-{date}.zip` à la demande, avec JSON + tous les fichiers. Endpoint `/api/rgpd/export/archive` à ajouter.

### 2. Effacement effectif
`POST /api/rgpd/erasure` met `is_anonymized=true` mais **ne supprime pas** :
- Les documents sur S3 (PII visibles : cartes d'identité, justificatifs de revenus…)
- Les messages échangés
- Les contrats signés (à garder pour valeur probante 5 ans après fin de bail, mais doivent être anonymisés)

**Recommandation** : après `erasure`, lancer un job async qui :
- Supprime les documents S3 sauf ceux à conservation légale obligatoire (loi 89 = baux 5 ans, factures 10 ans)
- Remplace les contenus de messages par "[Supprimé à la demande de l'utilisateur]"
- Anonymise tous les `payer_name`, `tenant_name`, etc. dénormalisés

### 3. Délais légaux
Le RGPD demande de répondre sous **1 mois** (extensible à 3 si justifié). Aucun tracking d'une "demande RGPD reçue le ___". Solution : table `rgpd_requests` avec `requested_at`, `completed_at`, `type` (EXPORT, ERASURE).

### 4. Consent UI manquant côté frontend
Il n'y a pas d'écran `/profile/rgpd` qui :
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
