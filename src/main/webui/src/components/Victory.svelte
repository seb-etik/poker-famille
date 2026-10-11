<script>
  import { onDestroy } from 'svelte';
  import Avatar from './Avatar.svelte';

  /** seat: the winner's seat view (nickname, avatar, stack). */
  /** onRestart: only for the host, who can start a new game from here. */
  let { seat, onClose, onRestart = null } = $props();

  const COLORS = ['#ffd23f', '#d62839', '#2d66b3', '#1f7a4a', '#ffffff', '#ff8c42'];
  // Drawn once: each piece falls from a random spot, with its own colour, delay and speed.
  const confetti = Array.from({ length: 60 }, (_, i) => ({
    id: i,
    left: Math.random() * 100,
    delay: Math.random() * 1.5,
    duration: 2.5 + Math.random() * 2,
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
  <div class="card">
    <div class="trophy">🏆</div>
    <div class="avatar"><Avatar id={seat.avatar} size="100%" /></div>
    <h2>{seat.nickname} remporte la partie !</h2>
    <p>{seat.stack} jetons</p>
    {#if onRestart}
      <button class="restart" onclick={(event) => { event.stopPropagation(); onRestart(); }}>Rejouer</button>
    {:else}
      <p>En attente de l'hôte pour rejouer…</p>
    {/if}
    <p class="hint">Touchez ailleurs pour fermer</p>
  </div>
</div>

<style>
  .victory {
    position: fixed;
    inset: 0;
    z-index: 10;
    display: grid;
    place-items: center;
    overflow: hidden;
    background: rgb(0 0 0 / 0.7);
    animation: fade-in 0.4s ease-out;
  }
  .card {
    position: relative;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 0.4rem;
    padding: 1.5rem 2rem;
    border-radius: 1rem;
    background: radial-gradient(#1f7a4a, #0f4a2c);
    border: 3px solid #ffd23f;
    text-align: center;
    animation: pop 0.6s cubic-bezier(0.2, 1.6, 0.4, 1);
  }
  .trophy {
    font-size: 3rem;
    line-height: 1;
  }
  .avatar {
    width: 5rem;
    height: 5rem;
    border-radius: 50%;
    border: 3px solid #ffd23f;
    box-shadow: 0 0 1.5rem #ffd23f;
    overflow: hidden;
  }
  .avatar :global(.emoji) {
    font-size: 3rem;
  }
  h2 {
    margin: 0.3rem 0 0;
    color: #ffd23f;
  }
  p {
    margin: 0;
  }
  .restart {
    margin-top: 0.4rem;
    min-width: 10rem;
    min-height: 3rem;
    font-size: 1.1rem;
    font-weight: 700;
    background: #b8860b;
  }
  .hint {
    font-size: 0.75rem;
    opacity: 0.7;
  }
  .piece {
    position: absolute;
    top: -1rem;
    width: 0.5rem;
    height: 0.9rem;
    border-radius: 0.1rem;
    animation-name: fall;
    animation-timing-function: linear;
    animation-iteration-count: infinite;
  }
  @keyframes fall {
    from {
      transform: translateY(0) rotate(var(--tilt));
    }
    to {
      transform: translateY(110vh) rotate(calc(var(--tilt) + 720deg));
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
