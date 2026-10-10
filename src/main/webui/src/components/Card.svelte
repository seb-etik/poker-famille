<script>
  import { getContext } from 'svelte';
  import { backUrl, faceUrl } from '../lib/themes.js';

  /**
   * code: "AS", or null for a card seen from the back. Going from null to a code flips the card.
   * highlight / dim: to show the winning hand.
   */
  let { code = null, size = 'normal', highlight = false, dim = false } = $props();

  const theme = getContext('theme');
  // Keeps the face once known, so that the flip shows it while turning.
  let shownCode = $state(null);
  $effect.pre(() => {
    if (code) {
      shownCode = code;
    }
  });
</script>

<span class="card {size}" class:face-down={!code} class:highlight class:dim>
  <span class="inner">
    {#if shownCode}
      <img class="face" src={faceUrl(theme(), shownCode)} alt={shownCode} draggable="false" />
    {/if}
    <img class="back" src={backUrl(theme())} alt="" draggable="false" />
  </span>
</span>

<style>
  .card {
    display: inline-block;
    width: 2.7rem;
    aspect-ratio: 167 / 243;
    perspective: 30rem;
    flex: none;
  }
  .card.mini {
    width: 1.55rem;
  }
  .card.large {
    width: 3.6rem;
  }
  .inner {
    position: relative;
    display: block;
    width: 100%;
    height: 100%;
    transform-style: preserve-3d;
    transition: transform 0.5s ease;
  }
  .face-down .inner {
    transform: rotateY(180deg);
  }
  img {
    position: absolute;
    inset: 0;
    width: 100%;
    height: 100%;
    backface-visibility: hidden;
    border-radius: 6%;
    background: #fff;
    box-shadow: 0 1px 3px rgb(0 0 0 / 0.45);
    user-select: none;
  }
  .back {
    transform: rotateY(180deg);
  }
  .highlight img {
    box-shadow: 0 0 0 2px #ffd23f, 0 0 0.8rem #ffd23f;
  }
  .highlight {
    transform: translateY(-0.2rem);
    transition: transform 0.3s ease;
  }
  .dim {
    opacity: 0.45;
  }
</style>
