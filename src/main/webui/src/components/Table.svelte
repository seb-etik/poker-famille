<script>
  import { onDestroy, untrack } from 'svelte';
  import ActionBar from './ActionBar.svelte';
  import Card from './Card.svelte';
  import Seat from './Seat.svelte';
  import { TableConnection } from '../lib/connection.svelte.js';
  import { categoryLabel, rotateSeats, seatPosition, totalPot } from '../lib/table.js';

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
  const shareUrl = $derived(`${location.origin}/table/${code}`);

  let copied = $state(false);

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
    {#if mySeat !== null}
      <button class="link" onclick={() => connection.leave()}>Se lever</button>
    {:else}
      <span></span>
    {/if}
  </header>

  {#if connection.status === 'notFound'}
    <p class="notice">Cette table n'existe pas (ou plus).</p>
  {:else if !state}
    <p class="notice">Connexion…</p>
  {:else}
    {#if connection.status !== 'open'}<p class="banner">Reconnexion…</p>{/if}

    <div class="table">
      <div class="felt">
        <div class="center">
          {#if hand}
            <div class="board">
              {#each hand.board as card (card)}<Card code={card} />{/each}
            </div>
            <div class="pot">Pot : {totalPot(state)}</div>
          {:else}
            <div class="pot">
              {seatedCount < 2 ? 'Choisissez vos places' : state.you.isHost ? '' : "En attente de l'hôte…"}
            </div>
          {/if}
          {#if state.table.status === 'waiting' && state.you.isHost && playersWithChips >= 2}
            <button class="primary" onclick={() => connection.start()}>Lancer la partie</button>
          {/if}
        </div>
      </div>

      {#each rotateSeats(state.seats, mySeat) as place (place.number)}
        {@const position = seatPosition(place.position)}
        <div class="place" style="left: {position.left}%; top: {position.top}%">
          {#if place.seat}
            <Seat
              seat={place.seat}
              isMe={place.number === mySeat}
              isButton={hand?.buttonSeat === place.number}
              {handRunning}
              toAct={handRunning && hand.toActSeat === place.number}
              remainingMs={hand?.toActSeat === place.number ? remainingMs : null}
            />
          {:else if mySeat === null}
            <button class="sit" onclick={() => connection.sit(place.number)}>S'asseoir</button>
          {/if}
        </div>
      {/each}
    </div>

    {#if state.lastResult}
      <p class="result">{winnersText(state.lastResult)}</p>
    {/if}

    <ul class="log">
      {#each connection.log as line, i (i)}<li>{line}</li>{/each}
    </ul>

    {#if connection.error}<p class="error">{connection.error}</p>{/if}
  {/if}

  {#if state?.allowedActions}
    <ActionBar {state} onAct={(action, amount) => connection.act(action, amount)} />
  {/if}
</div>

<style>
  .screen {
    display: flex;
    flex-direction: column;
    min-height: 100dvh;
  }
  header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0.5rem 1rem;
  }
  .link {
    background: none;
    padding: 0.3rem;
  }
  .code {
    font-weight: 700;
    letter-spacing: 0.1em;
  }
  .table {
    position: relative;
    width: min(100%, 30rem, calc((100dvh - 16rem) * 0.75));
    margin: 0 auto;
    aspect-ratio: 3 / 4;
  }
  .felt {
    position: absolute;
    inset: 14% 12%;
    border-radius: 50%;
    background: radial-gradient(#1f7a4a, #0f4a2c);
    border: 0.6rem solid #5a3a1f;
    display: grid;
    place-items: center;
  }
  .center {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 0.5rem;
  }
  .board {
    display: flex;
    gap: 0.25rem;
    min-height: 3.4rem;
  }
  .pot {
    font-weight: 700;
    color: #fff;
    text-align: center;
  }
  .place {
    position: absolute;
    transform: translate(-50%, -50%);
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
  .notice,
  .banner {
    text-align: center;
  }
  .banner {
    background: #7a5a10;
    margin: 0;
    padding: 0.25rem;
  }
  .result {
    text-align: center;
    font-weight: 700;
    color: #ffd23f;
    margin: 0.25rem 1rem;
  }
  .log {
    list-style: none;
    margin: 0 auto;
    padding: 0 1rem;
    max-width: 30rem;
    font-size: 0.85rem;
    opacity: 0.8;
    flex: 1;
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
  }
</style>
