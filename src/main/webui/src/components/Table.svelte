<script>
  import { onDestroy, untrack } from 'svelte';
  import ActionBar from './ActionBar.svelte';
  import Card from './Card.svelte';
  import Seat from './Seat.svelte';
  import Victory from './Victory.svelte';
  import { TableConnection } from '../lib/connection.svelte.js';
  import { blindOf, categoryLabel, dealDelay, nextBlindsText, rotateSeats, seatPosition, soundFor, totalPot, winningCards } from '../lib/table.js';
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

  // Board length before the last update: the cards after it are the new ones to deal.
  // Not reactive on purpose: read when the new cards are created, updated after the render.
  const previousBoard = { length: 0 };
  $effect(() => {
    previousBoard.length = hand?.board.length ?? 0;
  });

  // Seat of the game winner while the celebration is on screen.
  let victorySeat = $state(null);

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

  function winnersText(result) {
    return result.payouts
      .flatMap((payout) => Object.entries(payout.winners))
      .map(([seat, amount]) => {
        const shown = result.shownHands[seat];
        const name = state.seats[seat]?.nickname ?? `Siège ${Number(seat) + 1}`;
        return `${name} gagne ${amount}${shown ? ` (${categoryLabel(shown.category)})` : ''}`;
      })
      .join(' · ');
  }
</script>

<div class="screen">
  <header>
    <button class="link" onclick={onExit}>← Accueil</button>
    <button class="code" onclick={share}>Table {code} {copied ? '✓ lien copié' : '⤴'}</button>
    <span class="tools">
      <button class="link" onclick={toggleSound} aria-label={muted ? 'Activer le son' : 'Couper le son'}>
        {muted ? '🔇' : '🔊'}
      </button>
      {#if mySeat !== null}
        <button class="link" onclick={() => connection.leave()}>Se lever</button>
      {/if}
    </span>
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
          {#if hand}
            <div class="board">
              {#each hand.board as card, i (card)}
                <span in:dropIn={{ delay: dealDelay(i, previousBoard.length) }}>
                  <Card code={card} highlight={highlight.includes(card)} dim={highlight.length > 0 && !highlight.includes(card)} />
                </span>
              {/each}
            </div>
            <div class="pot">Pot : {totalPot(state)}</div>
          {:else}
            <div class="pot">
              {seatedCount < 2 ? 'Choisissez vos places' : state.you.isHost ? '' : "En attente de l'hôte…"}
            </div>
          {/if}
          <div class="blinds">
            Blindes {state.table.smallBlind} / {state.table.bigBlind}{#if state.table.nextBlinds}{` · niveau ${state.table.blindLevel}`}{/if}
          </div>
          {#if nextBlindsText(state.table)}
            <div class="blinds">Ensuite {nextBlindsText(state.table)}</div>
          {/if}
          {#if state.lastResult}
            <div class="result">{winnersText(state.lastResult)}</div>
          {:else if connection.log[0]}
            <div class="last-action">{connection.log[0]}</div>
          {/if}
          {#if state.table.status === 'waiting' && state.you.isHost && playersWithChips >= 2}
            <button class="primary" onclick={() => connection.start()}>Lancer la partie</button>
          {/if}
        </div>
      </div>

      <!-- The other players around the oval; my own seat is in the strip below. -->
      <div class="places">
        {#each rotateSeats(state.seats, mySeat) as place (place.number)}
          {#if place.number !== mySeat}
            {@const position = seatPosition(place.position)}
            <div class="place" style="left: {position.left}%; top: {position.top}%">
              {#if place.seat}
                <Seat
                  seat={place.seat}
                  isButton={hand?.buttonSeat === place.number}
                  blind={blindOf(hand, place.number)}
                  {handRunning}
                  highlightCards={highlight}
                  handNumber={hand?.number ?? 0}
                  position={place.position}
                  winner={winnerSeats.has(place.number)}
                  toAct={handRunning && hand.toActSeat === place.number}
                  remainingMs={hand?.toActSeat === place.number ? remainingMs : null}
                />
              {:else if mySeat === null}
                <button class="sit" onclick={() => connection.sit(place.number)}>S'asseoir</button>
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

    {#if mySeat !== null && state.seats[mySeat]}
      <div class="me">
        <Seat
          variant="me"
          seat={state.seats[mySeat]}
          isButton={hand?.buttonSeat === mySeat}
          blind={blindOf(hand, mySeat)}
          {handRunning}
          highlightCards={highlight}
          handNumber={hand?.number ?? 0}
          winner={winnerSeats.has(mySeat)}
          toAct={myTurn}
          remainingMs={myTurn ? remainingMs : null}
        />
      </div>
    {/if}

    {#if state.table.winnerSeat !== null && state.seats[state.table.winnerSeat]}
      <p class="champion">🏆 {state.seats[state.table.winnerSeat].nickname} a gagné la partie</p>
    {/if}

    {#if victorySeat !== null && state.seats[victorySeat]}
      <Victory seat={state.seats[victorySeat]} onClose={() => (victorySeat = null)} />
    {/if}

    {#if connection.error}<p class="error">{connection.error}</p>{/if}
  {/if}

  {#if state?.allowedActions}
    <ActionBar {state} onAct={(action, amount) => connection.act(action, amount)} />
  {/if}
</div>

<style>
  /* One screen, no scrolling: header, table (takes what is left), my strip, actions. */
  .screen {
    display: flex;
    flex-direction: column;
    height: 100dvh;
    overflow: hidden;
  }
  header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: calc(0.25rem + env(safe-area-inset-top)) 0.5rem 0.25rem;
  }
  .tools {
    display: flex;
    gap: 0.25rem;
  }
  .link {
    background: none;
    padding: 0.3rem;
  }
  .code {
    font-weight: 700;
    letter-spacing: 0.1em;
    padding: 0.4rem 0.7rem;
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
    inset: 2.6rem 2.4rem 1.8rem;
    border-radius: 50%;
    background: radial-gradient(#1f7a4a, #0f4a2c);
    border: 0.5rem solid #5a3a1f;
    display: grid;
    place-items: center;
  }
  .center {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 0.35rem;
    max-width: 90%;
    text-align: center;
  }
  .board {
    display: flex;
    gap: 0.2rem;
    min-height: 3.9rem;
  }
  .pot {
    font-weight: 700;
    color: #fff;
  }
  .blinds {
    font-size: 0.75rem;
    opacity: 0.75;
  }
  .result {
    font-weight: 700;
    color: #ffd23f;
    font-size: 0.85rem;
  }
  .last-action {
    font-size: 0.8rem;
    opacity: 0.85;
  }
  /* Seats sit on the rim of the felt; this box keeps the outer ones on screen. */
  .places {
    position: absolute;
    inset: 2.3rem 2.9rem 1.4rem;
    pointer-events: none;
  }
  .place {
    position: absolute;
    transform: translate(-50%, -50%);
    pointer-events: auto;
  }
  .me {
    padding: 0.5rem 1rem;
    border-top: 1px solid #2b4d3b;
    background: #0e2218;
  }
  .flight {
    position: absolute;
    left: 50%;
    top: 50%;
    transform: translate(-50%, -50%);
    padding: 0.15rem 0.5rem;
    border-radius: 1rem;
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
  .sit {
    font-size: 0.75rem;
    padding: 0.4rem 0.6rem;
    background: rgb(255 255 255 / 0.15);
  }
  .primary {
    background: #b8860b;
    min-height: 2.6rem;
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
    padding: 0.25rem;
    background: #7a5a10;
    text-align: center;
    z-index: 2;
  }
  .champion {
    margin: 0;
    padding: 0.4rem;
    text-align: center;
    font-weight: 700;
    color: #000;
    background: #ffd23f;
  }
  .error {
    position: fixed;
    top: 3rem;
    left: 1rem;
    right: 1rem;
    padding: 0.75rem;
    border-radius: 0.5rem;
    background: #7a2230;
    text-align: center;
    z-index: 3;
  }
</style>
