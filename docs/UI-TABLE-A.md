# Refonte de la table : direction A « Table claire »

Choisie par la famille le 11 octobre 2026 dans le canevas Claude Design « Poker en famille — revue UI »
(rangée « Direction A retenue »). Maquettes 390 × 844 : A · Table claire (check possible), A · Face à une mise,
A · Attente, A · Abattage, A · Victoire. Les initiales en rond des maquettes remplacent les vrais avatars.

Front uniquement : aucun changement de serveur ni de protocole n'est nécessaire.

## Décisions de la famille

- On garde le jeu de cartes actuel (`themes/classique`) et la palette actuelle (fond `#0b1a12`, panneaux `#10261b`,
  vert `#2b5a41`, or `#ffd23f`, rouge `#7a2230`, bleu PB `#2d66b3`).
- **Quatre boutons d'action seulement**, toujours dans cet ordre et à la même place :
  1. **Coucher** (rouge)
  2. **Check**, ou **Suivre** + montant quand il y a une mise à suivre (vert). Même place, le libellé change.
  3. **Relancer** + montant (or plein). « Miser » n'existe plus comme libellé : `bet` et `raise` s'affichent « Relancer ».
  4. **Tapis** + montant du tapis (contour or).
- Pas de raccourcis ½ pot / pot, pas de champ de saisie : un seul curseur au-dessus des boutons, avec la valeur
  affichée en or à droite.

## Barre d'actions (`ActionBar.svelte`)

- Grille de 4 colonnes égales, boutons de 62 px de haut ; le montant s'affiche sur une 2e ligne (14 px) sous le
  libellé (16 px gras).
- Curseur (révisé le 17 octobre, voir la fin du document) : toucher « Relancer » remplace la rangée de boutons par
  « ✕ · curseur · Relancer + montant » ; le second toucher envoie la relance. Seulement si relancer est possible et
  `maxAmount > minAmount` ; sinon « Relancer » envoie directement le seul montant possible.
- Correspondance avec `allowedActions` :
  - Coucher : `fold`.
  - 2e bouton : `check` si permis, sinon `call` avec `callAmount`.
  - Relancer : `bet` ou `raise` avec le montant du curseur (départ à `minAmount`). Grisé si aucun des deux n'est permis.
  - Tapis : `bet`/`raise` à `maxAmount` si permis ; sinon, quand `callAmount` vaut tout le tapis, `call`.
    Affiche le tapis restant du joueur.
- Un bouton non permis reste à sa place, grisé (pas de décalage des autres).

## Écran de table (`Table.svelte`, `Seat.svelte`)

- **En-tête** : retour, « TABLE QRJQ » avec en dessous « Blindes 10 / 20 · niveau 2 », menu à droite.
  Le partage, le son et « Se lever » passent dans ce menu (en attente, « Inviter » est aussi au centre de la table).
- **Ovale plus compact** : il occupe la zone sous l'en-tête jusqu'à environ 550 px, bord bois `#5b3a20` de 10 px.
- **Sièges adverses** : avatar 52 px, surnom 14 px gras, tapis dans une pastille sombre. Couché : siège à 45 %
  d'opacité, la pastille dit « Couché(e) ». Donneur : jeton blanc « D » posé sur le feutre à côté du siège.
- **Mises** : un jeton or + montant posé devant chaque joueur, côté centre, jamais sur le texte du centre.
- **Centre** : 5 emplacements de cartes (les cartes pas encore sorties en pointillés), puis « POT » en petites
  capitales, le montant en or 32 px, et une seule ligne grise : « Niveau 3 (15/30) dans 4 mains ».
- **Pastille d'appel** sous le pot, quand c'est à moi : « À toi de parler » ou « À toi : 40 pour suivre »
  (contour or).
- **Ma place**, bandeau sous la table : avatar 54 px avec le minuteur en anneau or autour, badge PB/GB/D sur
  l'avatar, surnom en or, « 960 jetons », secondes restantes ; mes deux cartes à droite (70 × 102, légèrement
  inclinées).

## Autres états

- **Attente** : places libres en cercles pointillés « + Libre » (boutons) ; au centre « 4 joueurs assis », les
  réglages (blindes, tapis, montée) et un bouton « Inviter » ; en bas, pour l'hôte, « Lancer la partie » en or
  pleine largeur, et « Se lever » à côté de ma place.
- **Abattage** : les cartes qui forment la main gagnante remontent de 8 px avec un contour or, les autres
  passent à 45 % ; au centre « Seb gagne 480 » en or et « Brelan de rois » en dessous ; cartes montrées par les
  autres en petit (34 × 50) près de leur siège avec le nom de leur main. Ma place surlignée si je gagne (+ 480).
  La barre du bas est remplacée par « Main suivante dans 4 s » avec une barre de progression.
- **Victoire** : carte centrale à contour or : trophée, avatar 96 px, « Seb remporte la partie », total des
  jetons, avatars des autres joueurs, bouton « Rejouer » (hôte) avec « Mêmes joueurs, tapis et blindes remis à
  zéro » ; « Retour à l'accueil » en dessous. Confettis discrets.

## À garder

Animations existantes (distribution, jetons vers le pot puis vers le gagnant, retournement), sons, coupure si
`prefers-reduced-motion`, cibles tactiles d'au moins 44 px, `env(safe-area-inset-bottom)` sous la barre.

## Bugs relevés pendant la revue

- Arrivée par le lien d'une table : la première lettre tapée dans le surnom fait basculer sur la table
  (`App.svelte` passe à `Table` dès que `profile.nickname` n'est plus vide). Ne basculer qu'au clic sur « Rejoindre ».
- Accueil à 390 px : le bouton « Rejoindre » déborde à droite.

## V2 : retours de la famille du 11 octobre

Maquettes dans le canevas, rangée « V2 après les retours de la famille » : Retours du 11 octobre (photos iPhone),
Placement automatique des adversaires, V2 · 3 joueurs, V2 · 5 joueurs, V2 · 8 joueurs.

### Placement automatique des sièges

Cause des chevauchements : `seatPosition` place chaque siège selon son numéro réel sur 8 emplacements fixes.
À 3 joueurs, les adversaires tombent à l'est et à l'ouest, sur la ligne du pot et de la pastille « À toi ».

Nouvelle règle : la position à l'écran dépend du **nombre d'adversaires**, pas du numéro de siège. On garde l'ordre
du jeu (sens des aiguilles d'une montre en partant de ma gauche) et on remplit d'abord le haut de la table ; l'est
et l'ouest bas (O2, E2) ne servent qu'à partir de 7 joueurs.

| Adversaires | Emplacements, de ma gauche à ma droite |
| --- | --- |
| 1 | N |
| 2 | NO, NE |
| 3 | NO, N, NE |
| 4 | O1, NO, NE, E1 |
| 5 | O1, NO, N, NE, E1 |
| 6 | O2, O1, NO, NE, E1, E2 |
| 7 | O2, O1, NO, N, NE, E1, E2 |

Centres d'avatar, en px dans la boîte de la table de 390 × 500 (à convertir en %) :
N (195, 62) · NO (100, 92) · NE (290, 92) · O1 (44, 196) · E1 (346, 196) · O2 (44, 410) · E2 (346, 410).
Les adversaires couchés gardent leur emplacement (grisés). En attente, les places libres suivent la même
logique ; un siège qui se libère en cours de partie ne fait pas bouger les autres avant la main suivante.

### Zones réservées, rien ne s'y superpose

- **Bande du tableau** : de 280 à 373 px dans la boîte, toute la largeur. Aucun siège, aucune mise.
- **Pot** : centré sous le tableau, montant seul (le texte « Niveau … dans … mains » quitte la table).
- **Ma mise** : jeton au centre en bas de l'ovale, au-dessus de mon bandeau.
- Mises des adversaires : jeton posé entre l'avatar et le centre, au-dessus de la bande du tableau (O2/E2 : à
  l'intérieur, à côté du siège).

### Autres corrections

- **Table plus large** : l'ovale déborde de 18 px de chaque côté de l'écran (largeur 426 px pour 390, le rail
  est coupé par les bords).
- **Cartes du tableau plus grandes** : 64 × 93 (au lieu de 52 × 76), écart 6 px.
- **Prochain niveau** dans l'en-tête : « Blindes 10 / 20 · 15 / 30 dans 7 mains ».
- **Pastille « À toi »** retirée de la table : l'info va dans mon bandeau, « À toi · 20 pour suivre · 28 s ».
- **Bouton donneur « D »** : badge sur l'avatar (en bas à droite), comme PB et GB ; jamais posé sur le feutre.
- **Cartes cachées des adversaires** : petit éventail 18 × 26 en haut à droite de l'avatar.
- **« Main suivante dans … » supprimé** (`Table.svelte`, vers la ligne 331) ; la main suivante démarre seule.
- **Blindes par défaut 10 / 20** à la création d'une table (`Home.svelte` : `smallBlind: 10`, `bigBlind: 20`),
  et côté serveur `TableSettings.DEFAULT` (`new TableSettings(10, 20, 1000, 10, 50)`) ; tapis de 1 000 inchangé.

## V3 : cartes du tableau plus grandes (option 2 retenue le 17 octobre)

Maquette dans le canevas, rangée « Cartes du tableau plus grandes » : « Cartes 2 · Plus grandes, chevauchées ».
Demande de la famille : les cartes du centre sont trop petites ; elles doivent être au moins aussi grandes que celles
du joueur en bas (70 × 102).

### Taille et chevauchement

- Cartes du tableau en **82 × 119** sur un écran de 390 px (ratio 167 / 243 inchangé), soit plus grandes que ma main.
- Les cartes se **chevauchent de 14 px** (environ 17 % de leur largeur) : `margin-left: calc(var(--board-card) * -0.17)`
  à partir de la 2e. Le coin gauche de chaque carte (valeur et couleur) reste entièrement visible.
- Ombre portée vers la gauche (`box-shadow: -3px 0 6px rgb(0 0 0 / 0.35)`) pour détacher chaque carte de la précédente.
- La taille suit la **largeur** de la table, plus la hauteur : aujourd'hui `--board-card: min(64px, 12.8cqh)` rétrécit
  les cartes sur iPhone dans Safari (barres du navigateur). Nouvelle règle :
  `--board-card: min(82px, (100cqw + 20px) / 5, 16.4cqh)` ; 5 cartes moins 4 chevauchements occupent la largeur
  moins 18 px de marge de chaque côté (82 px à 390 px de large, 76 px à 360 px). Le terme en `cqh` ne joue que sur
  une table de moins de 500 px de haut.
- Les emplacements vides (cartes pas encore sorties) ont la même taille et le même chevauchement, pour que les cartes
  arrivent à leur place définitive.

### Ajustements autour du tableau

- Tableau remonté de 14 px : haut à 266 px dans la boîte de 500 (au lieu de 280), bas vers 385.
- Pot juste en dessous, haut vers 396.
- Sièges : O1 et E1 remontent de 8 px (centre d'avatar à 188 au lieu de 196) ; NO, NE et N remontent de 4 px
  (88 et 58). O2 et E2 ne bougent pas. Les mises suivent leur siège.
- Vérifier à 7 adversaires que les pastilles de tapis de O1/E1 ne touchent pas la première et la dernière carte.

### Abattage

- Les cartes de la main gagnante passent **au premier plan** (`z-index`) en plus de remonter de 8 px avec le contour or,
  pour que le contour ne soit pas caché par la carte suivante ; les autres restent à 45 %.

Mes cartes (`MyPlace.svelte`, 70 × 102) ne changent pas.

### Écarts constatés à l'implémentation (17 octobre)

- **O2 et E2 descendent de 10 px** (centre d'avatar à 420 au lieu de 410, mises avec eux) : avec le tableau de
  82 × 119 (bas à 385), l'avatar de O2/E2 commençait à 384 et son éventail de cartes cachées à 376, sur le coin de la
  première et de la dernière carte. À 420, l'éventail commence à 386 et le siège finit vers 489.
- **Abattage à 7 adversaires** : la ligne du gagnant passait sur les cartes montrées de O2/E2, et le nom de main
  ajouté sous les sièges faisait déborder O1/E1 sur le haut du tableau. Le gagnant s'affiche donc sur deux lignes
  (« Seb gagne 480 », puis « Brelan de rois ») dans la largeur libre entre O2 et E2 ; sous chaque siège, la pastille
  affiche la catégorie courte (« Double paire ») à la place du tapis pendant l'abattage, sans ligne en plus.

## V4 : pas de relance proportionnel aux blindes (17 octobre)

Demande de la famille : le curseur de relance avance à l'unité, c'est trop précis.

### Règle

- **Pas du curseur** = la moitié de la grosse blinde courante, arrondie **vers le bas** à une valeur ronde de la
  suite 1, 2, 5, 10, 25, 50, 100, 250, 500, 1 000… (au moins 1).

| Blindes | Pas |
| --- | --- |
| 10 / 20 | 10 |
| 15 / 30 | 10 |
| 25 / 50 | 25 |
| 50 / 100 | 50 |
| 100 / 200 | 100 |

- Positions possibles du curseur : `minAmount`, puis chaque multiple du pas strictement entre `minAmount` et
  `maxAmount`, puis `maxAmount` (le tapis exact). Le minimum et le tapis restent donc toujours atteignables, même
  s'ils ne sont pas ronds (ex. min 35 → 35, 40, 50, 60… ; tapis 957 → …, 940, 950, 957).
- Le montant affiché (valeur du curseur et bouton « Relancer ») est toujours l'une de ces positions.

### Implémentation (front seulement)

- `ActionBar.svelte` : calculer le pas à partir de `state.table.bigBlind` ; construire la liste des positions ;
  le curseur va de 0 au nombre de positions moins 1 (`step="1"`), et la valeur envoyée est la position choisie.
  Plus simple et plus fiable qu'un `step` HTML sur les montants, qui ne gère pas un minimum ou un maximum non ronds.
- Une petite fonction pure dans `lib/` (`raiseStep(bigBlind)` et `raiseStops(min, max, step)`), testée avec Vitest :
  cas du tableau ci-dessus, minimum non rond, tapis non rond, `min == max`.
- Aucun changement serveur ni protocole : le serveur accepte déjà tout montant entre `minAmount` et `maxAmount`.

## V5 : classement des victoires (17 octobre)

Maquettes dans le canevas, rangée « Classement des victoires » : accès depuis l'accueil, page de la famille,
fin de partie. Les noms et chiffres des maquettes sont des exemples.

### Décisions

- **Un joueur = son surnom**, comparé sans tenir compte des majuscules, des accents ni des espaces autour
  (« Mamie », « mamie » et « Mamie  » sont la même personne). Le classement affiche le surnom et l'avatar de sa
  dernière partie. Changer de surnom crée une nouvelle ligne : c'est accepté.
- **Une victoire** = une partie terminée avec un seul joueur qui a encore des jetons, avec au moins 2 joueurs
  au départ. Une partie abandonnée ou une table purgée ne compte pas. Chaque « Rejouer » est une nouvelle partie.
- **Un seul classement pour toute l'instance** (c'est la famille), pas par table : les codes de table sont éphémères.
- On enregistre l'**historique des parties**, pas des compteurs ; le classement est recalculé à la lecture.

### Stockage : un fichier JSON sur un mini volume

- Fichier `results.json` : liste de parties `{ "endedAt": "2026-10-17T11:02:00Z", "players": ["Mamie", "Seb", "Tessa"],
  "winner": "Seb", "avatars": { "Seb": "lorelei:…", … } }` (surnoms tels que saisis ; la normalisation se fait au calcul).
- Chemin configurable : `poker.results.file` dans `application.properties`, surchargé en production par la
  variable `POKER_RESULTS_FILE=/data/results.json`. **Sans valeur (dev, tests), l'historique reste en mémoire.**
- `ResultStore` (`@ApplicationScoped`) : charge le fichier au démarrage (absent → liste vide ; illisible → le
  renommer en `results.json.broken-<date>` et repartir à vide, avec un log d'erreur) ; à chaque fin de partie,
  ajoute l'entrée et réécrit le fichier de façon atomique (écriture dans `results.json.tmp` puis `Files.move` avec
  `ATOMIC_MOVE`). Accès synchronisé. Le volume de données reste minuscule (quelques Ko par an).
- Appel dans `GameTable` à l'endroit où `winnerSeat` est fixé (fin de partie), via une interface injectée
  (comme `Outbox` / `Scheduler`) pour garder `GameTable` testable sans fichier.

### Kubernetes

Nouveau fichier `k8s/pvc.yaml`, ajouté aux `resources` de `kustomization.yaml` :

```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: poker-famille-data
  namespace: sbu
spec:
  accessModes: [ReadWriteOnce]
  resources:
    requests:
      storage: 100Mi
```

Sans `storageClassName`, la classe par défaut du cluster est utilisée (`local-path` sur k3s : vérifier avec
`kubectl get storageclass`). Dans `deployment.yaml` :

```yaml
    spec:
      securityContext:
        fsGroup: 1001            # le pod tourne en 1001 : il doit pouvoir écrire dans /data
      volumes:
        - name: data
          persistentVolumeClaim:
            claimName: poker-famille-data
      containers:
        - name: poker-famille
          env:
            - name: POKER_RESULTS_FILE
              value: /data/results.json
          volumeMounts:
            - name: data
              mountPath: /data
```

La stratégie `Recreate` et `replicas: 1` déjà en place sont indispensables avec un volume `ReadWriteOnce`.
Si `local-path` ignore `fsGroup` et que l'écriture échoue (permission refusée), ajouter un `initContainer`
`busybox` qui fait `chown 1001:1001 /data`.

### API et protocole

- `GET /api/leaderboard?period=month|all` → `[{ "nickname", "avatar", "wins", "games" }]` trié par victoires,
  puis par pourcentage, puis par nombre de parties. `month` = mois civil en cours, fuseau `Europe/Paris`.
- Événement `gameWon` enrichi pour l'écran de fin : `{ "seat", "wins": 6, "rank": 2, "top": [ {…}, {…}, {…} ] }`
  (classement du mois après cette victoire).

### Écrans

- **Accueil** : sous le profil, « 2ᵉ du classement · 5 victoires » (rien si le surnom n'a jamais joué) ; carte
  « Classement de la famille » avec le top 3 du mois et « Voir tout le classement ». Carte masquée tant que
  l'historique est vide.
- **Page Classement** (route `/classement`) : onglets « <mois en cours> » et « Depuis le début » ; podium des 3
  premiers (le 1er au centre, plus haut, contour or) ; suite en liste avec victoires, parties et pourcentage ; ma
  ligne en bas « Toi : 5 victoires sur 14 parties · 36 % » ; une phrase explique ce qui compte comme victoire.
- **Fin de partie** : sous « Seb remporte la partie », une phrase (« 6ᵉ victoire du mois · plus qu'une pour rattraper
  Mamie » / « prend la tête du classement ») et le top 3 du mois avec « +1 » sur le gagnant ; boutons « Rejouer »
  (hôte) et « Voir tout le classement ».

### Tests

- Normalisation des surnoms, calcul du classement (égalités, mois en cours), fichier absent / corrompu,
  écriture atomique, partie à 1 joueur non comptée, `Rejouer` qui compte une nouvelle partie.

### Écart décidé à l'implémentation (17 octobre) : pas de persistance

Sur ce cluster (Scaleway Kapsule), le plus petit volume persistant est de 1 Go facturé : la famille préfère s'en
passer. L'historique des parties reste **en mémoire** (`ResultStore`) et le classement repart à zéro à chaque
redémarrage du serveur (redéploiement). Pas de `results.json`, pas de `poker.results.file`, pas de
`k8s/pvc.yaml` ni de changement du `Deployment`.

## V6 : cartes des adversaires plus visibles à l'abattage (option 1 retenue le 17 octobre)

Maquette dans le canevas, rangée « Mieux voir les cartes à l'abattage » : « Abattage 1 · En grand près de chaque
joueur ». Demande de la famille : à la fin d'une main, on voit mal les cartes des adversaires et on ne comprend pas
pourquoi on a gagné ou perdu.

### Cartes montrées

- `Seat.svelte` : à l'abattage, les cartes montrées passent de 34 px à **52 × 76** (au lieu de
  `width={showdown ? 34 : 18}`), chevauchées de 14 px, ombre à gauche.
- Placées **à côté du siège, côté centre de la table** (à droite pour O1/O2/NO, à gauche pour E1/E2/NE, en dessous
  pour N), jamais sur la bande du tableau ni sur le pot. Les cartes cachées (18 × 26) disparaissent quand elles
  sont retournées.
- **De 6 à 7 adversaires** : 44 × 64 pour tenir sans chevauchement entre voisins (à vérifier sur la maquette V2 · 8
  joueurs).
- Les joueurs couchés ne montrent rien et restent grisés.

### Nom de la main

- Sous les cartes de chaque joueur montré, une étiquette avec le nom de sa main, en toutes lettres comme
  aujourd'hui (`categoryLabel`) et si possible précisée : « Full aux 7 par les rois », « Paire de rois ».
  Fond sombre, texte blanc, 13 px gras.
- **Gagnant** : étiquette or (`#ffd23f`, texte foncé), contour or sur ses deux cartes, avatar avec halo or et surnom
  en or. En cas de partage, tous les gagnants ont ce style.
- **Ma main** : la même étiquette dans mon bandeau, sous mes jetons (« Brelan de rois »), or si j'ai gagné.

### Tableau et centre

- Inchangé par rapport à V3 : les cartes du tableau qui font la main gagnante remontent avec le contour or et
  passent au premier plan, les autres à 45 %.
- Sous le tableau, une seule ligne : « Léo gagne 480 » (or, 22 px). Le nom de la main est déjà sur l'étiquette.
- Barre d'actions grisée pendant l'abattage, comme aujourd'hui.

### Données

- Rien à changer côté serveur : `lastResult.shownHands` donne déjà, pour chaque siège montré, la catégorie et les
  5 cartes (`bestCards`). La précision « aux 7 par les rois » se déduit de `bestCards` côté client (fonction pure
  dans `lib/table.js`, testée : full, brelan, paires, couleur, quinte, carte haute).
- La pause après abattage reste à 8 s (`poker.pause-after-showdown`) ; à allonger à 10-12 s si la famille trouve
  que ça passe trop vite.

### Écarts décidés à l'implémentation (17 octobre)

Le placement « à côté du siège » ne tient pas dans 390 px au-delà de quelques adversaires (cartes de NO et NE qui
se chevauchent à 2 adversaires, qui tombent sur N à 3 et 5, qui touchent O1/E1 dès 4 ; à 7, la zone libre au-dessus
du tableau ne loge que 3 paires de cartes). Règle retenue avec la famille (`showdownStyle` dans `lib/table.js`) :

| Adversaires | Cartes montrées | Étiquette |
| --- | --- | --- |
| 1 ou 2 | 52 × 76 **sous** le siège (côté centre), avatar visible | nom complet, une ligne, 186 px max |
| 3 | 44 × 64 sous le siège ; NO et NE écartés de 14 px vers l'extérieur | nom complet, une ligne, 140 px max, 12 px |
| 4 à 7 | 44 × 64 **à la place de l'avatar** | nom court (« Double paire ») à la place du tapis ; nom complet pour le gagnant, aligné côté centre pour O1/O2/E1/E2 |

- Les étiquettes restent sur une ligne pour ne jamais toucher le tableau ni un voisin ; les noms les plus longs
  (« Full aux valets par les dames ») peuvent être coupés à 2 ou 3 adversaires, avec le nom complet en info-bulle.
- Les cartes des perdants ne passent pas à 45 % (elles doivent rester lisibles pour comprendre la main) ; seul le
  tableau garde la mise en avant de la main gagnante.
- Plusieurs gagnants : « Léo et Mamie se partagent 480 » sur la ligne sous le tableau.
- Vérifié sans chevauchement (mesure des rectangles) à 1, 2, 3, 5 et 7 adversaires sur une table de 390 × 500.

## V7 : réglages par défaut et pause d'abattage (17 octobre)

Demandes de la famille ; aucun écran nouveau.

### Pause après l'abattage : 8 s → 12 s

- `PokerConfig.java` : `@WithDefault("12s")` sur `pauseAfterShowdown()` (au lieu de `"8s"`), pour laisser le temps
  de lire les cartes et les étiquettes de la V6. La pause après un coucher (`pauseAfterFold`, 5 s) ne change pas.
- Vérifier qu'aucun `poker.pause-after-showdown` dans `application.properties` ou dans `k8s/` ne la surcharge
  (les tests peuvent garder leurs délais courts).
- Ramenée à **10 s** le 17 octobre, à la demande de la famille : 12 s, c'était trop long.

### Montée des blindes par défaut : toutes les 5 mains, +100 %

- Serveur, `TableSettings.DEFAULT` : `new TableSettings(10, 20, 1000, 5, 100)` (au lieu de `10, 50`).
- Écran de création de table, `Home.svelte` : `everyHands: 5`, `percent: 100` ; le champ « Hausse (%) » accepte
  déjà jusqu'à 200.
- Progression obtenue avec les blindes par défaut : 10/20, 20/40, 40/80, 80/160, puis 150/300 (et non 160/320 :
  160 n'est pas une valeur ronde, `BlindSchedule` arrondit à 150), 300/600… Vérifié par `TableSettingsTest`.
- Mettre à jour les tests qui s'appuient sur l'ancien défaut, et `docs/PLAN.md` (section Interface : « par défaut
  +100 % toutes les 5 mains »).

## Correctif : la table ne rétrécit plus à mon tour (17 octobre)

Constat : la ligne du curseur (44 px + 8 px d'écart) apparaissait au-dessus des boutons à chacun de mes tours. La
barre passait de 81 à 133 px, la table perdait 52 px de haut et, comme la taille des cartes du tableau et des sièges
suit cette hauteur (V3), tout rétrécissait puis regrandissait : sur Safari (iPhone 13), cartes du tableau 66 × 96 →
57 × 83 et sièges à 70 % ; sur Chrome, 82 × 119 → 73 × 107.

Choix de la famille : le curseur s'ouvre au toucher de « Relancer », **à la place** de la rangée de boutons
(même hauteur, 62 px) : « ✕ » (annule), le curseur, puis « Relancer + montant » qui envoie la relance. Deux touchers
pour relancer au lieu d'un ; la barre garde toujours 81 px et la table ne change plus de taille. Le curseur se
referme à chaque nouveau tour. Logique dans `opensSlider` (`lib/actions.js`).

