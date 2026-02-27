# CLAUDE.md - Regles de traitement pour ce projet

## Regles generales

### Verification obligatoire avant push
- **Toujours lancer le build backend** (`mvn compile` ou `mvn verify`) avant de committer des changements Java. Ne jamais supposer que le code compile sans verification.
- **Toujours lancer le build frontend** (`ng build`) et verifier qu'il reussit completement. Un build partiel (TypeScript OK mais index generation failed) n'est PAS un succes.
- **Toujours lancer les tests** (`mvn test`, `ng test`) avant de pousser. Ne jamais pousser du code non teste.

### Modifications de fichiers
- **Preferer `Edit` a `Write`** pour les fichiers existants. Utiliser `Write` uniquement pour les nouveaux fichiers ou les reecritures completes explicitement demandees. Les edits ciblees reduisent le risque d'alterations involontaires.
- **Lire le fichier complet avant toute modification**. Ne jamais modifier un fichier qu'on n'a lu que partiellement.
- **Verifier les references avant suppression** de tout fichier. Chercher les imports, les references dans les configs (angular.json, pom.xml, etc.) avant de supprimer.

### Conventions du projet
- **Verifier les conventions existantes** avant de creer de nouvelles classes/patterns. Regarder comment les utilitaires, DTOs, exceptions sont organises dans le projet avant d'en creer de nouveaux.
- **Verifier la sequence des migrations Liquibase** avant d'ajouter un changeset. S'assurer que l'ID est unique et que le fichier est correctement reference dans le changelog principal.

### Communication honnete
- **Ne jamais declarer "build OK" ou "tests OK" sans les avoir reellement executes avec succes**. Si un outil n'est pas disponible (ex: Maven dans un sandbox), le dire explicitement au lieu de contourner.
- **Distinguer "verifie" de "suppose"**. Si on n'a pas pu verifier quelque chose, le signaler clairement.

## Stack technique

- **Backend**: Java 17+, Spring Boot, Liquibase, Maven
- **Frontend**: Angular, TypeScript, Bootstrap, SCSS
- **Base de donnees**: PostgreSQL (migrations Liquibase)

## Structure du projet

- `howners-api/src/main/java/` - Code Java backend (Spring Boot)
- `howners-api/src/main/resources/db/changelog/` - Migrations Liquibase
- `howners-api/src/app/` - Code Angular frontend
- `howners-api/src/app/core/` - Services et modeles partages
- `howners-api/src/app/features/` - Modules fonctionnels (lazy-loaded)
- `howners-api/src/app/shared/` - Composants partages
