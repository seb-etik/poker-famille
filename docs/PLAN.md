# Poker en famille — Plan de développement

Version du 10 octobre 2026. Source : document « Poker en famille — Plan de développement » sur claude.ai.

## Objectif et périmètre de la v1

Une web app (PWA) de Texas Hold'em No-Limit pour jouer en famille, à distance, sur téléphone, avec des jetons virtuels et sans argent réel. L'adoption par la famille repose sur une interface soignée : surnom, avatar, thème de cartes et joueurs visibles autour de la table.

**Dans la v1**

- Création d'une table avec un code à 4 lettres, rejoindre par code ou par lien
- 2 à 8 joueurs, sans compte : surnom et avatar choisis à l'arrivée
- Texas Hold'em complet : blindes, 4 tours d'enchères, tapis et pots secondaires, abattage
- Table ovale sur mobile, joueur courant en bas, les autres répartis autour
- Choix du thème de cartes, propre à chaque joueur
- Reconnexion à sa place après une coupure réseau

**Hors v1**

- Comptes utilisateurs, historique et statistiques
- Tournois avec montée des blindes
- Chat, réactions emoji, upload de photos d'avatar
- Persistance des parties après un redémarrage du serveur

## Architecture technique

Un seul service Quarkus, déployé en un pod sur le cluster, sert à la fois le site Svelte et les WebSockets. Le serveur est l'arbitre : il mélange, distribue et applique les règles, et chaque joueur ne reçoit que ses propres cartes.

```
Téléphones (navigateur / PWA, front Svelte)
        │  HTTPS, wss
        ▼
Ingress nginx (TLS Let's Encrypt, timeouts 3600 s)
        │
        ▼
┌─ Cluster Kubernetes ──────────────────────────────┐
│  Pod Quarkus · 1 replica                          │
│   ├─ Fichiers statiques (build Svelte via Quinoa) │
│   ├─ Endpoint WebSocket (/ws/table/{code})        │
│   │     ▼                                         │
│   ├─ TableManager (tables en mémoire, sièges)     │
│   │     ▼                                         │
│   └─ Moteur de jeu (package engine)               │
└───────────────────────────────────────────────────┘
```

Les téléphones chargent le site puis ouvrent un WebSocket ; toute action passe par le TableManager, qui délègue les règles au moteur.

- **Front** : Svelte + Vite, PWA installable
- **Back** : Quarkus, Java 21, `quarkus-websockets-next`
- **État** : en mémoire, pas de base de données en v1
- **Concurrence** : les actions d'une table sont traitées une par une (verrou ou file par table)

## Briques du projet

Le projet est un seul projet Quarkus, front Svelte inclus. Le moteur de jeu vit dans le package `engine`, en Java pur sans dépendance à Quarkus, pour se tester en isolation.

| Brique | Rôle | Techno |
| --- | --- | --- |
| `engine` — moteur de jeu | Règles du Hold'em, paquet, enchères, pots, abattage | Java 21, JUnit |
| `engine` — évaluateur de mains | Meilleure main de 5 cartes parmi 7, comparaison | Java, tests exhaustifs |
| `server` — gestion des tables | Création, codes, sièges, cycle de vie des tables | Quarkus, `@ApplicationScoped` |
| `server` — endpoint temps réel | Connexions, messages, diffusion de l'état filtré par joueur | `quarkus-websockets-next` |
| `server` — minuteur d'action | Temps de jeu par joueur, action automatique (check ou fold) | Vert.x timers |
| `webui` — écran d'accueil | Surnom, avatar, thème, créer ou rejoindre | Svelte + Vite |
| `webui` — écran de table | Sièges, cartes, pot, actions, animations | Svelte, CSS, SVG |
| `webui` — client WebSocket | Connexion, reconnexion, ping, état local | Store Svelte |
| Assets | Thèmes de cartes SVG, avatars, sons | Fichiers statiques |
| Déploiement | Image, manifests, ingress, TLS, CI | Docker, Kubernetes, cert-manager, GitHub Actions |

Le build Svelte est intégré au build Quarkus par Quinoa (voir la section suivante) : une seule image sert le site et le WebSocket.

## Structure du projet et build

Un seul projet Quarkus contient l'API et le front Svelte ; une seule commande `mvn package` produit un jar qui sert les deux, et une seule image Docker. L'extension Quarkiverse **Quinoa** (`io.quarkiverse.quinoa:quarkus-quinoa`) fait le lien : elle lance le build Vite pendant le build Quarkus et sert le résultat.

```
poker-famille/
├── pom.xml
├── Dockerfile
├── .dockerignore
├── .github/workflows/        CI GitHub Actions
├── k8s/                      manifests (Deployment, Service, Ingress)
└── src/
    ├── main/
    │   ├── java/fr/famille/poker/
    │   │   ├── engine/       règles, évaluateur (Java pur, sans Quarkus)
    │   │   └── server/       TableManager, endpoint WebSocket
    │   ├── resources/
    │   │   └── application.properties
    │   └── webui/            projet Svelte + Vite
    │       ├── package.json
    │       ├── vite.config.js
    │       ├── public/themes/    thèmes de cartes SVG
    │       └── src/
    └── test/java/fr/famille/poker/engine/
```

**`application.properties`**

```properties
# Quinoa installe Node lui-même : rien à préinstaller, ni en local ni dans Docker
quarkus.quinoa.package-manager-install=true
quarkus.quinoa.package-manager-install.node-version=24.21.0
quarkus.quinoa.build-dir=dist
quarkus.quinoa.enable-spa-routing=true
quarkus.quinoa.ignored-path-prefixes=/ws,/q,/api
# en dev, Quinoa lance Vite et fait proxy (rechargement à chaud)
quarkus.quinoa.dev-server.port=5173
```

**Commandes**

- `quarkus dev` (ou `mvn quarkus:dev`) : back et front en mode dev, avec rechargement à chaud des deux
- `mvn package` : build Svelte + jar Quarkus dans `target/quarkus-app/`
- `docker build -t poker-famille .` : image complète

**`Dockerfile`** (multi-étapes : l'image finale ne contient ni Maven ni Node)

```dockerfile
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/target/quarkus-app/lib/ ./lib/
COPY --from=build /build/target/quarkus-app/*.jar ./
COPY --from=build /build/target/quarkus-app/app/ ./app/
COPY --from=build /build/target/quarkus-app/quarkus/ ./quarkus/
EXPOSE 8080
USER 1001
ENTRYPOINT ["java", "-jar", "quarkus-run.jar"]
```

**`.dockerignore`** : `target/`, `src/main/webui/node_modules/`, `src/main/webui/dist/`.

Versions retenues au démarrage (10 octobre 2026) : Quarkus 3.40.1, Quinoa 2.9.2 (choisie par le CLI Quarkus pour cette plateforme), Node 24.21.0 LTS, Svelte 5, Vite 8. Node 20.18.0, prévu initialement, est écarté : Node 20 est en fin de vie et Vite 8 exige Node 20.19 ou plus.

## Modèle de domaine et déroulé d'une main

Le package `engine` contient une poignée de classes immuables ou bien encapsulées, et une machine à états qui fait avancer la main.

| Classe | Responsabilité |
| --- | --- |
| `Card`, `Rank`, `Suit` | Une carte ; code court `AS`, `TD`, `9H` |
| `Deck` | 52 cartes, mélange avec `SecureRandom`, tirage |
| `Player` | `id`, surnom, avatar, tapis, connecté ou non |
| `Seat` | Numéro, joueur, cartes privées, mise du tour, statut (actif, couché, tapis, absent) |
| `Table` | Code, sièges, position du bouton, blindes, main en cours |
| `Hand` | Paquet, cartes communes, étape, joueur qui doit parler, pots |
| `BettingRound` | Mise la plus haute, relance minimale, joueurs ayant parlé depuis la dernière relance |
| `Pot` | Montant et sièges éligibles (pot principal et pots secondaires) |
| `HandEvaluator`, `HandRank` | Meilleure main de 5 parmi 7 ; `HandRank` comparable (catégorie + kickers) |

Machine à états d'une main :

```
Nouvelle main ─► Pré-flop ─► Flop ─► Turn ─► River
(2 cartes)       (enchères)  (+3)    (+1)    (+1)
     ▲              │          │       │       │
     │              └──────────┴───────┴───────┤ tous se couchent sauf un
     │                         ▼               ▼
     └──── main suivante ── Paiement ◄──── Abattage
                        (pots distribués)  (meilleure main)
```

Chaque tour d'enchères commence à gauche du bouton (à gauche de la grosse blinde au pré-flop) ; si tous les joueurs restants sont à tapis, le moteur distribue les cartes restantes et passe directement à l'abattage.

**Fin d'un tour d'enchères** : tous les joueurs actifs ont parlé depuis la dernière relance et ont misé le même montant, ou sont à tapis.

**Pots secondaires** : à la fin de chaque tour, trier les mises par montant et créer un pot par palier de tapis ; chaque pot ne revient qu'à ses sièges éligibles.

**Cas à tester en priorité** : heads-up (le bouton paie la petite blinde et parle en premier au pré-flop), relance minimale, tapis inférieur à la relance minimale, égalité et partage du pot (jeton impair au premier joueur après le bouton).

**Règles précisées pendant l'itération 2**

- Les montants de `bet` et `raise` sont des totaux pour le tour (« relancer à 60 »), pas des incréments.
- Se coucher n'est possible que face à une mise : quand le check est gratuit, `fold` est refusé (évite les erreurs de clic).
- Un tapis inférieur à la relance minimale oblige les autres à rejouer, mais ne rouvre la relance qu'à ceux qui n'ont pas encore parlé. Simplification : plusieurs tapis incomplets qui totalisent une relance complète ne rouvrent pas non plus la relance.
- Si la grosse blinde est à tapis pour moins qu'une grosse blinde, les autres doivent quand même suivre une grosse blinde complète.
- La part d'une mise que personne n'a suivie est rendue à la fin du tour.
- Pas de cartes brûlées : elles n'apportent rien avec un mélange `SecureRandom`.
- Quand plus personne ne peut miser (tous à tapis), le moteur distribue le tableau d'un coup ; c'est au serveur d'étaler l'animation.
- À l'abattage, tous les joueurs encore en jeu montrent leurs cartes (pas de cartes jetées face cachée en v1).
- Le bouton passe au joueur suivant qui a des jetons, en sautant sièges vides et joueurs ruinés (pas de règle du « bouton mort »).

## Protocole WebSocket

Le client envoie des intentions, le serveur les valide et renvoie l'état complet de la table, filtré pour chaque joueur. Le client n'a jamais à recalculer les règles : il affiche ce qu'il reçoit.

Endpoint : `wss://<domaine>/ws/table/{code}`. Messages en JSON avec un champ `type`.

**Le contrat détaillé (champs, exemples, codes d'erreur) est figé dans `docs/PROTOCOL.md`**, qui fait foi. Ajouts par rapport à ce tableau : création de table par `POST /api/tables`, messages `pong` et `leave` sans numéro de siège, événements renommés (`handStarted`, `action`, `board`, `showdown`, `win`).

| Sens | Type | Contenu |
| --- | --- | --- |
| Client → serveur | `join` | `playerId` (UUID stocké localement), `nickname`, `avatar` |
| Client → serveur | `sit` / `leave` | Numéro de siège |
| Client → serveur | `start` | Démarrer la partie (créateur de la table) |
| Client → serveur | `action` | `fold`, `check`, `call`, `bet`, `raise` + montant |
| Client → serveur | `ping` | Garde la connexion active (toutes les 25 s) |
| Serveur → client | `state` | État de la table vu par ce joueur (voir ci-dessous) |
| Serveur → client | `event` | Événement à animer : `dealt`, `bet`, `flop`, `showdown`, `win` |
| Serveur → client | `error` | Action refusée, avec un message lisible |

**Contenu de `state`** : sièges (surnom, avatar, tapis, mise en cours, statut connecté), cartes communes, pot(s), joueur qui doit parler, actions autorisées avec montants min et max, temps restant, et **uniquement les cartes privées du destinataire**. Les cartes des autres ne partent qu'à l'abattage.

**Règles d'or**

- Le serveur est la seule source de vérité ; une action hors tour ou invalide renvoie `error`.
- Le mélange utilise `SecureRandom`.
- Le `playerId` permet de retrouver sa place après une reconnexion ; le siège reste réservé pendant la main en cours.

## Interface

Trois écrans pour la v1 : l'accueil, la salle d'attente et la table. Le soin porté aux animations et à la personnalisation fera l'adoption.

**Écran d'accueil**

- Saisie du surnom et choix de l'avatar dans une galerie (avatars générés, par exemple avec DiceBear)
- Choix du thème de cartes avec un aperçu
- Boutons « Créer une table » et « Rejoindre » avec le code
- Profil mémorisé dans `localStorage` (avec `try/catch`)

**Salle d'attente**

- Code de la table et bouton de partage du lien
- Avatars des joueurs qui arrivent ; le créateur lance la partie

**Écran de table (portrait mobile)**

- Table ovale, joueur courant toujours en bas, les autres répartis par rotation des sièges
- Chaque siège : avatar, surnom, tapis, mise en cours, deux cartes dos visible
- Joueur qui doit parler : halo et minuteur circulaire autour de l'avatar
- Centre : cartes communes et pot
- Barre d'actions en bas : Fold, Check/Suivre, Relancer avec curseur et raccourcis (½ pot, pot, tapis)
- Siège grisé quand un joueur est déconnecté

**Thèmes de cartes**

- Un thème = un dossier `themes/<nom>/` avec 52 SVG (`AS.svg`, `KH.svg`…), un `back.svg` et un `theme.json`
- Choix purement local : le serveur ne connaît pas le thème
- Partir d'un jeu SVG open source, puis créer des thèmes maison (dos de carte personnalisé)

**Animations (CSS + transitions Svelte)**

- Distribution des cartes depuis le centre, retournement à l'abattage
- Jetons qui glissent vers le pot puis vers le gagnant
- Mise en avant de la main gagnante

## Déploiement Kubernetes

Un Deployment à un seul replica, un Service et un Ingress avec TLS suffisent. L'état des tables est en mémoire : deux pods auraient chacun leurs propres tables.

- **Image** : Dockerfile multi-étapes décrit dans « Structure du projet et build » ; image JVM pour commencer, image native GraalVM en option
- **Deployment** : `replicas: 1`, stratégie `Recreate`, probes sur `/q/health/ready` et `/q/health/live` (extension `quarkus-smallrye-health`)
- **Ingress nginx** : annotations `proxy-read-timeout` et `proxy-send-timeout` à 3600 pour garder les WebSockets ouverts
- **TLS** : cert-manager + Let's Encrypt, indispensable pour `wss://` et l'installation de la PWA
- **Redéploiement** : il efface les parties en cours ; déployer hors des sessions de jeu
- **Option** : l'extension `quarkus-kubernetes` peut générer les manifests à partir de `application.properties`

**Mise en œuvre retenue (itération 5)** — remplace les points nginx ci-dessus :

- Cluster : namespace `sbu` existant ; Ingress **Traefik** (`IngressRoute`, entrée `websecure`), qui relaie les WebSockets sans réglage ; le ping client toutes les 25 s les garde ouverts.
- TLS : `Certificate` cert-manager émis par le `ClusterIssuer` `letsencrypt-prod`.
- Domaine : `poker.sbu.playersgroupe.fr`, entrée DNS `CNAME lb.preprod.playersgroupe.fr` (pas de wildcard).
- Image : `ghcr.io/seb-etik/poker-famille`, paquet public (pas de secret de pull), tags `latest` et `sha-<commit>`.
- CI GitHub Actions : `./mvnw verify` sur chaque pull request et sur `main` ; sur `main`, build et push de l'image.
- Déploiement manuel, hors des parties en cours :
  - premier déploiement : `kubectl apply -k k8s/`
  - nouvelle version : `kubectl -n sbu rollout restart deployment/poker-famille` (image `latest`, `imagePullPolicy: Always`)

## Plan d'itérations

Six itérations, chacune terminée par quelque chose qui tourne. Le moteur passe en premier, car c'est le cœur et il se teste sans interface.

1. **Évaluateur de mains**
   - Modèle `Card`, `Rank`, `Suit`, `Deck` ; évaluation des 9 catégories de mains et départage par kickers
   - Terminé quand : tests couvrant chaque catégorie, les égalités et la quinte basse A-2-3-4-5
   - **Fait** : `HandEvaluator` essaie les 21 combinaisons de 5 cartes ; `HandRank` se compare par catégorie puis départageurs (égalité = `compareTo == 0`)
2. **Moteur d'une main**
   - Blindes, distribution, 4 tours d'enchères, tapis, pots secondaires, abattage
   - Terminé quand : une main complète se joue en test unitaire, y compris un tapis à 3 joueurs avec pot secondaire
   - **Fait** : `Hand` (machine à états), `BettingRound`, `Pot.split`, `Table` (sièges, bouton) ; un test joue 5 000 mains aléatoires et vérifie la conservation des jetons
3. **Serveur Quarkus**
   - `TableManager`, endpoint WebSocket, messages `join`, `action`, `state`, minuteur
   - Terminé quand : deux clients de test (ou `websocat`) jouent une main de bout en bout
   - **Fait** : `GameTable` (Java pur, testée avec un faux `Outbox` et un faux `Scheduler`), `TableManager`, `TableEndpoint`, `POST /api/tables`, minuteurs Vert.x, purge des tables abandonnées ; `TableEndpointTest` fait jouer une main complète à deux clients WebSocket. Délais réglables par `poker.*` dans `application.properties`
4. **Front minimal**
   - Svelte : accueil sans fioritures, table fonctionnelle, barre d'actions, reconnexion
   - Terminé quand : trois onglets de navigateur jouent une partie complète
   - **Fait** : accueil, table, barre d'actions, reconnexion (backoff + retour au premier plan), testés dans Chrome avec trois onglets jusqu'à la ruine de deux joueurs. Le paramètre d'URL `?as=<nom>` donne à un onglet son propre profil (sinon `localStorage` est partagé par tous les onglets) ; avatars en emoji en attendant l'itération 6
5. **Déploiement**
   - Image, manifests, Ingress, TLS sur le cluster, CI GitHub Actions
   - Terminé quand : une partie se joue depuis deux téléphones en 4G
   - **Fait** : `Dockerfile`, `k8s/` (kustomize), `.github/workflows/ci.yml` ; reste à valider le critère sur téléphones
6. **Finition visuelle** (avant de montrer à la famille)
   - Avatars, thèmes de cartes, animations, sons, installation PWA
   - Terminé quand : la partie test en famille est lancée

## Évolutions possibles

- Réactions emoji et petit chat à la table
- Upload de photos pour les avatars (endpoint d'upload + volume persistant)
- Mode tournoi avec montée des blindes
- Persistance des tables dans Redis pour survivre aux redéploiements
- Historique des mains et statistiques par joueur
- Mode « apprentissage » pour les enfants : affichage de la force de sa main
