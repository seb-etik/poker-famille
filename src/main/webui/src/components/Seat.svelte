<script>
  import Avatar from './Avatar.svelte';
  import Card from './Card.svelte';
  import { fromCenter, toCenter } from '../lib/transitions.js';

  /**
   * variant 'opponent': compact, around the oval, cards beside the avatar.
   * variant 'me': the strip at the bottom of the screen, with my cards in large.
   */
  let {
    seat,
    variant = 'opponent',
    isButton = false,
    blind = null,
    handRunning = false,
    toAct = false,
    remainingMs = null,
    actionSeconds = 30,
    highlightCards = [],
    handNumber = 0,
    position = 0,
    winner = false,
  } = $props();

  const isMe = $derived(variant === 'me');
  const progress = $derived(remainingMs == null ? 0 : Math.min(1, remainingMs / (actionSeconds * 1000)));

  // An opponent's bet sits between the player and the centre of the table, like chips pushed forward.
  const angle = $derived(Math.PI / 2 + (position * Math.PI) / 4);
  const betStyle = $derived(
    isMe ? '' : `left: calc(50% - ${Math.cos(angle) * 4}rem); top: calc(50% - ${Math.sin(angle) * 3.4}rem)`,
  );
</script>

<div
  class="seat {variant}"
  class:folded={seat.status === 'folded'}
  class:away={!seat.connected}
  class:to-act={toAct}
  class:winner
  style="--progress: {progress}"
>
  <div class="avatar">
    <Avatar id={seat.avatar} size="100%" />
    {#if isButton}<span class="dealer">D</span>{/if}
    {#if blind}
      <span class="blind {blind}" title={blind === 'small' ? 'Petite blinde' : 'Grosse blinde'}>
        {blind === 'small' ? 'PB' : 'GB'}
      </span>
    {/if}
  </div>

  <div class="identity">
    <div class="name">{seat.nickname}</div>
    <div class="stack">
      {#if handRunning && seat.status === 'allIn'}Tapis{:else}{seat.stack}{/if}
      {#if !seat.connected}<span class="badge">· absent</span>{/if}
    </div>
  </div>

  {#if seat.cards || seat.hasCards}
    <!-- Recreated on each hand, so that the new cards are dealt again. -->
    {#key handNumber}
      <div class="cards">
        {#each [0, 1] as i (i)}
          <span class="slot" in:fromCenter|global={{ delay: i * 300 + position * 40 }}>
            <Card
              code={seat.cards?.[i] ?? null}
              size={isMe ? 'large' : 'mini'}
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
    display: grid;
    color: #fff;
    font-size: 0.75rem;
    line-height: 1.15;
  }

  /* Around the oval: avatar and cards on one line, name and stack below. */
  .opponent {
    grid-template-columns: auto auto;
    grid-template-areas:
      'avatar cards'
      'identity identity';
    justify-items: center;
    align-items: center;
    column-gap: 0.15rem;
  }
  .opponent .avatar {
    width: 2.3rem;
    height: 2.3rem;
  }

  /* My strip: avatar, name and stack, then my cards in large. */
  .me {
    grid-template-columns: auto 1fr auto;
    grid-template-areas: 'avatar identity cards';
    align-items: center;
    column-gap: 0.6rem;
    font-size: 0.9rem;
  }
  .me .avatar {
    width: 3rem;
    height: 3rem;
  }
  .me .name {
    color: #ffd23f;
  }

  .avatar {
    grid-area: avatar;
    position: relative;
    border-radius: 50%;
    border: 3px solid #e8d8a8;
    background: #0d3b26;
    box-sizing: border-box;
  }
  .avatar :global(img),
  .avatar :global(.emoji) {
    width: 100%;
    height: 100%;
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
  .dealer {
    position: absolute;
    right: -0.35rem;
    bottom: -0.25rem;
    width: 1rem;
    height: 1rem;
    border-radius: 50%;
    background: #fff;
    color: #000;
    font-size: 0.65rem;
    font-weight: 700;
    display: grid;
    place-items: center;
  }

  .blind {
    position: absolute;
    left: -0.45rem;
    bottom: -0.25rem;
    padding: 0 0.2rem;
    border-radius: 0.5rem;
    font-size: 0.6rem;
    font-weight: 700;
    line-height: 1rem;
    color: #fff;
  }
  .blind.small {
    background: #2d66b3;
  }
  .blind.big {
    background: #b8860b;
  }

  .identity {
    grid-area: identity;
    text-align: center;
    min-width: 0;
  }
  .me .identity {
    text-align: left;
  }
  .name {
    font-weight: 600;
    max-width: 5.5rem;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .me .name {
    max-width: none;
  }
  .stack {
    opacity: 0.9;
  }
  .badge {
    opacity: 0.8;
  }

  .cards {
    grid-area: cards;
    display: flex;
  }
  .opponent .slot + .slot {
    margin-left: -0.6rem;
  }
  .me .cards {
    gap: 0.3rem;
  }
  .slot {
    display: inline-block;
  }

  .bet {
    position: absolute;
    transform: translate(-50%, -50%);
    padding: 0 0.4rem;
    border-radius: 1rem;
    background: #ffd23f;
    color: #000;
    font-weight: 700;
    white-space: nowrap;
    z-index: 1;
  }
  /* My bet: on the edge of the table, above my strip. */
  .me .bet {
    left: 50%;
    top: -0.9rem;
  }

  .folded {
    opacity: 0.45;
  }
  .away {
    filter: grayscale(1);
  }
</style>
