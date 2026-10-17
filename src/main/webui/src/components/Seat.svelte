<script>
  import Avatar from './Avatar.svelte';
  import Card from './Card.svelte';
  import { fromCenter } from '../lib/transitions.js';

  /**
   * An opponent at the table (my own place is MyPlace). Its place and its bet are set by the table
   * (lib/layout.js); this component draws the avatar with its badges, its cards and its stack.
   * At the showdown (docs/UI-TABLE-A.md, V6): handLabel names the hand shown; placement 'below' puts the cards
   * (showdownWidth px wide) under the seat with the label under them, 'avatar' puts them in place of the avatar
   * with the label in place of the stack. A winner gets the gold style: label, outline of both cards, halo.
   */
  let {
    seat,
    isButton = false,
    blind = null,
    handRunning = false,
    toAct = false,
    remainingMs = null,
    actionSeconds = 30,
    handNumber = 0,
    dealDelay = 0,
    winner = false,
    handLabel = null,
    placement = 'below',
    showdownWidth = 52,
    labelWidth = null,
    nudge = 0,
    labelAlign = 'center',
  } = $props();

  const progress = $derived(remainingMs == null ? 0 : Math.min(1, remainingMs / (actionSeconds * 1000)));
  const folded = $derived(seat.status === 'folded');
  const showdown = $derived(Boolean(seat.cards) && !handRunning);
  const mode = $derived(!showdown ? 'fan' : placement === 'below' ? 'below' : 'in-avatar');
  const cardHeight = $derived((showdownWidth * 243) / 167);
</script>

<div
  class="seat"
  class:folded
  class:away={!seat.connected}
  class:to-act={toAct}
  class:winner
  class:in-avatar={mode === 'in-avatar'}
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
      <div class="cards {mode}" style={mode === 'below' ? `--nudge: ${nudge}px` : ''}>
        {#each [0, 1] as i (i)}
          <span class="slot slot-{i}" in:fromCenter|global={{ delay: i * 300 + dealDelay }}>
            <Card code={seat.cards?.[i] ?? null} width={showdown ? showdownWidth : 18} highlight={showdown && winner} />
          </span>
        {/each}
      </div>
    {/key}
  {/if}

  <div class="name">{seat.nickname}</div>
  {#if handLabel && mode === 'in-avatar'}
    <!-- In place of the stack: no extra line, nothing reaches the board. -->
    <div class="hand-label align-{labelAlign}">{handLabel}</div>
  {:else}
    <div class="stack">
      {#if folded}Couché(e){:else if handRunning && seat.status === 'allIn'}Tapis{:else}{seat.stack}{/if}
    </div>
  {/if}
  {#if handLabel && mode === 'below'}
    <div
      class="hand-label below"
      class:small={showdownWidth < 52}
      title={handLabel}
      style="top: calc(100% + {cardHeight + 10}px); max-width: {labelWidth}px; --nudge: {nudge}px"
    >
      {handLabel}
    </div>
  {/if}
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
  /* The winner of the hand: gold halo and gold nickname. */
  .winner .avatar {
    border-color: #ffd23f;
    box-shadow: 0 0 14px 3px rgb(255 210 63 / 0.7);
    animation: shine 1.2s ease-in-out 3;
  }
  .winner .name {
    color: #ffd23f;
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
  /* Shown cards under the seat, towards the centre (V6), overlapping by 14 px. */
  /* nudge: NO and NE lean outwards a little, to leave room for the label of N. */
  .cards.below {
    top: calc(100% + 4px);
    left: 50%;
    transform: translateX(calc(-50% + var(--nudge, 0px)));
  }
  /* Or in place of the avatar when the table is crowded. */
  .in-avatar .avatar {
    visibility: hidden;
  }
  .cards.in-avatar {
    top: 26px; /* the centre of the avatar */
    left: 50%;
    transform: translate(-50%, -50%);
  }
  .cards.below .slot,
  .cards.in-avatar .slot {
    transform: none;
  }
  .cards.below .slot-1,
  .cards.in-avatar .slot-1 {
    margin-left: -14px;
  }
  .cards.below :global(img),
  .cards.in-avatar :global(img) {
    box-shadow: -3px 0 6px rgb(0 0 0 / 0.35);
  }

  .hand-label {
    padding: 1px 8px;
    border-radius: 10px;
    background: rgb(0 0 0 / 0.7);
    color: #fff;
    font-size: 13px;
    font-weight: 700;
    text-align: center;
  }
  .hand-label.below {
    padding: 1px 6px;
    position: absolute;
    left: 50%;
    transform: translateX(calc(-50% + var(--nudge, 0px)));
    width: max-content;
    box-sizing: border-box;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  .hand-label.below.small {
    font-size: 12px;
  }
  /* One line: a second one would reach the side seats below (O1, E1) on a full table. */
  .in-avatar .hand-label {
    font-size: 12px;
    white-space: nowrap;
  }
  /* A side seat near the edge of the screen: its label grows towards the centre of the table. */
  .in-avatar .hand-label.align-start {
    align-self: flex-start;
  }
  .in-avatar .hand-label.align-end {
    align-self: flex-end;
  }
  .winner .hand-label {
    background: #ffd23f;
    color: #1a1a1a;
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
