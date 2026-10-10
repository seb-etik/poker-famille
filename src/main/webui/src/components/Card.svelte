<script>
  import { cardLabel } from '../lib/table.js';

  /** code: "AS"; null shows the back of the card. */
  let { code = null, size = 'normal' } = $props();

  const label = $derived(code ? cardLabel(code) : null);
</script>

{#if label}
  <span class="card {size}" class:red={label.red}>
    <span class="rank">{label.rank}</span><span class="suit">{label.suit}</span>
  </span>
{:else}
  <span class="card back {size}"></span>
{/if}

<style>
  .card {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 2.4rem;
    height: 3.4rem;
    border-radius: 0.35rem;
    background: #fff;
    color: #1a1a1a;
    font-weight: 700;
    font-size: 1.05rem;
    box-shadow: 0 1px 3px rgb(0 0 0 / 0.4);
    line-height: 1;
  }
  .card.small {
    width: 1.7rem;
    height: 2.4rem;
    font-size: 0.8rem;
  }
  .red {
    color: #c0182a;
  }
  .back {
    background: repeating-linear-gradient(45deg, #1d4f91 0 4px, #2d66b3 4px 8px);
    border: 2px solid #fff;
  }
</style>
