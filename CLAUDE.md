# Poker en famille

Web app (PWA) de Texas Hold'em No-Limit pour jouer en famille sur téléphone, avec des jetons virtuels.
Le plan complet (périmètre, architecture, protocole, interface, itérations) est dans `docs/PLAN.md` :
le lire avant toute tâche et s'y tenir. Toute décision qui s'en écarte est proposée avant d'être codée,
puis reportée dans `docs/PLAN.md`.

## Langue

- Échanges, documentation et messages de commit en français.
- Code, noms de classes, de méthodes et de variables en anglais.

## Stack

- Un seul projet Quarkus (Java 21, Maven) qui contient le back et le front.
- Front : Svelte + Vite dans `src/main/webui`, intégré au build par l'extension Quarkiverse Quinoa.
- Temps réel : `quarkus-websockets-next`, endpoint `/ws/table/{code}`, messages JSON avec un champ `type`.
- État des tables en mémoire, pas de base de données en v1.
- Image Docker unique (Dockerfile multi-étapes), déployée sur un cluster Kubernetes personnel, 1 replica.
- Dépôt et CI sur GitHub (GitHub Actions).

## Commandes

- `quarkus dev` ou `./mvnw quarkus:dev` : back et front en mode dev, rechargement à chaud
- `./mvnw test` : tests Java (moteur, serveur, intégration WebSocket)
- `npm test` dans `src/main/webui` : tests Vitest du front (aussi lancés par `./mvnw package`)
- Tester à plusieurs dans un navigateur : `http://localhost:8080/?as=mamie`, `?as=papa`… (un profil par onglet)
- `./mvnw package` : build complet (Svelte + jar) dans `target/quarkus-app/`
- `docker build -t poker-famille .` : image complète

## Organisation du code

- `src/main/java/fr/famille/poker/engine/` : moteur de jeu en **Java pur, sans aucune dépendance Quarkus**.
  Règles, paquet, enchères, pots, évaluateur de mains. Entièrement couvert par des tests JUnit.
- `src/main/java/fr/famille/poker/server/` : `TableManager`, endpoint WebSocket, minuteur, DTO des messages.
- `src/main/webui/` : application Svelte. Les thèmes de cartes sont dans `public/themes/<nom>/`.
- `k8s/` : manifests Kubernetes. `.github/workflows/` : CI.

## Règles du projet

- Le serveur est la seule source de vérité. Le client n'envoie que des intentions et affiche l'état reçu.
- Un joueur ne reçoit jamais les cartes privées des autres avant l'abattage.
- Mélange avec `SecureRandom`.
- Les actions d'une même table sont traitées une par une.
- Le moteur est développé en TDD : écrire les tests des cas listés dans le plan avant l'implémentation
  (heads-up, relance minimale, tapis inférieur à la relance minimale, pots secondaires, partage du pot).

## Façon de travailler

- Avancer itération par itération, dans l'ordre de `docs/PLAN.md`. Ne pas commencer une itération
  avant que la précédente remplisse son critère « Terminé quand », et faire valider par l'utilisateur.
- Le protocole WebSocket (types de messages et format de `state`) est le contrat entre back et front.
  Le figer dans `docs/PROTOCOL.md` avant de paralléliser le travail back, front et CI.
- Une fois le contrat figé, le travail peut être réparti entre sous-agents (back, front, CI/déploiement),
  chacun limité à ses dossiers ; l'intégration et la relecture se font ensuite dans la session principale.
- Petits commits, un sujet par commit ; `./mvnw test` doit passer avant chaque commit.

## CI et déploiement

- Dépôt : `github.com-seb-etik:seb-etik/poker-famille.git` (alias SSH du compte seb-etik).
- GitHub Actions : `./mvnw verify` sur chaque pull request et sur `main` ; sur `main`, build et push de
  l'image publique `ghcr.io/seb-etik/poker-famille` (tags `latest` et `sha-…`).
- Déploiement manuel par l'utilisateur dans le namespace `sbu` : `kubectl apply -k k8s/`, puis
  `kubectl -n sbu rollout restart deployment/poker-famille` pour une nouvelle version. Ne jamais appliquer
  sur le cluster sans demande explicite. Détails dans `docs/PLAN.md`, section « Déploiement Kubernetes ».
