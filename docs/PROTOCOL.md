# Protocole client ↔ serveur

Contrat entre le back (Quarkus) et le front (Svelte). Toute évolution passe d'abord par ce fichier.

Principes, repris de `PLAN.md` :

- Le serveur est la seule source de vérité. Le client envoie des **intentions** et affiche l'**état** reçu.
- Après chaque changement, chaque joueur reçoit l'état complet de la table **vu par lui**.
- Un joueur ne reçoit jamais les cartes privées des autres avant l'abattage.
- Les `event` ne servent qu'aux animations : le client doit pouvoir tout afficher à partir du seul `state`.

## 1. Créer une table (HTTP)

```
POST /api/tables
Content-Type: application/json

{
  "smallBlind": 10, "bigBlind": 20, "startingStack": 1000,
  "blindIncreaseEveryHands": 10, "blindIncreasePercent": 50
}                                                               ← tous les champs facultatifs
```

Réponse `201 Created` :

```json
{ "code": "KPRV" }
```

- Valeurs par défaut : petite blinde 10, grosse blinde 20, tapis de départ 1000, blindes en hausse de 100 %
  toutes les 5 mains (10/20, 20/40, 40/80, 80/160, 150/300…).
- Montée des blindes : `blindIncreaseEveryHands` de 1 à 100 (0 = blindes fixes), `blindIncreasePercent`
  de 10 à 200. À chaque niveau, la petite blinde monte du pourcentage, arrondie à une valeur ronde
  (1 à 10, puis 15, 20, 25, 30, 40, 50, 60, 80, 100…) et toujours en hausse ; la grosse garde le rapport
  de départ. Exemple à +50 % : 5/10, 8/16, 10/20, 15/30, 25/50, 40/80, 60/120, 100/200.
- Réglages hors bornes : `400 Bad Request`.
- Le code fait 4 lettres majuscules, sans `I` ni `O` (confusion avec 1 et 0).
- Le lien à partager est `https://<domaine>/table/KPRV` (routage SPA côté front).
- Une table sans aucun joueur connecté depuis 30 minutes est supprimée.

## 1 bis. Classement de la famille (HTTP)

```
GET /api/leaderboard?period=month      ← mois civil en cours, heure de Paris (par défaut)
GET /api/leaderboard?period=all        ← depuis le démarrage du serveur
```

Réponse `200` : `[{ "nickname": "Mamie", "avatar": "fox", "wins": 5, "games": 14, "rank": 2 }, …]`, triée par
victoires, puis pourcentage de victoires, puis nombre de parties ; les joueurs à égalité sur les trois partagent
leur rang. Un joueur est son surnom, comparé sans majuscules, accents ni espaces autour ; surnom et avatar
affichés sont ceux de sa dernière partie. Une victoire est une partie gagnée jusqu'au bout (tous les autres joueurs
ont fini sans jetons). L'historique est **en mémoire** : il repart à zéro à chaque redémarrage du serveur.
Période inconnue : `400`.

## 2. WebSocket

Endpoint : `wss://<domaine>/ws/table/{code}`. Tous les messages sont des objets JSON avec un champ `type`.
Les noms de champs sont en anglais (camelCase) ; les valeurs énumérées aussi, en minuscules (`allIn`, `preflop`).

Si le code n'existe pas, le serveur envoie une `error` de code `TABLE_NOT_FOUND` puis ferme la connexion.

### Identité et reconnexion

- Le client génère un `playerId` (UUID) une fois pour toutes et le garde dans `localStorage`.
- Le `playerId` est **secret** : quiconque le connaît peut jouer à la place du joueur. Le serveur ne
  l'envoie jamais aux autres joueurs ; dans `state`, les joueurs sont désignés par leur numéro de siège.
- Une reconnexion avec le même `playerId` retrouve le même siège, le même tapis et les mêmes cartes.
- Si le même `playerId` ouvre une deuxième connexion (deuxième onglet), la nouvelle remplace l'ancienne,
  qui est fermée.

## 3. Messages client → serveur

| `type` | Champs | Effet |
| --- | --- | --- |
| `join` | `playerId`, `nickname` (1 à 20 car.), `avatar` (identifiant opaque, 64 car. max) | Premier message obligatoire. Entre dans la salle (sans siège) ou retrouve son siège. |
| `sit` | `seat` (0 à 7) | S'assoit sur un siège libre. Arrivée pendant une main : joue à partir de la suivante. |
| `leave` | — | Quitte son siège. Pendant une main, le joueur se couche et le siège se libère à la fin de la main. |
| `start` | — | Lance la partie. Réservé à l'hôte, au moins deux joueurs assis. |
| `restart` | — | Après une victoire (`table.winnerSeat` renseigné), relance une partie avec les joueurs assis : tapis de départ pour tous, blindes au niveau 1, bouton au premier joueur. Réservé à l'hôte. |
| `action` | `action` (`fold`, `check`, `call`, `bet`, `raise`), `amount` (pour `bet` et `raise`) | Joue son tour. |
| `ping` | — | Garde la connexion ouverte, toutes les 25 s. Réponse : `pong`. |

**Montants** : pour `bet` et `raise`, `amount` est le **total** misé dans le tour une fois l'action faite
(« relancer à 60 »), entre `minAmount` et `maxAmount` de `allowedActions`. `maxAmount` correspond au tapis.

Exemples :

```json
{ "type": "join", "playerId": "6f1c…", "nickname": "Mamie", "avatar": "fox-3" }
{ "type": "sit", "seat": 2 }
{ "type": "action", "action": "raise", "amount": 60 }
```

## 4. Messages serveur → client

### `state`

Envoyé après `join` puis après chaque changement. Exemple pendant une main, vu par le siège 2 :

```json
{
  "type": "state",
  "table": {
    "code": "KPRV",
    "smallBlind": 5,
    "bigBlind": 10,
    "startingStack": 1000,
    "status": "playing",
    "hostSeat": 0,
    "blindLevel": 1,
    "handsUntilNextLevel": 6,
    "nextBlinds": { "smallBlind": 8, "bigBlind": 16 },
    "winnerSeat": null
  },
  "you": { "seat": 2, "isHost": false },
  "seats": [
    { "seat": 0, "nickname": "Papa", "avatar": "bear-1", "stack": 970, "bet": 30,
      "status": "active", "connected": true, "hasCards": true, "cards": null },
    null,
    { "seat": 2, "nickname": "Mamie", "avatar": "fox-3", "stack": 990, "bet": 10,
      "status": "active", "connected": true, "hasCards": true, "cards": ["AS", "KD"] },
    null, null, null, null, null
  ],
  "hand": {
    "number": 4,
    "stage": "preflop",
    "buttonSeat": 0,
    "smallBlindSeat": 1,
    "bigBlindSeat": 2,
    "board": [],
    "pots": [],
    "currentBet": 30,
    "toActSeat": 2,
    "remainingMs": 27400
  },
  "allowedActions": {
    "types": ["fold", "call", "raise"],
    "callAmount": 20,
    "minAmount": 50,
    "maxAmount": 1000
  },
  "lastResult": null
}
```

Champs :

- `table.status` : `waiting` (salle d'attente) ou `playing`. Retour à `waiting` quand il reste moins de
  deux joueurs avec des jetons.
- `table.smallBlind` / `table.bigBlind` : blindes du niveau en cours. `table.blindLevel` : niveau, à partir
  de 1. `table.handsUntilNextLevel` : mains restant à jouer à ce niveau (0 : la prochaine main monte) ;
  `table.nextBlinds` : blindes du niveau suivant. Ces deux derniers valent `null` si les blindes sont fixes.
- `table.hostSeat` : siège de l'hôte, `null` s'il n'est pas assis.
- `table.winnerSeat` : siège du joueur qui a remporté la partie (seul à avoir encore des jetons), `null`
  tant que la partie n'est pas finie ; remis à `null` quand l'hôte relance une partie (`restart`). `you.seat` : `null` si spectateur.
- `seats` : toujours 8 entrées, `null` pour un siège libre.
  - `status` : `active`, `folded`, `allIn`, `sittingOut`.
  - `bet` : mise dans le tour en cours (les tours précédents sont dans `hand.pots`).
  - `hasCards` : le joueur a des cartes devant lui (afficher les dos).
  - `cards` : les deux cartes, **uniquement** pour le destinataire, ou pour tous à l'abattage ; sinon `null`.
- `hand` : `null` hors d'une main.
  - `stage` : `preflop`, `flop`, `turn`, `river`, `complete`.
  - `smallBlindSeat` / `bigBlindSeat` : sièges qui ont payé la petite et la grosse blinde (en heads-up, le bouton paie la petite). Les montants sont dans `table.smallBlind` / `table.bigBlind`.
  - `pots` : `[{ "amount": 450, "seats": [0, 1, 2] }, …]`, pot principal d'abord ; sièges éligibles.
  - `toActSeat` : `null` quand la main est terminée. `remainingMs` : temps restant pour agir.
- `allowedActions` : non `null` seulement pour le joueur qui doit parler. `callAmount` : jetons qu'ajoute
  un `call` (tapis compris). `minAmount`/`maxAmount` : bornes de `bet`/`raise`, `0` si interdit.
- `lastResult` : résultat de la main qui vient de se terminer, affiché pendant la pause entre deux mains :

```json
"lastResult": {
  "handNumber": 4,
  "payouts": [
    { "amount": 450, "winners": { "2": 450 } },
    { "amount": 500, "winners": { "1": 500 } }
  ],
  "shownHands": {
    "0": { "category": "highCard", "cards": ["KS", "QS", "JD", "9H", "7D"] },
    "1": { "category": "onePair", "cards": ["AH", "AD", "KS", "9H", "7D"] },
    "2": { "category": "threeOfAKind", "cards": ["KS", "KD", "KC", "9H", "7D"] }
  }
}
```

`shownHands` est vide quand tout le monde s'est couché ; `cards` y donne les 5 cartes de la meilleure main
(pour les mettre en avant). Catégories : `highCard`, `onePair`, `twoPair`, `threeOfAKind`, `straight`,
`flush`, `fullHouse`, `fourOfAKind`, `straightFlush`.

### `event`

Envoyé juste **avant** le `state` qui en résulte, pour animer la transition.

| `name` | Champs | Moment |
| --- | --- | --- |
| `blindsUp` | `level`, `smallBlind`, `bigBlind` | Les blindes montent, juste avant le `handStarted` de la main |
| `handStarted` | `handNumber`, `buttonSeat` | Blindes posées et cartes distribuées |
| `action` | `seat`, `action`, `amount` (total misé dans le tour) | Un joueur a joué (y compris action automatique) |
| `board` | `stage`, `cards` (nouvelles cartes seulement) | Flop, turn ou river |
| `showdown` | — | Les cartes sont dévoilées (voir `seats[].cards` et `lastResult`) |
| `win` | `payouts` (même format que `lastResult`) | Les pots sont distribués |
| `gameWon` | `seat`, `wins`, `rank`, `top` | Il ne reste qu'un joueur avec des jetons : il remporte la partie. `wins` et `rank` : ses victoires et son rang du mois après cette partie ; `top` : les 3 premiers du mois (format de `/api/leaderboard`). Les trois valent `null` si la partie ne compte pas (adversaires partis avec leurs jetons). |

Quand tous les joueurs sont à tapis, plusieurs `board` partent d'affilée : le client les anime à la suite.

```json
{ "type": "event", "name": "action", "seat": 0, "action": "raise", "amount": 30 }
```

### `error`

```json
{ "type": "error", "code": "ILLEGAL_ACTION", "message": "Le montant doit être entre 50 et 1000." }
```

`message` est en français et s'affiche tel quel. `code` sert à la logique du client :

| `code` | Cas |
| --- | --- |
| `TABLE_NOT_FOUND` | Code de table inconnu (la connexion est ensuite fermée) |
| `INVALID_MESSAGE` | JSON illisible, `type` inconnu, champ manquant ou invalide |
| `NOT_JOINED` | Message envoyé avant `join` |
| `SEAT_TAKEN` | Siège occupé ou numéro invalide |
| `ALREADY_SEATED` | `sit` alors qu'on a déjà un siège |
| `NOT_HOST` | `start` envoyé par un autre joueur que l'hôte |
| `NOT_ENOUGH_PLAYERS` | `start` avec moins de deux joueurs assis |
| `ILLEGAL_ACTION` | Action hors tour ou refusée par les règles |

### `pong`

Réponse à `ping`, sans autre champ.

## 5. Déroulé de la partie

- **Hôte** : le premier joueur qui rejoint la table. S'il se lève (`leave`), l'hôte devient le joueur assis
  suivant dans l'ordre des sièges.
- **Tapis** : un joueur qui s'assoit reçoit le tapis de départ. S'il se lève puis se rassoit, il retrouve
  son tapis précédent (le serveur le garde par `playerId`) : se relever ne recharge pas les jetons.
- **Enchaînement** : après `start`, les mains s'enchaînent seules. Pause de 5 s après une main gagnée sans
  abattage, 12 s après un abattage, pour laisser le temps de lire les cartes montrées.
- **Minuteur** : 30 s pour agir. À l'expiration, le serveur joue `check` si c'est possible, sinon `fold`.
- **Déconnexion** : le joueur garde son siège et son tapis, `connected` passe à `false`. À son tour, le
  serveur joue tout de suite pour lui (`check` sinon `fold`) au lieu d'attendre 30 s.
- **Joueur ruiné** : il reste assis en `sittingOut` et regarde ; pas de recave en v1.
