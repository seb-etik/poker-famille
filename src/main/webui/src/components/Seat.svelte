<script>
  import Card from './Card.svelte';
  import Avatar from './Avatar.svelte';
  import { fromCenter, toCenter } from '../lib/transitions.js';

  let { seat, isMe = false, isButton = false, handRunning = false, toAct = false, remainingMs = null, actionSeconds = 30, highlightCards = [], handNumber = 0, position = 0, winner = false } = $props();

  // Bets sit between the player and the centre of the table, like chips pushed forward.
  const angle = $derived(Math.PI / 2 + (position * Math.PI) / 4);
  // Other seats have their cards below the avatar: their bet goes a little further.
  const betDistance = $derived(isMe ? 3.4 : 4.8);
  const betStyle = $derived(
    `left: calc(50% - ${Math.cos(angle) * betDistance}rem); top: calc(50% - ${Math.sin(angle) * betDistance}rem)`,
  );

  const progress = $derived(remainingMs == null ? 0 : Math.min(1, remainingMs / (actionSeconds * 1000)));
</script>

<div
  class="seat"
  class:me={isMe}
  class:folded={seat.status === 'folded'}
  class:away={!seat.connected}
  class:to-act={toAct}
  class:winner
  style="--progress: {progress}"
>
  <div class="identity">
    <div class="avatar">
      <Avatar id={seat.avatar} size="100%" />
      {#if isButton}<span class="dealer">D</span>{/if}
    </div>
    <div class="name">{seat.nickname}</div>
    <div class="stack">
      {#if handRunning && seat.status === 'allIn'}Tapis{:else}{seat.stack}{/if}
    </div>
    {#if !seat.connected}<div class="badge">déconnecté</div>{/if}
  </div>
  {#if seat.cards || seat.hasCards}
    <!-- Recreated on each hand, so that the new cards are dealt again. -->
    {#key handNumber}
      <div class="cards">
        {#each [0, 1] as i (i)}
          <span class="slot" in:fromCenter|global={{ delay: i * 300 + position * 40 }}>
            <Card
              code={seat.cards?.[i] ?? null}
              size={isMe ? 'normal' : 'small'}
              highlight={highlightCards.includes(seat.cards?.[i])}
              dim={highlightCards.length > 0 && !highlightCards.includes(seat.cards?.[i])}
            />
          </span>
        {/each}
      </div>
    {/key}
  {/if}
  {#if seat.bet > 0}<div class="bet" style={betStyle} out:toCenter>{seat.bet}</div>{/if}
</div>

<style>
  .seat {
    position: relative;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 0.2rem;
    min-width: 4.5rem;
    font-size: 0.8rem;
    color: #fff;
  }
  .avatar {
    position: relative;
    display: grid;
    place-items: center;
    width: 2.8rem;
    height: 2.8rem;
    border-radius: 50%;
    background: #0d3b26;
    border: 3px solid #e8d8a8;
    overflow: visible;
  }
  .avatar :global(img),
  .avatar :global(.emoji) {
    border-radius: 50%;
  }
  .to-act .avatar {
    border-color: transparent;
    background:
      linear-gradient(#0d3b26, #0d3b26) padding-box,
      conic-gradient(#ffd23f calc(var(--progress) * 360deg), #555 0) border-box;
    box-shadow: 0 0 0.8rem #ffd23f;
  }
  .winner .avatar {
    animation: shine 1.2s ease-in-out 3;
  }
  @keyframes shine {
    50% {
      box-shadow: 0 0 1.4rem 0.4rem #ffd23f;
    }
  }
  .slot {
    display: inline-block;
  }
  .dealer {
    position: absolute;
    right: -0.5rem;
    bottom: -0.3rem;
    width: 1.1rem;
    height: 1.1rem;
    border-radius: 50%;
    background: #fff;
    color: #000;
    font-size: 0.7rem;
    font-weight: 700;
    display: grid;
    place-items: center;
  }
  .name {
    font-weight: 600;
    max-width: 5.5rem;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .me .name {
    color: #ffd23f;
  }
  .stack {
    opacity: 0.9;
  }
  .cards {
    display: flex;
    gap: 0.15rem;
  }
  .identity {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 0.1rem;
  }
  /* My own seat: cards beside me rather than below, to keep the bottom of the screen free. */
  .me {
    flex-direction: row;
    gap: 0.5rem;
  }
  .bet {
    position: absolute;
    transform: translate(-50%, -50%);
    white-space: nowrap;
    padding: 0 0.4rem;
    border-radius: 1rem;
    background: #ffd23f;
    color: #000;
    font-weight: 700;
  }
  .folded {
    opacity: 0.45;
  }
  .away {
    filter: grayscale(1);
  }
  .badge {
    font-size: 0.65rem;
    opacity: 0.8;
  }
</style>
