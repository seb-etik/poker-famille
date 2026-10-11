<script>
  import { backUrl, faceUrl } from '../lib/themes.js';

  /**
   * code: "AS", or null for a card seen from the back. Going from null to a code flips the card.
   * width: in pixels, or any CSS length (the height follows the card's ratio).
   * highlight / dim: to show the winning hand.
   */
  let { code = null, width = 46, highlight = false, dim = false } = $props();

  // Keeps the face once known, so that the flip shows it while turning.
  let shownCode = $state(null);
  $effect.pre(() => {
    if (code) {
      shownCode = code;
    }
  });
</script>

<span class="card" class:face-down={!code} class:highlight class:dim style="width: {typeof width === 'number' ? `${width}px` : width}">
  <span class="inner">
    {#if shownCode}
      <img class="face" src={faceUrl(shownCode)} alt={shownCode} draggable="false" />
    {/if}
    <img class="back" src={backUrl()} alt="" draggable="false" />
  </span>
</span>

<style>
  .card {
    display: inline-block;
    aspect-ratio: 167 / 243;
    perspective: 30rem;
    flex: none;
    transition:
      transform 0.3s ease,
      opacity 0.3s ease;
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
  /* The cards of the winning hand rise with a gold outline; the others fade. */
  .highlight {
    transform: translateY(-8px);
  }
  .highlight img {
    box-shadow:
      0 0 0 2px #ffd23f,
      0 0 0.8rem #ffd23f;
  }
  .dim {
    opacity: 0.45;
  }
</style>
