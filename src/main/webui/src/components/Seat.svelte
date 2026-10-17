<script>
  import Avatar from './Avatar.svelte';
  import Card from './Card.svelte';
  import { fromCenter } from '../lib/transitions.js';

  /**
   * An opponent at the table (my own place is MyPlace). Its place and its bet are set by the table
   * (lib/layout.js); this component draws the avatar with its badges, its cards and its stack.
   * shownHand: short name of the hand shown at the showdown ("Brelan"), in place of the stack.
   */
  let {
    seat,
    isButton = false,
    blind = null,
    handRunning = false,
    toAct = false,
    remainingMs = null,
    actionSeconds = 30,
    highlightCards = [],
    handNumber = 0,
    dealDelay = 0,
    winner = false,
    shownHand = null,
  } = $props();

  const progress = $derived(remainingMs == null ? 0 : Math.min(1, remainingMs / (actionSeconds * 1000)));
  const folded = $derived(seat.status === 'folded');
  const showdown = $derived(Boolean(seat.cards) && !handRunning);
</script>

<div
  class="seat"
  class:folded
  class:away={!seat.connected}
  class:to-act={toAct}
  class:winner
  class:showing={showdown}
  style="--progress: {progress}"
>
  <div class="avatar">
    <Avatar id={seat.avatar} size="100%" />
    {#if blind}<span class="badge blind {blind}">{blind === 'small' ? 'PB' : 'GB'}</span>{/if}
    {#if isButton}<span class="badge dealer">D</span>{/if}
  </div>

  {#if seat.cards || seat.hasCards}
    <!-- Recreated on each hand, so that the new cards are dealt again. -->
    {#key handNumber}
      <div class="cards" class:shown={showdown}>
        {#each [0, 1] as i (i)}
          <span class="slot slot-{i}" in:fromCenter|global={{ delay: i * 300 + dealDelay }}>
            <Card
              code={seat.cards?.[i] ?? null}
              width={showdown ? 34 : 18}
              highlight={highlightCards.includes(seat.cards?.[i])}
              dim={highlightCards.length > 0 && !highlightCards.includes(seat.cards?.[i])}
            />
          </span>
        {/each}
      </div>
    {/key}
  {/if}

  <div class="name">{seat.nickname}</div>
  <!-- At the showdown the pill names the hand instead of the stack: no extra line, nothing hits the board. -->
  <div class="stack" class:hand-name={shownHand}>
    {#if shownHand}{shownHand}{:else if folded}Couché(e){:else if handRunning && seat.status === 'allIn'}Tapis{:else}{seat.stack}{/if}
  </div>
</div>

<style>
  .seat {
    position: relative;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 2px;
    width: 80px;
    color: #fff;
  }
  .avatar {
    position: relative;
    width: 52px;
    height: 52px;
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
    box-shadow: 0 0 12px #ffd23f;
  }
  .winner .avatar {
    animation: shine 1.2s ease-in-out 3;
  }
  @keyframes shine {
    50% {
      box-shadow: 0 0 22px 6px #ffd23f;
    }
  }

  /* PB / GB at the bottom left of the avatar, D at the bottom right: never on the felt. */
  .badge {
    position: absolute;
    bottom: -4px;
    min-width: 16px;
    padding: 0 3px;
    border-radius: 8px;
    font-size: 10px;
    font-weight: 700;
    line-height: 16px;
    text-align: center;
    box-sizing: border-box;
  }
  .blind {
    left: -8px;
  }
  .blind.small {
    background: #2d66b3;
  }
  .blind.big {
    background: #b8860b;
  }
  .dealer {
    right: -6px;
    background: #fff;
    color: #000;
  }

  /* Hidden cards: a small fan at the top right of the avatar. */
  .cards {
    position: absolute;
    top: -8px;
    left: calc(50% + 14px);
    display: flex;
  }
  .slot-0 {
    transform: rotate(-8deg);
  }
  .slot-1 {
    margin-left: -8px;
    transform: rotate(8deg);
  }
  /* At the showdown, the shown cards take the place of the avatar; the name and the hand stay below. */
  .showing .avatar {
    visibility: hidden;
  }
  .cards.shown {
    top: 1px;
    left: 50%;
    transform: translateX(-50%);
  }
  .cards.shown .slot {
    transform: none;
  }
  .cards.shown .slot-1 {
    margin-left: 2px;
  }

  .name {
    max-width: 80px;
    font-size: 14px;
    font-weight: 700;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .stack {
    padding: 1px 8px;
    border-radius: 10px;
    background: rgb(0 0 0 / 0.45);
    font-size: 12px;
  }
  .stack.hand-name {
    color: #ffd23f;
    font-size: 11px;
    white-space: nowrap;
  }

  .folded {
    opacity: 0.45;
  }
  .away {
    filter: grayscale(1);
  }
</style>
