<script>
  import Avatar from './Avatar.svelte';
  import Card from './Card.svelte';
  import { fromCenter, toCenter } from '../lib/transitions.js';

  /**
   * My place, in the strip under the table: avatar with the timer ring, my two cards in large.
   * turnText: "À toi · 20 pour suivre · 28 s" when it is my turn.
   * won: chips I won in the hand just finished, or 0. onLeave: "Se lever", shown while waiting.
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
    turnText = null,
    won = 0,
    onLeave = null,
  } = $props();

  const progress = $derived(remainingMs == null ? 0 : Math.min(1, remainingMs / (actionSeconds * 1000)));
</script>

<div class="place" class:to-act={toAct} class:won={won > 0} class:folded={seat.status === 'folded'} style="--progress: {progress}">
  <div class="avatar">
    <Avatar id={seat.avatar} size="100%" />
    {#if blind}<span class="badge blind {blind}">{blind === 'small' ? 'PB' : 'GB'}</span>{/if}
    {#if isButton}<span class="badge dealer">D</span>{/if}
  </div>

  <div class="identity">
    <div class="name">{seat.nickname}</div>
    <div class="stack">
      {#if handRunning && seat.status === 'allIn'}Tapis{:else}{seat.stack} jetons{/if}
      {#if won > 0}<strong class="gain">+ {won}</strong>{/if}
    </div>
    {#if turnText}<div class="turn">{turnText}</div>{/if}
    {#if onLeave}<button class="leave" onclick={onLeave}>Se lever</button>{/if}
  </div>

  {#if seat.cards || seat.hasCards}
    {#key handNumber}
      <div class="cards">
        {#each [0, 1] as i (i)}
          <span class="slot slot-{i}" in:fromCenter|global={{ delay: i * 300 }}>
            <Card
              code={seat.cards?.[i] ?? null}
              width={70}
              highlight={highlightCards.includes(seat.cards?.[i])}
              dim={highlightCards.length > 0 && !highlightCards.includes(seat.cards?.[i])}
            />
          </span>
        {/each}
      </div>
    {/key}
  {/if}

  {#if seat.bet > 0}
    <span class="bet" out:toCenter><span class="chip"></span>{seat.bet}</span>
  {/if}
</div>

<style>
  .place {
    position: relative;
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 10px 16px;
    border-top: 1px solid #2b5a41;
    background: #10261b;
  }
  .won {
    background: linear-gradient(90deg, rgb(255 210 63 / 0.18), #10261b 70%);
    border-top-color: #ffd23f;
  }
  .avatar {
    position: relative;
    flex: none;
    width: 54px;
    height: 54px;
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
  /* The timer: a gold ring that empties around my avatar. */
  .to-act .avatar {
    border-color: transparent;
    background:
      linear-gradient(#0d3b26, #0d3b26) padding-box,
      conic-gradient(#ffd23f calc(var(--progress) * 360deg), #555 0) border-box;
    box-shadow: 0 0 12px #ffd23f;
  }
  .badge {
    position: absolute;
    bottom: -4px;
    min-width: 18px;
    padding: 0 4px;
    border-radius: 9px;
    font-size: 10px;
    font-weight: 700;
    line-height: 18px;
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

  .identity {
    flex: 1;
    min-width: 0;
  }
  .name {
    color: #ffd23f;
    font-size: 16px;
    font-weight: 700;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .stack {
    font-size: 14px;
  }
  .gain {
    margin-left: 6px;
    color: #ffd23f;
  }
  .turn {
    font-size: 14px;
    font-weight: 600;
    color: #ffd23f;
  }
  .leave {
    margin-top: 4px;
    padding: 6px 12px;
    min-height: 32px;
    font-size: 13px;
    background: none;
    border: 1px solid #2b5a41;
  }

  .cards {
    display: flex;
    gap: 6px;
  }
  .slot-0 :global(.card) {
    transform: rotate(-4deg);
  }
  .slot-1 :global(.card) {
    transform: rotate(4deg);
  }
  .slot :global(.card.highlight) {
    transform: translateY(-8px);
  }

  .bet {
    position: absolute;
    left: 50%;
    top: -18px;
    transform: translate(-50%, -50%);
    display: flex;
    align-items: center;
    gap: 3px;
    font-size: 13px;
    font-weight: 700;
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
  .folded .cards {
    opacity: 0.45;
  }
</style>
