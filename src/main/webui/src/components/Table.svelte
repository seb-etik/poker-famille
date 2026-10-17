<script>
  import { onDestroy, untrack } from 'svelte';
  import ActionBar from './ActionBar.svelte';
  import Card from './Card.svelte';
  import MyPlace from './MyPlace.svelte';
  import Seat from './Seat.svelte';
  import Victory from './Victory.svelte';
  import { myTurnText } from '../lib/actions.js';
  import { TableConnection } from '../lib/connection.svelte.js';
  import { handName } from '../lib/hands.js';
  import { seatLayout } from '../lib/layout.js';
  import {
    blindOf,
    categoryLabel,
    dealDelay,
    headerBlindsText,
    showdownStyle,
    soundFor,
    totalPot,
    winnersLine,
    winningCards,
  } from '../lib/table.js';
  import { isMuted, play, setMuted } from '../lib/sounds.js';
  import { dropIn, toCenter } from '../lib/transitions.js';

  let { code, profile, onExit, onLeaderboard } = $props();

  // One connection for the life of this screen: App recreates it when the code changes.
  const connection = untrack(() => new TableConnection(code, $state.snapshot(profile)));
  onDestroy(() => connection.close());

  // Ticks the action countdown between two states.
  let now = $state(performance.now());
  const ticker = setInterval(() => (now = performance.now()), 250);
  onDestroy(() => clearInterval(ticker));

  const state = $derived(connection.state);
  const mySeat = $derived(state?.you.seat ?? null);
  const hand = $derived(state?.hand ?? null);
  const handRunning = $derived(hand !== null && hand.stage !== 'complete');
  const seatedCount = $derived(state ? state.seats.filter(Boolean).length : 0);
  // The server refuses to start with fewer than two players who still have chips.
  const playersWithChips = $derived(state ? state.seats.filter((seat) => seat && seat.stack > 0).length : 0);
  const remainingMs = $derived(
    hand?.remainingMs == null ? null : Math.max(0, hand.remainingMs - (now - connection.receivedAt)),
  );
  const highlight = $derived(state ? winningCards(state) : []);
  const shareUrl = $derived(`${location.origin}/table/${code}`);

  let copied = $state(false);
  // Height of the table box: seats shrink with it below the 500 px it was designed for (Safari's bars).
  let boxHeight = $state(500);
  const seatScale = $derived(Math.min(1, boxHeight / 500));
  let menuOpen = $state(false);

  // Waiting room: before the first hand, after players left, but not after a victory (its own screen).
  const waiting = $derived(state !== null && state.table.status === 'waiting' && state.table.winnerSeat === null);
  // Between two hands: the result stays on the table until the next deal.
  const showingResult = $derived(!waiting && hand?.stage === 'complete' && state.lastResult !== null);
  const me = $derived(mySeat !== null ? state.seats[mySeat] : null);
  // The action bar stays on screen for a seated player during the game, greyed when it is not my turn.
  const showActions = $derived(!waiting && me !== null && state.table.winnerSeat === null && hand !== null);

  // Who sits where on screen depends on how many opponents there are (lib/layout.js). During a game the list
  // is frozen at each new hand: nobody moves when a player sits down or leaves in the middle of a hand.
  let dealtSeats = $state([]);
  $effect(() => {
    hand?.number;
    dealtSeats = untrack(() => occupiedSeats());
  });
  function occupiedSeats() {
    return state ? state.seats.flatMap((seat, number) => (seat && number !== mySeat ? [number] : [])) : [];
  }
  const layout = $derived.by(() => {
    if (!state) {
      return [];
    }
    // While waiting, every place is shown, free ones included.
    const numbers = waiting ? [0, 1, 2, 3, 4, 5, 6, 7].filter((n) => n !== mySeat) : dealtSeats;
    return seatLayout(numbers, mySeat);
  });

  function myWinnings(result) {
    return result?.payouts.reduce((sum, payout) => sum + (payout.winners[mySeat] ?? 0), 0) ?? 0;
  }

  // Showdown (V6): cards in large under the seats while there is room, in place of the avatars beyond.
  const showdown = $derived(showdownStyle(dealtSeats.length));

  /** The hand a seat showed: in full ("Full aux 7 par les rois"), or short on a crowded table except for winners. */
  function handLabelOf(seatNumber) {
    const shown = state.lastResult?.shownHands[seatNumber];
    if (!shown || handRunning) {
      return null;
    }
    const full = showdown.fullNames || winnerSeats.has(seatNumber);
    return full ? handName(shown.category, shown.cards) : categoryLabel(shown.category);
  }

  // Board length before the last update: the cards after it are the new ones to deal.
  // Not reactive on purpose: read when the new cards are created, updated after the render.
  const previousBoard = { length: 0 };
  $effect(() => {
    previousBoard.length = hand?.board.length ?? 0;
  });

  // Seat of the game winner while the celebration is on screen.
  let victorySeat = $state(null);
  // The winner's standing in the leaderboard of the month, sent with gameWon (null if the game did not count).
  let victoryEvent = $state(null);
  // A new game has started: the celebration is over.
  $effect(() => {
    if (state?.table.winnerSeat === null) {
      victorySeat = null;
    }
  });

  // The pot flies to the winners once the board is fully dealt.
  let flights = $state([]);
  let streetsInBatch = 0;
  const unsubscribe = connection.onEvent((event) => {
    const sound = soundFor(event);
    if (sound) {
      play(sound);
    }
    if (event.name === 'gameWon') {
      // Shown once the pot has flown to the winner.
      setTimeout(() => {
        victoryEvent = event;
        victorySeat = event.seat;
      }, 1500);
    }
    if (event.name === 'board') {
      streetsInBatch++;
    } else if (event.name === 'win') {
      const wait = 400 + Math.max(0, streetsInBatch - 1) * 1000;
      launchFlights(event.payouts, wait);
      streetsInBatch = 0;
    } else {
      streetsInBatch = 0;
    }
  });
  onDestroy(unsubscribe);

  function launchFlights(payouts, wait) {
    const launched = payouts.flatMap((payout) =>
      Object.entries(payout.winners).map(([seat, amount]) => {
        // My own winnings fly down to my strip.
        const place = layout.find((p) => p.number === Number(seat)) ?? { left: 50, top: 100 };
        return { id: `${Date.now()}-${seat}-${amount}`, amount, wait, left: place.left, top: place.top };
      }),
    );
    flights = [...flights, ...launched];
    setTimeout(() => (flights = flights.filter((f) => !launched.includes(f))), wait + 1200);
  }

  let muted = $state(isMuted());

  function toggleSound() {
    muted = !muted;
    setMuted(muted);
  }

  // A chime when it becomes my turn.
  const myTurn = $derived(handRunning && mySeat !== null && hand.toActSeat === mySeat);
  const turnText = $derived(
    myTurn && remainingMs !== null ? myTurnText(state.allowedActions, Math.ceil(remainingMs / 1000)) : null,
  );
  $effect(() => {
    if (myTurn) {
      play('turn');
    }
  });

  const winnerSeats = $derived(
    new Set(state?.lastResult?.payouts.flatMap((payout) => Object.keys(payout.winners).map(Number)) ?? []),
  );

  async function share() {
    try {
      if (navigator.share) {
        await navigator.share({ title: 'Poker en famille', text: `Viens jouer ! Code ${code}`, url: shareUrl });
      } else {
        await navigator.clipboard.writeText(shareUrl);
        copied = true;
        setTimeout(() => (copied = false), 2000);
      }
    } catch {
      // Share dialog dismissed: nothing to do.
    }
  }

</script>

<div class="screen">
  <header>
    <button class="icon" onclick={onExit} aria-label="Retour à l'accueil">‹</button>
    <div class="title">
      <div class="code">TABLE {code}</div>
      {#if state}<div class="subtitle">{headerBlindsText(state.table)}</div>{/if}
    </div>
    <button class="icon" onclick={() => (menuOpen = !menuOpen)} aria-label="Menu" aria-expanded={menuOpen}>⋯</button>
    {#if menuOpen}
      <div class="menu" role="menu">
        <button role="menuitem" onclick={() => { share(); menuOpen = false; }}>{copied ? '✓ Lien copié' : 'Inviter (partager le lien)'}</button>
        <button role="menuitem" onclick={toggleSound}>{muted ? '🔇 Son coupé' : '🔊 Son activé'}</button>
        {#if mySeat !== null}
          <button role="menuitem" onclick={() => { connection.leave(); menuOpen = false; }}>Se lever</button>
        {/if}
      </div>
    {/if}
  </header>

  {#if connection.status === 'notFound'}
    <p class="notice">Cette table n'existe pas (ou plus).</p>
  {:else if !state}
    <p class="notice">Connexion…</p>
  {:else}
    <!-- The table box: coordinates of lib/layout.js are percentages of it (designed for 390 × 500). -->
    <div class="table" bind:clientHeight={boxHeight} style="--seat-scale: {seatScale}">
      {#if connection.status !== 'open'}<p class="banner">Reconnexion…</p>{/if}

      <div class="felt"></div>

      {#if waiting}
        <div class="waiting">
          <div class="seated">{seatedCount} joueur{seatedCount > 1 ? 's' : ''} assis</div>
          <div class="settings">Blindes {state.table.smallBlind} / {state.table.bigBlind} · tapis {state.table.startingStack}</div>
          <button class="invite" onclick={share}>{copied ? '✓ Lien copié' : 'Inviter'}</button>
        </div>
      {:else if hand}
        <!-- Reserved band (280 to 373 px of the box): nothing else goes there. -->
        <div class="board">
          {#each [0, 1, 2, 3, 4] as i (i)}
            {#if hand.board[i]}
              {#key hand.board[i]}
                <span in:dropIn={{ delay: dealDelay(i, previousBoard.length) }}>
                  <Card
                    width="var(--board-card)"
                    code={hand.board[i]}
                    highlight={highlight.includes(hand.board[i])}
                    dim={highlight.length > 0 && !highlight.includes(hand.board[i])}
                  />
                </span>
              {/key}
            {:else}
              <span class="slot-empty"></span>
            {/if}
          {/each}
        </div>

        <!-- Under the board: the pot, or the winners once the hand is over. -->
        <div class="under-board">
          {#if showingResult}
            <!-- One line: the name of each hand is on its label. -->
            <div class="winner-line">{winnersLine(state.lastResult, state.seats)}</div>
          {:else}
            <div class="pot"><span class="chip"></span>{totalPot(state)}</div>
          {/if}
        </div>
      {/if}

      {#each layout as place (place.number)}
        {@const seat = state.seats[place.number]}
        <div class="place" style="left: {place.left}%; top: {place.top}%">
          {#if seat}
            <Seat
              {seat}
              isButton={!waiting && hand?.buttonSeat === place.number}
              blind={waiting ? null : blindOf(hand, place.number)}
              {handRunning}
              handNumber={hand?.number ?? 0}
              winner={showingResult && winnerSeats.has(place.number)}
              handLabel={handLabelOf(place.number)}
              placement={showdown.placement}
              showdownWidth={showdown.width}
              labelWidth={showdown.labelWidth}
              nudge={place.slot === 'NO' ? -showdown.nudge : place.slot === 'NE' ? showdown.nudge : 0}
              labelAlign={place.slot.startsWith('O') ? 'start' : place.slot.startsWith('E') ? 'end' : 'center'}
              toAct={handRunning && hand.toActSeat === place.number}
              remainingMs={hand?.toActSeat === place.number ? remainingMs : null}
            />
          {:else if waiting && mySeat === null}
            <button class="free" onclick={() => connection.sit(place.number)}>+ Libre</button>
          {:else if waiting}
            <span class="free">Libre</span>
          {/if}
        </div>
        {#if seat && seat.bet > 0}
          <span class="bet" style="left: {place.betLeft}%; top: {place.betTop}%" out:toCenter>
            <span class="chip"></span>{seat.bet}
          </span>
        {/if}
      {/each}

      {#each flights as flight (flight.id)}
        <div class="flight" style="--to-left: {flight.left}%; --to-top: {flight.top}%; animation-delay: {flight.wait}ms">
          +{flight.amount}
        </div>
      {/each}
    </div>

    {#if me}
      <MyPlace
        seat={me}
        isButton={!waiting && hand?.buttonSeat === mySeat}
        blind={waiting ? null : blindOf(hand, mySeat)}
        {handRunning}
        handNumber={hand?.number ?? 0}
        toAct={myTurn}
        remainingMs={myTurn ? remainingMs : null}
        {turnText}
        won={showingResult ? myWinnings(state.lastResult) : 0}
        handLabel={mySeat !== null ? handLabelOf(mySeat) : null}
        onLeave={waiting ? () => connection.leave() : null}
      />
    {/if}

    <!-- The bottom of the screen: what I can do now. -->
    {#if state.table.winnerSeat !== null && state.seats[state.table.winnerSeat]}
      <div class="bottom champion">
        🏆 {state.seats[state.table.winnerSeat].nickname} a gagné la partie
        {#if state.you.isHost}<button onclick={() => connection.restart()}>Rejouer</button>{/if}
      </div>
    {:else if waiting}
      <div class="bottom">
        {#if state.you.isHost}
          <button class="start" disabled={playersWithChips < 2} onclick={() => connection.start()}>Lancer la partie</button>
          {#if playersWithChips < 2}<p class="hint">Il faut au moins deux joueurs assis.</p>{/if}
        {:else if mySeat === null}
          <p class="hint">Choisis une place libre.</p>
        {:else}
          <p class="hint">En attente de l'hôte pour lancer la partie…</p>
        {/if}
      </div>
    {:else if showActions}
      <ActionBar
        allowed={myTurn ? state.allowedActions : null}
        stack={me.stack}
        bigBlind={state.table.bigBlind}
        onAct={(action, amount) => connection.act(action, amount)}
      />
    {/if}

    {#if victorySeat !== null && state.seats[victorySeat]}
      <Victory
        seat={state.seats[victorySeat]}
        others={state.seats.filter((seat, n) => seat && n !== victorySeat)}
        event={victoryEvent}
        {onLeaderboard}
        onClose={() => (victorySeat = null)}
        onRestart={state.you.isHost ? () => connection.restart() : null}
      />
    {/if}

    {#if connection.error}<p class="error">{connection.error}</p>{/if}
  {/if}
</div>

<style>
  /* One screen, no scrolling: header, table (takes what is left), my place, the bottom bar. */
  .screen {
    display: flex;
    flex-direction: column;
    height: 100dvh;
    overflow: hidden;
    background: #0b1a12;
  }

  header {
    position: relative;
    display: flex;
    align-items: center;
    gap: 8px;
    padding: calc(6px + env(safe-area-inset-top)) 8px 6px;
    z-index: 3;
  }
  .icon {
    width: 44px;
    height: 44px;
    padding: 0;
    font-size: 26px;
    line-height: 1;
    background: none;
  }
  .title {
    flex: 1;
    min-width: 0;
    text-align: center;
  }
  .code {
    font-weight: 700;
    letter-spacing: 0.15em;
  }
  .subtitle {
    font-size: 13px;
    opacity: 0.75;
  }
  .menu {
    position: absolute;
    top: calc(52px + env(safe-area-inset-top));
    right: 8px;
    z-index: 5;
    display: flex;
    flex-direction: column;
    min-width: 220px;
    padding: 6px;
    border-radius: 12px;
    background: #10261b;
    border: 1px solid #2b5a41;
    box-shadow: 0 8px 24px rgb(0 0 0 / 0.5);
  }
  .menu button {
    min-height: 44px;
    text-align: left;
    background: none;
  }

  .table {
    position: relative;
    flex: 1;
    min-height: 0;
    width: 100%;
    max-width: 32rem;
    margin: 0 auto;
    /* Board cards follow the width of the table: five cards minus four overlaps fill it, 18 px from each side
       (82 × 119 at 390 px wide). The cqh term only applies to a table less than 500 px high. */
    container-type: size;
    --board-card: min(82px, (100cqw + 20px) / 5, 16.4cqh);
  }
  /* The oval is wider than the screen: 18 px out on each side, the rail cut by the edges. */
  .felt {
    position: absolute;
    top: 6%;
    bottom: 2%;
    left: -18px;
    right: -18px;
    border-radius: 50%;
    background: radial-gradient(#1f7a4a, #0f4a2c);
    border: 10px solid #5b3a20;
  }

  .board {
    position: absolute;
    top: 53.2%;
    left: 50%;
    transform: translateX(-50%);
    display: flex;
  }
  /* No line box under the cards: the board ends where its cards end. */
  .board > * {
    line-height: 0;
  }
  /* The cards overlap by 17 % of their width; the left corner (rank and suit) stays visible. */
  .board > * + * {
    margin-left: calc(var(--board-card) * -0.17);
  }
  .board :global(img) {
    box-shadow: -3px 0 6px rgb(0 0 0 / 0.35);
  }
  .slot-empty {
    width: var(--board-card);
    aspect-ratio: 167 / 243;
    border-radius: 6%;
    border: 1.5px dashed rgb(255 255 255 / 0.3);
    box-sizing: border-box;
  }
  /* As wide as the free space between the low side seats (O2, E2). */
  .under-board {
    position: absolute;
    top: calc(53.2% + var(--board-card) * 243 / 167 + 11px);
    left: 50%;
    transform: translateX(-50%);
    width: 54%;
    text-align: center;
    line-height: 1.2;
  }
  .pot {
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 6px;
    font-size: 28px;
    font-weight: 700;
    color: #ffd23f;
    line-height: 1;
  }
  .winner-line {
    font-size: 22px;
    font-weight: 700;
    color: #ffd23f;
  }
  .waiting {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 6px;
    text-align: center;
  }
  .seated {
    font-size: 18px;
    font-weight: 700;
  }
  .settings {
    font-size: 13px;
    opacity: 0.75;
  }
  .invite {
    margin-top: 6px;
    min-height: 44px;
    padding: 0 24px;
    background: #2b5a41;
  }

  /* Anchored on the avatar's centre (26 px below the top of the seat), shrunk with a short box. */
  .place {
    position: absolute;
    transform: translate(-50%, calc(-26px * var(--seat-scale))) scale(var(--seat-scale));
    transform-origin: 50% 0;
  }
  .free {
    display: grid;
    place-items: center;
    width: 52px;
    height: 52px;
    padding: 0;
    border-radius: 50%;
    border: 2px dashed rgb(255 255 255 / 0.4);
    background: rgb(0 0 0 / 0.25);
    font-size: 11px;
    color: #fff;
  }
  .bet {
    position: absolute;
    transform: translate(-50%, -50%);
    display: flex;
    align-items: center;
    gap: 3px;
    font-size: 13px;
    font-weight: 700;
    white-space: nowrap;
  }
  .chip {
    width: 14px;
    height: 14px;
    border-radius: 50%;
    background: #ffd23f;
    border: 2px dashed #b8860b;
    box-sizing: border-box;
  }
  .pot .chip {
    width: 20px;
    height: 20px;
  }

  .bottom {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 6px;
    padding: 10px 12px calc(12px + env(safe-area-inset-bottom));
    background: #10261b;
    border-top: 1px solid #2b5a41;
  }
  .start {
    width: 100%;
    min-height: 56px;
    font-size: 18px;
    font-weight: 700;
    background: #b8860b;
  }
  .hint {
    margin: 0;
    font-size: 14px;
    opacity: 0.8;
  }
  .champion {
    flex-direction: row;
    justify-content: center;
    gap: 12px;
    font-weight: 700;
    color: #000;
    background: #ffd23f;
  }
  .champion button {
    min-height: 44px;
    padding: 0 18px;
    background: #0f4a2c;
    color: #fff;
    font-weight: 700;
  }

  .flight {
    position: absolute;
    left: 50%;
    top: 70%;
    transform: translate(-50%, -50%);
    padding: 2px 8px;
    border-radius: 12px;
    background: #ffd23f;
    color: #000;
    font-weight: 700;
    opacity: 0;
    pointer-events: none;
    animation: flight 1s ease-in-out forwards;
  }
  @keyframes flight {
    0% {
      left: 50%;
      top: 70%;
      opacity: 1;
    }
    75% {
      opacity: 1;
    }
    100% {
      left: var(--to-left);
      top: var(--to-top);
      opacity: 0;
    }
  }
  @media (prefers-reduced-motion: reduce) {
    .flight {
      display: none;
    }
  }

  .notice {
    text-align: center;
  }
  .banner {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    margin: 0;
    padding: 4px;
    background: #7a5a10;
    text-align: center;
    z-index: 2;
  }
  .error {
    position: fixed;
    top: 64px;
    left: 16px;
    right: 16px;
    padding: 12px;
    border-radius: 8px;
    background: #7a2230;
    text-align: center;
    z-index: 6;
  }
</style>
