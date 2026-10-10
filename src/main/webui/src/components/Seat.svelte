<script>
  import Card from './Card.svelte';
  import { avatarEmoji } from '../lib/profile.js';

  let { seat, isMe = false, isButton = false, handRunning = false, toAct = false, remainingMs = null, actionSeconds = 30 } = $props();

  const progress = $derived(remainingMs == null ? 0 : Math.min(1, remainingMs / (actionSeconds * 1000)));
</script>

<div
  class="seat"
  class:me={isMe}
  class:folded={seat.status === 'folded'}
  class:away={!seat.connected}
  class:to-act={toAct}
  style="--progress: {progress}"
>
  <div class="avatar">
    {avatarEmoji(seat.avatar)}
    {#if isButton}<span class="dealer">D</span>{/if}
  </div>
  <div class="name">{seat.nickname}</div>
  <div class="stack">
    {#if handRunning && seat.status === 'allIn'}Tapis{:else}{seat.stack}{/if}
  </div>
  {#if seat.cards}
    <div class="cards">
      {#each seat.cards as code (code)}<Card {code} size="small" />{/each}
    </div>
  {:else if seat.hasCards}
    <div class="cards"><Card size="small" /><Card size="small" /></div>
  {/if}
  {#if seat.bet > 0}<div class="bet">{seat.bet}</div>{/if}
  {#if !seat.connected}<div class="badge">déconnecté</div>{/if}
</div>

<style>
  .seat {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 0.1rem;
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
    font-size: 1.6rem;
  }
  .to-act .avatar {
    border-color: transparent;
    background:
      linear-gradient(#0d3b26, #0d3b26) padding-box,
      conic-gradient(#ffd23f calc(var(--progress) * 360deg), #555 0) border-box;
    box-shadow: 0 0 0.8rem #ffd23f;
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
  .bet {
    margin-top: 0.1rem;
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
