<script>
  import { onDestroy, untrack } from 'svelte';
  import ActionBar from './ActionBar.svelte';
  import Card from './Card.svelte';
  import MyPlace from './MyPlace.svelte';
  import Seat from './Seat.svelte';
  import Victory from './Victory.svelte';
  import { callPrompt } from '../lib/actions.js';
  import { TableConnection } from '../lib/connection.svelte.js';
  import { handName } from '../lib/hands.js';
  import {
    blindOf,
    dealDelay,
    levelText,
    nextHandDelayMs,
    rotateSeats,
    seatPosition,
    soundFor,
    totalPot,
    winningCards,
  } from '../lib/table.js';
  import { isMuted, play, setMuted } from '../lib/sounds.js';
  import { dropIn } from '../lib/transitions.js';

  let { code, profile, onExit } = $props();

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
  let menuOpen = $state(false);

  // Waiting room: before the first hand, after players left, but not after a victory (its own screen).
  const waiting = $derived(state !== null && state.table.status === 'waiting' && state.table.winnerSeat === null);
  // Between two hands: the result stays on the table until the next deal.
  const showingResult = $derived(!waiting && hand?.stage === 'complete' && state.lastResult !== null);
  const me = $derived(mySeat !== null ? state.seats[mySeat] : null);
  const inHand = $derived(handRunning && me !== null && (me.status === 'active' || me.status === 'allIn'));

  // Countdown before the next hand, from the moment the result arrived.
  let resultAt = $state(0);
  $effect(() => {
    if (state?.lastResult?.handNumber) {
      resultAt = performance.now();
    }
  });
  const nextHandMs = $derived(state?.lastResult ? nextHandDelayMs(state.lastResult) : 0);
  const nextHandLeft = $derived(Math.max(0, nextHandMs - (now - resultAt)));

  /** "Seb gagne 480" and the name of his hand, for each winner of the hand. */
  function results(result) {
    return result.payouts
      .flatMap((payout) => Object.entries(payout.winners))
      .reduce((merged, [seat, amount]) => {
        const existing = merged.find((r) => r.seat === Number(seat));
        if (existing) {
          existing.amount += amount;
        } else {
          const shown = result.shownHands[seat];
          merged.push({
            seat: Number(seat),
            name: state.seats[seat]?.nickname ?? `Siège ${Number(seat) + 1}`,
            amount,
            hand: shown ? handName(shown.category, shown.cards) : null,
          });
        }
        return merged;
      }, []);
  }

  function myWinnings(result) {
    return result?.payouts.reduce((sum, payout) => sum + (payout.winners[mySeat] ?? 0), 0) ?? 0;
  }

  function shownHandOf(seatNumber) {
    const shown = state.lastResult?.shownHands[seatNumber];
    return shown && !handRunning ? handName(shown.category, shown.cards) : null;
  }

  // Board length before the last update: the cards after it are the new ones to deal.
  // Not reactive on purpose: read when the new cards are created, updated after the render.
  const previousBoard = { length: 0 };
  $effect(() => {
    previousBoard.length = hand?.board.length ?? 0;
  });

  // Seat of the game winner while the celebration is on screen.
  let victorySeat = $state(null);
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
      setTimeout(() => (victorySeat = event.seat), 1500);
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
    const places = rotateSeats(state.seats, mySeat);
    const launched = payouts.flatMap((payout) =>
      Object.entries(payout.winners).map(([seat, amount]) => {
        const place = places.find((p) => p.number === Number(seat));
        return { id: `${Date.now()}-${seat}-${amount}`, amount, wait, ...seatPosition(place.position) };
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
      {#if state}
        <div class="subtitle">
          Blindes {state.table.smallBlind} / {state.table.bigBlind}{#if state.table.nextBlinds}{` · niveau ${state.table.blindLevel}`}{/if}
        </div>
      {/if}
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
    <div class="table">
      {#if connection.status !== 'open'}<p class="banner">Reconnexion…</p>{/if}

      <div class="felt">
        <div class="center">
          {#if waiting}
            <div class="seated">{seatedCount} joueur{seatedCount > 1 ? 's' : ''} assis</div>
            <div class="settings">
              Blindes {state.table.smallBlind} / {state.table.bigBlind} · tapis {state.table.startingStack}
            </div>
            <div class="settings">{levelText(state.table) ?? 'Blindes fixes'}</div>
            <button class="invite" onclick={share}>{copied ? '✓ Lien copié' : 'Inviter'}</button>
          {:else if hand}
            <div class="board">
              {#each [0, 1, 2, 3, 4] as i (i)}
                {#if hand.board[i]}
                  {#key hand.board[i]}
                    <span in:dropIn={{ delay: dealDelay(i, previousBoard.length) }}>
                      <Card
                        width={42}
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
            {#if showingResult}
              {#each results(state.lastResult) as result (result.seat)}
                <div class="winner-line">{result.name} gagne {result.amount}</div>
                {#if result.hand}<div class="winner-hand">{result.hand}</div>{/if}
              {/each}
            {:else}
              <div class="pot-label">POT</div>
              <div class="pot">{totalPot(state)}</div>
              {#if levelText(state.table)}<div class="level">{levelText(state.table)}</div>{/if}
              {#if callPrompt(state)}<div class="call-pill">{callPrompt(state)}</div>{/if}
            {/if}
          {/if}
        </div>
      </div>

      <!-- The other players around the oval; my own place is in the strip below. -->
      <div class="places">
        {#each rotateSeats(state.seats, mySeat) as place (place.number)}
          {#if place.number !== mySeat}
            {@const position = seatPosition(place.position)}
            <div class="place" style="left: {position.left}%; top: {position.top}%">
              {#if place.seat}
                <Seat
                  seat={place.seat}
                  position={place.position}
                  isButton={!waiting && hand?.buttonSeat === place.number}
                  blind={waiting ? null : blindOf(hand, place.number)}
                  {handRunning}
                  highlightCards={highlight}
                  handNumber={hand?.number ?? 0}
                  winner={winnerSeats.has(place.number)}
                  shownHand={shownHandOf(place.number)}
                  toAct={handRunning && hand.toActSeat === place.number}
                  remainingMs={hand?.toActSeat === place.number ? remainingMs : null}
                />
              {:else if mySeat === null}
                <button class="free" onclick={() => connection.sit(place.number)}>+ Libre</button>
              {:else if waiting}
                <span class="free">Libre</span>
              {/if}
            </div>
          {/if}
        {/each}

        {#each flights as flight (flight.id)}
          <div class="flight" style="--to-left: {flight.left}%; --to-top: {flight.top}%; animation-delay: {flight.wait}ms">
            +{flight.amount}
          </div>
        {/each}
      </div>
    </div>

    {#if me}
      <MyPlace
        seat={me}
        isButton={!waiting && hand?.buttonSeat === mySeat}
        blind={waiting ? null : blindOf(hand, mySeat)}
        {handRunning}
        highlightCards={highlight}
        handNumber={hand?.number ?? 0}
        toAct={myTurn}
        remainingMs={myTurn ? remainingMs : null}
        won={showingResult ? myWinnings(state.lastResult) : 0}
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
    {:else if showingResult}
      <div class="bottom next-hand">
        <span>Main suivante dans {Math.ceil(nextHandLeft / 1000)} s</span>
        <span class="progress"><span style="width: {(100 * (nextHandMs - nextHandLeft)) / nextHandMs}%"></span></span>
      </div>
    {:else if inHand}
      <ActionBar
        allowed={state.allowedActions}
        stack={me.stack}
        onAct={(action, amount) => connection.act(action, amount)}
      />
    {/if}

    {#if victorySeat !== null && state.seats[victorySeat]}
      <Victory
        seat={state.seats[victorySeat]}
        others={state.seats.filter((seat, n) => seat && n !== victorySeat)}
        onClose={() => (victorySeat = null)}
        onRestart={state.you.isHost ? () => connection.restart() : null}
        {onExit}
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
  }
  .felt {
    position: absolute;
    inset: 44px 34px 30px;
    border-radius: 50%;
    background: radial-gradient(#1f7a4a, #0f4a2c);
    border: 10px solid #5b3a20;
    display: grid;
    place-items: center;
  }
  .center {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;
    max-width: 92%;
    text-align: center;
  }
  .board {
    display: flex;
    gap: 4px;
    margin-bottom: 6px;
  }
  .slot-empty {
    width: 42px;
    aspect-ratio: 167 / 243;
    border-radius: 6%;
    border: 1.5px dashed rgb(255 255 255 / 0.3);
    box-sizing: border-box;
  }
  .pot-label {
    font-size: 12px;
    letter-spacing: 0.15em;
    opacity: 0.8;
  }
  .pot {
    font-size: 32px;
    font-weight: 700;
    color: #ffd23f;
    line-height: 1;
  }
  .level,
  .settings {
    font-size: 13px;
    opacity: 0.7;
  }
  .call-pill {
    margin-top: 6px;
    padding: 4px 12px;
    border-radius: 14px;
    border: 1.5px solid #ffd23f;
    color: #ffd23f;
    font-size: 14px;
    font-weight: 600;
  }
  .winner-line {
    font-size: 22px;
    font-weight: 700;
    color: #ffd23f;
  }
  .winner-hand {
    font-size: 15px;
  }
  .seated {
    font-size: 18px;
    font-weight: 700;
  }
  .invite {
    margin-top: 8px;
    min-height: 44px;
    padding: 0 24px;
    background: #2b5a41;
  }

  /* Seats sit on the rim of the felt; this box keeps the outer ones on screen. */
  .places {
    position: absolute;
    inset: 46px 38px 34px;
    pointer-events: none;
  }
  .place {
    position: absolute;
    transform: translate(-50%, -50%);
    pointer-events: auto;
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
  .next-hand {
    font-size: 15px;
  }
  .progress {
    width: 100%;
    height: 6px;
    border-radius: 3px;
    background: rgb(255 255 255 / 0.15);
    overflow: hidden;
  }
  .progress span {
    display: block;
    height: 100%;
    background: #ffd23f;
    transition: width 0.25s linear;
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
    top: 50%;
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
      top: 50%;
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
