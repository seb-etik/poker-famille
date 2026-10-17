<script>
  import { actionButtons } from '../lib/actions.js';
  import { raiseStep, raiseStops } from '../lib/raise.js';

  /**
   * allowed: `allowedActions`, or null when it is not my turn (every button greyed, nothing moves).
   * stack: my chips, shown on Tapis. bigBlind: of the current level, sets the step of the slider.
   */
  let { allowed, stack, bigBlind, onAct } = $props();

  const NONE = { types: [], callAmount: 0, minAmount: 0, maxAmount: 0 };
  const current = $derived(allowed ?? NONE);
  const canRaise = $derived(current.types.includes('bet') || current.types.includes('raise'));
  // The slider moves over round amounts (half the big blind), plus the exact minimum and stack.
  const stops = $derived(raiseStops(current.minAmount, current.maxAmount, raiseStep(bigBlind)));
  // Writable derived: back to the first stop (the minimum) on each new turn, and the slider can move it.
  let position = $derived.by(() => {
    stops;
    return 0;
  });
  const amount = $derived(stops[Math.min(position, stops.length - 1)]);
  const buttons = $derived(actionButtons(current, stack, amount));
</script>

<div class="bar">
  {#if canRaise && current.maxAmount > current.minAmount}
    <div class="slider">
      <span>Relance</span>
      <input
        type="range"
        min="0"
        max={stops.length - 1}
        step="1"
        bind:value={position}
        aria-label="Montant de la relance"
        aria-valuetext={String(amount)}
      />
      <strong>{amount}</strong>
    </div>
  {/if}
  <div class="buttons">
    {#each buttons as button (button.key)}
      <button
        class={button.key}
        disabled={!button.enabled}
        onclick={() => onAct(button.action, button.actionAmount ?? undefined)}
      >
        <span class="label">{button.label}</span>
        {#if button.amount !== null}<span class="amount">{button.amount}</span>{/if}
      </button>
    {/each}
  </div>
</div>

<style>
  .bar {
    display: flex;
    flex-direction: column;
    gap: 8px;
    padding: 8px 12px calc(10px + env(safe-area-inset-bottom));
    background: #10261b;
    border-top: 1px solid #2b5a41;
  }
  .slider {
    display: flex;
    align-items: center;
    gap: 12px;
    height: 44px;
    font-size: 14px;
  }
  .slider span {
    opacity: 0.8;
  }
  .slider strong {
    min-width: 3.5em;
    text-align: right;
    color: #ffd23f;
    font-size: 18px;
  }
  input[type='range'] {
    flex: 1;
    min-width: 0;
    margin: 0;
    padding: 0;
    border: none;
    background: none;
    accent-color: #ffd23f;
  }
  .buttons {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 8px;
  }
  .buttons button {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: 62px;
    padding: 4px;
    border-radius: 12px;
    line-height: 1.15;
  }
  .label {
    font-size: 16px;
    font-weight: 700;
  }
  .amount {
    font-size: 14px;
  }
  .fold {
    background: #7a2230;
  }
  .call {
    background: #2b5a41;
  }
  .raise {
    background: #b8860b;
    color: #fff;
  }
  .allIn {
    background: transparent;
    border: 2px solid #ffd23f;
    color: #ffd23f;
  }
  button:disabled {
    opacity: 0.35;
  }
</style>
