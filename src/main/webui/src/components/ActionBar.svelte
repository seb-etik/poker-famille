<script>
  import { raiseShortcuts } from '../lib/table.js';

  let { state, onAct } = $props();

  const allowed = $derived(state.allowedActions);
  const can = (type) => allowed.types.includes(type);
  const raiseType = $derived(can('bet') ? 'bet' : can('raise') ? 'raise' : null);
  const shortcuts = $derived(raiseType ? raiseShortcuts(state) : null);
  // Writable derived: follows the minimum on each new turn, and the slider can move it.
  let amount = $derived(allowed.minAmount);

  function raiseLabel() {
    if (amount === allowed.maxAmount) {
      return `Tapis (${amount})`;
    }
    return raiseType === 'bet' ? `Miser ${amount}` : `Relancer à ${amount}`;
  }
</script>

<div class="bar">
  {#if raiseType}
    <div class="raise">
      <input type="range" min={allowed.minAmount} max={allowed.maxAmount} step="1" bind:value={amount} />
      <div class="shortcuts">
        <button onclick={() => (amount = shortcuts.halfPot)}>½ pot</button>
        <button onclick={() => (amount = shortcuts.pot)}>Pot</button>
        <button onclick={() => (amount = shortcuts.allIn)}>Tapis</button>
      </div>
    </div>
  {/if}
  <div class="main">
    {#if can('fold')}<button class="fold" onclick={() => onAct('fold')}>Se coucher</button>{/if}
    {#if can('check')}<button onclick={() => onAct('check')}>Checker</button>{/if}
    {#if can('call')}<button onclick={() => onAct('call')}>Suivre {allowed.callAmount}</button>{/if}
    {#if raiseType}
      <button class="raise-button" onclick={() => onAct(raiseType, amount)}>{raiseLabel()}</button>
    {/if}
  </div>
</div>

<style>
  .bar {
    position: sticky;
    bottom: 0;
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
    padding: 0.75rem 1rem calc(0.75rem + env(safe-area-inset-bottom));
    background: #10261b;
    border-top: 1px solid #2b4d3b;
  }
  .main {
    display: flex;
    gap: 0.5rem;
  }
  .main button {
    flex: 1;
    min-height: 3rem;
    font-size: 1rem;
  }
  .raise input {
    width: 100%;
  }
  .shortcuts {
    display: flex;
    gap: 0.5rem;
  }
  .shortcuts button {
    flex: 1;
  }
  .fold {
    background: #7a2230;
  }
  .raise-button {
    background: #b8860b;
  }
</style>
