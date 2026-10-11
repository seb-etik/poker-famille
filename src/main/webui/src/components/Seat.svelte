<script>
  import Avatar from './Avatar.svelte';
  import Card from './Card.svelte';
  import { fromCenter, toCenter } from '../lib/transitions.js';

  /**
   * An opponent around the oval (my own place is MyPlace).
   * position: place on the oval, 0 at the bottom then clockwise; bets and the dealer chip go towards the centre.
   * shownHand: name of the hand shown at the showdown, under the cards.
   */
  let {
    seat,
    position = 0,
    isButton = false,
    blind = null,
    handRunning = false,
    toAct = false,
    remainingMs = null,
    actionSeconds = 30,
    highlightCards = [],
    handNumber = 0,
    winner = false,
    shownHand = null,
  } = $props();

  const progress = $derived(remainingMs == null ? 0 : Math.min(1, remainingMs / (actionSeconds * 1000)));
  const folded = $derived(seat.status === 'folded');
  const showdown = $derived(Boolean(seat.cards) && !handRunning);

  // Which side faces the centre of the table: right for the seats on the left, left for those on the right,
  // none for the seat at the top. The bet and the dealer chip go on that side, away from the centre's text.
  const angle = $derived(Math.PI / 2 + (position * Math.PI) / 4);
  const inward = $derived(Math.cos(angle) < -0.1 ? 1 : Math.cos(angle) > 0.1 ? -1 : 0);
  const betStyle = $derived(
    inward === 0 ? 'left: 50%; top: calc(100% + 12px)' : `left: calc(50% + ${inward * 50}px); top: 34px`,
  );
  const dealerStyle = $derived(
    inward === 0 ? 'left: calc(50% + 42px); top: 26px' : `left: calc(50% + ${inward * 44}px); top: 6px`,
  );
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
    {#if blind}<span class="blind {blind}">{blind === 'small' ? 'PB' : 'GB'}</span>{/if}
  </div>

  {#if seat.cards || seat.hasCards}
    <!-- Recreated on each hand, so that the new cards are dealt again. -->
    {#key handNumber}
      <div class="cards" class:outer-left={inward === 1} class:outer-right={inward === -1} class:shown={showdown}>
        {#each [0, 1] as i (i)}
          <span class="slot" in:fromCenter|global={{ delay: i * 300 + position * 40 }}>
            <Card
              code={seat.cards?.[i] ?? null}
              width={showdown ? 34 : 22}
              highlight={highlightCards.includes(seat.cards?.[i])}
              dim={highlightCards.length > 0 && !highlightCards.includes(seat.cards?.[i])}
            />
          </span>
        {/each}
      </div>
    {/key}
  {/if}

  <div class="name">{seat.nickname}</div>
  <div class="stack">
    {#if folded}Couché(e){:else if handRunning && seat.status === 'allIn'}Tapis{:else}{seat.stack}{/if}
  </div>
  {#if shownHand}<div class="hand-name">{shownHand}</div>{/if}

  {#if isButton}<span class="dealer" style={dealerStyle}>D</span>{/if}
  {#if seat.bet > 0}
    <span class="bet" style={betStyle} out:toCenter><span class="chip"></span>{seat.bet}</span>
  {/if}
</div>

<style>
  .seat {
    position: relative;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 2px;
    width: 76px;
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
  .blind {
    position: absolute;
    left: -8px;
    bottom: -4px;
    padding: 0 4px;
    border-radius: 8px;
    font-size: 10px;
    font-weight: 700;
    line-height: 16px;
  }
  .blind.small {
    background: #2d66b3;
  }
  .blind.big {
    background: #b8860b;
  }

  /* Backs: small, on the corner of the avatar. Shown at the showdown: over the avatar, readable. */
  .cards {
    position: absolute;
    top: -6px;
    left: calc(50% + 10px);
    display: flex;
  }
  /* Seats on the left keep their backs on their outer side too, away from the board. */
  .cards.outer-left {
    left: auto;
    right: calc(50% + 10px);
  }
  .slot + .slot {
    margin-left: -12px;
  }
  /* At the showdown, the shown cards take the place of the avatar; the name and the hand stay below. */
  .showing .avatar {
    visibility: hidden;
  }
  .cards.shown {
    top: 1px;
    left: 50%;
    right: auto;
    transform: translateX(-50%);
  }
  /* Side seats: the shown cards lean outwards, clear of the board. */
  .cards.shown.outer-left {
    left: auto;
    right: calc(50% - 22px);
    transform: none;
  }
  .cards.shown.outer-right {
    left: calc(50% - 22px);
    transform: none;
  }
  .cards.shown .slot + .slot {
    margin-left: 2px;
  }

  .name {
    max-width: 76px;
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
  .hand-name {
    font-size: 11px;
    color: #ffd23f;
    white-space: nowrap;
  }

  .dealer {
    position: absolute;
    transform: translate(-50%, -50%);
    width: 20px;
    height: 20px;
    border-radius: 50%;
    background: #fff;
    color: #000;
    font-size: 11px;
    font-weight: 700;
    display: grid;
    place-items: center;
    box-shadow: 0 1px 2px rgb(0 0 0 / 0.5);
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
    z-index: 1;
  }
  .chip {
    width: 14px;
    height: 14px;
    border-radius: 50%;
    background: #ffd23f;
    border: 2px dashed #b8860b;
    box-sizing: border-box;
  }

  .folded {
    opacity: 0.45;
  }
  .away {
    filter: grayscale(1);
  }
</style>
