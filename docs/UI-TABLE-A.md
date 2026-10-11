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
- Ligne du curseur (44 px) : « Relance », le curseur, la valeur. Affichée seulement si relancer est possible et
  `maxAmount > minAmount`.
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
