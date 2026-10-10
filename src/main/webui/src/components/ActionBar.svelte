<script>
  let { state, onAct } = $props();

  const allowed = $derived(state.allowedActions);
  const can = (type) => allowed.types.includes(type);
  const raiseType = $derived(can('bet') ? 'bet' : can('raise') ? 'raise' : null);
  // Writable derived: follows the minimum on each new turn, and the slider can move it.
  let amount = $derived(allowed.minAmount);
</script>

<div class="bar">
  {#if raiseType && allowed.maxAmount > allowed.minAmount}
    <input
      type="range"
      min={allowed.minAmount}
      max={allowed.maxAmount}
      step="1"
      bind:value={amount}
      aria-label="Montant"
    />
  {/if}
  <div class="buttons">
    {#if can('fold')}<button class="fold" onclick={() => onAct('fold')}>Se coucher</button>{/if}
    {#if can('check')}<button onclick={() => onAct('check')}>Check</button>{/if}
    {#if can('call')}<button onclick={() => onAct('call')}>Suivre {allowed.callAmount}</button>{/if}
    {#if raiseType}
      <button class="raise" onclick={() => onAct(raiseType, amount)}>
        {raiseType === 'bet' ? 'Miser' : 'Relancer à'} {amount}
      </button>
    {/if}
  </div>
</div>

<style>
  .bar {
    display: flex;
    flex-direction: column;
    gap: 0.6rem;
    padding: 0.6rem 0.75rem calc(0.6rem + env(safe-area-inset-bottom));
    background: #10261b;
    border-top: 1px solid #2b4d3b;
  }
  input[type='range'] {
    display: block;
    width: 100%;
    margin: 0;
    padding: 0;
    border: none;
    background: none;
    accent-color: #ffd23f;
  }
  .buttons {
    display: flex;
    gap: 0.5rem;
  }
  .buttons button {
    flex: 1;
    min-height: 3rem;
    padding: 0.4rem;
    font-size: 0.95rem;
  }
  .fold {
    background: #7a2230;
  }
  .raise {
    background: #b8860b;
  }
</style>
