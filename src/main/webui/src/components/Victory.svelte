<script>
  import { onDestroy } from 'svelte';
  import Avatar from './Avatar.svelte';
  import { findMe, rankLabel, victoryPhrase } from '../lib/leaderboard.js';

  /**
   * seat: the winner's seat view; others: the other seated players.
   * event: the gameWon event, with the winner's standing of the month when the game counted.
   * onRestart: only for the host, who can start a new game from here.
   */
  let { seat, others = [], event = null, onClose, onRestart = null, onLeaderboard } = $props();

  const phrase = $derived(event ? victoryPhrase(event, seat.nickname) : null);
  const top = $derived(event?.top ?? []);
  const winnerInTop = $derived(findMe(top, seat.nickname));

  const COLORS = ['#ffd23f', '#d62839', '#2d66b3', '#1f7a4a', '#ffffff'];
  // Drawn once and kept discreet: few small pieces, falling slowly.
  const confetti = Array.from({ length: 30 }, (_, i) => ({
    id: i,
    left: Math.random() * 100,
    delay: Math.random() * 2,
    duration: 4 + Math.random() * 3,
    color: COLORS[i % COLORS.length],
    tilt: Math.round(Math.random() * 360),
  }));

  const autoClose = setTimeout(() => onClose(), 8000);
  onDestroy(() => clearTimeout(autoClose));
</script>

<div class="victory" role="dialog" aria-label="Fin de partie" tabindex="-1" onclick={onClose} onkeydown={onClose}>
  {#each confetti as piece (piece.id)}
    <span
      class="piece"
      style="left: {piece.left}%; background: {piece.color}; animation-delay: {piece.delay}s;
        animation-duration: {piece.duration}s; --tilt: {piece.tilt}deg"
    ></span>
  {/each}

  <!-- Taps inside the card do not close it. -->
  <div class="card" role="presentation" onclick={(event) => event.stopPropagation()}>
    <div class="trophy">🏆</div>
    <div class="avatar"><Avatar id={seat.avatar} size="100%" /></div>
    <h2>{seat.nickname} remporte la partie</h2>
    <p class="total">{seat.stack} jetons</p>

    {#if phrase}<p class="phrase">{phrase}</p>{/if}

    {#if top.length > 0}
      <ol class="top">
        {#each top as entry (entry.nickname)}
          <li class:winner={entry === winnerInTop}>
            <span class="rank">{rankLabel(entry.rank)}</span>
            <span class="other"><Avatar id={entry.avatar} size="100%" /></span>
            <span class="who">{entry.nickname}</span>
            <span class="wins">{entry.wins}{#if entry === winnerInTop}<strong class="plus-one">+1</strong>{/if}</span>
          </li>
        {/each}
      </ol>
    {:else if others.length > 0}
      <div class="others">
        {#each others as other (other.seat)}
          <span class="other" title={other.nickname}><Avatar id={other.avatar} size="100%" /></span>
        {/each}
      </div>
    {/if}

    {#if onRestart}
      <button class="restart" onclick={onRestart}>
        <span>Rejouer</span>
        <small>Mêmes joueurs, tapis et blindes remis à zéro</small>
      </button>
    {:else}
      <p class="wait">En attente de l'hôte pour rejouer…</p>
    {/if}
    <button class="home" onclick={onLeaderboard}>Voir tout le classement</button>
  </div>
</div>

<style>
  .victory {
    position: fixed;
    inset: 0;
    z-index: 10;
    display: grid;
    place-items: center;
    padding: 16px;
    overflow: hidden;
    background: rgb(0 0 0 / 0.7);
    animation: fade-in 0.4s ease-out;
  }
  .card {
    position: relative;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;
    width: min(100%, 340px);
    padding: 24px 20px 16px;
    border-radius: 20px;
    background: #10261b;
    border: 2px solid #ffd23f;
    box-shadow: 0 0 40px rgb(255 210 63 / 0.25);
    text-align: center;
    box-sizing: border-box;
    animation: pop 0.6s cubic-bezier(0.2, 1.6, 0.4, 1);
  }
  .trophy {
    font-size: 40px;
    line-height: 1;
  }
  .avatar {
    width: 96px;
    height: 96px;
    border-radius: 50%;
    border: 3px solid #ffd23f;
    box-shadow: 0 0 24px #ffd23f;
    overflow: hidden;
    box-sizing: border-box;
  }
  .avatar :global(.emoji) {
    font-size: 56px;
  }
  h2 {
    margin: 4px 0 0;
    font-size: 22px;
    color: #ffd23f;
  }
  .total {
    margin: 0;
    font-size: 16px;
  }
  .others {
    display: flex;
    gap: 6px;
    margin: 4px 0;
  }
  .other {
    display: grid;
    place-items: center;
    width: 32px;
    height: 32px;
    border-radius: 50%;
    background: #0d3b26;
    overflow: hidden;
    opacity: 0.75;
  }
  .other :global(.emoji) {
    font-size: 20px;
  }
  .phrase {
    margin: 0;
    font-size: 14px;
    opacity: 0.9;
  }
  .top {
    list-style: none;
    width: 100%;
    margin: 4px 0;
    padding: 0;
  }
  .top li {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 4px 0;
    text-align: left;
  }
  .top li.winner {
    color: #ffd23f;
    font-weight: 700;
  }
  .plus-one {
    margin-left: 6px;
  }
  .top .rank {
    width: 2.2em;
  }
  .top .who {
    flex: 1;
  }
  .restart {
    display: flex;
    flex-direction: column;
    align-items: center;
    width: 100%;
    margin-top: 8px;
    padding: 10px;
    min-height: 56px;
    border-radius: 14px;
    background: #b8860b;
    color: #fff;
  }
  .restart span {
    font-size: 18px;
    font-weight: 700;
  }
  .restart small {
    font-size: 12px;
    opacity: 0.9;
  }
  .wait {
    margin: 8px 0 0;
    opacity: 0.85;
  }
  .home {
    min-height: 44px;
    background: none;
    text-decoration: underline;
    opacity: 0.8;
  }
  .piece {
    position: absolute;
    top: -1rem;
    width: 6px;
    height: 10px;
    border-radius: 1px;
    opacity: 0.7;
    animation-name: fall;
    animation-timing-function: linear;
    animation-iteration-count: infinite;
  }
  @keyframes fall {
    from {
      transform: translateY(0) rotate(var(--tilt));
    }
    to {
      transform: translateY(110vh) rotate(calc(var(--tilt) + 540deg));
    }
  }
  @keyframes pop {
    from {
      transform: scale(0.3);
      opacity: 0;
    }
  }
  @keyframes fade-in {
    from {
      opacity: 0;
    }
  }
  @media (prefers-reduced-motion: reduce) {
    .piece {
      display: none;
    }
    .card,
    .victory {
      animation: none;
    }
  }
</style>
